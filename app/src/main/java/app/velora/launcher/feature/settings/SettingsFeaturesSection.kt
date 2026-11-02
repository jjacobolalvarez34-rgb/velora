package app.velora.launcher.feature.settings

import androidx.annotation.StringRes
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import app.velora.launcher.R
import app.velora.launcher.core.model.ThemeSettings

/** Fondo propio y funciones que dependen de un acceso del sistema (separadas de [SettingsActions]). */
interface FeatureActions {
    fun onPickPhoto()
    fun onRemovePhoto()
    fun onExportTheme()
    fun onImportTheme()
    fun onSystemWallpaper()
    fun onNotifications(enabled: Boolean)
    fun onNotificationAccess()
    fun onDoubleTapLock(enabled: Boolean)
    fun onLockAccess()
    fun onUsageTime(enabled: Boolean)
    fun onUsageAccess()
    fun onContactsSearch(enabled: Boolean)

    /** Ayuda por fabricante: la lista de optimización de batería del sistema. */
    fun onOpenBattery()
}

/** Si el sistema ya dio el acceso que cada función necesita. Se vuelve a leer al regresar a Ajustes. */
@Immutable
data class FeatureStatus(
    val notificationAccess: Boolean = false,
    val lockService: Boolean = false,
    /** El gesto de bloqueo existe desde Android 9. */
    val lockSupported: Boolean = true,
    val usageAccess: Boolean = false,
    val contactsPermission: Boolean = false,
)

/** Fondo: una foto propia encima del tema elegido (ADR-022), o el fondo del teléfono. */
internal fun LazyListScope.backgroundSection(theme: ThemeSettings, actions: FeatureActions) {
    item(key = "background") { SectionTitle(R.string.settings_background) }
    item(key = "background-photo") {
        Option(
            title = stringResource(
                if (theme.photoBackground == null) R.string.settings_photo_pick else R.string.settings_photo_change,
            ),
            subtitle = stringResource(R.string.settings_photo_summary),
            onClick = actions::onPickPhoto,
            icon = Icons.Outlined.Edit,
        )
    }
    if (theme.photoBackground != null) {
        item(key = "background-remove") {
            Option(
                title = stringResource(R.string.settings_photo_remove),
                subtitle = stringResource(R.string.settings_photo_remove_summary),
                onClick = actions::onRemovePhoto,
                icon = Icons.Outlined.Delete,
            )
        }
    }
    item(key = "background-system") {
        Option(
            title = stringResource(R.string.settings_system_wallpaper),
            subtitle = stringResource(R.string.settings_system_wallpaper_summary),
            onClick = actions::onSystemWallpaper,
            icon = Icons.Outlined.Settings,
        )
    }
}

/** Compartir temas de Velora con otros usuarios de Velora (ADR-027). */
internal fun LazyListScope.shareSection(actions: FeatureActions) {
    item(key = "share") { SectionTitle(R.string.settings_share_themes) }
    item(key = "share-export") {
        Option(
            title = stringResource(R.string.settings_theme_export),
            subtitle = stringResource(R.string.settings_theme_export_summary),
            onClick = actions::onExportTheme,
            icon = Icons.Outlined.Share,
        )
    }
    item(key = "share-import") {
        Option(
            title = stringResource(R.string.settings_theme_import),
            subtitle = stringResource(R.string.settings_theme_import_summary),
            onClick = actions::onImportTheme,
            icon = Icons.Outlined.Add,
        )
    }
}

/**
 * Notificaciones en el inicio y doble toque para bloquear. Antes de encender cualquiera se explica
 * qué acceso pide y para qué (SEGURIDAD §3: aviso destacado antes del diálogo del sistema).
 */
internal fun LazyListScope.featuresSection(theme: ThemeSettings, status: FeatureStatus, actions: FeatureActions) {
    item(key = "features") { SectionTitle(R.string.settings_features) }
    item(key = "notifications") {
        DisclosedSwitch(
            title = R.string.settings_notifications,
            summary = R.string.settings_notifications_summary,
            disclosure = R.string.settings_notifications_disclosure,
            checked = theme.notifications,
            onChange = actions::onNotifications,
            icon = Icons.Outlined.Notifications,
        )
    }
    if (theme.notifications && !status.notificationAccess) {
        item(key = "notifications-access") {
            Option(
                title = stringResource(R.string.settings_access_missing),
                subtitle = stringResource(R.string.settings_notifications_access),
                onClick = actions::onNotificationAccess,
                icon = Icons.Outlined.Settings,
            )
        }
    }
    if (status.lockSupported) lockItems(theme, status, actions)
    usageItems(theme, status, actions)
    contactsItems(theme, status, actions)
}

