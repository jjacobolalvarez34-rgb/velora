package app.velora.launcher.core.data.apps

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.LauncherActivityInfo
import android.content.pm.LauncherApps
import android.os.Handler
import android.os.Looper
import android.os.Process
import android.os.UserHandle
import android.os.UserManager
import androidx.core.content.ContextCompat
import app.velora.launcher.core.data.hidden.PrivateProfiles
import app.velora.launcher.core.data.log.Logger
import app.velora.launcher.core.model.AppEntry
import app.velora.launcher.core.model.AppKey
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn

/** Todas las apps abribles de todos los perfiles, ordenadas (ARQUITECTURA §4.1). */
interface AppsRepository {
    /**
     * Al arrancar, la lista de la caché (milisegundos); luego la del sistema, que se
     * actualiza sola con instalaciones y cambios.
     */
    val apps: StateFlow<List<AppEntry>>

    /** Vuelve a leer del sistema por si se perdieron eventos con el proceso muerto (B-008). */
    fun refresh()
}

/**
 * Lee las apps con `LauncherApps` (nunca `QUERY_ALL_PACKAGES`, SEGURIDAD §2).
 * Guarda los `LauncherActivityInfo` para que íconos y lanzamiento no vuelvan a consultar al sistema.
 */
class SystemAppsRepository(
    private val context: Context,
    scope: CoroutineScope,
    private val cache: AppsCache,
    private val onPackageChanged: (String) -> Unit = {},
    /** Tras cada lectura del sistema: paquete → `sourceDir` (cambia en cada actualización). */
    private val onSystemLoad: (Map<String, String>) -> Unit = {},
) : AppsRepository {

    private val launcherApps = context.getSystemService(LauncherApps::class.java)
    private val userManager = context.getSystemService(UserManager::class.java)
    private val infos = ConcurrentHashMap<String, LauncherActivityInfo>()
    private val refreshRequests = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    /** true tras la primera lectura del sistema: desde ahí hay `LauncherActivityInfo` para los íconos. */
    private val systemLoaded = MutableStateFlow(false)

    override val apps: StateFlow<List<AppEntry>> = flow {
        val cached = runCatching { cache.load(Locale.getDefault()) }
            .onFailure { Logger.w(TAG, it) { "Caché de apps ilegible" } }
            .getOrDefault(emptyList())
        if (cached.isNotEmpty()) emit(cached)
        var persisted = cached
        emitAll(
            merge(systemChanges(), refreshRequests)
                .onStart { emit(Unit) }
                .conflate()
                .map { loadAll() }
                .onEach { fresh ->
                    systemLoaded.value = true
                    onSystemLoad(currentVersions())
                    if (fresh != persisted) {
                        // El espacio privado nunca se guarda en disco: bloqueado no debe dejar rastro (B-016).
                        runCatching { cache.save(fresh.filterNot { it.isPrivate }) }
                            .onFailure { Logger.w(TAG, it) { "No se guardó la caché" } }
                        persisted = fresh
                    }
                },
        )
    }.flowOn(Dispatchers.IO)
        .distinctUntilChanged()
        .stateIn(scope, SharingStarted.Eagerly, emptyList())

    override fun refresh() {
        refreshRequests.tryEmit(Unit)
    }

    /**
     * El `LauncherActivityInfo` de la última carga, o null si la app ya no está. Si la lista
     * aún viene de la caché, espera a la primera lectura del sistema.
     */
    suspend fun infoFor(key: AppKey): LauncherActivityInfo? {
        systemLoaded.first { it }
        return infos[key.stableId]
    }

    private fun currentVersions(): Map<String, String> =
        infos.values.associate { it.componentName.packageName to it.applicationInfo.sourceDir.orEmpty() }

    fun userFor(key: AppKey): UserHandle? = userManager.getUserForSerialNumber(key.userSerial)

    private fun loadAll(): List<AppEntry> {
        val locale = Locale.getDefault()
        val me = Process.myUserHandle()
        val loaded = launcherApps.profiles.flatMap { user ->
            val serial = userManager.getSerialNumberForUser(user)
            val isPrivate = PrivateProfiles.isPrivate(launcherApps, user)
            // Espacio privado bloqueado: sus apps no existen para Velora, ni en la búsqueda (B-016, ADR-033).
            if (isPrivate && PrivateProfiles.isLocked(userManager, user)) return@flatMap emptyList()
            runCatching { launcherApps.getActivityList(null, user) }
                .onFailure { Logger.w(TAG, it) { "No se pudo leer el perfil $serial" } }
                .getOrDefault(emptyList())
                .filter { it.componentName.packageName != context.packageName }
                .map { info -> info.toEntry(serial, isWork = user != me && !isPrivate, isPrivate, locale) to info }
        }
        infos.clear()
        loaded.forEach { (entry, info) -> infos[entry.key.stableId] = info }
        return AppSorting.sorted(loaded.map { it.first }, locale)
    }

    private fun LauncherActivityInfo.toEntry(
        serial: Long,
        isWork: Boolean,
        isPrivate: Boolean,
        locale: Locale,
    ): AppEntry {
        val label = label?.toString()?.trim().orEmpty().ifEmpty { componentName.packageName }
        val isSystem = applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM != 0
        return AppEntry(
            key = AppKey(componentName.packageName, componentName.className, serial),
            label = label,
            section = AppSorting.sectionOf(label, locale),
            searchKey = AppSorting.searchKeyOf(label, locale),
            isWorkProfile = isWork,
            canUninstall = !isSystem,
            installedAtMs = firstInstallTime,
            isPrivate = isPrivate,
        )
    }

    /** Instalaciones, desinstalaciones, actualizaciones, apps suspendidas y cambio de idioma. */
    private fun systemChanges(): Flow<Unit> = callbackFlow {
        val callback = object : LauncherApps.Callback() {
            override fun onPackageAdded(packageName: String, user: UserHandle) = changed(packageName)
            override fun onPackageRemoved(packageName: String, user: UserHandle) = changed(packageName)
            override fun onPackageChanged(packageName: String, user: UserHandle) = changed(packageName)
            override fun onPackagesAvailable(names: Array<String>, user: UserHandle, replacing: Boolean) =
                names.forEach(::changed)
            override fun onPackagesUnavailable(names: Array<String>, user: UserHandle, replacing: Boolean) =
                names.forEach(::changed)
            override fun onPackagesSuspended(names: Array<String>, user: UserHandle) = names.forEach(::changed)
            override fun onPackagesUnsuspended(names: Array<String>, user: UserHandle) = names.forEach(::changed)

            private fun changed(packageName: String) {
                onPackageChanged(packageName)
                trySend(Unit)
            }
        }
        val localeReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                trySend(Unit)
            }
        }
        launcherApps.registerCallback(callback, Handler(Looper.getMainLooper()))
        ContextCompat.registerReceiver(
            context,
            localeReceiver,
            // Idioma y perfiles que se bloquean o desbloquean (trabajo en pausa, espacio privado).
            PrivateProfiles.changesFilter().apply { addAction(Intent.ACTION_LOCALE_CHANGED) },
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
        awaitClose {
            launcherApps.unregisterCallback(callback)
            context.unregisterReceiver(localeReceiver)
        }
    }.onEach { Logger.d(TAG) { "Cambio en las apps instaladas" } }

    private companion object {
        const val TAG = "AppsRepository"
    }
}
