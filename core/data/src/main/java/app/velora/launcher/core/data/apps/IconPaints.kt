package app.velora.launcher.core.data.apps

import android.graphics.Bitmap
import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Shader
import androidx.core.graphics.createBitmap
import app.velora.launcher.core.model.IconOptions

/*
 * Pinceles y capas comunes a todos los estilos de íconos de Velora. Todo se dibuja en un lienzo
 * de software (un `Bitmap`), así que el desenfoque y las sombras funcionan en cualquier versión.
 */

/** La forma elegida, centrada y reducida a [fraction] del lado. */
internal fun iconShape(spec: IconSpec, fraction: Float = 1f): Path =
    centeredShapePath(spec.options.shape, spec.sizePx, fraction)

/** Todo el lienzo menos la forma (para sombras internas): par-impar con un marco holgado. */
internal fun outsideOf(spec: IconSpec, fraction: Float = 1f): Path {
    val size = spec.sizePx.toFloat()
    return Path(iconShape(spec, fraction)).apply {
        fillType = Path.FillType.EVEN_ODD
        addRect(-size, -size, size * 2, size * 2, Path.Direction.CW)
    }
}

internal fun solidPaint(color: Int) = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color }

/** Pintura que solo cae sobre lo ya pintado (dentro de la forma), dentro de un `saveLayer`. */
internal fun atopPaint(color: Int) = solidPaint(color).apply { xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_ATOP) }

/** Degradado de arriba ([top]) a abajo ([bottom]) en un lienzo de lado [size]. */
internal fun verticalGradient(size: Float, top: Int, bottom: Int) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
    shader = LinearGradient(0f, 0f, 0f, size, top, bottom, Shader.TileMode.CLAMP)
}

/** Estilo del glifo con el grosor del usuario (acotado) multiplicado por [weightScale]. */
internal fun markStyle(
    spec: IconSpec,
    color: Int,
    fraction: Float = MarkStyle.GLYPH_FRACTION,
    weightScale: Float = 1f,
): MarkStyle {
    val weight = spec.options.weight.coerceIn(IconOptions.MIN_WEIGHT, IconOptions.MAX_WEIGHT)
    return MarkStyle(color, weight * weightScale, spec.options.softFill, fraction)
}

/** Grosor del trazo del glifo en píxeles del ícono (para que bordes y anillos le hagan juego). */
internal fun strokePx(spec: IconSpec, fraction: Float = MarkStyle.GLYPH_FRACTION): Float =
    markStyle(spec, 0, fraction).weight * spec.sizePx * fraction / GLYPH_UNITS

/** La marca sola sobre transparente, del tamaño del ícono: base de halos, sombras y píxeles. */
internal fun markLayer(spec: IconSpec, style: MarkStyle): Bitmap {
    val bitmap = createBitmap(spec.sizePx, spec.sizePx)
    Canvas(bitmap).drawMark(spec.mark, spec.sizePx, style)
    return bitmap
}

/** [layer] (solo su opacidad) teñido de [color], desenfocado [blur] px y corrido ([dx], [dy]). */
internal fun Canvas.drawBlurred(layer: Bitmap, color: Int, blur: Float, dx: Float = 0f, dy: Float = 0f) {
    val offset = IntArray(2)
    val alpha = layer.extractAlpha(Paint().apply { maskFilter = blurFilter(blur) }, offset)
    drawBitmap(alpha, offset[0] + dx, offset[1] + dy, solidPaint(color))
    alpha.recycle()
}

/** [path] desenfocado [blur] px (sombras internas y bordes de pigmento). */
internal fun Canvas.drawSoftPath(path: Path, paint: Paint, blur: Float) {
    paint.maskFilter = blurFilter(blur)
    drawPath(path, paint)
}

private fun blurFilter(radius: Float) = BlurMaskFilter(radius.coerceAtLeast(MIN_BLUR), BlurMaskFilter.Blur.NORMAL)

/** Lado del glifo de Tabler en sus propias unidades. */
private const val GLYPH_UNITS = 24f

/** `BlurMaskFilter` exige un radio positivo. */
private const val MIN_BLUR = 0.5f
