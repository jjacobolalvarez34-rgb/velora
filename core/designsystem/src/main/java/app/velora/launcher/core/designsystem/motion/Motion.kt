package app.velora.launcher.core.designsystem.motion

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Immutable

/**
 * Tokens de movimiento (ESTILO §5.2). Único lugar donde se escriben `spring(...)` y `tween(...)`.
 *
 * Con [reduced] = true ("Quitar animaciones" del sistema) lo espacial salta sin movimiento
 * y los efectos duran ≤ 50 ms (ESTILO §5.1, regla 4).
 */
@Immutable
class VeloraMotion(val reduced: Boolean) {

    /** Letras del alfabeto, presionar una fila. */
    fun <T> spatialFast(): FiniteAnimationSpec<T> =
        if (reduced) snap() else spring(dampingRatio = 0.9f, stiffness = 1400f)

    /** Entrar/salir de la lista completa, reordenar. */
    fun <T> spatialDefault(): FiniteAnimationSpec<T> =
        if (reduced) snap() else spring(dampingRatio = 0.85f, stiffness = 700f)

    /** Pop-ups, carpetas. */
    fun <T> spatialHero(): FiniteAnimationSpec<T> =
        if (reduced) snap() else spring(dampingRatio = 0.75f, stiffness = 380f)

    /** Alpha de elementos pequeños. */
    fun <T> effectFast(): FiniteAnimationSpec<T> =
        tween(durationMillis = if (reduced) REDUCED_EFFECT_MS else 90, easing = FastOutSlowInEasing)

    /** Fundidos de secciones. */
    fun <T> effectDefault(): FiniteAnimationSpec<T> =
        tween(durationMillis = if (reduced) REDUCED_EFFECT_MS else 180, easing = FastOutSlowInEasing)

    /** Escala al presionar una fila. */
    val pressScale: Float get() = if (reduced) 1f else 0.96f

    /** Retraso entre filas en la entrada escalonada del arranque en frío (ESTILO §5.3). */
    val staggerMillis: Int get() = if (reduced) 0 else 20

    private companion object {
        const val REDUCED_EFFECT_MS = 50
    }
}
