package app.velora.launcher.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import app.velora.launcher.core.data.log.Logger
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Foto propia como fondo de un tema (ADR-022). Se elige con el selector de fotos del sistema (sin
 * permisos), se copia reducida al tamaño de la pantalla a `files/backgrounds/` (no se respalda) y
 * se lee fuera del hilo principal.
 */
object BackgroundPhotos {

    private const val DIR = "backgrounds"
    private const val JPEG_QUALITY = 90
    private const val TAG = "BackgroundPhotos"

    /** Copia la foto elegida y devuelve su ruta, o null si no se pudo leer. */
    suspend fun import(context: Context, uri: Uri): String? = withContext(Dispatchers.IO) {
        runCatching {
            val metrics = context.resources.displayMetrics
            val target = maxOf(metrics.widthPixels, metrics.heightPixels)
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
            var sample = 1
            while (maxOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= target) sample *= 2
            val bitmap = context.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
            } ?: return@runCatching null
            val dir = File(context.filesDir, DIR).apply { mkdirs() }
            dir.listFiles()?.forEach { it.delete() }
            // Nombre nuevo cada vez: así la UI sabe que cambió.
            val file = File(dir, "fondo_${System.currentTimeMillis()}.jpg")
            file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, it) }
            file.absolutePath
        }.onFailure { Logger.w(TAG, it) { "No se pudo importar la foto" } }.getOrNull()
    }

    suspend fun load(path: String): ImageBitmap? = withContext(Dispatchers.IO) {
        runCatching { BitmapFactory.decodeFile(path)?.asImageBitmap() }.getOrNull()
    }

    suspend fun delete(context: Context) = withContext(Dispatchers.IO) {
        File(context.filesDir, DIR).listFiles()?.forEach { it.delete() }
    }
}
