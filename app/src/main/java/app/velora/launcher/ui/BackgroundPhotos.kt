package app.velora.launcher.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import app.velora.launcher.core.data.log.Logger
import java.io.File
import java.io.InputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Foto propia como fondo de un tema (ADR-022). Se elige con el selector de fotos del sistema (sin
 * permisos), se copia reducida al tamaño de la pantalla a `files/backgrounds/` (no se respalda) y
 * se lee fuera del hilo principal. Se guardan las últimas [MAX_PHOTOS] como "fondos anteriores":
 * quitar la foto del tema no borra el archivo; solo se borra a mano o cuando sobran.
 */
object BackgroundPhotos {

    /** Cuántas fotos se conservan como historial. */
    const val MAX_PHOTOS = 8

    private const val DIR = "backgrounds"
    private const val JPEG_QUALITY = 90
    private const val TAG = "BackgroundPhotos"

    /**
     * Copia la foto elegida y devuelve su ruta, o null si no se pudo leer. Si ya hay más de
     * [MAX_PHOTOS], se borran las más viejas, salvo las rutas de [keep] (las que están en uso).
     */
    suspend fun import(context: Context, uri: Uri, keep: Set<String> = emptySet()): String? =
        save(context, keep) { context.contentResolver.openInputStream(uri) }

    /** La foto que viene dentro de un tema compartido (ADR-027): se decodifica y reduce igual. */
    suspend fun import(context: Context, jpeg: ByteArray, keep: Set<String> = emptySet()): String? =
        save(context, keep) { jpeg.inputStream() }

    /** Los bytes de la foto guardada, para exportarla dentro de un tema. */
    suspend fun read(path: String): ByteArray? = withContext(Dispatchers.IO) {
        runCatching { File(path).readBytes() }.getOrNull()
    }

    suspend fun load(path: String): ImageBitmap? = withContext(Dispatchers.IO) {
        runCatching { BitmapFactory.decodeFile(path)?.asImageBitmap() }.getOrNull()
    }

    /** Miniatura reducida (lado mayor ≥ [maxSidePx]) para la fila de fondos anteriores. */
    suspend fun thumbnail(path: String, maxSidePx: Int): ImageBitmap? = withContext(Dispatchers.IO) {
        runCatching {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(path, bounds)
            val options = BitmapFactory.Options().apply {
                inSampleSize = sampleSize(bounds.outWidth, bounds.outHeight, maxSidePx)
            }
            BitmapFactory.decodeFile(path, options)?.asImageBitmap()
        }.getOrNull()
    }

    /** Las fotos guardadas, de la más nueva a la más vieja. */
    suspend fun history(context: Context): List<String> = withContext(Dispatchers.IO) {
        photos(context).map { it.absolutePath }
    }

    /** Borra una foto del historial (solo dentro de `files/backgrounds/`); true si se borró. */
    suspend fun delete(context: Context, path: String): Boolean = withContext(Dispatchers.IO) {
        val file = File(path)
        runCatching { file.parentFile?.canonicalPath == directory(context).canonicalPath && file.delete() }
            .getOrDefault(false)
    }

    private fun directory(context: Context) = File(context.filesDir, DIR)

    private fun photos(context: Context): List<File> =
        directory(context).listFiles()?.filter { it.isFile }?.sortedByDescending { it.lastModified() }.orEmpty()

    /** Potencia de 2 más grande que deja el lado mayor en al menos [target] píxeles. */
    private fun sampleSize(width: Int, height: Int, target: Int): Int {
        var sample = 1
        while (maxOf(width, height) / (sample * 2) >= target) sample *= 2
        return sample
    }

    /** [open] se llama dos veces: una para medir y otra para decodificar ya reducida. */
    private suspend fun save(context: Context, keep: Set<String>, open: () -> InputStream?): String? =
        withContext(Dispatchers.IO) {
            runCatching {
                val metrics = context.resources.displayMetrics
                val target = maxOf(metrics.widthPixels, metrics.heightPixels)
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                open()?.use { BitmapFactory.decodeStream(it, null, bounds) }
                if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return@runCatching null
                val options = BitmapFactory.Options().apply {
                    inSampleSize = sampleSize(bounds.outWidth, bounds.outHeight, target)
                }
                val bitmap = open()?.use { BitmapFactory.decodeStream(it, null, options) } ?: return@runCatching null
                val dir = directory(context).apply { mkdirs() }
                // Nombre nuevo cada vez: así la UI sabe que cambió.
                val file = File(dir, "fondo_${System.currentTimeMillis()}.jpg")
                file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, it) }
                prune(context, keep + file.absolutePath)
                file.absolutePath
            }.onFailure { Logger.w(TAG, it) { "No se pudo importar la foto" } }.getOrNull()
        }

    /** Deja las [MAX_PHOTOS] más nuevas; las de [keep] nunca se borran. */
    private fun prune(context: Context, keep: Set<String>) {
        photos(context)
            .drop(MAX_PHOTOS)
            .filterNot { it.absolutePath in keep }
            .forEach { it.delete() }
    }
}
