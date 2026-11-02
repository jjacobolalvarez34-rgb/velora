package app.velora.launcher.feature.settings

import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.ui.res.stringResource
import app.velora.launcher.R

/** Estado de la sección de respaldo. */
@Immutable
data class BackupUiState(
    /** Incluir la foto de fondo activa (el archivo pesa más). Apagado por defecto. */
    val includePhoto: Boolean = false,
    /** Se leyó un respaldo válido y se espera "Restaurar" o "Cancelar". */
    val confirmRestore: Boolean = false,
    /** Guardando o restaurando: los botones esperan. */
    val busy: Boolean = false,
)

/** Lo que la sección de respaldo puede pedir (lo implementa [BackupSharing]). */
@Stable
interface BackupActions {
    fun onExport()
    fun onImport()
    fun onIncludePhoto(enabled: Boolean)
    fun onConfirmRestore()
    fun onCancelRestore()
}

/**
 * Respaldo completo (ADR-035): guardar y restaurar favoritos, carpetas, ocultas, widgets (para
 * volver a agregarlos) y ajustes. Nunca notificaciones ni el PIN. El diálogo de confirmación lo
 * dibuja [BackupConfirmHost], fuera de la lista.
 */
internal fun LazyListScope.backupSection(state: BackupUiState, actions: BackupActions) {
    item(key = "backup") { SectionTitle(R.string.backup_section) }
    item(key = "backup-export") {
        Option(
            title = stringResource(R.string.backup_export),
            subtitle = stringResource(R.string.backup_export_summary),
            onClick = { if (!state.busy) actions.onExport() },
            icon = Icons.Outlined.Share,
        )
    }
    item(key = "backup-photo") {
        SwitchOption(
            title = stringResource(R.string.backup_include_photo),
            subtitle = stringResource(R.string.backup_include_photo_summary),
            checked = state.includePhoto,
            onChange = actions::onIncludePhoto,
        )
    }
    item(key = "backup-import") {
        Option(
            title = stringResource(R.string.backup_import),
            subtitle = stringResource(R.string.backup_import_summary),
            onClick = { if (!state.busy) actions.onImport() },
            icon = Icons.Outlined.Refresh,
        )
    }
}

/** "¿Restaurar el respaldo?": nada se reemplaza sin esta confirmación. */
@Composable
internal fun BackupConfirmHost(state: BackupUiState, actions: BackupActions) {
    if (!state.confirmRestore) return
    AlertDialog(
        onDismissRequest = actions::onCancelRestore,
        title = { Text(stringResource(R.string.backup_confirm_title)) },
        text = { Text(stringResource(R.string.backup_confirm_text)) },
        confirmButton = {
            TextButton(onClick = actions::onConfirmRestore) { Text(stringResource(R.string.backup_confirm_ok)) }
        },
        dismissButton = {
            TextButton(onClick = actions::onCancelRestore) { Text(stringResource(R.string.pin_cancel)) }
        },
    )
}

/** Sin acciones: valor por defecto de `SettingsScreen` y de las previews. */
internal object NoBackupActions : BackupActions {
    override fun onExport() = Unit
    override fun onImport() = Unit
    override fun onIncludePhoto(enabled: Boolean) = Unit
    override fun onConfirmRestore() = Unit
    override fun onCancelRestore() = Unit
}
