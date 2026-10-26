package app.velora.launcher.feature.apps

import android.app.ActivityOptions
import android.app.SearchManager
import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Rect
import android.net.Uri
import android.view.View
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Rect as ComposeRect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import app.velora.launcher.R
import app.velora.launcher.core.data.apps.AppLauncher
import app.velora.launcher.core.data.apps.LaunchResult
import app.velora.launcher.core.data.log.Logger
import app.velora.launcher.core.model.AppEntry
import app.velora.launcher.core.model.AppShortcut
import kotlin.math.roundToInt

/**
 * Abre apps desde la UI con la animación de origen del sistema: la app "crece" desde su fila
 * (ESTILO §5.3). Los errores se muestran con un mensaje amable, nunca un crash (B-014).
 */
@Stable
class AppOpener(private val context: Context, private val view: View, private val launcher: AppLauncher) {
    /** @param bounds rectángulo de la fila en coordenadas de la raíz de Compose. */
    fun open(app: AppEntry, bounds: ComposeRect?) {
        val local = bounds?.toAndroidRect()
        val options = local?.let {
            ActivityOptions.makeClipRevealAnimation(view, it.left, it.top, it.width(), it.height()).toBundle()
        }
        report(app, launcher.launch(app.key, local?.toScreen(), options))
    }

    fun openShortcut(shortcut: AppShortcut, bounds: ComposeRect?) {
        val local = bounds?.toAndroidRect()
        val result = launcher.launchShortcut(shortcut, local?.toScreen(), null)
        if (result != LaunchResult.Launched) {
            Toast.makeText(
                context,
                context.getString(R.string.error_app_launch, shortcut.label),
                Toast.LENGTH_SHORT,
            ).show()
        }
    }

    fun openInfo(app: AppEntry, bounds: ComposeRect?) {
        report(app, launcher.openAppInfo(app.key, bounds?.toAndroidRect()?.toScreen()))
    }

    /** La búsqueda la hace la app de búsqueda o el navegador del teléfono: Velora no usa red. */
    fun webSearch(query: String) {
        val search = Intent(Intent.ACTION_WEB_SEARCH).putExtra(SearchManager.QUERY, query)
        val browser = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/search?q=" + Uri.encode(query)))
        val opened = listOf(search, browser).any { intent ->
            try {
                context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                true
            } catch (e: ActivityNotFoundException) {
                Logger.w(TAG, e) { "Sin app para buscar en la web" }
                false
            }
        }
        if (!opened) Toast.makeText(context, R.string.error_open_system_settings, Toast.LENGTH_SHORT).show()
    }

    fun copy(text: String) {
        context.getSystemService(ClipboardManager::class.java)?.setPrimaryClip(ClipData.newPlainText(text, text))
        Toast.makeText(context, context.getString(R.string.copied, text), Toast.LENGTH_SHORT).show()
    }

    fun uninstall(app: AppEntry) {
        val intent = launcher.uninstallIntent(app.key)
        if (intent == null) {
            report(app, LaunchResult.Failed)
            return
        }
        try {
            context.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            Logger.w(TAG, e) { "Sin diálogo de desinstalación" }
            report(app, LaunchResult.Failed)
        }
    }

    private fun report(app: AppEntry, result: LaunchResult) {
        val message = when (result) {
            LaunchResult.Launched -> return
            LaunchResult.WorkProfilePaused -> context.getString(R.string.error_work_profile_paused)
            LaunchResult.Failed -> context.getString(R.string.error_app_launch, app.label)
        }
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    private fun ComposeRect.toAndroidRect() =
        Rect(left.roundToInt(), top.roundToInt(), right.roundToInt(), bottom.roundToInt())

    private fun Rect.toScreen(): Rect {
        val origin = IntArray(2).also(view::getLocationOnScreen)
        return Rect(this).apply { offset(origin[0], origin[1]) }
    }

    private companion object {
        const val TAG = "AppOpener"
    }
}

@Composable
fun rememberAppOpener(launcher: AppLauncher): AppOpener {
    val context = LocalContext.current
    val view = LocalView.current
    return remember(context, view, launcher) { AppOpener(context, view, launcher) }
}
