package app.velora.launcher.core.data.apps

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
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
 * Los ocho primeros viven aquí; los demás en [VeloraIconStylesMore] y [VeloraIconStylesCraft].
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
    private const val HALF = 0.5f
    private const val GLASS_TOP = 0.34f
    private const val GLASS_BOTTOM = 0.14f
    private const val GLASS_EDGE = 0.45f
    private const val GLASS_STROKE = 0.02f
    private const val GLASS_SHADOW = 0.3f
    private const val GLASS_SHADOW_BLUR = 0.03f
    private const val GLASS_SHADOW_DY = 0.012f
    private const val NIGHT_TOP = 0xFF22306A.toInt()
    private const val NIGHT_BOTTOM = 0xFF0A0F24.toInt()
    private const val NIGHT_TINT = 0.22f
    private const val STAR_LIGHT = 0xFFF2F4FF.toInt()
    private const val STAR_GLOW = 0xFFB4C0FF.toInt()
    private const val STAR_GLOW_ALPHA = 0.6f
    private const val STAR_GLOW_BLUR = 0.035f
    private const val MARBLE_TOP = 0xFFFBFAF7.toInt()
    private const val MARBLE_BOTTOM = 0xFFE8E6E1.toInt()
    private const val MARBLE_VEIN = 0xFF7D7A78.toInt()
    private const val MARBLE_INK = 0xFF3A3A40.toInt()
    private const val BLACK = 0xFF000000.toInt()
    private const val OPAQUE = 255

    fun paint(canvas: Canvas, spec: IconSpec) {
        when (spec.options.pack) {
            IconPack.Line -> line(canvas, spec)
            IconPack.Material -> solid(canvas, spec, spec.base)
            IconPack.Dots -> canvas.drawPath(iconShape(spec, DOT_FRACTION), solidPaint(spec.base))
            IconPack.Sticker -> sticker(canvas, spec)
            IconPack.Bright -> solid(canvas, spec, IconPalette.bright(spec.base), IconColorMath.INK)
            IconPack.Glass -> glass(canvas, spec)
            IconPack.Stellar -> stellar(canvas, spec)
            IconPack.Marble -> marble(canvas, spec)
            IconPack.Neon -> VeloraIconStylesMore.neon(canvas, spec)
            IconPack.Duotone -> VeloraIconStylesMore.duotone(canvas, spec)
            IconPack.Pixel -> VeloraIconStylesMore.pixel(canvas, spec)
            IconPack.Gradient -> VeloraIconStylesMore.gradient(canvas, spec)
            IconPack.Outline -> VeloraIconStylesMore.outline(canvas, spec)
            IconPack.Watercolor -> VeloraIconStylesCraft.watercolor(canvas, spec)
            IconPack.Paper -> VeloraIconStylesCraft.paper(canvas, spec)
            IconPack.Clay -> VeloraIconStylesCraft.clay(canvas, spec)
        }
    }

    /** Forma del tema y glifo de línea; con color de la app, el glifo toma ese color legible. */
    private fun line(canvas: Canvas, spec: IconSpec) {
        canvas.drawPath(iconShape(spec), solidPaint(spec.backdrop))
        val glyph = if (spec.options.color == IconColor.App) {
            IconPalette.contrasting(spec.base, spec.backdrop)
        } else {
            spec.tint
        }
        canvas.drawMark(spec.mark, spec.sizePx, markStyle(spec, glyph))
    }

    /** Forma llena de [color] con el glifo que mejor se lea encima (o el que se pida). */
    private fun solid(canvas: Canvas, spec: IconSpec, color: Int, glyph: Int = IconColorMath.onColor(color)) {
        canvas.drawPath(iconShape(spec), solidPaint(color))
        canvas.drawMark(spec.mark, spec.sizePx, markStyle(spec, glyph))
    }

    /** Pegatina: borde blanco grueso con sombra suave y un degradado de dos tonos dentro. */
    private fun sticker(canvas: Canvas, spec: IconSpec) {
        val size = spec.sizePx.toFloat()
        val border = solidPaint(IconColorMath.WHITE).apply {
            setShadowLayer(size * STICKER_SHADOW_RADIUS, 0f, size * STICKER_SHADOW_DY, STICKER_SHADOW)
        }
        canvas.drawPath(iconShape(spec, 1f - STICKER_INSET * 2), border)
        val inner = 1f - (STICKER_INSET + STICKER_BORDER) * 2
        val top = IconColorMath.shade(spec.base, STICKER_LIGHT)
        val bottom = IconColorMath.shade(spec.base, STICKER_DARK)
        canvas.drawPath(iconShape(spec, inner), verticalGradient(size, top, bottom))
        val glyph = IconColorMath.onColor(IconColorMath.blend(top, bottom, HALF))
        canvas.drawMark(spec.mark, spec.sizePx, markStyle(spec, glyph, STICKER_GLYPH))
    }

    /**
     * Vidrio para fondos con foto: blanco translúcido más claro arriba, filo claro y glifo blanco
     * con una sombra difusa que lo separa de las fotos claras.
     */
    private fun glass(canvas: Canvas, spec: IconSpec) {
        val size = spec.sizePx.toFloat()
        val stroke = size * GLASS_STROKE
        val white = IconColorMath.WHITE
        val top = IconColorMath.withAlpha(white, GLASS_TOP)
        canvas.drawPath(iconShape(spec), verticalGradient(size, top, IconColorMath.withAlpha(white, GLASS_BOTTOM)))
        val edge = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = stroke
            color = IconColorMath.withAlpha(white, GLASS_EDGE)
        }
        canvas.drawPath(iconShape(spec, 1f - stroke / size), edge)
        val glyph = markLayer(spec, markStyle(spec, white))
        val shadow = IconColorMath.withAlpha(BLACK, GLASS_SHADOW)
        canvas.drawBlurred(glyph, shadow, size * GLASS_SHADOW_BLUR, 0f, size * GLASS_SHADOW_DY)
        canvas.drawBitmap(glyph, 0f, 0f, null)
    }

    /** Cielo nocturno (teñido apenas con el color elegido), estrellas propias y glifo que brilla. */
    private fun stellar(canvas: Canvas, spec: IconSpec) {
        val size = spec.sizePx.toFloat()
        val tinted = spec.options.color != IconColor.Mono
        val top = if (tinted) IconColorMath.blend(NIGHT_TOP, spec.base, NIGHT_TINT) else NIGHT_TOP
        canvas.drawPath(iconShape(spec), verticalGradient(size, top, NIGHT_BOTTOM))
        val star = atopPaint(STAR_LIGHT)
        for (dot in IconOrnaments.stars(spec.seed)) {
            star.alpha = (dot.alpha * OPAQUE).toInt()
            canvas.drawCircle(dot.x * size, dot.y * size, dot.radius * size, star)
        }
        val glyph = markLayer(spec, markStyle(spec, STAR_LIGHT))
        canvas.drawBlurred(glyph, IconColorMath.withAlpha(STAR_GLOW, STAR_GLOW_ALPHA), size * STAR_GLOW_BLUR)
        canvas.drawBitmap(glyph, 0f, 0f, null)
    }

    /** Mármol blanco con vetas grises suaves; solo se pintan dentro de la forma (SRC_ATOP). */
    private fun marble(canvas: Canvas, spec: IconSpec) {
        val size = spec.sizePx.toFloat()
        canvas.drawPath(iconShape(spec), verticalGradient(size, MARBLE_TOP, MARBLE_BOTTOM))
        val vein = atopPaint(MARBLE_VEIN).apply {
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
        }
        for (streak in IconOrnaments.veins(spec.seed)) {
            vein.color = IconColorMath.withAlpha(MARBLE_VEIN, streak.alpha)
            vein.strokeWidth = streak.width * size
            val curve = Path().apply {
                moveTo(0f, streak.startY * size)
                cubicTo(
                    IconOrnaments.THIRD * size,
                    streak.bend1Y * size,
                    IconOrnaments.TWO_THIRDS * size,
                    streak.bend2Y * size,
                    size,
                    streak.endY * size,
                )
            }
            canvas.drawPath(curve, vein)
        }
        canvas.drawMark(spec.mark, spec.sizePx, markStyle(spec, MARBLE_INK))
    }
}

/** La marca corrida ([dx], [dy]) px: sombras y relieves del glifo sin capas extra. */
internal fun Canvas.drawMarkAt(spec: IconSpec, style: MarkStyle, dx: Float, dy: Float) {
    save()
    translate(dx, dy)
    drawMark(spec.mark, spec.sizePx, style)
    restore()
}
