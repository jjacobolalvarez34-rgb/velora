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

// Temas tranquilos: un degradado y una o dos formas (ADR-018).

/** Negro puro con un brillo apenas visible arriba. */
internal fun CacheDrawScope.night(): DrawResult {
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
internal fun CacheDrawScope.paper(): DrawResult {
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
internal fun CacheDrawScope.sakura(): DrawResult {
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
internal fun CacheDrawScope.forest(): DrawResult {
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
internal fun CacheDrawScope.aurora(): DrawResult {
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

/** Blanco con una sombra apenas visible abajo: nada compite con el texto. */
internal fun CacheDrawScope.snow(): DrawResult {
    val shade = Brush.verticalGradient(listOf(Color.Transparent, ArtColors.snowShade))
    return onDrawBehind {
        drawRect(ArtColors.snowBase)
        drawRect(shade)
    }
}

/** Marrón tostado con un brillo cálido arriba, como luz de una ventana. */
internal fun CacheDrawScope.coffee(): DrawResult {
    val base = Brush.verticalGradient(listOf(ArtColors.coffeeTop, ArtColors.coffeeBottom))
    val glow = Brush.radialGradient(
        listOf(ArtColors.coffeeGlow, Color.Transparent),
        center = Offset(size.width * 0.8f, size.height * 0.05f),
        radius = size.width * 0.9f,
    )
    return onDrawBehind {
        drawRect(base)
        drawRect(glow)
    }
}

/** Durazno a rosa, un sol grande y bajo, y una línea de horizonte. */
internal fun CacheDrawScope.sunset(): DrawResult {
    val w = size.width
    val h = size.height
    val sky = Brush.verticalGradient(listOf(ArtColors.sunsetTop, ArtColors.sunsetBottom))
    val horizon = h * 0.72f
    val stroke = 1.dp.toPx()
    return onDrawBehind {
        drawRect(sky)
        drawCircle(ArtColors.sunsetSun, radius = w * 0.26f, center = Offset(w * 0.5f, horizon))
        drawLine(ArtColors.sunsetHorizon, Offset(0f, horizon), Offset(w, horizon), stroke)
    }
}

/** Azul petróleo con dos olas finas abajo. */
internal fun CacheDrawScope.ocean(): DrawResult {
    val w = size.width
    val h = size.height
    val sea = Brush.verticalGradient(listOf(ArtColors.oceanTop, ArtColors.oceanBottom))
    val waves = listOf(0.62f, 0.7f).map { y ->
        Path().apply {
            moveTo(0f, h * y)
            cubicTo(w * 0.3f, h * (y - 0.04f), w * 0.6f, h * (y + 0.04f), w, h * y)
        }
    }
    val stroke = Stroke(1.5.dp.toPx())
    return onDrawBehind {
        drawRect(sea)
        waves.forEach { drawPath(it, ArtColors.oceanWave, style = stroke) }
    }
}

/** Matcha: crema verdosa con un cuenco abstracto (dos arcos) y una hoja fina. */
internal fun CacheDrawScope.matcha(): DrawResult {
    val w = size.width
    val h = size.height
    val base = Brush.verticalGradient(listOf(ArtColors.matchaTop, ArtColors.matchaBottom))
    val bowl = Offset(w * 0.5f, h * 0.8f)
    val stroke = Stroke(1.5.dp.toPx())
    val leaf = Path().apply {
        moveTo(w * 0.12f, h * 0.22f)
        quadraticTo(w * 0.32f, h * 0.08f, w * 0.48f, h * 0.2f)
        quadraticTo(w * 0.3f, h * 0.3f, w * 0.12f, h * 0.22f)
    }
    return onDrawBehind {
        drawRect(base)
        drawCircle(ArtColors.matchaSoft, w * 0.42f, bowl)
        drawArc(
            ArtColors.matchaLine,
            0f,
            180f,
            false,
            bowl - Offset(w * 0.3f, w * 0.3f),
            Size(w * 0.6f, w * 0.6f),
            style = stroke,
        )
        drawPath(leaf, ArtColors.matchaLine, style = stroke)
    }
}

/** Lavanda: lila claro con tres manchas suaves de color, como acuarela. */
internal fun CacheDrawScope.lavender(): DrawResult {
    val w = size.width
    val h = size.height
    val base = Brush.verticalGradient(listOf(ArtColors.lavenderTop, ArtColors.lavenderBottom))
    val blobs = listOf(
        Triple(Offset(w * 0.15f, h * 0.15f), w * 0.6f, ArtColors.lavenderBlobA),
        Triple(Offset(w * 0.9f, h * 0.45f), w * 0.55f, ArtColors.lavenderBlobB),
        Triple(Offset(w * 0.3f, h * 0.95f), w * 0.7f, ArtColors.lavenderBlobA),
    ).map { (at, r, c) -> Brush.radialGradient(listOf(c, Color.Transparent), at, r) }
    return onDrawBehind {
        drawRect(base)
        blobs.forEach { drawRect(it) }
    }
}
