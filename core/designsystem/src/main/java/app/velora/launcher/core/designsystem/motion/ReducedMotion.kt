package app.velora.launcher.core.designsystem.motion

import android.content.ContentResolver
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext

private val animationScaleKeys = listOf(
    Settings.Global.ANIMATOR_DURATION_SCALE,
    Settings.Global.TRANSITION_ANIMATION_SCALE,
)

/** true si el usuario activó "Quitar animaciones" (alguna escala de animación en 0). */
fun ContentResolver.isReducedMotionEnabled(): Boolean =
    animationScaleKeys.any { key -> Settings.Global.getFloat(this, key, 1f) == 0f }

/**
 * Estado de "Quitar animaciones" que se actualiza en vivo si el usuario lo cambia
 * con el launcher abierto.
 */
@Composable
fun rememberReducedMotion(): Boolean {
    val resolver = LocalContext.current.contentResolver
    var reduced by remember(resolver) { mutableStateOf(resolver.isReducedMotionEnabled()) }
    DisposableEffect(resolver) {
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                reduced = resolver.isReducedMotionEnabled()
            }
        }
        animationScaleKeys.forEach { key ->
            resolver.registerContentObserver(Settings.Global.getUriFor(key), false, observer)
        }
        onDispose { resolver.unregisterContentObserver(observer) }
    }
    return reduced
}
