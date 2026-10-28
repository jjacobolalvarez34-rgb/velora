package app.velora.launcher.core.data.apps

import android.content.Context
import android.content.pm.LauncherActivityInfo
import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Shader
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import androidx.core.graphics.createBitmap
import androidx.core.graphics.drawable.toBitmap
import androidx.core.graphics.scale
import app.velora.launcher.core.model.IconColor
import app.velora.launcher.core.model.IconPack
import app.velora.launcher.core.model.IconShape
import app.velora.launcher.core.model.IconStyle

/**
 * Convierte el ícono de una app al estilo elegido (ESTILO §7). Corre en `Dispatchers.Default`.
 * Las capas de un ícono adaptativo miden 108 unidades y se ven las 72 del centro: por eso se
 * dibujan ¼ más grandes por cada lado antes de recortar.
 */
internal class IconRenderer(private val context: Context, private val densityDpi: Int) {

    private val glyphs = GlyphLibrary(context)

    /** [packIcon] solo con `IconStyle.Pack`: lo que el pack dio para esta app (null = no la cubre). */
    fun render(info: LauncherActivityInfo, request: IconRequest, packIcon: PackIcon? = null): Bitmap {
        val size = request.sizePx
        val shape = request.options.shape
        return when (request.style) {
            IconStyle.System, IconStyle.None -> info.getBadgedIcon(densityDpi).toBitmap(size, size)

            IconStyle.Circle -> badge(info, shaped(fullBleed(info.getIcon(densityDpi), size), shape))

            IconStyle.Minimal -> badge(info, velora(info, request))

            IconStyle.Pack -> badge(
                info,
                when (packIcon) {
                    is PackIcon.Exact -> exactPackIcon(packIcon, size)
                    is PackIcon.Composed -> composePackIcon(info.getIcon(densityDpi), packIcon, size)
                    null -> shaped(fullBleed(info.getIcon(densityDpi), size), shape)
                },
            )
        }
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

    /** Recorte en la forma elegida con borde suavizado (un `clipPath` dejaría el borde dentado). */
    private fun shaped(source: Bitmap, shape: IconShape): Bitmap {
        val size = source.width
        val output = createBitmap(size, size)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
            shader = BitmapShader(source, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
        }
        Canvas(output).drawPath(iconShapePath(shape, 0f, 0f, size.toFloat()), paint)
        return output
    }

    /** Ícono de Velora (ADR-021, ADR-028): forma, material del estilo y glifo propio. */
    private fun velora(info: LauncherActivityInfo, request: IconRequest): Bitmap {
        val output = createBitmap(request.sizePx, request.sizePx)
        val spec = IconSpec(
            sizePx = request.sizePx,
            options = request.options,
            mark = if (request.options.pack == IconPack.Dots) IconMark.None else markFor(info),
            base = baseColor(info, request),
            tint = request.tint,
            backdrop = request.backdrop,
            seed = info.componentName.packageName.hashCode(),
        )
        VeloraIconStyles.paint(Canvas(output), spec)
        return output
    }

    /**
     * En este orden: el trazo de Tabler que le toca a la app ([GlyphMatcher]); el ícono temático
     * (monocromo) de Android 13+ de la propia app; su inicial. Nunca su logo a color.
     */
    private fun markFor(info: LauncherActivityInfo): IconMark {
        val label = info.label?.toString().orEmpty()
        val glyph = GlyphMatcher.match(info.componentName.packageName, label, info.applicationInfo.category)
            ?.let(glyphs::paths)
        val monochrome = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            (info.getIcon(densityDpi) as? AdaptiveIconDrawable)?.monochrome
        } else {
            null
        }
        val initial = label.trim().firstOrNull { it.isLetterOrDigit() }?.uppercase()
        return when {
            glyph != null -> IconMark.Glyph(glyph)
            monochrome != null -> IconMark.Monochrome(monochrome)
            initial != null -> IconMark.Initial(initial)
            else -> IconMark.None
        }
    }

    /** El color según `IconColor`; con el de la app y un ícono sin color, cae al del tema. */
    private fun baseColor(info: LauncherActivityInfo, request: IconRequest): Int {
        val theme = if (IconColorMath.alphaOf(request.accent) == 0) request.tint else request.accent
        return when (request.options.color) {
            IconColor.Theme -> theme
            IconColor.App -> appColor(info) ?: theme
            IconColor.Mono -> IconColorMath.mono(request.backdrop)
        }
    }

    /** Color principal del ícono original, sobre una copia diminuta (barato y sin ruido). */
    private fun appColor(info: LauncherActivityInfo): Int? {
        val large = fullBleed(info.getIcon(densityDpi), SAMPLE_SIZE * SAMPLE_OVERDRAW)
        val small = large.scale(SAMPLE_SIZE, SAMPLE_SIZE)
        val pixels = IntArray(SAMPLE_SIZE * SAMPLE_SIZE)
        small.getPixels(pixels, 0, SAMPLE_SIZE, 0, 0, SAMPLE_SIZE, SAMPLE_SIZE)
        return IconColorMath.dominant(pixels)
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

        /** Muestra de 24×24 para el color de la app, dibujada al doble y reducida (promedia). */
        const val SAMPLE_SIZE = 24
        const val SAMPLE_OVERDRAW = 2
    }
}
