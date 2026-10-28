package app.velora.launcher.core.data.apps

import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Shader
import app.velora.launcher.core.model.IconColor
import app.velora.launcher.core.model.IconOptions
import app.velora.launcher.core.model.IconPack

/**
 * Todo lo que necesita un ícono de Velora ya resuelto: [base] es el color elegido por
 * `IconColor` (acento, color de la app o mono); [tint] y [backdrop] son los del tema; [seed]
 * fija los adornos de cada app.
 */
internal class IconSpec(
    val sizePx: Int,
    val options: IconOptions,
    val mark: IconMark,
    val base: Int,
    val tint: Int,
    val backdrop: Int,
    val seed: Int,
)

/**
 * Los estilos de los íconos de Velora (ADR-028), dibujados en un lienzo vacío de `sizePx`.
 * Diseño propio: misma forma, mismo glifo y mismo margen óptico en todos; solo cambia el material.
 */
internal object VeloraIconStyles {

    private const val DOT_FRACTION = 0.45f
    private const val STICKER_INSET = 0.07f
    private const val STICKER_BORDER = 0.055f
    private const val STICKER_SHADOW_RADIUS = 0.035f
    private const val STICKER_SHADOW_DY = 0.018f
    private const val STICKER_SHADOW = 0x40000000
    private const val STICKER_LIGHT = 1.18f
    private const val STICKER_DARK = 0.78f
    private const val STICKER_GLYPH = 0.48f
    private const val GLASS_FILL = 0.22f
    private const val GLASS_EDGE = 0.4f
    private const val GLASS_STROKE = 0.02f
    private const val NIGHT_TOP = 0xFF22306A.toInt()
    private const val NIGHT_BOTTOM = 0xFF0A0F24.toInt()
    private const val NIGHT_TINT = 0.22f
    private const val STAR_LIGHT = 0xFFF2F4FF.toInt()
    private const val MARBLE_TOP = 0xFFFBFAF7.toInt()
    private const val MARBLE_BOTTOM = 0xFFE8E6E1.toInt()
    private const val MARBLE_VEIN = 0xFF7D7A78.toInt()
    private const val MARBLE_INK = 0xFF3A3A40.toInt()

    fun paint(canvas: Canvas, spec: IconSpec) {
        when (spec.options.pack) {
            IconPack.Line -> line(canvas, spec)
            IconPack.Material -> solid(canvas, spec, spec.base)
            IconPack.Dots -> canvas.drawPath(shape(spec, DOT_FRACTION), fill(spec.base))
            IconPack.Sticker -> sticker(canvas, spec)
            IconPack.Bright -> solid(canvas, spec, IconColorMath.vivid(spec.base), IconColorMath.INK)
            IconPack.Glass -> glass(canvas, spec)
            IconPack.Stellar -> stellar(canvas, spec)
            IconPack.Marble -> marble(canvas, spec)
        }
    }

    /** Forma del tema y glifo de línea; con color de la app, el glifo toma ese color. */
    private fun line(canvas: Canvas, spec: IconSpec) {
        canvas.drawPath(shape(spec), fill(spec.backdrop))
        val glyph = if (spec.options.color == IconColor.App) readable(spec.base, spec.backdrop) else spec.tint
        canvas.drawMark(spec.mark, spec.sizePx, markStyle(spec, glyph))
    }

    /** Forma llena de [color] con el glifo que mejor se lea encima (o el que se pida). */
    private fun solid(canvas: Canvas, spec: IconSpec, color: Int, glyph: Int = IconColorMath.onColor(color)) {
        canvas.drawPath(shape(spec), fill(color))
        canvas.drawMark(spec.mark, spec.sizePx, markStyle(spec, glyph))
    }

    /** Pegatina: borde blanco grueso con sombra suave y un degradado de dos tonos dentro. */
    private fun sticker(canvas: Canvas, spec: IconSpec) {
        val size = spec.sizePx.toFloat()
        val border = fill(IconColorMath.WHITE).apply {
            setShadowLayer(size * STICKER_SHADOW_RADIUS, 0f, size * STICKER_SHADOW_DY, STICKER_SHADOW)
        }
        canvas.drawPath(shape(spec, 1f - STICKER_INSET * 2), border)
        val inner = 1f - (STICKER_INSET + STICKER_BORDER) * 2
        val top = IconColorMath.shade(spec.base, STICKER_LIGHT)
        val bottom = IconColorMath.shade(spec.base, STICKER_DARK)
        canvas.drawPath(shape(spec, inner), gradient(size, top, bottom))
        val glyph = IconColorMath.onColor(IconColorMath.blend(top, bottom, 0.5f))
        canvas.drawMark(spec.mark, spec.sizePx, markStyle(spec, glyph, STICKER_GLYPH))
    }

