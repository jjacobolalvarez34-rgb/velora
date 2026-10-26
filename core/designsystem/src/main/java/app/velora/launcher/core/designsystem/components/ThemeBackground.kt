package app.velora.launcher.core.designsystem.components

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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.dp
import app.velora.launcher.core.designsystem.theme.ArtColors
import app.velora.launcher.core.model.ThemePreset
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * Fondo de cada tema (ADR-018), dibujado por Velora: no cambia el wallpaper del sistema ni pide
 * permisos. Las formas se calculan una vez por tamaño (`drawWithCache`); dibujar es barato y no
 * hay animación, así que no gasta batería. "Tu fondo" deja ver el wallpaper con un velo suave.
 */
@Composable
fun ThemeBackground(preset: ThemePreset, modifier: Modifier = Modifier) {
    if (preset == ThemePreset.Wallpaper) {
        WallpaperScrim(modifier)
        return
    }
    Box(
        modifier = modifier
            .fillMaxSize()
            .drawWithCache {
                when (preset) {
                    ThemePreset.Sakura -> sakura()
                    ThemePreset.Forest -> forest()
                    ThemePreset.PixelNight -> pixelNight()
                    ThemePreset.Aurora -> aurora()
                    ThemePreset.Wallpaper -> onDrawBehind {}
                }
            },
    )
}

/** Cielo rosa, luna, montaña con nieve y una rama con flores; pétalos que caen. */
private fun CacheDrawScope.sakura(): DrawResult {
    val sky = Brush.verticalGradient(listOf(ArtColors.sakuraSkyTop, ArtColors.sakuraSkyBottom))
    val w = size.width
    val h = size.height
    val (mountain, snow, branch) = sakuraShapes(w, h)
    val random = Random(SEED)
    val flowers = List(7) { Offset(w * (0.05f + 0.08f * it), h * (0.05f + random.nextFloat() * 0.06f)) }
    val petals = List(PETALS) {
        Offset(random.nextFloat() * w, random.nextFloat() * h * 0.7f) to
            random.nextFloat() * 180f
    }
    val petalSize = Size(9.dp.toPx(), 5.dp.toPx())
    val flowerRadius = 7.dp.toPx()
    return onDrawBehind {
        drawRect(sky)
        drawCircle(ArtColors.sakuraMoon, radius = w * 0.3f, center = Offset(w * 0.8f, h * 0.24f))
        drawPath(mountain, ArtColors.sakuraMountain)
        drawPath(snow, ArtColors.sakuraSnow)
        drawPath(branch, ArtColors.sakuraBranch, style = Stroke(4.dp.toPx()))
        flowers.forEach { flower(it, flowerRadius) }
        petals.forEach { (at, angle) ->
            rotate(angle, pivot = at) {
                drawOval(ArtColors.sakuraPetalLight, topLeft = at, size = petalSize)
            }
        }
    }
}

/** Montaña, nieve de la cumbre y rama, como trazos. */
private fun sakuraShapes(w: Float, h: Float): Triple<Path, Path, Path> {
    val mountain = Path().apply {
        moveTo(0f, h * 0.78f)
        lineTo(w * 0.28f, h * 0.6f)
        lineTo(w * 0.55f, h * 0.72f)
        lineTo(w * 0.82f, h * 0.52f)
        lineTo(w, h * 0.62f)
        lineTo(w, h)
        lineTo(0f, h)
        close()
    }
    val snow = Path().apply {
        moveTo(w * 0.74f, h * 0.575f)
        lineTo(w * 0.82f, h * 0.52f)
        lineTo(w * 0.9f, h * 0.56f)
        lineTo(w * 0.84f, h * 0.565f)
        lineTo(w * 0.8f, h * 0.555f)
        close()
    }
    val branch = Path().apply {
        moveTo(-w * 0.05f, h * 0.06f)
        cubicTo(w * 0.25f, h * 0.02f, w * 0.45f, h * 0.12f, w * 0.62f, h * 0.08f)
    }
    return Triple(mountain, snow, branch)
}

private fun DrawScope.flower(center: Offset, radius: Float) {
    repeat(5) { i ->
        val angle = (i * 2 * PI / 5).toFloat()
        val at = Offset(center.x + cos(angle) * radius, center.y + sin(angle) * radius)
        drawCircle(ArtColors.sakuraPetal, radius = radius * 0.75f, center = at)
    }
    drawCircle(ArtColors.sakuraPetalLight, radius = radius * 0.45f, center = center)
}

