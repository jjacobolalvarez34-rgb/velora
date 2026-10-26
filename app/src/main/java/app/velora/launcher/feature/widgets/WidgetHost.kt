package app.velora.launcher.feature.widgets

import android.app.Activity
import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetHostView
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import app.velora.launcher.core.data.favorites.WidgetEntity
import app.velora.launcher.core.data.favorites.WidgetsRepository
import app.velora.launcher.core.data.log.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Un widget disponible para agregar. */
@Immutable
data class WidgetOption(val provider: AppWidgetProviderInfo, val label: String, val appLabel: String)

/**
 * Widgets del inicio (ADR-020, SEGURIDAD §3.4). Un único `AppWidgetHost` por proceso: escucha
 * solo con el inicio visible (`onStart`/`onStop`) y libera los ids que ya no se usan (B-019).
 */
class WidgetHost(context: Context, private val repository: WidgetsRepository, private val scope: CoroutineScope) {

    private val appContext = context.applicationContext
    private val manager = AppWidgetManager.getInstance(appContext)
    private val host = AppWidgetHost(appContext, HOST_ID)

    /** Lo que se está agregando mientras el sistema pide permiso o el widget se configura. */
    private var pending: Pair<Int, AppWidgetProviderInfo>? = null

    val widgets: StateFlow<List<WidgetEntity>> =
        repository.widgets.stateIn(scope, SharingStarted.Eagerly, emptyList())

    fun startListening() {
        runCatching { host.startListening() }.onFailure { Logger.w(TAG, it) { "startListening" } }
        scope.launch { removeOrphans() }
    }

    fun stopListening() {
        runCatching { host.stopListening() }.onFailure { Logger.w(TAG, it) { "stopListening" } }
    }

    /** Widgets instalados, agrupables por app. Lee `PackageManager`: siempre fuera del hilo principal. */
    suspend fun options(): List<WidgetOption> = withContext(Dispatchers.IO) {
        val pm = appContext.packageManager
        manager.installedProviders.map { info ->
            val appLabel = runCatching {
                pm.getApplicationLabel(pm.getApplicationInfo(info.provider.packageName, 0)).toString()
            }.getOrDefault(info.provider.packageName)
            WidgetOption(info, info.loadLabel(pm).orEmpty(), appLabel)
        }.sortedWith(compareBy({ it.appLabel.lowercase() }, { it.label.lowercase() }))
    }

    suspend fun preview(option: WidgetOption, densityDpi: Int): ImageBitmap? = withContext(Dispatchers.IO) {
        runCatching {
            (
                option.provider.loadPreviewImage(appContext, densityDpi)
                    ?: option.provider.loadIcon(appContext, densityDpi)
                )
                ?.toBitmap()?.asImageBitmap()
        }.getOrNull()
    }

    /** Paso 1: reservar un id y pedir permiso al sistema (solo la primera vez por proveedor). */
    fun beginAdd(activity: Activity, option: WidgetOption) {
        val id = host.allocateAppWidgetId()
        val info = option.provider
        pending = id to info
        if (manager.bindAppWidgetIdIfAllowed(id, info.profile, info.provider, null)) {
            configureOrSave(activity)
            return
        }
        val bind = Intent(AppWidgetManager.ACTION_APPWIDGET_BIND)
            .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id)
            .putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER, info.provider)
            .putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER_PROFILE, info.profile)
        try {
            @Suppress("DEPRECATION")
            activity.startActivityForResult(bind, REQUEST_BIND)
        } catch (e: ActivityNotFoundException) {
            Logger.w(TAG, e) { "Sin diálogo para permitir widgets" }
            cancel()
        }
    }

    /** Respuesta del diálogo de permiso o de la configuración del widget. */
    fun onActivityResult(activity: Activity, requestCode: Int, resultCode: Int): Boolean {
        if (requestCode != REQUEST_BIND && requestCode != REQUEST_CONFIGURE) return false
        when {
            resultCode != Activity.RESULT_OK -> cancel()
            requestCode == REQUEST_BIND -> configureOrSave(activity)
            else -> save()
        }
        return true
    }

    /** Vista del widget, o null si su app ya no lo ofrece. */
    fun createView(context: Context, appWidgetId: Int): AppWidgetHostView? {
        val info = manager.getAppWidgetInfo(appWidgetId) ?: return null
        return host.createView(context, appWidgetId, info)
    }

    fun labelOf(appWidgetId: Int): String =
        manager.getAppWidgetInfo(appWidgetId)?.loadLabel(appContext.packageManager).orEmpty()

    fun remove(appWidgetId: Int) {
        host.deleteAppWidgetId(appWidgetId)
        scope.launch { repository.remove(appWidgetId) }
    }

    fun setHeight(appWidgetId: Int, heightDp: Int) {
        scope.launch { repository.setHeight(appWidgetId, heightDp) }
    }

    private fun configureOrSave(activity: Activity) {
        val (id, info) = pending ?: return
        if (info.configure == null) {
            save()
            return
        }
        try {
            host.startAppWidgetConfigureActivityForResult(activity, id, 0, REQUEST_CONFIGURE, null)
        } catch (e: ActivityNotFoundException) {
            Logger.w(TAG, e) { "Sin pantalla de configuración" }
            save()
        }
    }

    private fun save() {
        val (id, info) = pending ?: return
        pending = null
        val density = appContext.resources.displayMetrics.density
        val heightDp = (info.minHeight / density).toInt().coerceAtLeast(DEFAULT_HEIGHT_DP)
        scope.launch { repository.add(id, info.provider.flattenToString(), heightDp) }
    }

    private fun cancel() {
        pending?.let { (id, _) -> host.deleteAppWidgetId(id) }
        pending = null
    }

    /** Widgets cuya app se desinstaló: se liberan para no acumular ids huérfanos (B-019). */
    private fun removeOrphans() {
        widgets.value.filter { manager.getAppWidgetInfo(it.appWidgetId) == null }.forEach { remove(it.appWidgetId) }
    }

    private companion object {
        const val TAG = "WidgetHost"
        const val HOST_ID = 1_024
        const val REQUEST_BIND = 4_101
        const val REQUEST_CONFIGURE = 4_102
        const val DEFAULT_HEIGHT_DP = 120
    }
}
