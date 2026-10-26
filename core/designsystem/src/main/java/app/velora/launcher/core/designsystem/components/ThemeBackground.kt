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
            .drawWithCache {
                when (preset) {
                    ThemePreset.Night -> night()
                    ThemePreset.Paper -> paper()
                    ThemePreset.Sakura -> sakura()
                    ThemePreset.Forest -> forest()
                    ThemePreset.Aurora -> aurora()
                    ThemePreset.PixelNight -> pixelNight()
                    ThemePreset.Ash -> ash()
                    ThemePreset.Wallpaper -> onDrawBehind {}
                }
            },
    )
}

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

/** Negro puro con un brillo apenas visible arriba. */
private fun CacheDrawScope.night(): DrawResult {
    val glow = Brush.radialGradient(
        listOf(ArtColors.nightGlow, Color.Transparent),
        center = Offset(size.width * 0.3f, size.height * 0.1f),
        radius = size.width,
    )
    return onDrawBehind {
        drawRect(ArtColors.nightBase)
        drawRect(glow)
    }
}

/** Crema con renglones muy tenues, como una libreta. */
private fun CacheDrawScope.paper(): DrawResult {
    val step = 36.dp.toPx()
    val lines = generateSequence(step * 3) { it + step }.takeWhile { it < size.height }.toList()
    val shade = Brush.verticalGradient(listOf(Color.Transparent, ArtColors.paperShade))
    val stroke = 1.dp.toPx()
    return onDrawBehind {
        drawRect(ArtColors.paperBase)
        lines.forEach { y -> drawLine(ArtColors.paperLine, Offset(0f, y), Offset(size.width, y), stroke) }
        drawRect(shade)
    }
}

/** Cielo rosa, una luna grande y pocos pétalos. */
private fun CacheDrawScope.sakura(): DrawResult {
    val w = size.width
    val h = size.height
    val sky = Brush.verticalGradient(listOf(ArtColors.sakuraSkyTop, ArtColors.sakuraSkyBottom))
    val random = Random(SEED)
    val petals = List(PETALS) {
        Offset(random.nextFloat() * w, h * (0.15f + random.nextFloat() * 0.6f)) to random.nextFloat() * 180f
    }
    val petal = Size(10.dp.toPx(), 6.dp.toPx())
    return onDrawBehind {
        drawRect(sky)
        drawCircle(ArtColors.sakuraMoon, radius = w * 0.32f, center = Offset(w * 0.78f, h * 0.2f))
        petals.forEach { (at, angle) -> rotate(angle, pivot = at) { drawOval(ArtColors.sakuraPetal, at, petal) } }
    }
}

/** Verde profundo con una sola curva suave que cruza la pantalla. */
private fun CacheDrawScope.forest(): DrawResult {
    val w = size.width
    val h = size.height
    val sky = Brush.verticalGradient(listOf(ArtColors.forestTop, ArtColors.forestBottom))
    val curve = Path().apply {
        moveTo(w * 0.55f, 0f)
        cubicTo(w * 1.1f, h * 0.3f, w * 0.4f, h * 0.6f, w, h * 0.95f)
    }
    val stroke = Stroke(1.5.dp.toPx())
    return onDrawBehind {
        drawRect(sky)
        drawPath(curve, ArtColors.forestCurve, style = stroke)
    }
}

/** Azul noche con tres luces suaves que se mezclan. */
private fun CacheDrawScope.aurora(): DrawResult {
    val w = size.width
    val h = size.height
    val sky = Brush.verticalGradient(listOf(ArtColors.auroraTop, ArtColors.auroraBottom))
    val lights = listOf(
        Brush.radialGradient(listOf(ArtColors.auroraTeal, Color.Transparent), Offset(w * 0.15f, h * 0.25f), w * 0.8f),
        Brush.radialGradient(listOf(ArtColors.auroraViolet, Color.Transparent), Offset(w * 0.9f, h * 0.5f), w * 0.75f),
        Brush.radialGradient(listOf(ArtColors.auroraBlue, Color.Transparent), Offset(w * 0.35f, h * 0.9f), w * 0.7f),
    )
    return onDrawBehind {
        drawRect(sky)
        lights.forEach { drawRect(it) }
    }
}

/** Morado con estrellas cuadradas, alineadas a una cuadrícula de 4 dp. */
private fun CacheDrawScope.pixelNight(): DrawResult {
    val px = 4.dp.toPx()
    val random = Random(SEED)
    val stars = List(STARS) {
        Offset(snap(random.nextFloat() * size.width, px), snap(random.nextFloat() * size.height, px))
    }
    val sky = Brush.verticalGradient(listOf(ArtColors.pixelTop, ArtColors.pixelBottom))
    val block = Size(px, px)
    return onDrawBehind {
        drawRect(sky)
        stars.forEach { drawRect(ArtColors.pixelStar, it, block) }
    }
}

/**
 * Ceniza: negro cálido, el resplandor de una hoguera desde abajo, brasas que suben y bordes
 * oscurecidos. Fantasía oscura, sin nada de ningún juego.
 */
private fun CacheDrawScope.ash(): DrawResult {
    val w = size.width
    val h = size.height
    val sky = Brush.verticalGradient(listOf(ArtColors.ashTop, ArtColors.ashBottom))
    val glow = Brush.radialGradient(
        listOf(ArtColors.ashGlow, Color.Transparent),
        Offset(w * 0.5f, h * 1.02f),
        h * 0.55f,
    )
    val vignette = Brush.radialGradient(
        listOf(Color.Transparent, ArtColors.ashVignette),
        Offset(w / 2f, h / 2f),
        h * 0.75f,
    )
    val random = Random(SEED)
    // Las brasas se juntan cerca de la hoguera y se apagan al subir.
    val embers = List(EMBERS) {
        val rise = random.nextFloat()
        Triple(
            Offset(w * (0.5f + (random.nextFloat() - 0.5f) * (0.3f + rise)), h * (1f - rise * 0.75f)),
            (1f - rise).coerceIn(0.15f, 1f),
            (0.8f + random.nextFloat() * 1.6f).dp.toPx(),
        )
    }
    return onDrawBehind {
        drawRect(sky)
        drawRect(glow)
        embers.forEach { (at, alpha, radius) -> drawCircle(ArtColors.ashEmber.copy(alpha = alpha), radius, at) }
        drawRect(vignette)
    }
}

private fun snap(value: Float, grid: Float) = (value / grid).toInt() * grid

private const val SEED = 7
private const val PETALS = 7
private const val STARS = 40
private const val EMBERS = 46
