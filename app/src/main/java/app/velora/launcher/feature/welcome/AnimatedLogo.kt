package app.velora.launcher.feature.welcome

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.InfiniteTransition
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.velora.launcher.core.designsystem.theme.BrandColors
import app.velora.launcher.core.designsystem.theme.VeloraTheme
import kotlinx.coroutines.launch

private val FloatDistance = 6.dp

/**
 * La marca de Velora (ADR-029): una "V" hecha de dos cintas de luz que caen y se juntan, como
 * agua, con una estrella pequeña arriba a la derecha. Vectorial: nítida a cualquier tamaño.
 * Al entrar, las cintas se dibujan; luego flota despacio y el halo respira. Con "Quitar
 * animaciones", quieta y completa.
 */
@Composable
fun AnimatedLogo(logoSize: Dp, modifier: Modifier = Modifier) {
    val motion = VeloraTheme.motion
    val draw = remember { Animatable(if (motion.reduced) 1f else 0f) }
    LaunchedEffect(Unit) { launch { draw.animateTo(1f, motion.spatialHero()) } }
    val float = ambient(motion.reduced) { animateFloat(-1f, 1f, motion.ambientFloat(), label = "float") }
    val glow = ambient(motion.reduced) { animateFloat(0f, 1f, motion.ambientSweep(), label = "glow") }
    Canvas(
        modifier
            .size(logoSize)
            .graphicsLayer { translationY = float.value * FloatDistance.toPx() },
    ) {
        drawHalo(glow.value)
        drawMark(draw.value.coerceIn(0f, 1f))
    }
}

/** Un halo suave detrás, que respira entre 70 % y 100 %. */
private fun DrawScope.drawHalo(breath: Float) {
    val radius = size.minDimension * (HALO_MIN + HALO_RANGE * breath)
    drawCircle(
        Brush.radialGradient(listOf(BrandColors.glow, Color.Transparent), center, radius),
        radius,
        center,
    )
}

/** Las dos cintas (la izquierda más ancha) y la estrella; [progress] 0..1 las va dibujando. */
private fun DrawScope.drawMark(progress: Float) {
    val w = size.width
    val left = Path().apply {
        moveTo(w * 0.2f, w * 0.22f)
        cubicTo(w * 0.28f, w * 0.48f, w * 0.38f, w * 0.68f, w * 0.5f, w * 0.8f)
    }
    val right = Path().apply {
        moveTo(w * 0.8f, w * 0.3f)
        cubicTo(w * 0.72f, w * 0.5f, w * 0.62f, w * 0.68f, w * 0.5f, w * 0.8f)
    }
    val brush = Brush.linearGradient(
        listOf(BrandColors.silver, BrandColors.lilac),
        Offset(0f, 0f),
        Offset(w, w),
    )
    drawPartial(left, progress, brush, w * LEFT_WIDTH)
    drawPartial(right, progress, brush, w * RIGHT_WIDTH)
    val star = progress.coerceIn(STAR_START, 1f).let { (it - STAR_START) / (1f - STAR_START) }
    if (star > 0f) drawStar(Offset(w * 0.8f, w * 0.17f), w * STAR_SIZE * star)
}

/** Dibuja solo el primer [fraction] del trazo: así la cinta "cae" al entrar. */
private fun DrawScope.drawPartial(path: Path, fraction: Float, brush: Brush, width: Float) {
    if (fraction <= 0f) return
    val measure = PathMeasure().apply { setPath(path, false) }
    val partial = Path()
    measure.getSegment(0f, measure.length * fraction, partial, true)
    drawPath(partial, brush, style = Stroke(width = width, cap = StrokeCap.Round))
}

/** Estrella de cuatro puntas, fina, como un destello. */
private fun DrawScope.drawStar(at: Offset, radius: Float) {
    val thin = radius * STAR_WAIST
    val path = Path().apply {
        moveTo(at.x, at.y - radius)
        quadraticTo(at.x + thin, at.y - thin, at.x + radius, at.y)
        quadraticTo(at.x + thin, at.y + thin, at.x, at.y + radius)
        quadraticTo(at.x - thin, at.y + thin, at.x - radius, at.y)
        quadraticTo(at.x - thin, at.y - thin, at.x, at.y - radius)
        close()
    }
    drawPath(path, BrandColors.silver)
    drawCircle(BrandColors.glow, radius * STAR_GLOW, at)
}

/** Una animación infinita, o un valor fijo en 0 si el usuario quitó las animaciones. */
@Composable
private fun ambient(reduced: Boolean, animation: @Composable InfiniteTransition.() -> State<Float>): State<Float> {
    if (reduced) return remember { mutableFloatStateOf(0f) }
    return rememberInfiniteTransition(label = "logo").animation()
}

private const val HALO_MIN = 0.42f
private const val HALO_RANGE = 0.12f
private const val LEFT_WIDTH = 0.11f
private const val RIGHT_WIDTH = 0.065f
private const val STAR_SIZE = 0.09f
private const val STAR_WAIST = 0.18f
private const val STAR_START = 0.7f
private const val STAR_GLOW = 1.6f
