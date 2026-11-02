package app.velora.launcher.core.data.hidden

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.LauncherApps
import android.os.Build
import android.os.UserHandle
import android.os.UserManager
import androidx.annotation.ChecksSdkIntAtLeast
import androidx.core.content.ContextCompat
import app.velora.launcher.core.data.log.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.withContext

/** Estado del espacio privado de Android 15+ (ADR-033). */
enum class PrivateSpaceState {
    /** Android < 15, sin espacio privado creado o sin `ACCESS_HIDDEN_PROFILES`. */
    Unavailable,
    Locked,
    Unlocked,
}

/**
 * Cómo reconocer el perfil privado entre los de `LauncherApps` (lo usa también la lista de apps).
 * Todo protegido por versión y `runCatching`: algunos OEM lanzan `SecurityException`.
 */
object PrivateProfiles {

    @ChecksSdkIntAtLeast(api = Build.VERSION_CODES.VANILLA_ICE_CREAM)
    val supported: Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM

    /** true si [user] es el espacio privado. Binder: llamar fuera del hilo principal. */
    fun isPrivate(launcherApps: LauncherApps, user: UserHandle): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.VANILLA_ICE_CREAM) return false
        return runCatching {
            launcherApps.getLauncherUserInfo(user)?.userType == UserManager.USER_TYPE_PROFILE_PRIVATE
        }.getOrDefault(false)
    }

    /** Bloqueado = en "modo silencioso": sus apps no corren y no se muestran (B-016). */
    fun isLocked(userManager: UserManager, user: UserHandle): Boolean =
        runCatching { userManager.isQuietModeEnabled(user) }.getOrDefault(true)

    /** El perfil privado, si existe y Velora puede verlo. */
    fun find(launcherApps: LauncherApps): UserHandle? {
        if (!supported) return null
        val profiles = runCatching { launcherApps.profiles }.getOrDefault(emptyList())
        return profiles.firstOrNull { isPrivate(launcherApps, it) }
    }

    /** Avisos del sistema al bloquear, desbloquear, crear o borrar un perfil. */
    fun changesFilter(): IntentFilter = IntentFilter().apply {
        addAction(Intent.ACTION_MANAGED_PROFILE_AVAILABLE)
        addAction(Intent.ACTION_MANAGED_PROFILE_UNAVAILABLE)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            addAction(Intent.ACTION_PROFILE_AVAILABLE)
            addAction(Intent.ACTION_PROFILE_UNAVAILABLE)
            addAction(Intent.ACTION_PROFILE_ADDED)
            addAction(Intent.ACTION_PROFILE_REMOVED)
        }
    }
}

/**
 * Bloquear y desbloquear el espacio privado desde Velora (ADR-033). Desbloquear pide la
 * credencial del sistema (Velora nunca la ve); al cambiar, la lista de apps se relee sola.
 */
class PrivateSpaceRepository(private val context: Context, scope: CoroutineScope) {

    private val launcherApps = context.getSystemService(LauncherApps::class.java)
    private val userManager = context.getSystemService(UserManager::class.java)
    private val refreshRequests = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    val state: StateFlow<PrivateSpaceState> = merge(profileChanges(), refreshRequests)
        .onStart { emit(Unit) }
        .conflate()
        .map { read() }
        .flowOn(Dispatchers.IO)
        .stateIn(scope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), PrivateSpaceState.Unavailable)

    /** Vuelve a mirar el estado (al abrir el panel). */
    fun refresh() {
        refreshRequests.tryEmit(Unit)
    }

    /**
     * Pide al sistema bloquear ([locked] = true) o desbloquear. Al desbloquear el sistema
     * muestra su pantalla de credencial y devuelve false aunque luego se desbloquee.
     */
    suspend fun setLocked(locked: Boolean): Boolean = withContext(Dispatchers.IO) {
        // El espacio privado existe desde Android 15; antes no hay nada que bloquear.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.VANILLA_ICE_CREAM) return@withContext false
        val user = PrivateProfiles.find(launcherApps) ?: return@withContext false
        runCatching { userManager.requestQuietModeEnabled(locked, user) }
            .onFailure { Logger.w(TAG, it) { "No se pudo cambiar el espacio privado" } }
            .getOrDefault(false)
            .also { refresh() }
    }

    private fun read(): PrivateSpaceState {
        val user = PrivateProfiles.find(launcherApps) ?: return PrivateSpaceState.Unavailable
        return if (PrivateProfiles.isLocked(userManager, user)) PrivateSpaceState.Locked else PrivateSpaceState.Unlocked
    }

    private fun profileChanges() = callbackFlow {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                trySend(Unit)
            }
        }
        ContextCompat.registerReceiver(
            context,
            receiver,
            PrivateProfiles.changesFilter(),
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
        awaitClose { context.unregisterReceiver(receiver) }
    }

    private companion object {
        const val TAG = "PrivateSpace"
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
