package app.velora.launcher.core.data.apps

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.drawable.Drawable
import androidx.core.graphics.createBitmap
import androidx.core.graphics.drawable.toBitmap

/**
 * Ícono de una app que el pack no cubre, con sus piezas (como los launchers clásicos): el ícono
 * original reducido a `scale`, recortado por `iconmask` (lo opaco de la máscara se borra), con
 * `iconback` detrás e `iconupon` encima. Corre en `Dispatchers.Default`.
 */
internal fun composePackIcon(original: Drawable, pieces: PackIcon.Composed, sizePx: Int): Bitmap {
    val output = createBitmap(sizePx, sizePx)
    val canvas = Canvas(output)
    val inner = (sizePx * pieces.scale).toInt().coerceIn(1, sizePx)
    val offset = (sizePx - inner) / 2
    original.setBounds(offset, offset, offset + inner, offset + inner)
    original.draw(canvas)
    pieces.mask?.let { canvas.drawLayer(it, sizePx, PorterDuff.Mode.DST_OUT) }
    pieces.back?.let { canvas.drawLayer(it, sizePx, PorterDuff.Mode.DST_OVER) }
    pieces.upon?.let { canvas.drawLayer(it, sizePx, PorterDuff.Mode.SRC_OVER) }
    return output
}

/** Dibujo propio del pack para la app, al tamaño final. */
internal fun exactPackIcon(icon: PackIcon.Exact, sizePx: Int): Bitmap = icon.drawable.toBitmap(sizePx, sizePx)

private fun Canvas.drawLayer(drawable: Drawable, sizePx: Int, mode: PorterDuff.Mode) {
    val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
        xfermode = PorterDuffXfermode(mode)
    }
    drawBitmap(drawable.toBitmap(sizePx, sizePx), 0f, 0f, paint)
}
