package app.velora.launcher.core.data.apps

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PathMeasure
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import kotlin.math.hypot

/**
 * Lo que va encima de la forma en un ícono de Velora (ADR-021): el glifo de Tabler; si no hay,
 * el monocromo de Android 13+ de la propia app; si tampoco, su inicial.
 */
internal sealed interface IconMark {
    class Glyph(val paths: List<Path>) : IconMark
    class Monochrome(val drawable: Drawable) : IconMark
    class Initial(val letter: String) : IconMark
    data object None : IconMark
}

/**
 * Cómo dibujar la marca: [color], grosor [weight] en unidades del glifo (24 de ancho), [softFill]
 * (relleno suave dentro de los trazos cerrados) y [fraction] del lado que ocupa.
 */
internal class MarkStyle(
    val color: Int,
    val weight: Float,
    val softFill: Boolean,
    val fraction: Float = GLYPH_FRACTION,
) {
    companion object {
        /** El glifo ocupa el 55 % del ícono: el margen óptico de ESTILO §7. */
        const val GLYPH_FRACTION = 0.55f
    }
}

private const val GLYPH_VIEWBOX = 24f
private const val INITIAL_FRACTION = 0.42f
private const val SOFT_FILL_ALPHA = 0.2f

/** 108 / 72 = 1.5 → ¼ del tamaño visible por lado en las capas adaptativas. */
private const val ADAPTIVE_INSET_DIVISOR = 4

/** Un contorno cuyo final queda a menos de esto (unidades del glifo) de su inicio está cerrado. */
private const val CLOSED_TOLERANCE = 0.6f

internal fun Canvas.drawMark(mark: IconMark, sizePx: Int, style: MarkStyle) {
    when (mark) {
        is IconMark.Glyph -> drawGlyph(mark.paths, sizePx, style)

        is IconMark.Monochrome -> {
            val inset = sizePx / ADAPTIVE_INSET_DIVISOR
            val drawable = mark.drawable.mutate()
            drawable.colorFilter = PorterDuffColorFilter(style.color, PorterDuff.Mode.SRC_IN)
            drawable.setBounds(-inset, -inset, sizePx + inset, sizePx + inset)
            drawable.draw(this)
        }

        is IconMark.Initial -> drawInitial(mark.letter, sizePx, style.color)

        IconMark.None -> Unit
    }
}

/** Trazos de 24×24 con extremos redondos, centrados en [MarkStyle.fraction] del ícono. */
private fun Canvas.drawGlyph(paths: List<Path>, sizePx: Int, style: MarkStyle) {
    val scale = sizePx * style.fraction / GLYPH_VIEWBOX
    val offset = (sizePx - GLYPH_VIEWBOX * scale) / 2f
    save()
    translate(offset, offset)
    scale(scale, scale)
    if (style.softFill) {
        val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = IconColorMath.withAlpha(style.color, SOFT_FILL_ALPHA)
            this.style = Paint.Style.FILL
        }
        drawPath(closedContours(paths), fill)
    }
    val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = style.color
        this.style = Paint.Style.STROKE
        strokeWidth = style.weight
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    paths.forEach { drawPath(it, stroke) }
    restore()
}

/**
 * Solo los contornos cerrados del glifo (los de Tabler a veces terminan donde empiezan sin `Z`),
 * con relleno par-impar: en una cámara se rellena el cuerpo y el lente queda hueco.
 */
private fun closedContours(paths: List<Path>): Path {
    val result = Path().apply { fillType = Path.FillType.EVEN_ODD }
    val measure = PathMeasure()
    val start = FloatArray(2)
    val end = FloatArray(2)
    for (path in paths) {
        measure.setPath(path, false)
        do {
            val length = measure.length
            if (length <= 0f) continue
            measure.getPosTan(0f, start, null)
            measure.getPosTan(length, end, null)
            if (measure.isClosed || hypot(end[0] - start[0], end[1] - start[1]) < CLOSED_TOLERANCE) {
                measure.getSegment(0f, length, result, true)
                result.close()
            }
        } while (measure.nextContour())
    }
    return result
}

/** La inicial de la app, centrada: mejor que un hueco cuando no hay glifo. */
private fun Canvas.drawInitial(letter: String, sizePx: Int, color: Int) {
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = color
        textSize = sizePx * INITIAL_FRACTION
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }
    val baseline = sizePx / 2f - (paint.descent() + paint.ascent()) / 2f
    drawText(letter, sizePx / 2f, baseline, paint)
}
