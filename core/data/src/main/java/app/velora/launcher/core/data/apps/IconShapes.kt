package app.velora.launcher.core.data.apps

import android.graphics.Path
import app.velora.launcher.core.model.IconShape
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sign
import kotlin.math.sin

/**
 * Matemática pura de las formas de ícono (sin `android.graphics`, para probarla en la JVM).
 * Las coordenadas van de -1 a 1 alrededor del centro.
 */
internal object IconShapeMath {

    /** Exponente de la superelipse: 2 es un círculo, 4 un "squircle" suave, ∞ un cuadrado. */
    const val SQUIRCLE_EXPONENT = 4.0

    /** Radio de las esquinas del cuadrado redondeado, en fracción del lado. */
    const val ROUNDED_RADIUS = 0.28f

    /** Esquina "recta" de la lágrima: casi en punta, con un mínimo de suavizado óptico. */
    const val TEARDROP_TIP_RADIUS = 0.06f

    private const val FULL_TURN = 2 * Math.PI

    /**
     * Puntos (x, y intercalados) de la superelipse |x|ⁿ + |y|ⁿ = 1 con [steps] vértices.
     * Se parametriza con cos/sin elevados a 2/n: reparte bien los puntos en las curvas.
     */
    fun superellipse(steps: Int, exponent: Double = SQUIRCLE_EXPONENT): FloatArray {
        val points = FloatArray(steps * 2)
        val power = 2.0 / exponent
        for (i in 0 until steps) {
            val angle = FULL_TURN * i / steps
            val c = cos(angle)
            val s = sin(angle)
            points[i * 2] = (sign(c) * abs(c).pow(power)).toFloat()
            points[i * 2 + 1] = (sign(s) * abs(s).pow(power)).toFloat()
        }
        return points
    }

    /**
     * Radios de las esquinas (8 valores: x/y de arriba-izq., arriba-der., abajo-der., abajo-izq.,
     * el orden de `Path.addRoundRect`) para un lado [size]; null si la forma no es un rectángulo
     * redondeado (círculo y squircle se dibujan aparte).
     */
    fun cornerRadii(shape: IconShape, size: Float): FloatArray? = when (shape) {
        IconShape.Circle, IconShape.Squircle -> null

        IconShape.Rounded -> FloatArray(CORNERS) { size * ROUNDED_RADIUS }

        IconShape.Teardrop -> {
            val round = size / 2f
            val tip = size * TEARDROP_TIP_RADIUS
            floatArrayOf(round, round, round, round, tip, tip, round, round)
        }
    }

    private const val CORNERS = 8
}

/** Vértices del squircle: con 128 el borde se ve continuo hasta en los íconos grandes. */
private const val SQUIRCLE_STEPS = 128

/** La forma elegida como `Path`, en el cuadrado [left], [top] de lado [size]. */
internal fun iconShapePath(shape: IconShape, left: Float, top: Float, size: Float): Path {
    val path = Path()
    val half = size / 2f
    val cx = left + half
    val cy = top + half
    val radii = IconShapeMath.cornerRadii(shape, size)
    when {
        radii != null -> path.addRoundRect(left, top, left + size, top + size, radii, Path.Direction.CW)

        shape == IconShape.Squircle -> {
            val points = IconShapeMath.superellipse(SQUIRCLE_STEPS)
            path.moveTo(cx + points[0] * half, cy + points[1] * half)
            for (i in 1 until points.size / 2) path.lineTo(cx + points[i * 2] * half, cy + points[i * 2 + 1] * half)
            path.close()
        }

        else -> path.addCircle(cx, cy, half, Path.Direction.CW)
    }
    return path
}

/** La forma centrada en un lienzo de [canvasSize], reducida a [fraction] de su lado. */
internal fun centeredShapePath(shape: IconShape, canvasSize: Int, fraction: Float): Path {
    val size = canvasSize * fraction
    val offset = (canvasSize - size) / 2f
    return iconShapePath(shape, offset, offset, size)
}
