package app.velora.launcher.core.designsystem.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import app.velora.launcher.core.designsystem.theme.VeloraTheme

/**
 * Velo degradado sobre el wallpaper: oscurece arriba (reloj, barra de estado) y abajo
 * (favoritos) para mantener el contraste del texto (ESTILO §2). El centro queda limpio.
 */
@Composable
fun WallpaperScrim(modifier: Modifier = Modifier) {
    val colors = VeloraTheme.onWallpaper
    Box(
        modifier = modifier
            .fillMaxSize()
            .drawWithCache {
                val brush = Brush.verticalGradient(
                    0f to colors.scrimTop,
                    0.35f to Color.Transparent,
                    0.55f to Color.Transparent,
                    1f to colors.scrimBottom,
                )
                onDrawBehind { drawRect(brush) }
            },
    )
}
