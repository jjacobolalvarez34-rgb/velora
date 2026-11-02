package app.velora.launcher.ui

import android.app.WallpaperManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import app.velora.launcher.core.data.log.Logger

/**
 * Fondos animados (Wallpaper Engine y cualquier fondo de pantalla en vivo, ADR-037). Con el tema
 * "Tu fondo", Velora solo deja ver el fondo del sistema y le avisa de los toques y de la posición,
 * igual que el launcher del teléfono. **Nunca cambia el fondo**: no usa `setBitmap`/`setStream`.
 *
 * @return qué hacer con un toque en un espacio vacío del inicio (null si el tema dibuja su fondo).
 */
@Composable
internal fun rememberLiveWallpaperTap(enabled: Boolean): ((Offset) -> Unit)? {
    val view = LocalView.current
    val manager = remember(view) { WallpaperManager.getInstance(view.context) }
    // Centrado: los fondos con desplazamiento (parallax) se ven quietos en su punto medio.
    DisposableEffect(view, enabled) {
        if (enabled) {
            runCatching { view.windowToken?.let { manager.setWallpaperOffsets(it, CENTER, 0f) } }
        }
        onDispose { }
    }
    if (!enabled) return null
    val context = LocalContext.current
    return remember(view, manager, context) {
        { at: Offset ->
            val token = view.windowToken
            if (token != null) {
                val location = IntArray(2).also(view::getLocationOnScreen)
                runCatching {
                    manager.sendWallpaperCommand(
                        token,
                        WallpaperManager.COMMAND_TAP,
                        (location[0] + at.x).toInt(),
                        (location[1] + at.y).toInt(),
                        0,
                        null,
                    )
                }.onFailure { Logger.w(TAG, it) { "El fondo no recibió el toque" } }
            }
        }
    }
}

private const val CENTER = 0.5f
private const val TAG = "LiveWallpaper"
