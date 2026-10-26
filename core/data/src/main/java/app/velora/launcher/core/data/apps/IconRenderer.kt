package app.velora.launcher.core.data.apps

import android.content.Context
import android.content.pm.LauncherActivityInfo
import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.RectF
import android.graphics.Shader
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

    /**
     * @param tint color del logo en línea (el del texto del tema).
     * @param backdrop círculo translúcido detrás del logo en línea.
     */
    fun render(info: LauncherActivityInfo, sizePx: Int, style: IconStyle, tint: Int, backdrop: Int): Bitmap =
        when (style) {
            IconStyle.System, IconStyle.None -> info.getBadgedIcon(densityDpi).toBitmap(sizePx, sizePx)
            IconStyle.Circle -> badge(info, circle(fullBleed(info.getIcon(densityDpi), sizePx), sizePx))
            IconStyle.Minimal -> badge(info, minimal(info.getIcon(densityDpi), sizePx, tint, backdrop))
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
    private fun circle(source: Bitmap, sizePx: Int, colorFilter: ColorMatrixColorFilter? = null): Bitmap {
        val output = createBitmap(sizePx, sizePx)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
            shader = BitmapShader(source, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
            this.colorFilter = colorFilter
        }
        val radius = sizePx / 2f
        Canvas(output).drawCircle(radius, radius, radius, paint)
        return output
    }

    /**
     * Logo en línea: el ícono temático (monocromo) de Android 13+ pintado del color del texto,
     * sobre un círculo translúcido. Si la app no lo trae, su ícono en grises suaves mezclados
     * con ese color, un poco más chico dentro del mismo círculo: nada desentona.
     */
    private fun minimal(icon: Drawable, sizePx: Int, tint: Int, backdrop: Int): Bitmap {
        val output = createBitmap(sizePx, sizePx)
        val canvas = Canvas(output)
        val radius = sizePx / 2f
        canvas.drawCircle(radius, radius, radius, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = backdrop })
        val monochrome = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            (icon as? AdaptiveIconDrawable)?.monochrome
        } else {
            null
        }
        if (monochrome != null) {
            val inset = sizePx / ADAPTIVE_INSET_DIVISOR
            monochrome.colorFilter = PorterDuffColorFilter(tint, PorterDuff.Mode.SRC_IN)
            monochrome.drawLayer(canvas, -inset, sizePx + inset)
        } else {
            val inner = circle(fullBleed(icon, sizePx), sizePx, softGrayscale(tint))
            val pad = sizePx * FALLBACK_PADDING
            canvas.drawBitmap(inner, null, RectF(pad, pad, sizePx - pad, sizePx - pad), Paint(Paint.FILTER_BITMAP_FLAG))
        }
        return output
    }

    /** Grises con poco contraste, llevados hacia el color del texto (claro u oscuro según el tema). */
    private fun softGrayscale(tint: Int): ColorMatrixColorFilter {
        val matrix = ColorMatrix().apply { setSaturation(0f) }
        val keep = 1f - FALLBACK_TINT_MIX
        val r = android.graphics.Color.red(tint) * FALLBACK_TINT_MIX
        val g = android.graphics.Color.green(tint) * FALLBACK_TINT_MIX
        val b = android.graphics.Color.blue(tint) * FALLBACK_TINT_MIX
        matrix.postConcat(
            ColorMatrix(
                floatArrayOf(
                    keep, 0f, 0f, 0f, r,
                    0f, keep, 0f, 0f, g,
                    0f, 0f, keep, 0f, b,
                    0f, 0f, 0f, 1f, 0f,
                ),
            ),
        )
        return ColorMatrixColorFilter(matrix)
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
        const val FALLBACK_PADDING = 0.14f
        const val FALLBACK_TINT_MIX = 0.45f
    }
}
