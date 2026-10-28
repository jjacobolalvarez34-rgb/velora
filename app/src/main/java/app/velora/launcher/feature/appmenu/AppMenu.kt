package app.velora.launcher.feature.appmenu

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.List
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import app.velora.launcher.R
import app.velora.launcher.core.data.apps.ShortcutsRepository
import app.velora.launcher.core.data.notifications.NotificationActions
import app.velora.launcher.core.data.usage.UsageRepository
import app.velora.launcher.core.designsystem.theme.Sizes
import app.velora.launcher.core.designsystem.theme.Spacing
import app.velora.launcher.core.model.AppEntry
import app.velora.launcher.core.model.AppNotification
import app.velora.launcher.core.model.AppShortcut
import app.velora.launcher.feature.apps.AppActions
import app.velora.launcher.feature.apps.AppIcon

/** Fuente de atajos para los menús; la provee `VeloraApp`. null en previews: sin atajos. */
val LocalShortcutsRepository = staticCompositionLocalOf<ShortcutsRepository?> { null }

/**
 * Menú de una app como panel desde abajo (PRODUCTO, MVP): el ícono y el nombre arriba, sus
 * atajos, y las acciones de Velora con su ícono. Al final, los ajustes de Velora.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppMenuSheet(
    app: AppEntry,
    isFavorite: Boolean,
    actions: AppActions,
    onOpenSettings: () -> Unit,
    onDismiss: () -> Unit,
    extras: AppMenuExtras = AppMenuExtras(),
) {
    val shortcuts by rememberShortcuts(app)
    val usage by produceState<Long?>(null, app.key, extras.usage) { value = extras.usage?.todayMs(app.key.packageName) }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState()) {
        // Con notificaciones y atajos puede ser largo: se desplaza.
        Column(Modifier.verticalScroll(rememberScrollState()).padding(bottom = Spacing.l)) {
            Header(app, usage)
            extras.onSetLimit?.let { LimitChooser(extras.usageLimit, it) }
            extras.notificationActions?.let { AppNotificationsList(extras.notifications, it, onDismiss) }
            if (extras.notifications.isNotEmpty()) HorizontalDivider(Modifier.padding(vertical = Spacing.s))
            shortcuts.forEach { shortcut ->
                MenuItem(Icons.Outlined.PlayArrow, shortcut.label, onDismiss) { actions.openShortcut(shortcut, null) }
            }
            if (shortcuts.isNotEmpty()) HorizontalDivider(Modifier.padding(vertical = Spacing.s))
            AppItems(app, isFavorite, actions, extras.onAddToFolder, onDismiss)
            HorizontalDivider(Modifier.padding(vertical = Spacing.s))
            MenuItem(
                Icons.Outlined.Settings,
                stringResource(R.string.settings_launcher_label),
                onDismiss,
                onOpenSettings,
            )
        }
    }
}

/** Lo que Velora hace con la app: favorito, carpeta, información, ocultar, desinstalar. */
@Composable
private fun AppItems(
    app: AppEntry,
    isFavorite: Boolean,
    actions: AppActions,
    onAddToFolder: (() -> Unit)?,
    onDismiss: () -> Unit,
) {
    FavoriteItem(app, isFavorite, actions, onDismiss)
    onAddToFolder?.let { addToFolder ->
        MenuItem(
            Icons.AutoMirrored.Outlined.List,
            stringResource(R.string.app_menu_add_to_folder),
            onDismiss,
            addToFolder,
        )
    }
    MenuItem(Icons.Outlined.Info, stringResource(R.string.app_menu_info), onDismiss) {
        actions.openInfo(app, null)
    }
    MenuItem(Icons.Outlined.Clear, stringResource(R.string.app_menu_hide), onDismiss) { actions.hide(app) }
    if (app.canUninstall) {
        MenuItem(Icons.Outlined.Delete, stringResource(R.string.app_menu_uninstall), onDismiss) {
            actions.uninstall(app)
        }
    }
}

@Composable
private fun FavoriteItem(app: AppEntry, isFavorite: Boolean, actions: AppActions, onDismiss: () -> Unit) {
    if (isFavorite) {
        MenuItem(Icons.Filled.Star, stringResource(R.string.app_menu_remove_favorite), onDismiss) {
            actions.removeFavorite(app)
        }
    } else {
        MenuItem(Icons.Outlined.Star, stringResource(R.string.app_menu_add_favorite), onDismiss) {
            actions.addFavorite(app)
        }
    }
}

/**
 * Lo opcional del panel: notificaciones de la app (ADR-023), tiempo de uso de hoy (ADR-024) y
 * "Añadir a carpeta". Vacío en previews y con las funciones apagadas.
 */
@Immutable
data class AppMenuExtras(
    val notifications: List<AppNotification> = emptyList(),
    val notificationActions: NotificationActions? = null,
    val usage: UsageRepository? = null,
    val onAddToFolder: (() -> Unit)? = null,
    /** Freno de uso de esta app en minutos por día (ADR-024); [onSetLimit] null = función apagada. */
    val usageLimit: Int? = null,
    val onSetLimit: ((Int?) -> Unit)? = null,
)

@Composable
private fun Header(app: AppEntry, usageMs: Long?) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(horizontal = Spacing.l, vertical = Spacing.m),
    ) {
        AppIcon(key = app.key, size = Sizes.favoriteIcon)
        Spacer(Modifier.width(Spacing.m))
        Column {
            Text(app.label, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onSurface)
            usageMs?.let {
                Text(
                    stringResource(R.string.app_menu_usage_today, formatUsage(it)),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/** "1 h 20 min", "35 min" o "menos de 1 min". */
@Composable
private fun formatUsage(ms: Long): String {
    val minutes = ms / MS_PER_MINUTE
    val hours = minutes / MINUTES_PER_HOUR
    return when {
        minutes < 1 -> stringResource(R.string.usage_under_minute)
        hours == 0L -> stringResource(R.string.usage_minutes, minutes)
        else -> stringResource(R.string.usage_hours_minutes, hours, minutes % MINUTES_PER_HOUR)
    }
}

private const val MS_PER_MINUTE = 60_000L
private const val MINUTES_PER_HOUR = 60L

/** Fila de un menú en panel: ícono a la izquierda y texto; tocarla cierra el panel y hace la acción. */
@Composable
internal fun MenuItem(icon: ImageVector, label: String, onDismiss: () -> Unit, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(label) },
        leadingContent = { Icon(icon, contentDescription = null) },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = Modifier.clickable {
            onDismiss()
            onClick()
        },
    )
}

/** Los atajos se piden al abrir el menú y fuera del hilo principal (B-018). */
@Composable
private fun rememberShortcuts(app: AppEntry) = LocalShortcutsRepository.current.let { repository ->
    produceState(emptyList<AppShortcut>(), app.key, repository) {
        if (repository != null) value = repository.shortcutsFor(app.key)
    }
}
