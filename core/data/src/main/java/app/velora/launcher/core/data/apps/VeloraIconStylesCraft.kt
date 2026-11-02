package app.velora.launcher.core.data.apps

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.Shader
import app.velora.launcher.core.model.IconColor
import kotlin.math.cos
import kotlin.math.sin

/**
 * Estilos "de taller" de Velora (ADR-028): Acuarela, Papel y Arcilla. Materiales que imitan algo
 * hecho a mano, deterministas por app (la semilla es el hash del paquete). Diseño propio.
 */
internal object VeloraIconStylesCraft {

    private const val BLOB_STEPS = 96
    private const val WASH_SATURATION = 0.6f
    private const val WASH_VALUE = 0.9f
    private const val WASH_ALPHA = 0.45f
    private const val WASH_UNDER = 0.55f
    private const val PIGMENT = 0.75f
    private const val PIGMENT_ALPHA = 0.5f
    private const val PIGMENT_WIDTH = 0.012f
    private const val PIGMENT_BLUR = 0.006f
    private const val FULL_TURN = 2 * Math.PI

    private const val PAPER_SIZE = 0.9f
    private const val PAPER_TOP = 0xFFFBF8F1.toInt()
    private const val PAPER_BOTTOM = 0xFFF1EADC.toInt()
    private const val PAPER_SHADOW = 0x3A000000
    private const val PAPER_SHADOW_BLUR = 0.045f
    private const val PAPER_SHADOW_DY = 0.028f
    private const val PAPER_GLYPH = 0.5f
    private const val CUT_SHADOW = 0.3f
    private const val CUT_SHADOW_BLUR = 0.012f
    private const val CUT_SHADOW_DY = 0.012f

    private const val CLAY_SIZE = 0.88f
    private const val CLAY_SATURATION = 0.5f
    private const val CLAY_VALUE = 0.86f
    private const val CLAY_GLYPH = 0.5f
    private const val CLAY_DROP_BLUR = 0.05f
    private const val CLAY_DROP_DY = 0.035f
    private const val CLAY_DROP_ALPHA = 0.4f
    private const val CLAY_RIM = 0.03f
    private const val CLAY_RIM_BLUR = 0.035f
    private const val CLAY_LIGHT_ALPHA = 0.75f
    private const val CLAY_SHADE = 0.62f
    private const val CLAY_SHADE_ALPHA = 0.5f
    private const val CLAY_SHINE_X = 0.36f
    private const val CLAY_SHINE_Y = 0.3f
    private const val CLAY_SHINE_RADIUS = 0.34f
    private const val CLAY_SHINE_ALPHA = 0.35f
    private const val CLAY_PRESS = 0.012f
    private const val CLAY_PRESS_ALPHA = 0.7f

    private const val BLACK = 0xFF000000.toInt()

    /**
     * Acuarela: tres aguadas translúcidas de borde irregular (el pigmento se junta en el borde)
     * sobre una base clara, y el glifo en la tinta profunda del mismo tono.
     */
    fun watercolor(canvas: Canvas, spec: IconSpec) {
        val size = spec.sizePx.toFloat()
        val wash = IconPalette.pastel(spec.base, WASH_SATURATION, WASH_VALUE)
        val washes = IconBlob.washes(spec.options.shape, spec.seed, BLOB_STEPS)
        val under = IconColorMath.blend(wash, IconColorMath.WHITE, WASH_UNDER)
        canvas.drawPath(blobPath(washes.first(), size), solidPaint(under))
        val pigment = IconColorMath.withAlpha(IconColorMath.shade(wash, PIGMENT), PIGMENT_ALPHA)
        for (layer in washes) {
            val path = blobPath(layer, size)
            canvas.drawPath(path, solidPaint(IconColorMath.withAlpha(wash, WASH_ALPHA)))
            val edge = solidPaint(pigment).apply {
                style = Paint.Style.STROKE
                strokeWidth = size * PIGMENT_WIDTH
            }
            canvas.drawSoftPath(path, edge, size * PIGMENT_BLUR)
        }
        canvas.drawMark(spec.mark, spec.sizePx, markStyle(spec, IconPalette.deep(spec.base)))
    }