/** Tiempo de uso de hoy en el panel de cada app (ADR-024). */
private fun LazyListScope.usageItems(theme: ThemeSettings, status: FeatureStatus, actions: FeatureActions) {
    item(key = "usage-time") {
        DisclosedSwitch(
            title = R.string.settings_usage_time,
            summary = R.string.settings_usage_time_summary,
            disclosure = R.string.settings_usage_time_disclosure,
            checked = theme.usageTime,
            onChange = actions::onUsageTime,
            icon = Icons.Outlined.DateRange,
        )
    }
    if (theme.usageTime && !status.usageAccess) {
        item(key = "usage-access") {
            Option(
                title = stringResource(R.string.settings_access_missing),
                subtitle = stringResource(R.string.settings_usage_time_access),
                onClick = actions::onUsageAccess,
                icon = Icons.Outlined.Settings,
            )
        }
    }
}

/** Contactos en el buscador (ADR-025). */
private fun LazyListScope.contactsItems(theme: ThemeSettings, status: FeatureStatus, actions: FeatureActions) {
    item(key = "contacts-search") {
        DisclosedSwitch(
            title = R.string.settings_contacts,
            summary = R.string.settings_contacts_summary,
            disclosure = R.string.settings_contacts_disclosure,
            checked = theme.contactsSearch,
            onChange = actions::onContactsSearch,
            icon = Icons.Outlined.Person,
        )
    }
    if (theme.contactsSearch && !status.contactsPermission) {
        item(key = "contacts-access") {
            Option(
                title = stringResource(R.string.settings_access_missing),
                subtitle = stringResource(R.string.settings_contacts_access),
                onClick = { actions.onContactsSearch(true) },
                icon = Icons.Outlined.Settings,
            )
        }
    }
}

private fun LazyListScope.lockItems(theme: ThemeSettings, status: FeatureStatus, actions: FeatureActions) {
    item(key = "double-tap-lock") {
        DisclosedSwitch(
            title = R.string.settings_double_tap_lock,
            summary = R.string.settings_double_tap_lock_summary,
            disclosure = R.string.settings_double_tap_lock_disclosure,
            checked = theme.doubleTapLock,
            onChange = actions::onDoubleTapLock,
            icon = Icons.Outlined.Lock,
        )
    }
    if (theme.doubleTapLock && !status.lockService) {
        item(key = "lock-access") {
            Option(
                title = stringResource(R.string.settings_access_missing),
                subtitle = stringResource(R.string.settings_double_tap_lock_access),
                onClick = actions::onLockAccess,
                icon = Icons.Outlined.Settings,
            )
        }
    }
}

/** Interruptor que, al encenderse, primero muestra un aviso; apagarlo es inmediato. */
@Composable
private fun DisclosedSwitch(
    @StringRes title: Int,
    @StringRes summary: Int,
    @StringRes disclosure: Int,
    checked: Boolean,
    onChange: (Boolean) -> Unit,
    icon: ImageVector,
) {
    var asking by rememberSaveable { mutableStateOf(false) }
    SwitchOption(
        title = stringResource(title),
        subtitle = stringResource(summary),
        checked = checked,
        onChange = { enabled -> if (enabled) asking = true else onChange(false) },
        icon = icon,
    )
    if (asking) {
        AlertDialog(
            onDismissRequest = { asking = false },
            title = { Text(stringResource(title)) },
            text = { Text(stringResource(disclosure)) },
            confirmButton = {
                TextButton(onClick = {
                    asking = false
                    onChange(true)
                }) { Text(stringResource(R.string.settings_disclosure_continue)) }
            },
            dismissButton = {
                TextButton(onClick = { asking = false }) { Text(stringResource(R.string.settings_disclosure_cancel)) }
            },
        )
    }
}
