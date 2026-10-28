package app.velora.launcher.core.designsystem.components

import androidx.compose.ui.graphics.Color
import kotlin.math.exp
import kotlin.math.floor

// Motor de arte procedural: ruido de valor determinista, fbm y domain warping. Sin librerías:
// cada tema es una función de píxel ([PixelArt]) que se evalúa fuera del hilo principal.

/** Un color en flotantes 0–1, precalculado una vez por dibujo (nunca por píxel). */
internal class Rgb(val r: Float, val g: Float, val b: Float)

internal fun Color.rgb() = Rgb(red, green, blue)

/** El píxel que se está pintando: cada tema lo mezcla capa a capa. */
internal class Pixel {
    var r = 0f
    var g = 0f
    var b = 0f

    fun set(c: Rgb) {
        r = c.r
        g = c.g
        b = c.b
    }

    /** Mezcla hacia [c] en proporción [t] (0 = nada, 1 = todo). */
    fun mix(c: Rgb, t: Float) {
        val k = t.coerceIn(0f, 1f)
        r += (c.r - r) * k
        g += (c.g - g) * k
        b += (c.b - b) * k
    }

    /** Suma luz de color [c] con intensidad [t]: brillos, nebulosas, auroras. */
    fun add(c: Rgb, t: Float) {
        val k = t.coerceAtLeast(0f)
        r += c.r * k
        g += c.g * k
        b += c.b * k
    }

    /** Aclara (>1) u oscurece (<1) sin cambiar el tono. */
    fun scale(f: Float) {
        r *= f
        g *= f
        b *= f
    }
}

/**
 * Arte de un tema, píxel a píxel. [x] e [y] van en "anchos de pantalla" (x de 0 a 1, y de 0 a
 * alto/ancho) para que el ruido no se estire; [v] es la altura relativa (0 arriba, 1 abajo).
 */
internal fun interface PixelArt {
    fun shade(x: Float, y: Float, v: Float, out: Pixel)
}

/** Ruido determinista (semilla fija): el mismo tema se ve igual en cada arranque. */
internal object Noise {
    /** Un valor 0–1 pseudoaleatorio por celda de la rejilla. */
    fun hash(x: Int, y: Int, seed: Int): Float {
        var h = x * 374_761_393 + y * 668_265_263 + seed * 1_442_695_041
        h = (h xor (h ushr 13)) * 1_274_126_177
        h = h xor (h ushr 16)
        return (h and 0xFFFFFF) / 16_777_215f
    }

    /** Ruido de valor suave (interpolación quíntica), 0–1. */
    fun value(x: Float, y: Float, seed: Int): Float {
        val fx = floor(x)
        val fy = floor(y)
        val ix = fx.toInt()
        val iy = fy.toInt()
        val tx = fade(x - fx)
        val ty = fade(y - fy)
        val top = lerp(hash(ix, iy, seed), hash(ix + 1, iy, seed), tx)
        val bottom = lerp(hash(ix, iy + 1, seed), hash(ix + 1, iy + 1, seed), tx)
        return lerp(top, bottom, ty)
    }

    /** Suma de [octaves] capas de ruido, cada una al doble de detalle y rotada (sin rejilla visible). */
    fun fbm(x: Float, y: Float, seed: Int, octaves: Int = 5): Float {
        var px = x
        var py = y
        var amplitude = 0.5f
        var sum = 0f
        var norm = 0f
        repeat(octaves) { i ->
            sum += value(px, py, seed + i * 31) * amplitude
            norm += amplitude
            val rx = ROT_C * px - ROT_S * py
            py = (ROT_S * px + ROT_C * py) * LACUNARITY
            px = rx * LACUNARITY
            amplitude *= GAIN
        }
        return sum / norm
    }

    /** fbm con domain warping: el ruido deforma sus propias coordenadas (formas orgánicas, como humo). */
    fun warped(x: Float, y: Float, seed: Int, strength: Float, octaves: Int = 5): Float {
        val qx = fbm(x, y, seed + 101, 4)
        val qy = fbm(x + 5.2f, y + 1.3f, seed + 203, 4)
        return fbm(x + strength * qx, y + strength * qy, seed, octaves)
    }

    private fun fade(t: Float) = t * t * t * (t * (t * 6f - 15f) + 10f)

    private const val ROT_C = 0.8f
    private const val ROT_S = 0.6f
    private const val LACUNARITY = 2.03f
    private const val GAIN = 0.5f
}

internal fun lerp(a: Float, b: Float, t: Float) = a + (b - a) * t

/** 0 antes de [e0], 1 después de [e1] y una curva suave entre ambos. */
internal fun smooth(e0: Float, e1: Float, x: Float): Float {
    val t = ((x - e0) / (e1 - e0)).coerceIn(0f, 1f)
    return t * t * (3f - 2f * t)
}

/** Una mancha suave (gaussiana) de radio [r] alrededor de ([cx], [cy]). */
internal fun blob(x: Float, y: Float, cx: Float, cy: Float, r: Float): Float {
    val dx = x - cx
    val dy = y - cy
    return exp(-(dx * dx + dy * dy) / (r * r))
}
