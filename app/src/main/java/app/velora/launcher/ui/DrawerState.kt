package app.velora.launcher.ui

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.animate
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.unit.Velocity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * Apertura de la lista completa: 0 = inicio, 1 = lista. Sigue al dedo 1:1 y al soltar la toma un
 * resorte con la velocidad del dedo (ESTILO §5.1, reglas 2 y 3). Leer [progress] solo en
 * `graphicsLayer {}` para no recomponer en cada frame.
 */
@Stable
class DrawerState(private val scope: CoroutineScope) {

    var progress by mutableFloatStateOf(0f)
        private set

    /** Distancia de arrastre que equivale a abrir del todo; la fija el layout. */
    var dragDistancePx: Float = 1f

    /** Lo fija la UI desde los tokens de movimiento (respeta "Quitar animaciones"). */
    var spec: AnimationSpec<Float>? = null

    private var settleJob: Job? = null

    val isOpen: Boolean get() = progress > 0f

    /** Arrastre en píxeles: negativo (hacia arriba) abre. */
    fun dragBy(deltaPx: Float) {
        settleJob?.cancel()
        progress = (progress - deltaPx / dragDistancePx).coerceIn(0f, 1f)
    }

    /** Al soltar: decide abrir o cerrar según la velocidad (px/s) o, si es lenta, la posición. */
    fun settle(velocityPx: Float) {
        val velocity = -velocityPx / dragDistancePx
        val target = when {
            velocity > FLING_THRESHOLD -> 1f
            velocity < -FLING_THRESHOLD -> 0f
            progress > HALF_OPEN -> 1f
            else -> 0f
        }
        animateTo(target, velocity)
    }

    /**
     * Atrás predictivo: mientras el gesto avanza la lista baja un poco y se aclara, para que se
     * vea qué va a pasar; [fraction] es el avance del gesto (0..1).
     */
    fun peekBack(fraction: Float) {
        settleJob?.cancel()
        progress = 1f - fraction.coerceIn(0f, 1f) * BACK_PEEK
    }

    fun open() = animateTo(1f)

    fun close() = animateTo(0f)

    /** Sin animación: al irse a otra app el inicio queda listo para volver (ESTILO §5.3). */
    fun snapClosed() {
        settleJob?.cancel()
        progress = 0f
    }

    private fun animateTo(target: Float, initialVelocity: Float = 0f) {
        settleJob?.cancel()
        val animationSpec = spec
        if (animationSpec == null) {
            progress = target
            return
        }
        settleJob = scope.launch {
            animate(progress, target, initialVelocity, animationSpec) { value, _ ->
                progress = value.coerceIn(0f, 1f)
            }
        }
    }

    /**
     * Cerrar deslizando hacia abajo cuando la lista ya está arriba del todo, y volver a abrir
     * si el dedo sube antes de soltar.
     */
    val nestedScroll = object : NestedScrollConnection {
        override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
            if (source != NestedScrollSource.UserInput || available.y >= 0f || progress >= 1f) return Offset.Zero
            dragBy(available.y)
            return Offset(0f, available.y)
        }

        override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
            if (source != NestedScrollSource.UserInput || available.y <= 0f) return Offset.Zero
            dragBy(available.y)
            return Offset(0f, available.y)
        }

        override suspend fun onPreFling(available: Velocity): Velocity {
            if (progress >= 1f) return Velocity.Zero
            settle(available.y)
            return available
        }
    }

    private companion object {
        /** Aperturas por segundo a partir de las cuales el gesto cuenta como lanzamiento. */
        const val FLING_THRESHOLD = 1.5f

        /** Al soltar despacio: pasada la mitad, abre. */
        const val HALF_OPEN = 0.5f

        /** Cuánto se cierra la lista siguiendo el gesto de atrás antes de soltar. */
        const val BACK_PEEK = 0.35f
    }
}

@Composable
fun rememberDrawerState(): DrawerState {
    val scope = rememberCoroutineScope()
    return remember(scope) { DrawerState(scope) }
}
