package app.velora.launcher.feature.hidden

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import kotlin.math.abs

/**
 * Acceso rápido a las apps ocultas (ADR-034): **deslizar con dos dedos hacia arriba** o
 * **pellizcar hacia adentro** con dos dedos en el inicio.
 *
 * Solo reacciona con dos o más dedos. Mira los toques en el pase `Initial` (antes que los hijos y
 * que los gestos del inicio) y, en cuanto hay un segundo dedo, los consume: así el arrastre de un
 * dedo (abrir la lista), mantener presionado (menú) y el doble toque (bloquear) se cancelan en vez
 * de dispararse. Con un solo dedo no toca nada. Aplicarlo **antes** (por fuera) de los gestos del inicio.
 */
@Composable
fun Modifier.hiddenAppsGesture(onOpen: () -> Unit): Modifier {
    val haptics = LocalHapticFeedback.current
    val open by rememberUpdatedState(onOpen)
    return pointerInput(Unit) {
        val limits = GestureLimits(swipePx = SwipeDistance.toPx(), minSpanPx = MinPinchSpan.toPx())
        awaitEachGesture {
            awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
            if (awaitTwoFingerGesture(limits)) {
                haptics.performHapticFeedback(HapticFeedbackType.GestureThresholdActivate)
                open()
            }
        }
    }
}

/**
 * Sigue el gesto hasta que se levantan todos los dedos. true si con dos dedos el centro subió
 * lo pedido (más vertical que horizontal) o la separación se redujo a menos de [PINCH_RATIO].
 */
private suspend fun AwaitPointerEventScope.awaitTwoFingerGesture(limits: GestureLimits): Boolean {
    var start: TwoFingers? = null
    var recognized = false
    while (true) {
        val event = awaitPointerEvent(PointerEventPass.Initial)
        val pressed = event.changes.filter { it.pressed }
        if (pressed.isEmpty()) return recognized
        if (pressed.size < 2) continue
        // Desde el segundo dedo el gesto es nuestro: nada de abrir la lista ni el menú.
        event.changes.forEach(PointerInputChange::consume)
        val now = TwoFingers.of(pressed)
        val first = start ?: now.also { start = it }
        if (!recognized && isOpenGesture(first, now, limits)) recognized = true
    }
}

private fun isOpenGesture(start: TwoFingers, now: TwoFingers, limits: GestureLimits): Boolean {
    val delta = now.center - start.center
    val swipeUp = -delta.y >= limits.swipePx && abs(delta.y) > abs(delta.x)
    val pinchIn = start.span > limits.minSpanPx && now.span < start.span * PINCH_RATIO
    return swipeUp || pinchIn
}

/** Distancias del gesto ya en píxeles. */
private class GestureLimits(val swipePx: Float, val minSpanPx: Float)

/** Centro y separación de los dos primeros dedos. */
private class TwoFingers(val center: Offset, val span: Float) {
    companion object {
        fun of(pressed: List<PointerInputChange>): TwoFingers {
            val a = pressed[0].position
            val b = pressed[1].position
            return TwoFingers(center = (a + b) / 2f, span = (a - b).getDistance())
        }
    }
}

/** Lo bastante largo para no confundirlo con apoyar dos dedos sin querer. */
private val SwipeDistance = 72.dp

/** Pellizco: los dedos terminan a menos del 60 % de la separación inicial. */
private const val PINCH_RATIO = 0.6f

/** Por debajo de esto los dedos ya empezaron juntos: no es un pellizco claro. */
private val MinPinchSpan = 48.dp
