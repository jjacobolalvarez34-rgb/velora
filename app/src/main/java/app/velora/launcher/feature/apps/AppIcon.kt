package app.velora.launcher.feature.apps

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import app.velora.launcher.core.data.apps.IconRepository
import app.velora.launcher.core.model.AppKey

/** Caché de íconos para las filas; la provee `VeloraApp`. null en previews: se dibuja el hueco. */
val LocalIconRepository = staticCompositionLocalOf<IconRepository?> { null }

/**
 * Ícono de una app. Lee primero la caché (sin parpadeo) y, si no está, lo renderiza fuera del
 * hilo principal. Mientras tanto deja el espacio reservado para que la fila no salte.
 */
@Composable
fun AppIcon(key: AppKey, size: Dp, modifier: Modifier = Modifier) {
    val icons = LocalIconRepository.current
    val sizePx = with(LocalDensity.current) { size.roundToPx() }
    val bitmap: ImageBitmap? by produceState(icons?.cached(key, sizePx), icons, key, sizePx) {
        if (value == null && icons != null) value = icons.load(key, sizePx)
    }
    val current = bitmap
    if (current != null) {
        Image(bitmap = current, contentDescription = null, modifier = modifier.size(size))
    } else {
        Spacer(modifier = modifier.size(size))
    }
}
