package app.velora.launcher.feature.settings

import android.net.Uri
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.lifecycleScope
import app.velora.launcher.R
import app.velora.launcher.core.data.backup.BackupFile
import app.velora.launcher.core.data.backup.UserBackup
import app.velora.launcher.core.data.backup.VeloraBackup
import app.velora.launcher.core.data.log.Logger
import app.velora.launcher.ui.BackgroundPhotos
import java.time.LocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Respaldo completo (ADR-035): guardar todo en un `.velora-backup` o restaurarlo, con el
 * selector de archivos del sistema (sin permisos). Restaurar pide confirmación antes de
 * reemplazar nada. Crear antes de `onStart` (registra los resultados), como [ThemeSharing].
 */
internal class BackupSharing(
    private val activity: ComponentActivity,
    private val backup: () -> UserBackup,
    /** Ruta de la foto de fondo activa, si hay. */
    private val photoPath: () -> String?,
) : BackupActions {

    /** Lo que dibuja [backupSection]. */
    var ui by mutableStateOf(BackupUiState())
        private set

    /** El respaldo leído que espera la confirmación; solo en memoria. */
    private var pending: VeloraBackup? = null

    private val saveFile = activity.registerForActivityResult(
        ActivityResultContracts.CreateDocument(BackupFile.MIME),
    ) { uri ->
        uri?.let(::write)
    }

    private val openFile = activity.registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let(::read)
    }

    override fun onExport() = saveFile.launch("velora-${LocalDate.now()}.${BackupFile.EXTENSION}")

    override fun onImport() = openFile.launch(arrayOf("text/*", "application/octet-stream", "*/*"))

    override fun onIncludePhoto(enabled: Boolean) {
        ui = ui.copy(includePhoto = enabled)
    }

    override fun onCancelRestore() {
        pending = null
        ui = ui.copy(confirmRestore = false)
    }

    override fun onConfirmRestore() {
        val saved = pending ?: return
        pending = null
        ui = ui.copy(confirmRestore = false, busy = true)
        activity.lifecycleScope.launch {
            val photo = saved.photo?.let { BackgroundPhotos.import(activity, it, keep = setOfNotNull(photoPath())) }
            val widgets = runCatching { backup().restore(saved, photo) }
                .onFailure { Logger.w(TAG, it) { "No se pudo restaurar el respaldo" } }
                .getOrNull()
            ui = ui.copy(busy = false)
            when {
                widgets == null -> toast(activity.getString(R.string.backup_import_failed))

                widgets.isEmpty() -> toast(activity.getString(R.string.backup_imported))

                else -> toast(
                    activity.resources.getQuantityString(R.plurals.backup_widgets_readd, widgets.size, widgets.size),
                )
            }
        }
    }

    private fun write(uri: Uri) {
        ui = ui.copy(busy = true)
        activity.lifecycleScope.launch {
            val photo = if (ui.includePhoto) photoPath()?.let { BackgroundPhotos.read(it) } else null
            val ok = withContext(Dispatchers.IO) {
                runCatching {
                    val text = BackupFile.encode(backup().create(photo))
                    activity.contentResolver.openOutputStream(uri)?.use { it.write(text.toByteArray()) } != null
                }.onFailure { Logger.w(TAG, it) { "No se pudo guardar el respaldo" } }.getOrDefault(false)
            }
            ui = ui.copy(busy = false)
            toast(activity.getString(if (ok) R.string.backup_exported else R.string.backup_export_failed))
        }
    }

    private fun read(uri: Uri) {
        activity.lifecycleScope.launch {
            val saved = withContext(Dispatchers.IO) {
                runCatching {
                    activity.contentResolver.openInputStream(uri)?.use { input ->
                        // Nunca leer más del límite: un archivo enorme no es un respaldo.
                        val bytes = input.readNBytesCompat(BackupFile.MAX_BYTES + 1)
                        if (bytes.size > BackupFile.MAX_BYTES) null else BackupFile.decode(bytes.decodeToString())
                    }
                }.onFailure { Logger.w(TAG, it) { "No se pudo leer el respaldo" } }.getOrNull()
            }
            if (saved == null) {
                toast(activity.getString(R.string.backup_import_failed))
            } else {
                pending = saved
                ui = ui.copy(confirmRestore = true)
            }
        }
    }

    private fun toast(message: String) = Toast.makeText(activity, message, Toast.LENGTH_LONG).show()

    private companion object {
        const val TAG = "BackupSharing"
    }
}
