package app.velora.launcher.core.designsystem.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.CacheDrawScope
import androidx.compose.ui.draw.DrawResult
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import app.velora.launcher.core.designsystem.theme.ArtColors
import app.velora.launcher.core.designsystem.theme.VeloraTheme
import app.velora.launcher.core.model.ThemePreset
import kotlin.random.Random

/**
 * Fondo de cada tema (ADR-018), dibujado por Velora: no cambia el wallpaper del sistema ni pide
 * permisos. Pocas formas y mucho aire: el protagonista es el texto. Las formas se calculan una
 * vez por tamaño (`drawWithCache`) y no hay animación, así que no gasta batería.
 * "Tu fondo" deja ver el wallpaper con un velo suave.
 */
@Composable
fun ThemeBackground(preset: ThemePreset, modifier: Modifier = Modifier, photo: ImageBitmap? = null) {
    if (photo != null) {
        PhotoBackground(photo, modifier)
        return
    }
    if (preset == ThemePreset.Wallpaper) {
        WallpaperScrim(modifier)
        return
    }
    Box(
        modifier = modifier
            .fillMaxSize()
            .drawWithCache { (ThemeArt[preset] ?: { onDrawBehind {} })() },
    )
}

/** Qué dibuja cada tema; "Tu fondo" no está: deja ver el wallpaper. */
private val ThemeArt: Map<ThemePreset, CacheDrawScope.() -> DrawResult> = mapOf(
    ThemePreset.Night to { night() },
    ThemePreset.Paper to { paper() },
    ThemePreset.Sakura to { sakura() },
    ThemePreset.Forest to { forest() },
    ThemePreset.Aurora to { aurora() },
    ThemePreset.PixelNight to { pixelNight() },
    ThemePreset.Ash to { ash() },
    ThemePreset.Sunset to { sunset() },
    ThemePreset.Ocean to { ocean() },
    ThemePreset.Snow to { snow() },
    ThemePreset.Coffee to { coffee() },
    ThemePreset.Galaxy to { galaxy() },
    ThemePreset.Neon to { neon() },
    ThemePreset.Matcha to { matcha() },
    ThemePreset.Lavender to { lavender() },
)

/**
 * Foto propia del usuario (ADR-022) llenando la pantalla, con un velo para que el texto se lea:
 * oscuro en los temas de texto claro, claro en los de texto oscuro.
 */
@Composable
private fun PhotoBackground(photo: ImageBitmap, modifier: Modifier) {
    Box(modifier.fillMaxSize()) {
        Image(photo, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        if (VeloraTheme.onWallpaper.isLight) {
            Box(Modifier.fillMaxSize().background(ArtColors.lightVeil))
        } else {
            WallpaperScrim()
        }
    }
}
