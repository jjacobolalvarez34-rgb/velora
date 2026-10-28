package app.velora.launcher.core.data.apps

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt

private const val CHANNEL = 255f
private const val ALPHA_SHIFT = 24
private const val RED_SHIFT = 16
private const val GREEN_SHIFT = 8
private const val BYTE = 0xFF

private fun alpha(color: Int) = color ushr ALPHA_SHIFT
private fun red(color: Int) = (color shr RED_SHIFT) and BYTE
private fun green(color: Int) = (color shr GREEN_SHIFT) and BYTE
private fun blue(color: Int) = color and BYTE

/**
 * Color de los íconos de Velora en ARGB (`Int`), con matemática propia para probarla en la JVM
 * sin `android.graphics.Color` (y sin `androidx.palette`, que no está en dependencias).
 */
internal object IconColorMath {

    const val WHITE = 0xFFFFFFFF.toInt()

    /** Negro suave para glifos y la variante oscura de Mono: menos duro que #000. */
    const val INK = 0xFF1C1B20.toInt()

    /** Blanco apenas cálido para la variante clara de Mono. */
    const val PAPER = 0xFFF5F5F7.toInt()

    private const val RGB_MASK = 0x00FFFFFF

    /** Por encima de esta luminancia relativa el glifo va oscuro. */
    private const val LIGHT_LUMINANCE = 0.45

    private const val SRGB_KNEE = 0.03928
    private const val SRGB_LINEAR_DIVISOR = 12.92
    private const val SRGB_OFFSET = 0.055
    private const val SRGB_SCALE = 1.055
    private const val SRGB_GAMMA = 2.4
    private const val LUMA_R = 0.2126
    private const val LUMA_G = 0.7152
    private const val LUMA_B = 0.0722

    private const val HUE_SECTORS = 6
    private const val DEGREES = 360f
    private const val SECTOR_DEGREES = 60f

    /** "Vivo": saturación y brillo mínimos del estilo Bright. */
    private const val VIVID_SATURATION = 0.62f
    private const val VIVID_VALUE = 0.96f

    /** Por debajo de esta saturación un color es gris: no se le inventa un tono. */
    private const val GREY_SATURATION = 0.1f

    /** Píxeles casi transparentes: borde del ícono, no cuentan. */
    private const val MIN_ALPHA = 0.5f

    /** Cubetas de tono para buscar el color principal (de 30°). */
    private const val HUE_BUCKETS = 12

    /** Peso mínimo de un píxel gris: solo decide si el ícono entero es gris. */
    private const val GREY_WEIGHT = 0.05f

    fun argb(alpha: Int, red: Int, green: Int, blue: Int): Int =
        (alpha shl ALPHA_SHIFT) or (red shl RED_SHIFT) or (green shl GREEN_SHIFT) or blue

    /** Opacidad 0–255 de un color ARGB. */
    fun alphaOf(color: Int): Int = alpha(color)

    /** El mismo color con opacidad [fraction] (0–1). */
    fun withAlpha(color: Int, fraction: Float): Int =
        ((fraction.coerceIn(0f, 1f) * CHANNEL).roundToInt() shl ALPHA_SHIFT) or (color and RGB_MASK)

    /** Luminancia relativa WCAG (0 negro, 1 blanco). */
    fun luminance(color: Int): Double {
        fun linear(channel: Int): Double {
            val c = channel / CHANNEL.toDouble()
            return if (c <= SRGB_KNEE) c / SRGB_LINEAR_DIVISOR else ((c + SRGB_OFFSET) / SRGB_SCALE).pow(SRGB_GAMMA)
        }
        return LUMA_R * linear(red(color)) + LUMA_G * linear(green(color)) + LUMA_B * linear(blue(color))
    }

    /** Glifo legible encima de [background]: blanco sobre oscuro, tinta sobre claro. */
    fun onColor(background: Int): Int = if (luminance(background) > LIGHT_LUMINANCE) INK else WHITE

    /** Mono: forma clara si el fondo de los íconos del tema es claro, oscura si no. */
    fun mono(backdrop: Int): Int = if (luminance(backdrop) > LIGHT_LUMINANCE) PAPER else INK

