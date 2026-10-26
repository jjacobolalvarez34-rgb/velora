package app.velora.launcher.feature.apps

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import app.velora.launcher.core.data.apps.IconRepository
import app.velora.launcher.core.data.apps.IconRequest
import app.velora.launcher.core.designsystem.theme.VeloraTheme
import app.velora.launcher.core.model.AppKey
import app.velora.launcher.core.model.AppNotification
import app.velora.launcher.core.model.IconStyle

/** Caché de íconos para las filas; la provee `VeloraApp`. null en previews: se dibuja el hueco. */
val LocalIconRepository = staticCompositionLocalOf<IconRepository?> { null }

/** Notificaciones por app ([NotificationsRepository.keyOf]); vacío si la función está apagada. */
val LocalNotifications = staticCompositionLocalOf<Map<String, AppNotification>> { emptyMap() }

/** Estilo de íconos elegido en Ajustes → Tema. */
val LocalIconStyle = staticCompositionLocalOf { IconStyle.Circle }

/**
 * Ícono de una app. Lee primero la caché (sin parpadeo) y, si no está, lo renderiza fuera del
 * hilo principal. Mientras tanto deja el espacio reservado para que la fila no salte.
 */
@Composable
fun AppIcon(key: AppKey, size: Dp, modifier: Modifier = Modifier) {
    val icons = LocalIconRepository.current
    val sizePx = with(LocalDensity.current) { size.roundToPx() }
    val style = LocalIconStyle.current
    val colors = VeloraTheme.onWallpaper
    val tint = colors.iconGlyph.toArgb()
    val backdrop = colors.iconCircle.toArgb()
    val request = remember(key, sizePx, style, tint, backdrop) { IconRequest(key, sizePx, style, tint, backdrop) }
    val bitmap: ImageBitmap? by produceState(icons?.cached(request), icons, request) {
        if (value == null && icons != null) value = icons.load(request)
    }
    val current = bitmap
    if (current != null) {
        Image(bitmap = current, contentDescription = null, modifier = modifier.size(size))
    } else {
        Spacer(modifier = modifier.size(size))
    }
}
