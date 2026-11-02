package app.velora.launcher.core.data.apps

import kotlin.math.max
import kotlin.math.min

/**
 * Colores derivados para cada material de los íconos de Velora (ADR-028): a partir del color
 * elegido por `IconColor` se sacan el pastel, el tono profundo, el neón o los extremos del
 * degradado. Matemática pura en ARGB, probada en la JVM.
 */
internal object IconPalette {

    /** Debajo de esta saturación el color es gris: no se le inventa un tono. */
    private const val GREY_SATURATION = 0.1f

    private const val PASTEL_SATURATION = 0.55f
    private const val PASTEL_VALUE = 0.93f
    private const val DEEP_MAX_SATURATION = 0.85f
    private const val DEEP_VALUE = 0.27f
    private const val MID_MIN_VALUE = 0.55f
    private const val MID_MAX_VALUE = 0.85f

    /** Bright: el color vivo se aclara hasta esta luminancia para que el glifo oscuro se lea. */
    const val BRIGHT_MIN_LUMINANCE = 0.3
    private const val BRIGHT_STEP = 0.1f
    private const val BRIGHT_STEPS = 9

    private const val CONTRAST_STEP = 0.15f
    private const val CONTRAST_STEPS = 6

    private const val NEON_SATURATION = 0.72f
    private const val NEON_WHITE = 0xFFEAF1FF.toInt()

    private const val GRADIENT_MIN_SATURATION = 0.55f
    private const val GRADIENT_HUE_SHIFT = 18f
    private const val GRADIENT_LIGHT_SATURATION = 0.78f
    private const val GRADIENT_DARK_SATURATION = 1.1f
    private const val GRADIENT_LIGHT_VALUE = 0.95f
    private const val GRADIENT_DARK_VALUE = 0.6f

    /** El mismo tono, suave y claro (acuarela, arcilla): saturación como mucho [saturation]. */
    fun pastel(color: Int, saturation: Float = PASTEL_SATURATION, value: Float = PASTEL_VALUE): Int {
        val (h, s, v) = IconColorMath.toHsv(color)
        return IconColorMath.fromHsv(h, min(s, saturation), max(v, value))
    }

    /** El mismo tono casi negro: glifo "de tinta" sobre materiales claros. */
    fun deep(color: Int): Int {
        val (h, s, _) = IconColorMath.toHsv(color)
        return IconColorMath.fromHsv(h, min(s, DEEP_MAX_SATURATION), DEEP_VALUE)
    }

    /** Tono medio (ni pálido ni profundo): segunda tinta del duotono. */
    fun midTone(color: Int): Int {
        val (h, s, v) = IconColorMath.toHsv(color)
        return IconColorMath.fromHsv(h, s, v.coerceIn(MID_MIN_VALUE, MID_MAX_VALUE))
    }

    /** Vivo y lo bastante claro para un glifo oscuro (un azul puro pasa a azul cielo). */
    fun bright(color: Int): Int {
        val vivid = IconColorMath.vivid(color)
        val step = (0..BRIGHT_STEPS).firstOrNull {
            IconColorMath.luminance(IconColorMath.blend(vivid, IconColorMath.WHITE, it * BRIGHT_STEP)) >=
                BRIGHT_MIN_LUMINANCE
        } ?: BRIGHT_STEPS
        return IconColorMath.blend(vivid, IconColorMath.WHITE, step * BRIGHT_STEP)
    }

    /**
     * [color] llevado hacia blanco o tinta lo justo para leerse sobre [background], conservando
     * el tono todo lo posible. Si ya se lee, no cambia.
     */
    fun contrasting(color: Int, background: Int): Int {
        val lightBackground = isLight(background)
        val target = if (lightBackground) IconColorMath.INK else IconColorMath.WHITE
        val step = (0..CONTRAST_STEPS).firstOrNull {
            isLight(IconColorMath.blend(color, target, it * CONTRAST_STEP)) != lightBackground
        } ?: CONTRAST_STEPS
        return if (step == 0) color else IconColorMath.blend(color, target, step * CONTRAST_STEP)
    }

    /** Color del tubo de neón: saturado y a pleno brillo; los grises, blanco frío. */
    fun neon(color: Int): Int {
        val (h, s, _) = IconColorMath.toHsv(color)
        return if (s < GREY_SATURATION) NEON_WHITE else IconColorMath.fromHsv(h, max(s, NEON_SATURATION), 1f)
    }

    /** Extremos del degradado diagonal: claro con el tono corrido a un lado, profundo al otro. */
    fun gradientStops(color: Int): IntArray {
        val (h, s, v) = IconColorMath.toHsv(color)
        val grey = s < GREY_SATURATION
        val saturation = if (grey) s else max(s, GRADIENT_MIN_SATURATION)
        val light = max(v, GRADIENT_LIGHT_VALUE)
        return intArrayOf(
            IconColorMath.fromHsv(h - GRADIENT_HUE_SHIFT, saturation * GRADIENT_LIGHT_SATURATION, light),
            IconColorMath.fromHsv(
                h + GRADIENT_HUE_SHIFT,
                min(1f, saturation * GRADIENT_DARK_SATURATION),
                light * GRADIENT_DARK_VALUE,
            ),
        )
    }

    /** Claro = el glifo que lo lee es tinta. */
    fun isLight(color: Int): Boolean = IconColorMath.onColor(color) == IconColorMath.INK
}