    /** Vidrio para fondos con foto: blanco translúcido, filo claro y glifo blanco. */
    private fun glass(canvas: Canvas, spec: IconSpec) {
        val size = spec.sizePx.toFloat()
        val stroke = size * GLASS_STROKE
        canvas.drawPath(shape(spec), fill(IconColorMath.withAlpha(IconColorMath.WHITE, GLASS_FILL)))
        val edge = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = stroke
            color = IconColorMath.withAlpha(IconColorMath.WHITE, GLASS_EDGE)
        }
        canvas.drawPath(shape(spec, 1f - stroke / size), edge)
        canvas.drawMark(spec.mark, spec.sizePx, markStyle(spec, IconColorMath.WHITE))
    }

    /** Cielo nocturno (teñido apenas con el color elegido) y estrellas propias de cada app. */
    private fun stellar(canvas: Canvas, spec: IconSpec) {
        val size = spec.sizePx.toFloat()
        val tinted = spec.options.color != IconColor.Mono
        val top = if (tinted) IconColorMath.blend(NIGHT_TOP, spec.base, NIGHT_TINT) else NIGHT_TOP
        canvas.drawPath(shape(spec), gradient(size, top, NIGHT_BOTTOM))
        val star = fill(STAR_LIGHT).apply { xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_ATOP) }
        for (dot in IconOrnaments.stars(spec.seed)) {
            star.alpha = (dot.alpha * OPAQUE).toInt()
            canvas.drawCircle(dot.x * size, dot.y * size, dot.radius * size, star)
        }
        canvas.drawMark(spec.mark, spec.sizePx, markStyle(spec, STAR_LIGHT))
    }

    /** Mármol blanco con vetas grises suaves; solo se pintan dentro de la forma (SRC_ATOP). */
    private fun marble(canvas: Canvas, spec: IconSpec) {
        val size = spec.sizePx.toFloat()
        canvas.drawPath(shape(spec), gradient(size, MARBLE_TOP, MARBLE_BOTTOM))
        val vein = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_ATOP)
        }
        for (streak in IconOrnaments.veins(spec.seed)) {
            val p = streak.points
            vein.color = IconColorMath.withAlpha(MARBLE_VEIN, streak.alpha)
            vein.strokeWidth = streak.width * size
            val curve = Path().apply {
                moveTo(p[0] * size, p[1] * size)
                cubicTo(p[2] * size, p[3] * size, p[4] * size, p[5] * size, p[6] * size, p[7] * size)
            }
            canvas.drawPath(curve, vein)
        }
        canvas.drawMark(spec.mark, spec.sizePx, markStyle(spec, MARBLE_INK))
    }

    /** El color de la app, oscurecido o aclarado hasta leerse sobre el fondo del tema. */
    private fun readable(color: Int, background: Int): Int {
        val lightBackground = IconColorMath.onColor(background) == IconColorMath.INK
        return if (lightBackground == (IconColorMath.onColor(color) == IconColorMath.INK)) {
            IconColorMath.shade(color, if (lightBackground) READABLE_DARKEN else READABLE_LIGHTEN)
        } else {
            color
        }
    }

    private fun shape(spec: IconSpec, fraction: Float = 1f): Path =
        centeredShapePath(spec.options.shape, spec.sizePx, fraction)

    private fun fill(color: Int) = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color }

    private fun gradient(size: Float, top: Int, bottom: Int) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        shader = LinearGradient(0f, 0f, 0f, size, top, bottom, Shader.TileMode.CLAMP)
    }

    private fun markStyle(spec: IconSpec, color: Int, fraction: Float = MarkStyle.GLYPH_FRACTION): MarkStyle {
        val weight = spec.options.weight.coerceIn(IconOptions.MIN_WEIGHT, IconOptions.MAX_WEIGHT)
        return MarkStyle(color, weight, spec.options.softFill, fraction)
    }

    private const val OPAQUE = 255
    private const val READABLE_DARKEN = 0.55f
    private const val READABLE_LIGHTEN = 1.6f
}
