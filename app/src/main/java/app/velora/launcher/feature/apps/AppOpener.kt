package app.velora.launcher.feature.apps

import android.app.ActivityOptions
import android.content.ActivityNotFoundException
import android.content.Context
import android.graphics.Rect
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
