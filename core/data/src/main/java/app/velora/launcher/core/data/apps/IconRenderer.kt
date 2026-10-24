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

    fun render(info: LauncherActivityInfo, sizePx: Int, style: IconStyle, tint: Int): Bitmap = when (style) {
        IconStyle.System, IconStyle.None -> info.getBadgedIcon(densityDpi).toBitmap(sizePx, sizePx)
        IconStyle.Circle -> badge(info, circle(fullBleed(info.getIcon(densityDpi), sizePx), sizePx))
        IconStyle.Minimal -> badge(info, minimal(info.getIcon(densityDpi), sizePx, tint))
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
     * Una sola tinta: el ícono temático (monocromo) de Android 13+ si la app lo trae; si no,
     * el ícono en escala de grises teñido con el acento.
     */
    private fun minimal(icon: Drawable, sizePx: Int, tint: Int): Bitmap {
        val monochrome = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            (icon as? AdaptiveIconDrawable)?.monochrome
        } else {
            null
        }
        if (monochrome == null) return circle(fullBleed(icon, sizePx), sizePx, tintedGrayscale(tint))

        val output = createBitmap(sizePx, sizePx)
        val canvas = Canvas(output)
        val radius = sizePx / 2f
        val backdrop = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = tint
            alpha = MINIMAL_BACKDROP_ALPHA
        }
        canvas.drawCircle(radius, radius, radius, backdrop)
        val inset = sizePx / ADAPTIVE_INSET_DIVISOR
        monochrome.colorFilter = PorterDuffColorFilter(tint, PorterDuff.Mode.SRC_IN)
        monochrome.drawLayer(canvas, -inset, sizePx + inset)
        return output
    }

    /** Escala de grises multiplicada por el color de acento: mantiene la gama del tema. */
    private fun tintedGrayscale(tint: Int): ColorMatrixColorFilter {
        val gray = ColorMatrix().apply { setSaturation(0f) }
        val r = android.graphics.Color.red(tint) / CHANNEL_MAX
        val g = android.graphics.Color.green(tint) / CHANNEL_MAX
        val b = android.graphics.Color.blue(tint) / CHANNEL_MAX
        gray.postConcat(ColorMatrix().apply { setScale(r, g, b, 1f) })
        return ColorMatrixColorFilter(gray)
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
        const val MINIMAL_BACKDROP_ALPHA = 40
        const val CHANNEL_MAX = 255f
    }
}
