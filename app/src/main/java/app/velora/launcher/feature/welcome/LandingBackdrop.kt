package app.velora.launcher.feature.welcome

import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import app.velora.launcher.core.designsystem.theme.BrandColors
import app.velora.launcher.core.designsystem.theme.VeloraTheme

/**
 * Fondo del landing y de la introducción: negro con dos luces suaves (lila y turquesa) que se
 * mueven muy despacio, como una aurora. Se lee en la fase de dibujo: animarlo no recompone. Con
 * "Quitar animaciones", quietas.
 */
@Composable
fun Modifier.landingBackground(): Modifier {
    val motion = VeloraTheme.motion
    if (motion.reduced) return drawBehind { drawAurora(HALF) }
    val drift = rememberInfiniteTransition(
        label = "landing",
    ).animateFloat(0f, 1f, motion.ambientFloat(), label = "drift")
    return drawBehind { drawAurora(drift.value) }
}

private fun DrawScope.drawAurora(t: Float) {
    drawRect(BrandColors.background)
    val a = Offset(size.width * (0.15f + 0.2f * t), size.height * (0.22f + 0.06f * t))
    val b = Offset(size.width * (0.9f - 0.2f * t), size.height * (0.62f - 0.05f * t))
    val ra = size.width * 0.8f
    val rb = size.width * 0.75f
    drawCircle(Brush.radialGradient(listOf(BrandColors.auroraA, Color.Transparent), a, ra), ra, a)
    drawCircle(Brush.radialGradient(listOf(BrandColors.auroraB, Color.Transparent), b, rb), rb, b)
}

private const val HALF = 0.5f
