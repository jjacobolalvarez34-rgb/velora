package app.velora.launcher.core.designsystem.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import app.velora.launcher.core.model.ThemePreset
import java.io.File
import java.io.IOException
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.min
import kotlin.random.Random
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext

/**
 * Genera y guarda el arte de cada tema. Se calcula a resolución reducida (la mitad o menos) en
 * `Dispatchers.Default`, repartido en franjas entre los núcleos, y al dibujarse se escala con
 * filtrado bilineal: sale suave y cuesta poco. Se guarda en memoria (LRU por bytes) y en disco
 * (`cacheDir/art/`, PNG sin pérdida para que el tramado anti-bandas sobreviva). Subir
 * [ART_VERSION] invalida lo guardado cuando cambia algún dibujo.
 */
internal object ArtCache {
    private val memory = object : LruCache<String, ImageBitmap>(MEMORY_BYTES) {
        override fun sizeOf(key: String, value: ImageBitmap) = value.width * value.height * 4
    }

    @Volatile
    private var cleaned = false

    fun key(preset: ThemePreset, width: Int, height: Int) = "${preset.name}_${width}x${height}_v$ART_VERSION"

    /** Lo que ya está en memoria; se puede llamar desde la composición (no toca disco). */
    fun cached(key: String): ImageBitmap? = memory.get(key)

    /** Disco o, si no está, lo genera. Nunca en el hilo principal. */
    suspend fun load(context: Context, key: String, art: () -> PixelArt, width: Int, height: Int): ImageBitmap {
        memory.get(key)?.let { return it }
        val file = File(File(context.cacheDir, DIR), "$key.png")
        val image = withContext(Dispatchers.IO) { read(file) }
            ?: render(art(), width, height).also { withContext(Dispatchers.IO) { write(file, it) } }
        memory.put(key, image)
        return image
    }

    private suspend fun render(art: PixelArt, width: Int, height: Int): ImageBitmap = withContext(Dispatchers.Default) {
        val scale = min(MAX_SCALE, MAX_WIDTH / width.toFloat())
        val w = ceil(width * scale).toInt().coerceAtLeast(1)
        val h = ceil(height * scale).toInt().coerceAtLeast(1)
        val frame = Frame(IntArray(w * h), w, h)
        val bands = Runtime.getRuntime().availableProcessors().coerceIn(1, MAX_BANDS)
        coroutineScope {
            List(bands) { band ->
                async { renderRows(art, frame, band * h / bands, (band + 1) * h / bands) }
            }.awaitAll()
        }
        Bitmap.createBitmap(frame.pixels, w, h, Bitmap.Config.ARGB_8888).asImageBitmap()
    }

    /** Los píxeles que se están calculando, de [w]×[h]. */
    private class Frame(val pixels: IntArray, val w: Int, val h: Int)

    private suspend fun renderRows(art: PixelArt, frame: Frame, from: Int, to: Int) = coroutineScope {
        val w = frame.w
        val h = frame.h
        val pixels = frame.pixels
        val out = Pixel()
        val unit = 1f / w
        for (row in from until to) {
            ensureActive()
            val y = row * unit
            val v = row / (h - 1f).coerceAtLeast(1f)
            for (col in 0 until w) {
                out.r = 0f
                out.g = 0f
                out.b = 0f
                art.shade(col * unit, y, v, out)
                pixels[row * w + col] = pack(out, Noise.hash(col, row, DITHER_SEED))
            }
        }
    }

    /** A entero ARGB, con medio paso de tramado: los degradados oscuros no se ven en escalones. */
    private fun pack(p: Pixel, dither: Float): Int {
        val d = (dither - 0.5f) * DITHER
        val r = (p.r * 255f + d).toInt().coerceIn(0, 255)
        val g = (p.g * 255f + d).toInt().coerceIn(0, 255)
        val b = (p.b * 255f + d).toInt().coerceIn(0, 255)
        return (0xFF shl 24) or (r shl 16) or (g shl 8) or b
    }

    private fun read(file: File): ImageBitmap? =
        if (file.exists()) BitmapFactory.decodeFile(file.path)?.asImageBitmap() else null

    /** Escritura atómica (archivo temporal + renombrar); un fallo de disco solo pierde la caché. */
    private fun write(file: File, image: ImageBitmap) {
        try {
            val dir = file.parentFile ?: return
            if (!cleaned) {
                cleaned = true
                dir.listFiles()?.filterNot { it.name.endsWith("_v$ART_VERSION.png") }?.forEach { it.delete() }
            }
            dir.mkdirs()
            val tmp = File(dir, "${file.name}.tmp")
            tmp.outputStream().use { image.asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, it) }
            file.delete()
            tmp.renameTo(file)
        } catch (_: IOException) {
            // Sin caché en disco: la próxima vez se vuelve a generar.
        }
    }

    /**
     * Grano fino de película: 128×128 de ruido blanco y negro (casi gris medio) que se repite sobre
     * todo el fondo a baja opacidad. Hecho una vez, fuera del hilo principal.
     */
    private val grain = lazy {
        val random = Random(DITHER_SEED)
        val pixels = IntArray(GRAIN * GRAIN) {
            val n = (random.nextFloat() + random.nextFloat()) - 1f
            val alpha = (abs(n) * 255f).toInt().coerceIn(0, 255)
            if (n > 0f) (alpha shl 24) or 0xFFFFFF else alpha shl 24
        }
        Bitmap.createBitmap(pixels, GRAIN, GRAIN, Bitmap.Config.ARGB_8888).asImageBitmap()
    }

    /** El grano si ya existe; se puede llamar desde la composición. */
    val grainIfReady: ImageBitmap? get() = if (grain.isInitialized()) grain.value else null

    /** El grano, creándolo la primera vez en segundo plano. */
    suspend fun grainTile(): ImageBitmap = withContext(Dispatchers.Default) { grain.value }

    private const val DIR = "art"
    private const val MAX_SCALE = 0.5f
    private const val MAX_WIDTH = 480f
    private const val MAX_BANDS = 4
    private const val MEMORY_BYTES = 12 * 1024 * 1024
    private const val DITHER = 1.6f
    private const val DITHER_SEED = 91
    private const val GRAIN = 128
}

/** Súbelo cuando cambie cualquier dibujo procedural: lo guardado en disco se descarta. */
internal const val ART_VERSION = 1