/** Verde azulado con una ola lima a la derecha, trama de puntos y rayas en la esquina. */
private fun CacheDrawScope.forest(): DrawResult {
    val w = size.width
    val h = size.height
    val wave = Path().apply {
        moveTo(w * 0.58f, 0f)
        cubicTo(w * 1.05f, h * 0.2f, w * 0.55f, h * 0.55f, w * 0.92f, h * 0.78f)
        cubicTo(w * 1.02f, h * 0.85f, w, h, w, h)
        lineTo(w, 0f)
        close()
    }
    val waveBrush = Brush.verticalGradient(listOf(ArtColors.forestWaveLight, ArtColors.forestWaveDark))
    val step = 28.dp.toPx()
    val dots = buildList {
        var y = step / 2f
        while (y < h) {
            var x = step / 2f
            while (x < w * 0.75f) {
                add(Offset(x, y))
                x += step
            }
            y += step
        }
    }
    val stripes = List(9) { it * 14.dp.toPx() }
    val dotRadius = 1.6.dp.toPx()
    return onDrawBehind {
        drawRect(ArtColors.forestBackground)
        dots.forEach { drawCircle(ArtColors.forestDot, dotRadius, it) }
        drawPath(wave, waveBrush)
        stripes.forEach { offset ->
            drawLine(
                ArtColors.forestStripe,
                start = Offset(w * 0.55f + offset, h),
                end = Offset(w + offset, h * 0.8f),
                strokeWidth = 2.dp.toPx(),
            )
        }
    }
}

/** Espacio morado con nebulosa, estrellas de píxeles y un planeta con anillo, todo en bloques. */
private fun CacheDrawScope.pixelNight(): DrawResult {
    val w = size.width
    val h = size.height
    val px = 4.dp.toPx()
    val random = Random(SEED)
    val stars = List(STARS) {
        Offset(snap(random.nextFloat() * w, px), snap(random.nextFloat() * h, px)) to
            (1 + random.nextInt(2))
    }
    val planetCenter = Offset(w * 0.72f, h * 0.3f)
    val planetRadius = w * 0.13f
    val planet = pixelDisc(planetCenter, planetRadius, px)
    val ring = pixelRing(planetCenter, planetRadius, px)
    val sky = Brush.verticalGradient(listOf(ArtColors.pixelTop, ArtColors.pixelBottom))
    val nebula = Brush.radialGradient(listOf(ArtColors.pixelNebula, Color.Transparent), Offset(w * 0.5f, 0f), w * 0.7f)
    val block = Size(px, px)
    return onDrawBehind {
        drawRect(sky)
        drawRect(nebula)
        stars.forEach { (at, scale) -> drawRect(ArtColors.pixelStar, at, Size(px * scale, px * scale)) }
        planet.forEach { (at, lit) ->
            drawRect(if (lit) ArtColors.pixelPlanetLight else ArtColors.pixelPlanetDark, at, block)
        }
        ring.forEach { drawRect(ArtColors.pixelRing, it, block) }
    }
}

/** Bloques de un disco; cada uno dice si está del lado iluminado (arriba a la izquierda). */
private fun pixelDisc(center: Offset, radius: Float, px: Float): List<Pair<Offset, Boolean>> = buildList {
    var y = center.y - radius
    while (y <= center.y + radius) {
        var x = center.x - radius
        while (x <= center.x + radius) {
            val dx = x - center.x
            val dy = y - center.y
            if (dx * dx + dy * dy <= radius * radius) add(Offset(snap(x, px), snap(y, px)) to (dx + dy < 0))
            x += px
        }
        y += px
    }
}

/** El anillo del planeta: una diagonal de bloques que lo cruza. */
private fun pixelRing(center: Offset, radius: Float, px: Float): List<Offset> = List(RING_BLOCKS) { i ->
    val t = i / RING_BLOCKS.toFloat()
    Offset(
        snap(center.x - radius * 1.6f + t * radius * 3.2f, px),
        snap(center.y + radius * 0.25f - t * radius * 0.5f, px),
    )
}

/** Azul noche con tres luces suaves que se mezclan y estrellas finas. */
private fun CacheDrawScope.aurora(): DrawResult {
    val w = size.width
    val h = size.height
    val sky = Brush.verticalGradient(listOf(ArtColors.auroraTop, ArtColors.auroraBottom))
    val lights = listOf(
        Brush.radialGradient(listOf(ArtColors.auroraTeal, Color.Transparent), Offset(w * 0.15f, h * 0.25f), w * 0.8f),
        Brush.radialGradient(listOf(ArtColors.auroraViolet, Color.Transparent), Offset(w * 0.9f, h * 0.5f), w * 0.75f),
        Brush.radialGradient(listOf(ArtColors.auroraBlue, Color.Transparent), Offset(w * 0.35f, h * 0.9f), w * 0.7f),
    )
    val random = Random(SEED)
    val stars = List(STARS) { Offset(random.nextFloat() * w, random.nextFloat() * h * 0.6f) }
    val starRadius = 0.8.dp.toPx()
    return onDrawBehind {
        drawRect(sky)
        lights.forEach { drawRect(it, size = Size(w, h)) }
        stars.forEach { drawCircle(ArtColors.auroraStar, starRadius, it) }
    }
}

/** Alinea a la cuadrícula de píxeles del tema pixel. */
private fun snap(value: Float, grid: Float) = (value / grid).toInt() * grid

private const val SEED = 7
private const val PETALS = 16
private const val STARS = 60
private const val RING_BLOCKS = 28
