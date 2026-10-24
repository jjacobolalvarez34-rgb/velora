package app.velora.launcher.feature.appmenu

import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.res.stringResource
import app.velora.launcher.R
import app.velora.launcher.core.designsystem.theme.Shapes
import app.velora.launcher.core.model.AppEntry
import app.velora.launcher.feature.apps.AppActions

/** Menú de pulsación larga (PRODUCTO, MVP). Los atajos de la app (B-018) llegan después. */
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
    DropdownMenu(expanded = expanded, onDismissRequest = onDismiss, shape = Shapes.menu, modifier = modifier) {
        if (isFavorite) {
            MenuItem(stringResource(R.string.app_menu_remove_favorite), onDismiss) { actions.removeFavorite(app) }
        } else {
            MenuItem(stringResource(R.string.app_menu_add_favorite), onDismiss) { actions.addFavorite(app) }
        }
        MenuItem(stringResource(R.string.app_menu_info), onDismiss) { actions.openInfo(app, bounds()) }
        if (app.canUninstall) {
            MenuItem(stringResource(R.string.app_menu_uninstall), onDismiss) { actions.uninstall(app) }
        }
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
