package app.velora.launcher.core.data.apps

import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import app.velora.launcher.core.model.IconColor
import kotlin.math.roundToInt

/**
 * Estilos gráficos de Velora (ADR-028): Neón, Duotono, Píxel, Degradado y Contorno. Diseño
 * propio, con la misma forma, el mismo glifo y el mismo margen óptico que el resto.
 */
internal object VeloraIconStylesMore {

    private const val NEON_NIGHT_TOP = 0xFF16151F.toInt()
    private const val NEON_NIGHT_BOTTOM = 0xFF07070B.toInt()
    private const val NEON_NIGHT_TINT = 0.12f
    private const val NEON_RING = 0.84f
    private const val NEON_RING_ALPHA = 0.3f
    private const val NEON_RING_WEIGHT = 0.45f
    private const val NEON_HALO_WIDE = 0.075f
    private const val NEON_HALO_WIDE_ALPHA = 0.9f
    private const val NEON_HALO_TIGHT = 0.022f
    private const val NEON_CORE_WHITE = 0.7f
    private const val NEON_CORE_WEIGHT = 0.42f

    private const val DUO_PALE = 0.84f
    private const val DUO_OFFSET = 0.032f

    private const val PIXEL_FRACTION = 0.6f
    private const val PIXEL_MONOCHROME_FRACTION = 0.74f
    private const val PIXEL_SHADOW = 0.7f

    private const val SHEEN_END = 0.62f
    private const val SHEEN_ALPHA = 0.3f
    private const val GRADIENT_SHADOW_ALPHA = 0.32f
    private const val GRADIENT_SHADOW_BLUR = 0.03f
    private const val GRADIENT_SHADOW_DY = 0.016f
    private const val HALF = 0.5f

    private const val OUTLINE_SIZE = 0.94f
    private const val OUTLINE_GLYPH = 0.5f

    /** Neón: forma casi negra con un anillo tenue y el glifo como tubo (núcleo claro y halo). */
    fun neon(canvas: Canvas, spec: IconSpec) {
        val size = spec.sizePx.toFloat()
        val light = IconPalette.neon(spec.base)
        val night = IconColorMath.blend(NEON_NIGHT_TOP, light, NEON_NIGHT_TINT)
        canvas.drawPath(iconShape(spec), verticalGradient(size, night, NEON_NIGHT_BOTTOM))
        val ring = solidPaint(IconColorMath.withAlpha(light, NEON_RING_ALPHA)).apply {
            style = Paint.Style.STROKE
            strokeWidth = strokePx(spec) * NEON_RING_WEIGHT
        }
        canvas.drawPath(iconShape(spec, NEON_RING), ring)
        val style = markStyle(spec, light)
        val glyph = markLayer(spec, style)
        canvas.drawBlurred(glyph, IconColorMath.withAlpha(light, NEON_HALO_WIDE_ALPHA), size * NEON_HALO_WIDE)
        canvas.drawBlurred(glyph, light, size * NEON_HALO_TIGHT)
        canvas.drawBitmap(glyph, 0f, 0f, null)
        if (spec.mark is IconMark.Glyph) {
            val core = IconColorMath.blend(light, IconColorMath.WHITE, NEON_CORE_WHITE)
            canvas.drawMark(spec.mark, spec.sizePx, MarkStyle(core, style.weight * NEON_CORE_WEIGHT, false))
        }
    }

    /** Duotono: fondo pálido del color, sombra del glifo corrida en el tono medio y glifo profundo. */
    fun duotone(canvas: Canvas, spec: IconSpec) {
        val size = spec.sizePx.toFloat()
        val pale = IconColorMath.blend(spec.base, IconColorMath.WHITE, DUO_PALE)
        canvas.drawPath(iconShape(spec), solidPaint(pale))
        val shift = size * DUO_OFFSET / 2
        canvas.drawMarkAt(spec, markStyle(spec, IconPalette.midTone(spec.base)), shift, shift)
        canvas.drawMarkAt(spec, markStyle(spec, IconPalette.deep(spec.base)), -shift, -shift)
    }