    /** El borde de una aguada como `Path` en un lienzo de lado [size]. */
    private fun blobPath(wash: Wash, size: Float): Path {
        val half = size / 2f
        val cx = half + wash.dx * half
        val cy = half + wash.dy * half
        val path = Path()
        for (i in wash.radii.indices) {
            val angle = FULL_TURN * i / wash.radii.size
            val r = wash.radii[i] * IconBlob.WASH_SCALE * half
            val x = cx + (cos(angle) * r).toFloat()
            val y = cy + (sin(angle) * r).toFloat()
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.close()
        return path
    }

    /** Papel: hoja crema con sombra suave y el glifo como una tira de papel de color pegada. */
    fun paper(canvas: Canvas, spec: IconSpec) {
        val size = spec.sizePx.toFloat()
        val sheet = iconShape(spec, PAPER_SIZE)
        val lifted = solidPaint(PAPER_BOTTOM).apply {
            setShadowLayer(size * PAPER_SHADOW_BLUR, 0f, size * PAPER_SHADOW_DY, PAPER_SHADOW)
        }
        canvas.drawPath(sheet, lifted)
        canvas.drawPath(sheet, verticalGradient(size, PAPER_TOP, PAPER_BOTTOM))
        val ink = if (spec.options.color == IconColor.Mono) {
            IconColorMath.INK
        } else {
            IconPalette.contrasting(spec.base, PAPER_BOTTOM)
        }
        val glyph = markLayer(spec, markStyle(spec, ink, PAPER_GLYPH))
        val shadow = IconColorMath.withAlpha(BLACK, CUT_SHADOW)
        canvas.drawBlurred(glyph, shadow, size * CUT_SHADOW_BLUR, 0f, size * CUT_SHADOW_DY)
        canvas.drawBitmap(glyph, 0f, 0f, null)
    }

    /** Arcilla: cuerpo pastel apoyado (sombra), con relieve suave y el glifo hundido. */
    fun clay(canvas: Canvas, spec: IconSpec) {
        val size = spec.sizePx.toFloat()
        val clay = IconPalette.pastel(spec.base, CLAY_SATURATION, CLAY_VALUE)
        val body = iconShape(spec, CLAY_SIZE)
        val drop = IconColorMath.withAlpha(IconPalette.deep(clay), CLAY_DROP_ALPHA)
        val resting = solidPaint(clay).apply { setShadowLayer(size * CLAY_DROP_BLUR, 0f, size * CLAY_DROP_DY, drop) }
        canvas.drawPath(body, resting)
        canvas.saveLayer(null, null)
        canvas.drawPath(body, solidPaint(clay))
        clayRelief(canvas, spec, clay)
        canvas.restore()
        val press = IconColorMath.withAlpha(IconColorMath.WHITE, CLAY_PRESS_ALPHA)
        canvas.drawMarkAt(spec, markStyle(spec, press, CLAY_GLYPH), 0f, size * CLAY_PRESS)
        canvas.drawMark(spec.mark, spec.sizePx, markStyle(spec, IconPalette.deep(clay), CLAY_GLYPH))
    }

    /**
     * Relieve dentro del cuerpo (en su capa, con SRC_ATOP): lo de afuera corrido hacia abajo a la
     * derecha ilumina el filo de arriba; corrido al revés oscurece el de abajo; y un brillo suave.
     */
    private fun clayRelief(canvas: Canvas, spec: IconSpec, clay: Int) {
        val size = spec.sizePx.toFloat()
        val outside = outsideOf(spec, CLAY_SIZE)
        val rim = size * CLAY_RIM
        val light = atopPaint(IconColorMath.withAlpha(IconColorMath.WHITE, CLAY_LIGHT_ALPHA))
        val shade = atopPaint(IconColorMath.withAlpha(IconColorMath.shade(clay, CLAY_SHADE), CLAY_SHADE_ALPHA))
        for ((offset, paint) in listOf(rim to light, -rim to shade)) {
            canvas.save()
            canvas.translate(offset, offset)
            canvas.drawSoftPath(outside, paint, size * CLAY_RIM_BLUR)
            canvas.restore()
        }
        val shine = atopPaint(IconColorMath.WHITE).apply {
            shader = RadialGradient(
                size * CLAY_SHINE_X,
                size * CLAY_SHINE_Y,
                size * CLAY_SHINE_RADIUS,
                IconColorMath.withAlpha(IconColorMath.WHITE, CLAY_SHINE_ALPHA),
                IconColorMath.withAlpha(IconColorMath.WHITE, 0f),
                Shader.TileMode.CLAMP,
            )
        }
        canvas.drawRect(0f, 0f, size, size, shine)
    }
}
