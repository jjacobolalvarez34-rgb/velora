package app.velora.launcher.core.designsystem.motion

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import app.velora.launcher.core.designsystem.theme.VeloraTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val EntranceOffset = 8.dp

/** Al volver de una app o desbloquear, el inicio sube un poco más: se nota, pero sin exagerar. */
private val ReturnOffset = 28.dp

/**
 * Cuántas veces se volvió al inicio. Cada cambio repite la entrada escalonada (volver de una app,
 * encender o desbloquear el teléfono). 0 = el primer arranque.
 */
val LocalEntranceGeneration = compositionLocalOf { 0 }

/**
 * Entrada del inicio (ESTILO §5.3): fundido + subida, escalonada por [index]. Se repite con
 * [LocalEntranceGeneration]. Solo anima propiedades de dibujo; no bloquea toques. Con "Quitar
 * animaciones" aparece sin moverse.
 */
@Composable
fun StaggeredEntrance(index: Int, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val motion = VeloraTheme.motion
    val generation = LocalEntranceGeneration.current
    val alpha = remember { Animatable(0f) }
    val lift = remember { Animatable(1f) }
    val offset = if (generation == 0) EntranceOffset else ReturnOffset
    LaunchedEffect(generation) {
        if (motion.reduced) {
            alpha.snapTo(1f)
            lift.snapTo(0f)
            return@LaunchedEffect
        }
        alpha.snapTo(0f)
        lift.snapTo(1f)
        // Escalonado puramente visual: nada espera a esta animación.
        delay(index * motion.staggerMillis.toLong())
        launch { alpha.animateTo(1f, motion.effectDefault()) }
        lift.animateTo(0f, motion.spatialDefault())
    }
    Box(
        modifier = modifier.graphicsLayer {
            this.alpha = alpha.value
            translationY = lift.value * offset.toPx()
        },
    ) {
        content()
    }
}