    /** Tono (0–360), saturación y brillo (0–1). */
    fun toHsv(color: Int): FloatArray {
        val r = red(color) / CHANNEL
        val g = green(color) / CHANNEL
        val b = blue(color) / CHANNEL
        val maxC = max(r, max(g, b))
        val delta = maxC - min(r, min(g, b))
        val hue = when {
            delta == 0f -> 0f
            maxC == r -> SECTOR_DEGREES * (((g - b) / delta).mod(HUE_SECTORS.toFloat()))
            maxC == g -> SECTOR_DEGREES * ((b - r) / delta + 2f)
            else -> SECTOR_DEGREES * ((r - g) / delta + 4f)
        }
        return floatArrayOf(hue, if (maxC == 0f) 0f else delta / maxC, maxC)
    }

    fun fromHsv(hue: Float, saturation: Float, value: Float, alpha: Int = BYTE): Int {
        val h = hue.mod(DEGREES) / SECTOR_DEGREES
        val chroma = value * saturation
        val x = chroma * (1f - abs(h.mod(2f) - 1f))
        val m = value - chroma
        val (r, g, b) = when (h.toInt()) {
            0 -> Triple(chroma, x, 0f)
            1 -> Triple(x, chroma, 0f)
            2 -> Triple(0f, chroma, x)
            3 -> Triple(0f, x, chroma)
            4 -> Triple(x, 0f, chroma)
            else -> Triple(chroma, 0f, x)
        }
        fun channel(c: Float) = ((c + m) * CHANNEL).roundToInt().coerceIn(0, BYTE)
        return argb(alpha, channel(r), channel(g), channel(b))
    }

    /** Mismo tono con el brillo multiplicado por [factor] (sombra o luz del degradado). */
    fun shade(color: Int, factor: Float): Int {
        val (h, s, v) = toHsv(color)
        return fromHsv(h, s, (v * factor).coerceIn(0f, 1f), alpha(color))
    }

    /** Versión viva del color (Bright); los grises se quedan grises pero luminosos. */
    fun vivid(color: Int): Int {
        val (h, s, v) = toHsv(color)
        val saturation = if (s < GREY_SATURATION) s else max(s, VIVID_SATURATION)
        return fromHsv(h, saturation, max(v, VIVID_VALUE))
    }

    /** Mezcla lineal de [from] hacia [to] en proporción [amount] (0–1), opaca. */
    fun blend(from: Int, to: Int, amount: Float): Int {
        fun mix(a: Int, b: Int) = (a + (b - a) * amount).roundToInt()
        return argb(BYTE, mix(red(from), red(to)), mix(green(from), green(to)), mix(blue(from), blue(to)))
    }

    /**
     * Color principal de un ícono a partir de sus píxeles (ya reducidos, p. ej. 24×24): se agrupan
     * por tono y gana la cubeta con más peso (opacidad × saturación × brillo), así un logo rojo
     * sobre fondo blanco da rojo y no rosa. Si todo es gris, devuelve el gris promedio.
     */
    fun dominant(pixels: IntArray): Int? {
        val weights = FloatArray(HUE_BUCKETS + 1)
        val sums = Array(HUE_BUCKETS + 1) { FloatArray(3) }
        for (pixel in pixels) {
            val a = alpha(pixel) / CHANNEL
            if (a < MIN_ALPHA) continue
            val (h, s, v) = toHsv(pixel)
            val grey = s < GREY_SATURATION
            val bucket = if (grey) HUE_BUCKETS else (h / DEGREES * HUE_BUCKETS).toInt().coerceIn(0, HUE_BUCKETS - 1)
            val weight = if (grey) a * GREY_WEIGHT else a * s * v
            weights[bucket] += weight
            sums[bucket][0] += red(pixel) * weight
            sums[bucket][1] += green(pixel) * weight
            sums[bucket][2] += blue(pixel) * weight
        }
        val best = weights.indices.maxBy { weights[it] }
        val total = weights[best]
        if (total <= 0f) return null
        val (r, g, b) = sums[best].map { (it / total).roundToInt().coerceIn(0, BYTE) }
        return argb(BYTE, r, g, b)
    }
}
