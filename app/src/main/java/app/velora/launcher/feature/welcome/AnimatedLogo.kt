package app.velora.launcher.feature.welcome

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.InfiniteTransition
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.velora.launcher.R
import app.velora.launcher.core.designsystem.theme.BrandColors
import app.velora.launcher.core.designsystem.theme.VeloraTheme
import kotlinx.coroutines.launch

private val FloatDistance = 6.dp
private const val FLOAT_ROTATION_DEGREES = 2f
private const val ENTRANCE_START_SCALE = 0.8f

/** Ancho de la franja de brillo respecto al logo. */
private const val SHINE_WIDTH = 0.35f

/**
 * El logo de Velora vivo: entra con un resorte, flota despacio y un brillo plateado recorre la
 * cinta (solo sobre sus píxeles: capa aparte + `BlendMode.SrcAtop`). Con "Quitar animaciones",
 * quieto. Solo anima propiedades de dibujo.
 */
@Composable
fun AnimatedLogo(logoSize: Dp, modifier: Modifier = Modifier) {
    val motion = VeloraTheme.motion
    val entrance = remember { Animatable(if (motion.reduced) 1f else 0f) }
    LaunchedEffect(Unit) {
        launch { entrance.animateTo(1f, motion.spatialHero()) }
    }
    val float = ambient(motion.reduced) { animateFloat(-1f, 1f, motion.ambientFloat(), label = "float") }
    val shine =
        ambient(motion.reduced) { animateFloat(-SHINE_WIDTH, 1f + SHINE_WIDTH, motion.ambientSweep(), label = "shine") }

    Image(
        painter = painterResource(R.drawable.velora_logo),
        contentDescription = null,
        modifier = modifier
            .size(logoSize)
            .graphicsLayer {
                val e = entrance.value
                alpha = e.coerceIn(0f, 1f)
                val scale = ENTRANCE_START_SCALE + (1f - ENTRANCE_START_SCALE) * e
                scaleX = scale
                scaleY = scale
                translationY = float.value * FloatDistance.toPx()
                rotationZ = float.value * FLOAT_ROTATION_DEGREES
                compositingStrategy = CompositingStrategy.Offscreen
            }
            .shine(shine, enabled = !motion.reduced),
    )
}

/** Franja plateada que cruza en diagonal, solo sobre los píxeles del logo. */
private fun Modifier.shine(progress: State<Float>, enabled: Boolean) = drawWithContent {
    drawContent()
    if (!enabled) return@drawWithContent
    val x = progress.value * size.width
    val band = SHINE_WIDTH * size.width
    drawRect(
        brush = Brush.linearGradient(
            colors = listOf(Color.Transparent, BrandColors.silver, Color.Transparent),
            start = Offset(x - band, 0f),
            end = Offset(x + band, size.height),
        ),
        blendMode = BlendMode.SrcAtop,
    )
}

/** Una animación infinita, o un valor fijo en 0 si el usuario quitó las animaciones. */
@Composable
private fun ambient(reduced: Boolean, animation: @Composable InfiniteTransition.() -> State<Float>): State<Float> {
    if (reduced) return remember { mutableFloatStateOf(0f) }
    return rememberInfiniteTransition(label = "logo").animation()
}
