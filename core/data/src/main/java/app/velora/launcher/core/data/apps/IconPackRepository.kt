package app.velora.launcher.core.data.apps

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.content.res.Resources
import android.graphics.drawable.Drawable
import android.os.Build
import android.util.Xml
import app.velora.launcher.core.data.log.Logger
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Un pack de íconos instalado (de la Play Store), para el selector de Ajustes. */
data class IconPackInfo(val packageName: String, val label: String)

/** Lo que un pack ofrece para una app: su dibujo propio, o piezas para componer uno. */
internal sealed interface PackIcon {
    class Exact(val drawable: Drawable) : PackIcon

    class Composed(val back: Drawable?, val mask: Drawable?, val upon: Drawable?, val scale: Float) : PackIcon
}

/**
 * Packs de íconos de terceros con el formato común de launchers (ADW/Nova/Lawnchair). Se
 * descubren con `queryIntentActivities` de las acciones declaradas en `<queries>` del manifiesto
 * (nunca `QUERY_ALL_PACKAGES`, SEGURIDAD §2). Todo corre en `Dispatchers.IO` y con `runCatching`:
 * los packs vienen de terceros y un XML roto no puede tumbar el launcher.
 */
class IconPackRepository(private val context: Context) {

    /** `appfilter.xml` ya interpretado por pack: se lee una vez por proceso. */
    private val filters = ConcurrentHashMap<String, AppFilter>()

    /** Packs instalados ordenados por nombre; vacío si no hay o si algo falla. */
    suspend fun installedPacks(): List<IconPackInfo> = withContext(Dispatchers.IO) {
        runCatching { queryPacks() }
            .onFailure { Logger.w(TAG, it) { "No se pudieron listar los packs de íconos" } }
            .getOrDefault(emptyList())
    }

    /** El pack se actualizó o se desinstaló: su `appfilter` puede haber cambiado. */
    fun invalidate(packageName: String) {
        filters.remove(packageName)
    }

    /** El ícono de [component] en [pack] a [densityDpi]; null si el pack no lo tiene ni sabe componerlo. */
    internal suspend fun iconFor(pack: String, component: ComponentName, densityDpi: Int): PackIcon? =
        withContext(Dispatchers.IO) {
            runCatching { resolve(pack, component, densityDpi) }
                .onFailure { Logger.w(TAG, it) { "El pack $pack no pudo dar el ícono de ${component.packageName}" } }
                .getOrNull()
        }

    private fun queryPacks(): List<IconPackInfo> {
        val pm = context.packageManager
        return ACTIONS.asSequence()
            .flatMap { action -> pm.activitiesFor(Intent(action)).asSequence() }
            .mapNotNull { it.activityInfo?.applicationInfo }
            .filter { it.packageName != context.packageName }
            .distinctBy { it.packageName }
            .map { IconPackInfo(it.packageName, it.loadLabel(pm).toString()) }
            .sortedBy { it.label.lowercase() }
            .toList()
    }

    private fun resolve(pack: String, component: ComponentName, densityDpi: Int): PackIcon? {
        val res = context.packageManager.getResourcesForApplication(pack)
        val filter = filters.getOrPut(pack) { readFilter(pack, res) }
        val exact = filter.drawableFor(component.packageName, component.className)
            ?.let { drawable(res, pack, it, densityDpi) }
        return when {
            exact != null -> PackIcon.Exact(exact)

            filter.canCompose -> PackIcon.Composed(
                back = filter.backFor(component.packageName)?.let { drawable(res, pack, it, densityDpi) },
                mask = filter.mask?.let { drawable(res, pack, it, densityDpi) },
                upon = filter.upon?.let { drawable(res, pack, it, densityDpi) },
                scale = filter.scale,
            )

            else -> null
        }
    }

    /** `res/xml/appfilter` y, si no está, `assets/appfilter.xml`; vacío si no hay ninguno. */
    @SuppressLint("DiscouragedApi") // Los nombres los dicta el pack: no hay R para buscarlos.
    private fun readFilter(pack: String, res: Resources): AppFilter {
        val id = res.getIdentifier(APPFILTER, "xml", pack)
        val elements = if (id != 0) {
            val parser = res.getXml(id)
            try {
                AppFilterParser.elements(parser)
            } finally {
                parser.close()
            }
        } else {
            runCatching {
                res.assets.open("$APPFILTER.xml").use { stream ->
                    AppFilterParser.elements(Xml.newPullParser().apply { setInput(stream, null) })
                }
            }.getOrDefault(emptyList())
        }
        return AppFilter.from(elements)
    }

    @SuppressLint("DiscouragedApi")
    private fun drawable(res: Resources, pack: String, name: String, densityDpi: Int): Drawable? {
        val id = res.getIdentifier(name, "drawable", pack).takeIf { it != 0 }
            ?: res.getIdentifier(name, "mipmap", pack).takeIf { it != 0 }
            ?: return null
        return runCatching { res.getDrawableForDensity(id, densityDpi, null) }.getOrNull()
    }

    private fun PackageManager.activitiesFor(intent: Intent): List<ResolveInfo> =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            queryIntentActivities(intent, PackageManager.ResolveInfoFlags.of(0))
        } else {
            @Suppress("DEPRECATION")
            queryIntentActivities(intent, 0)
        }

    private companion object {
        const val TAG = "IconPackRepository"
        const val APPFILTER = "appfilter"

        /** Acciones con que se anuncian los packs; las mismas que `<queries>` del manifiesto. */
        val ACTIONS = listOf(
            "org.adw.launcher.THEMES",
            "org.adw.launcher.icons.ACTION_PICK_ICON",
            "com.novalauncher.THEME",
            "com.teslacoilsw.launcher.THEME",
            "com.anddoes.launcher.THEME",
            "com.gau.go.launcherex.theme",
            "com.dlto.atom.launcher.THEME",
            "com.fede.launcher.THEME_ICONPACK",
            "ch.deletescape.lawnchair.ICONPACK",
        )
    }
}
