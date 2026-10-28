package app.velora.launcher.core.designsystem.components

import androidx.compose.ui.draw.CacheDrawScope
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.dp
import app.velora.launcher.core.designsystem.theme.ArtColors
import kotlin.math.abs
import kotlin.math.sin
import kotlin.random.Random

// Temas espaciales en vector: Noche pixel (todo en una rejilla de píxeles) y lo nítido de Galaxia.

/** Una rejilla de bloques de [px]: lo que cabe en la pantalla. */
private class PixelGrid(val px: Float, width: Float, height: Float) {
    val cols = (width / px).toInt() + 1
    val rows = (height / px).toInt() + 1
    val block = Size(px, px)

    fun at(col: Int, row: Int) = Offset(col * px, row * px)

    fun snapped(x: Float, y: Float) = Offset(snap(x, px), snap(y, px))
}

/**
 * Noche pixel con definición: cielo en tres tonos, una franja de nebulosa tramada, estrellas de
 * tres tamaños (las grandes en cruz), una luna escalonada con cráteres y montañas de píxeles.
 * Todo sobre la misma rejilla, así nada se ve borroso.
 */
internal fun CacheDrawScope.pixelNight(): DrawScope.() -> Unit {
    val w = size.width
    val h = size.height
    val grid = PixelGrid(4.dp.toPx(), w, h)
    val random = Random(SEED)
    val sky = Brush.verticalGradient(listOf(ArtColors.pixelTop, ArtColors.pixelMid, ArtColors.pixelBottom))
    val nebula = pixelNebula(grid, random)
    val small = List(STARS * 2) { grid.snapped(random.nextFloat() * w, random.nextFloat() * h * 0.7f) }
    val bright = List(STARS / 4) { grid.snapped(random.nextFloat() * w, random.nextFloat() * h * 0.6f) }
    val crosses = List(CROSS_STARS) {
        grid.snapped(w * (0.1f + random.nextFloat() * 0.8f), h * (0.08f + random.nextFloat() * 0.45f))
    }
    val moonCenter = grid.snapped(w * 0.74f, h * 0.16f)
    val moon = pixelDisc(grid, moonCenter, MOON_BLOCKS)
    val far = List(grid.cols) { col -> (grid.rows * (0.86f - 0.05f * sin(col / 7f) - 0.03f * sin(col / 3f))).toInt() }
    val near = List(grid.cols) { col -> (grid.rows * (0.92f - 0.04f * sin(col / 5f + 1f))).toInt() }
    return {
        drawRect(sky)
        nebula.forEach { drawRect(ArtColors.pixelNebula, it, grid.block) }
        small.forEachIndexed { i, at ->
            drawRect(ArtColors.pixelStar.copy(alpha = 0.35f + (i % 4) * 0.15f), at, grid.block)
        }
        bright.forEach { drawRect(ArtColors.pixelStar, it, grid.block) }
        crosses.forEach { drawCrossStar(it, grid) }
        drawCircle(ArtColors.pixelMoonGlow, MOON_BLOCKS * grid.px * 2.2f, moonCenter)
        moon.forEach { drawRect(ArtColors.pixelMoon, it, grid.block) }
        CRATERS.forEach { (dx, dy) ->
            drawRect(
                ArtColors.pixelCrater,
                moonCenter + Offset(dx * grid.px, dy * grid.px),
                grid.block * 2f,
            )
        }
        drawHills(far, ArtColors.pixelHillFar, grid)
        drawHills(near, ArtColors.pixelHillNear, grid)
    }
}

/** Una diagonal tramada en damero, más densa en el centro de la franja. */
private fun pixelNebula(grid: PixelGrid, random: Random): List<Offset> = buildList {
    for (row in 0 until grid.rows) {
        val center = grid.cols * (0.9f - row / grid.rows.toFloat() * 0.8f)
        for (col in 0 until grid.cols) {
            val distance = abs(col - center) / (grid.cols * 0.18f)
            if (distance < 1f && (row + col) % 2 == 0 && random.nextFloat() > distance) add(grid.at(col, row))
        }
    }
}

