package app.velora.launcher.core.data.apps

import android.content.Context
import android.content.pm.LauncherActivityInfo
import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.Shader
import android.graphics.Typeface
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import androidx.core.graphics.createBitmap
import androidx.core.graphics.drawable.toBitmap
import app.velora.launcher.core.model.IconStyle

/**
 * Convierte el ícono de una app al estilo elegido (ESTILO §7). Corre en `Dispatchers.Default`.
 * Las capas de un ícono adaptativo miden 108 unidades y se ven las 72 del centro: por eso se
 * dibujan ¼ más grandes por cada lado antes de recortar.
 */
internal class IconRenderer(private val context: Context, private val densityDpi: Int) {

    private val glyphs = GlyphLibrary(context)

    /**
     * @param tint color del logo en línea (el del texto del tema).
     * @param backdrop círculo translúcido detrás del logo en línea.
     */
    fun render(info: LauncherActivityInfo, sizePx: Int, style: IconStyle, tint: Int, backdrop: Int): Bitmap =
        when (style) {
            IconStyle.System, IconStyle.None -> info.getBadgedIcon(densityDpi).toBitmap(sizePx, sizePx)
            IconStyle.Circle -> badge(info, circle(fullBleed(info.getIcon(densityDpi), sizePx), sizePx))
            IconStyle.Minimal -> badge(info, minimal(info, sizePx, tint, backdrop))
        }

    /** El ícono a sangre completa en un cuadrado; los antiguos sobre un fondo neutro. */
    private fun fullBleed(icon: Drawable, sizePx: Int): Bitmap {
        val bitmap = createBitmap(sizePx, sizePx)
        val canvas = Canvas(bitmap)
        if (icon is AdaptiveIconDrawable) {
            val inset = sizePx / ADAPTIVE_INSET_DIVISOR
            icon.background?.drawLayer(canvas, -inset, sizePx + inset)
            icon.foreground?.drawLayer(canvas, -inset, sizePx + inset)
        } else {
            canvas.drawColor(LEGACY_BACKGROUND)
            val pad = (sizePx * LEGACY_PADDING).toInt()
            icon.drawLayer(canvas, pad, sizePx - pad)
        }
        return bitmap
    }

    /** Recorte circular con borde suavizado (un `clipPath` dejaría el borde dentado). */
    private fun circle(source: Bitmap, sizePx: Int): Bitmap {
        val output = createBitmap(sizePx, sizePx)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
            shader = BitmapShader(source, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
        }
        val radius = sizePx / 2f
        Canvas(output).drawCircle(radius, radius, radius, paint)
        return output
    }

    /**
     * Ícono en línea dentro de un círculo relleno (ADR-021), en este orden:
     * 1. el trazo de Tabler que le toca a la app ([GlyphMatcher]): todos con el mismo grosor;
     * 2. el ícono temático (monocromo) de Android 13+ que trae la propia app;
     * 3. la inicial de la app. Nunca su logo a color: así nada desentona.
     */
    private fun minimal(info: LauncherActivityInfo, sizePx: Int, glyphColor: Int, circleColor: Int): Bitmap {
        val output = createBitmap(sizePx, sizePx)
        val canvas = Canvas(output)
        val radius = sizePx / 2f
        canvas.drawCircle(radius, radius, radius, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = circleColor })
        val glyph = GlyphMatcher.match(
            info.componentName.packageName,
            info.label?.toString().orEmpty(),
            info.applicationInfo.category,
        )
            ?.let(glyphs::paths)
        val monochrome = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            (info.getIcon(densityDpi) as? AdaptiveIconDrawable)?.monochrome
        } else {
            null
        }
        when {
            glyph != null -> drawGlyph(canvas, glyph, sizePx, glyphColor)

            monochrome != null -> {
                val inset = sizePx / ADAPTIVE_INSET_DIVISOR
                monochrome.colorFilter = PorterDuffColorFilter(glyphColor, PorterDuff.Mode.SRC_IN)
                monochrome.drawLayer(canvas, -inset, sizePx + inset)
            }

            else -> drawInitial(canvas, info.label?.toString().orEmpty(), sizePx, glyphColor)
        }
        return output
    }

    /** Trazos de 24×24 con trazo 2 y extremos redondos, centrados al 55 % del círculo. */
    private fun drawGlyph(canvas: Canvas, paths: List<Path>, sizePx: Int, color: Int) {
        val scale = sizePx * GLYPH_FRACTION / GLYPH_VIEWBOX
        val offset = (sizePx - GLYPH_VIEWBOX * scale) / 2f
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            style = Paint.Style.STROKE
            strokeWidth = GLYPH_STROKE
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }
        canvas.save()
        canvas.translate(offset, offset)
        canvas.scale(scale, scale)
        paths.forEach { canvas.drawPath(it, paint) }
        canvas.restore()
    }

    /** La inicial de la app, centrada: mejor que un hueco cuando no hay ícono en línea. */
    private fun drawInitial(canvas: Canvas, label: String, sizePx: Int, color: Int) {
        val initial = label.trim().firstOrNull { it.isLetterOrDigit() }?.uppercase() ?: return
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            textSize = sizePx * INITIAL_FRACTION
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val baseline = sizePx / 2f - (paint.descent() + paint.ascent()) / 2f
        canvas.drawText(initial, sizePx / 2f, baseline, paint)
    }

    /** Insignia de perfil de trabajo encima del ícono ya recortado. */
    private fun badge(info: LauncherActivityInfo, bitmap: Bitmap): Bitmap {
        val drawable = BitmapDrawable(context.resources, bitmap)
        val badged = context.packageManager.getUserBadgedIcon(drawable, info.user)
        return if (badged === drawable) bitmap else badged.toBitmap(bitmap.width, bitmap.height)
    }

    private fun Drawable.drawLayer(canvas: Canvas, start: Int, end: Int) {
        setBounds(start, start, end, end)
        draw(canvas)
    }

    private companion object {
        /** 108 / 72 = 1.5 → ¼ del tamaño visible por lado. */
        const val ADAPTIVE_INSET_DIVISOR = 4
        const val LEGACY_PADDING = 0.16f
        const val LEGACY_BACKGROUND = 0xFFF1F3F4.toInt()
        const val GLYPH_VIEWBOX = 24f
        const val GLYPH_STROKE = 2f
        const val GLYPH_FRACTION = 0.55f
        const val INITIAL_FRACTION = 0.42f
    }
}
