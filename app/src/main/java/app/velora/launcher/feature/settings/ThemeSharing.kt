package app.velora.launcher.feature.settings

import android.net.Uri
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import app.velora.launcher.R
import app.velora.launcher.core.data.log.Logger
import app.velora.launcher.core.data.prefs.SharedTheme
import app.velora.launcher.core.data.prefs.ThemeFile
import app.velora.launcher.core.model.ThemeSettings
import app.velora.launcher.ui.BackgroundPhotos
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Compartir temas de Velora (ADR-027): guardar el tema actual en un archivo `.velora-theme` con
 * el selector de archivos del sistema, o abrir uno que alguien compartió. Sin permisos: el
 * usuario elige el archivo. Crear antes de `onStart` (registra los resultados).
 */
internal class ThemeSharing(
    private val activity: ComponentActivity,
    private val current: () -> ThemeSettings?,
    private val apply: (SharedTheme, photoPath: String?) -> Unit,
) {
    private val saveFile = activity.registerForActivityResult(
        ActivityResultContracts.CreateDocument(ThemeFile.MIME),
    ) { uri ->
        uri?.let(::write)
    }

    private val openFile = activity.registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let(::read)
    }

    fun export() {
        val name = current()?.preset?.name?.lowercase() ?: "velora"
        saveFile.launch("$name.${ThemeFile.EXTENSION}")
    }

    fun import() = openFile.launch(arrayOf("text/*", "application/octet-stream", "*/*"))

    private fun write(uri: Uri) {
        val theme = current() ?: return
        activity.lifecycleScope.launch {
            val photo = theme.photoBackground?.let { BackgroundPhotos.read(it) }
            val text = ThemeFile.encode(SharedTheme(theme.preset, theme.font, theme.clock, theme.icons, photo))
            val ok = withContext(Dispatchers.IO) {
                runCatching {
                    activity.contentResolver.openOutputStream(uri)?.use { it.write(text.toByteArray()) } != null
                }.onFailure { Logger.w(TAG, it) { "No se pudo guardar el tema" } }.getOrDefault(false)
            }
            toast(if (ok) R.string.theme_exported else R.string.theme_export_failed)
        }
    }

    private fun read(uri: Uri) {
        activity.lifecycleScope.launch {
            val theme = withContext(Dispatchers.IO) {
                runCatching {
                    activity.contentResolver.openInputStream(uri)?.use { input ->
                        // Nunca leer más del límite: un archivo enorme no es un tema.
                        val bytes = input.readNBytesCompat(ThemeFile.MAX_BYTES + 1)
                        if (bytes.size > ThemeFile.MAX_BYTES) null else ThemeFile.decode(bytes.decodeToString())
                    }
                }.onFailure { Logger.w(TAG, it) { "No se pudo leer el tema" } }.getOrNull()
            }
            if (theme == null) {
                toast(R.string.theme_import_failed)
                return@launch
            }
            val photoPath = theme.photo?.let { BackgroundPhotos.import(activity, it) }
            apply(theme, photoPath)
            toast(R.string.theme_imported)
        }
    }

    private fun toast(message: Int) = Toast.makeText(activity, message, Toast.LENGTH_SHORT).show()

    private companion object {
        const val TAG = "ThemeSharing"
    }
}

/** `InputStream.readNBytes` llegó en API 33: se lee a mano hasta [limit] bytes. */
private fun java.io.InputStream.readNBytesCompat(limit: Int): ByteArray {
    val out = java.io.ByteArrayOutputStream()
    val buffer = ByteArray(BUFFER)
    while (out.size() < limit) {
        val read = read(buffer, 0, minOf(buffer.size, limit - out.size()))
        if (read < 0) break
        out.write(buffer, 0, read)
    }
    return out.toByteArray()
}

private const val BUFFER = 64 * 1024
