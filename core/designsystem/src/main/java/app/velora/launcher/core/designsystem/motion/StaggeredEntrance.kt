package app.velora.launcher.core.designsystem.motion

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import app.velora.launcher.core.designsystem.theme.VeloraTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val EntranceOffset = 8.dp

/**
 * Entrada del arranque en frío (ESTILO §5.3): fundido + subida de 8 dp, escalonada por [index].
 * Solo anima propiedades de dibujo; no bloquea toques.
 */
@Composable
fun StaggeredEntrance(index: Int, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val motion = VeloraTheme.motion
    val alpha = remember { Animatable(0f) }
    val lift = remember { Animatable(1f) }
    LaunchedEffect(Unit) {
        // Escalonado puramente visual: nada espera a esta animación.
        delay(index * motion.staggerMillis.toLong())
        launch { alpha.animateTo(1f, motion.effectDefault()) }
        lift.animateTo(0f, motion.spatialDefault())
    }
    Box(
        modifier = modifier.graphicsLayer {
            this.alpha = alpha.value
            translationY = lift.value * EntranceOffset.toPx()
        },
    ) {
        content()
    }
}
