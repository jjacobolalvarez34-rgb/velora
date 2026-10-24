package app.velora.launcher.feature.appmenu

import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.res.stringResource
import app.velora.launcher.R
import app.velora.launcher.core.data.apps.ShortcutsRepository
import app.velora.launcher.core.designsystem.theme.Shapes
import app.velora.launcher.core.model.AppEntry
import app.velora.launcher.core.model.AppShortcut
import app.velora.launcher.feature.apps.AppActions

/** Fuente de atajos para los menús; la provee `VeloraApp`. null en previews: sin atajos. */
val LocalShortcutsRepository = staticCompositionLocalOf<ShortcutsRepository?> { null }

/** Menú de pulsación larga (PRODUCTO, MVP): atajos de la app arriba, acciones de Velora abajo. */
@Composable
fun AppMenu(
    expanded: Boolean,
    app: AppEntry,
    isFavorite: Boolean,
    actions: AppActions,
    bounds: () -> Rect?,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shortcuts by rememberShortcuts(app, expanded)
    DropdownMenu(expanded = expanded, onDismissRequest = onDismiss, shape = Shapes.menu, modifier = modifier) {
        shortcuts.forEach { shortcut ->
            MenuItem(shortcut.label, onDismiss) { actions.openShortcut(shortcut, bounds()) }
        }
        if (shortcuts.isNotEmpty()) HorizontalDivider()
        if (isFavorite) {
            MenuItem(stringResource(R.string.app_menu_remove_favorite), onDismiss) { actions.removeFavorite(app) }
        } else {
            MenuItem(stringResource(R.string.app_menu_add_favorite), onDismiss) { actions.addFavorite(app) }
        }
        MenuItem(stringResource(R.string.app_menu_hide), onDismiss) { actions.hide(app) }
        MenuItem(stringResource(R.string.app_menu_info), onDismiss) { actions.openInfo(app, bounds()) }
        if (app.canUninstall) {
            MenuItem(stringResource(R.string.app_menu_uninstall), onDismiss) { actions.uninstall(app) }
        }
    }
}

/** Los atajos se piden solo al abrir el menú y fuera del hilo principal (B-018). */
@Composable
private fun rememberShortcuts(app: AppEntry, expanded: Boolean) = LocalShortcutsRepository.current.let { repository ->
    produceState(emptyList<AppShortcut>(), app.key, expanded, repository) {
        if (expanded && repository != null) value = repository.shortcutsFor(app.key)
    }
}

@Composable
private fun MenuItem(label: String, onDismiss: () -> Unit, onClick: () -> Unit) {
    DropdownMenuItem(
        text = { Text(label) },
        onClick = {
            onDismiss()
            onClick()
        },
    )
}
