package app.velora.launcher.core.data.apps

import app.velora.launcher.core.model.IconShape
import java.util.Random
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Pixelado del estilo Píxel: la marca ya dibujada se reduce a una cuadrícula de bloques nítidos.
 * Matemática pura sobre los píxeles ARGB (sin `android.graphics`).
 */
internal object PixelArt {

    /** 16×16 sobre las 24 unidades del glifo: un trazo de 2 queda en 1–2 bloques. */
    const val GRID = 16

    /** Una celda se enciende con al menos esta opacidad media. */
    const val THRESHOLD = 0.4f

    private const val CHANNEL = 255f
    private const val ALPHA_SHIFT = 24
    private const val PIXEL_CENTER = 0.5f

    /**
     * Celdas encendidas (fila a fila) de la cuadrícula [window] sobre un lienzo de [width]×[width]
     * ([pixels] en ARGB).
     */
    fun cells(pixels: IntArray, width: Int, window: GridWindow, threshold: Float = THRESHOLD): BooleanArray {
        val grid = window.grid
        val sums = FloatArray(grid * grid)
        val counts = IntArray(grid * grid)
        val cell = window.area / grid
        for (y in 0 until width) {
            val row = cellOf(y, window.start, cell, grid)
            if (row < 0) continue
            for (x in 0 until width) {
                val col = cellOf(x, window.start, cell, grid)
                if (col < 0) continue
                sums[row * grid + col] += (pixels[y * width + x] ushr ALPHA_SHIFT) / CHANNEL
                counts[row * grid + col]++
            }
        }
        return BooleanArray(grid * grid) { counts[it] > 0 && sums[it] / counts[it] >= threshold }
    }

    /** Celda a la que cae el centro del píxel [index]; -1 si queda fuera de la cuadrícula. */
    private fun cellOf(index: Int, start: Float, cell: Float, grid: Int): Int {
        val position = index + PIXEL_CENTER - start
        if (position < 0f) return -1
        val cellIndex = (position / cell).toInt()
        return if (cellIndex < grid) cellIndex else -1
    }
}

/** Cuadrícula de [grid]×[grid] celdas sobre el cuadrado de lado [area] px que empieza en [start] px. */
internal class GridWindow(val start: Float, val area: Float, val grid: Int = PixelArt.GRID)

/** Una aguada de la acuarela: [radii] del borde por ángulo (1 = medio lado) y su corrimiento. */
internal class Wash(val radii: FloatArray, val dx: Float, val dy: Float)

/**
 * Manchas orgánicas del estilo Acuarela: el borde de la forma elegida con un ruido suave y
 * determinista por app (semilla = hash del paquete), siempre hacia adentro para no salirse.
 * Coordenadas de -1 a 1 alrededor del centro, como [IconShapeMath].
 */
internal object IconBlob {

    /** Cuánto puede entrar el borde (fracción del radio de la forma). */
    const val ROUGHNESS = 0.09f

    /** Las aguadas se dibujan un poco más chicas que la forma para que el corrimiento quepa. */
    const val WASH_SCALE = 0.95f

    /** Corrimiento máximo de cada aguada (en medio lado). */
    const val WASH_DRIFT = 0.04f

    const val WASHES = 3

    private const val HARMONICS = 4
    private const val MIN_FREQUENCY = 2
    private const val FREQUENCY_CHOICES = 2
    private const val LAYER_SALT = 7919L
    private const val FULL_TURN = 2 * Math.PI

    /**
     * Distancia del centro al borde de [shape] en la dirección [angle] (radianes, y hacia abajo
     * como en pantalla). La esquina en punta de la lágrima es la de abajo a la derecha.
     */
    fun boundary(shape: IconShape, angle: Double): Float {
        val c = cos(angle)
        val s = sin(angle)
        return when (shape) {
            IconShape.Circle -> 1f

            IconShape.Squircle -> squircle(abs(c), abs(s))

            IconShape.Rounded -> roundedSquare(abs(c), abs(s), 2 * IconShapeMath.ROUNDED_RADIUS)

            IconShape.Teardrop -> {
                val tip = c > 0 && s > 0
                roundedSquare(abs(c), abs(s), if (tip) 2 * IconShapeMath.TEARDROP_TIP_RADIUS else 1f)
            }
        }
    }

    /** Superelipse |x|ⁿ + |y|ⁿ = 1 cortada por el rayo (ux, uy): r = (uxⁿ + uyⁿ)^(-1/n). */
    private fun squircle(ux: Double, uy: Double): Float {
        val n = IconShapeMath.SQUIRCLE_EXPONENT
        return (ux.pow(n) + uy.pow(n)).pow(-1.0 / n).toFloat()
    }

    /** Las [WASHES] aguadas de una app, con [steps] puntos de borde cada una. */
    fun washes(shape: IconShape, seed: Int, steps: Int): List<Wash> = List(WASHES) { layer ->
        val random = Random(seed * LAYER_SALT + layer)
        val drift = { (random.nextFloat() * 2 - 1) * WASH_DRIFT }
        Wash(outline(shape, random, steps), drift(), drift())
    }

    /** Borde con ruido: suma de armónicos enteros (así cierra sin costura), normalizada a 0–1. */
    private fun outline(shape: IconShape, random: Random, steps: Int): FloatArray {
        val frequencies = IntArray(HARMONICS) { MIN_FREQUENCY + it + random.nextInt(FREQUENCY_CHOICES) }
        val amplitudes = FloatArray(HARMONICS) { (random.nextFloat() + 1f) / (it + 1) }
        val phases = DoubleArray(HARMONICS) { random.nextDouble() * FULL_TURN }
        val total = amplitudes.sum()
        return FloatArray(steps) { i ->
            val angle = FULL_TURN * i / steps
            var noise = 0.0
            for (k in 0 until HARMONICS) noise += amplitudes[k] * sin(frequencies[k] * angle + phases[k])
            val inward = ((noise / total + 1) / 2).toFloat().coerceIn(0f, 1f)
            boundary(shape, angle) * (1f - ROUGHNESS * inward)
        }
    }

    /**
     * Cuadrado de medio lado 1 con esquinas de radio [corner] (1 = círculo): se corta el rayo
     * (ux, uy) con el lado y, si cae en la esquina, con su arco.
     */
    private fun roundedSquare(ux: Double, uy: Double, corner: Float): Float {
        val t = 1.0 / max(ux, uy)
        val k = 1.0 - corner
        if (t * ux <= k || t * uy <= k) return t.toFloat()
        val b = (ux + uy) * k
        return (b + sqrt(b * b - 2 * k * k + corner.toDouble() * corner)).toFloat()
    }
}
