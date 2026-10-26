package app.velora.launcher.feature.appmenu

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
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
import app.velora.launcher.core.designsystem.theme.Sizes
import app.velora.launcher.core.designsystem.theme.Spacing
import app.velora.launcher.core.model.AppEntry
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
) {
    val shortcuts by rememberShortcuts(app)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState()) {
        Column(Modifier.padding(bottom = Spacing.l)) {
            Header(app)
            shortcuts.forEach { shortcut ->
                MenuItem(Icons.Outlined.PlayArrow, shortcut.label, onDismiss) { actions.openShortcut(shortcut, null) }
            }
            if (shortcuts.isNotEmpty()) HorizontalDivider(Modifier.padding(vertical = Spacing.s))
            FavoriteItem(app, isFavorite, actions, onDismiss)
            MenuItem(Icons.Outlined.Info, stringResource(R.string.app_menu_info), onDismiss) {
                actions.openInfo(app, null)
            }
            MenuItem(Icons.Outlined.Clear, stringResource(R.string.app_menu_hide), onDismiss) { actions.hide(app) }
            if (app.canUninstall) {
                MenuItem(Icons.Outlined.Delete, stringResource(R.string.app_menu_uninstall), onDismiss) {
                    actions.uninstall(app)
                }
            }
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

@Composable
private fun Header(app: AppEntry) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(horizontal = Spacing.l, vertical = Spacing.m),
    ) {
        AppIcon(key = app.key, size = Sizes.favoriteIcon)
        Spacer(Modifier.width(Spacing.m))
        Text(app.label, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onSurface)
    }
}

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
