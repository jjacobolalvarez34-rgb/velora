package app.velora.launcher.core.designsystem.components

import androidx.compose.ui.draw.CacheDrawScope
import androidx.compose.ui.draw.DrawResult
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.dp
import app.velora.launcher.core.designsystem.theme.ArtColors
import kotlin.random.Random

// Temas nocturnos con más detalle: estrellas, brasas, galaxias y neón.

/**
 * Neón: noche violeta, un sol rayado en el horizonte y un piso de cuadrícula en perspectiva, con
 * una línea turquesa que brilla donde se juntan.
 */
internal fun CacheDrawScope.neon(): DrawResult {
    val w = size.width
    val h = size.height
    val horizon = h * 0.68f
    val sky = Brush.verticalGradient(listOf(ArtColors.neonTop, ArtColors.neonBottom), endY = horizon)
    val sunRadius = w * 0.3f
    val sunCenter = Offset(w / 2f, horizon)
    val sun = Brush.verticalGradient(
        listOf(ArtColors.neonSunTop, ArtColors.neonSunBottom),
        horizon - sunRadius,
        horizon,
    )
    val stripe = 3.dp.toPx()
    val stripes = List(NEON_STRIPES) { i -> horizon - sunRadius * (0.5f - i * 0.11f) }
    val line = 1.dp.toPx()
    val vanishing = Offset(w / 2f, horizon)
    val vertical = (-NEON_COLUMNS..NEON_COLUMNS).map { i -> Offset(w / 2f + i * w / 5f, h) }
    val horizontal = List(NEON_ROWS) { i -> horizon + (h - horizon) * ((i + 1f) / NEON_ROWS).let { it * it } }
    val glow = Brush.verticalGradient(
        listOf(Color.Transparent, ArtColors.neonGlow, Color.Transparent),
        horizon - 24.dp.toPx(),
        horizon + 24.dp.toPx(),
    )
    return onDrawBehind {
        drawRect(ArtColors.neonFloor)
        drawRect(sky, size = Size(w, horizon))
        drawArc(sun, 180f, 180f, true, sunCenter - Offset(sunRadius, sunRadius), Size(sunRadius * 2, sunRadius * 2))
        stripes.forEach { y -> drawRect(ArtColors.neonTop.copy(alpha = 0.9f), Offset(0f, y), Size(w, stripe)) }
        vertical.forEach { drawLine(ArtColors.neonGrid, vanishing, it, line) }
        horizontal.forEach { y -> drawLine(ArtColors.neonGrid, Offset(0f, y), Offset(w, y), line) }
        drawRect(glow)
        drawLine(ArtColors.neonHorizon, Offset(0f, horizon), Offset(w, horizon), line * 2)
    }
}

/**
 * Ceniza: negro cálido, el resplandor de una hoguera desde abajo, brasas que suben y bordes
 * oscurecidos. Fantasía oscura, sin nada de ningún juego.
 */
internal fun CacheDrawScope.ash(): DrawResult {
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

internal fun snap(value: Float, grid: Float) = (value / grid).toInt() * grid

internal const val SEED = 7
internal const val PETALS = 7
internal const val STARS = 40
internal const val EMBERS = 46
internal const val CROSS_STARS = 6
internal const val MOON_BLOCKS = 7
internal const val GALAXY_ARM = 220
internal const val BRIGHT_STARS = 7
internal const val NEON_STRIPES = 4
internal const val NEON_COLUMNS = 6
internal const val NEON_ROWS = 9