/** Un círculo hecho de bloques: borde escalonado, como en los juegos de 8 bits. */
private fun pixelDisc(grid: PixelGrid, center: Offset, radius: Int): List<Offset> = buildList {
    for (dy in -radius..radius) {
        for (dx in -radius..radius) {
            if (dx * dx + dy * dy <= radius * radius) add(center + Offset(dx * grid.px, dy * grid.px))
        }
    }
}

private fun DrawScope.drawCrossStar(at: Offset, grid: PixelGrid) {
    drawRect(ArtColors.pixelStar, at, grid.block)
    listOf(Offset(grid.px, 0f), Offset(-grid.px, 0f), Offset(0f, grid.px), Offset(0f, -grid.px)).forEach {
        drawRect(ArtColors.pixelStarGlow, at + it, grid.block)
    }
}

/** Una columna de bloques por cada [tops]: el perfil escalonado de una montaña. */
private fun DrawScope.drawHills(tops: List<Int>, color: Color, grid: PixelGrid) {
    tops.forEachIndexed { col, top ->
        drawRect(color, grid.at(col, top), Size(grid.px, size.height - top * grid.px))
    }
}

/** Una estrella: dónde, de qué tamaño y qué tan brillante. */
private data class Star(val at: Offset, val radius: Float, val alpha: Float)

/**
 * Galaxia (encima de [galaxyArt]): un brazo de estrellas siguiendo la nebulosa, estrellas
 * sueltas, algunas brillantes con halo y un planeta con anillo abajo.
 */
internal fun CacheDrawScope.galaxyOverlay(): DrawScope.() -> Unit {
    val w = size.width
    val h = size.height
    val random = Random(SEED + 1)
    val stars = galaxyArm(random) + List(STARS * 3) {
        Star(
            Offset(random.nextFloat() * w, random.nextFloat() * h),
            (0.5f + random.nextFloat()).dp.toPx(),
            0.3f + random.nextFloat() * 0.7f,
        )
    }
    val bright = List(BRIGHT_STARS) { Offset(w * random.nextFloat(), h * random.nextFloat() * 0.7f) }
    val halo = 10.dp.toPx()
    val halos = bright.map { Brush.radialGradient(listOf(ArtColors.galaxyHalo, Color.Transparent), it, halo) }
    val brightRadius = 1.6.dp.toPx()
    val planet = ringedPlanet()
    return {
        stars.forEach { drawCircle(ArtColors.galaxyStar.copy(alpha = it.alpha), it.radius, it.at) }
        bright.forEachIndexed { i, at ->
            drawCircle(halos[i], halo, at)
            drawCircle(ArtColors.galaxyStar, brightRadius, at)
        }
        planet()
    }
}

/** El brazo de la galaxia: estrellas que siguen la franja de la nebulosa, más juntas en su centro. */
private fun CacheDrawScope.galaxyArm(random: Random): List<Star> {
    val w = size.width
    val h = size.height
    return List(GALAXY_ARM) {
        val x = random.nextFloat()
        val spread = (random.nextFloat() + random.nextFloat() - 1f) * 0.08f
        val y = h * (0.64f - 0.26f * x + spread)
        Star(Offset(x * w, y), (0.4f + random.nextFloat() * 0.9f).dp.toPx(), 0.25f + random.nextFloat() * 0.6f)
    }
}

/** Un planeta en degradado con su anillo, abajo a la derecha. Devuelve cómo dibujarlo. */
private fun CacheDrawScope.ringedPlanet(): DrawScope.() -> Unit {
    val center = Offset(size.width * 0.78f, size.height * 0.84f)
    val radius = size.width * 0.16f
    val brush = Brush.linearGradient(
        listOf(ArtColors.galaxyPlanetLight, ArtColors.galaxyPlanetDark),
        center - Offset(radius, radius),
        center + Offset(radius, radius),
    )
    val ring = Stroke(2.dp.toPx())
    val ringSize = Size(radius * 3.2f, radius * 0.9f)
    val ringTopLeft = center - Offset(ringSize.width / 2, ringSize.height / 2)
    return {
        rotate(PLANET_TILT, center) {
            drawCircle(brush, radius, center)
            drawOval(ArtColors.galaxyRing, ringTopLeft, ringSize, style = ring)
        }
    }
}

private val CRATERS = listOf(-2 to -1, 2 to 2, 1 to -3, -3 to 3)
private const val PLANET_TILT = -18f