    /** Píxel: forma llena y el glifo rasterizado a 16×16 bloques nítidos, con sombra de un bloque. */
    fun pixel(canvas: Canvas, spec: IconSpec) {
        val size = spec.sizePx
        canvas.drawPath(iconShape(spec), solidPaint(spec.base))
        // El monocromo de Android ocupa más que el glifo: su cuadrícula es más grande.
        val area = size * if (spec.mark is IconMark.Monochrome) PIXEL_MONOCHROME_FRACTION else PIXEL_FRACTION
        val start = (size - area) / 2f
        val layer = markLayer(spec, markStyle(spec, IconColorMath.WHITE, PIXEL_FRACTION))
        val pixels = IntArray(size * size)
        layer.getPixels(pixels, 0, size, 0, 0, size, size)
        layer.recycle()
        val cells = PixelArt.cells(pixels, size, GridWindow(start, area))
        val cell = area / PixelArt.GRID
        canvas.drawCells(cells, start, cell, IconColorMath.shade(spec.base, PIXEL_SHADOW), 1)
        canvas.drawCells(cells, start, cell, IconColorMath.onColor(spec.base), 0)
    }

    /** Bloques encendidos, alineados al píxel y sin suavizado (corridos [shift] celdas). */
    private fun Canvas.drawCells(cells: BooleanArray, start: Float, cell: Float, color: Int, shift: Int) {
        val paint = Paint().apply { this.color = color }
        fun edge(index: Int) = (start + index * cell).roundToInt().toFloat()
        for (i in cells.indices) {
            if (!cells[i]) continue
            val col = i % PixelArt.GRID + shift
            val row = i / PixelArt.GRID + shift
            drawRect(edge(col), edge(row), edge(col + 1), edge(row + 1), paint)
        }
    }

    /** Degradado diagonal vivo, brillo suave arriba y glifo claro con sombra (u oscuro si no se lee). */
    fun gradient(canvas: Canvas, spec: IconSpec) {
        val size = spec.sizePx.toFloat()
        val (light, dark) = IconPalette.gradientStops(spec.base)
        canvas.saveLayer(null, null)
        val body = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(0f, 0f, size, size, light, dark, Shader.TileMode.CLAMP)
        }
        canvas.drawPath(iconShape(spec), body)
        val sheen = atopPaint(IconColorMath.WHITE).apply {
            val top = IconColorMath.withAlpha(IconColorMath.WHITE, SHEEN_ALPHA)
            val clear = IconColorMath.withAlpha(IconColorMath.WHITE, 0f)
            shader = LinearGradient(0f, 0f, 0f, size * SHEEN_END, top, clear, Shader.TileMode.CLAMP)
        }
        canvas.drawRect(0f, 0f, size, size * SHEEN_END, sheen)
        canvas.restore()
        val ink = IconColorMath.onColor(IconColorMath.blend(light, dark, HALF))
        val glyph = markLayer(spec, markStyle(spec, ink))
        if (ink == IconColorMath.WHITE) {
            val shadow = IconColorMath.withAlpha(IconPalette.deep(dark), GRADIENT_SHADOW_ALPHA)
            canvas.drawBlurred(glyph, shadow, size * GRADIENT_SHADOW_BLUR, 0f, size * GRADIENT_SHADOW_DY)
        }
        canvas.drawBitmap(glyph, 0f, 0f, null)
    }

    /** Contorno: anillo de la forma con el grosor del glifo y el glifo, sin relleno. */
    fun outline(canvas: Canvas, spec: IconSpec) {
        val size = spec.sizePx.toFloat()
        val color = outlineColor(spec)
        val ring = strokePx(spec)
        val paint = solidPaint(color).apply {
            style = Paint.Style.STROKE
            strokeWidth = ring
        }
        canvas.drawPath(iconShape(spec, OUTLINE_SIZE - ring / size), paint)
        canvas.drawMark(spec.mark, spec.sizePx, markStyle(spec, color, OUTLINE_GLYPH))
    }

    /**
     * Sin relleno, el contorno se ve sobre el fondo del inicio: toma el color de la forma del
     * tema (que ya contrasta con él) o el elegido, llevado a esa misma claridad.
     */
    private fun outlineColor(spec: IconSpec): Int = when {
        spec.options.color == IconColor.Mono || spec.base == spec.tint -> IconColorMath.withAlpha(spec.backdrop, 1f)
        else -> IconPalette.contrasting(spec.base, spec.tint)
    }
}
