package app.velora.launcher.feature.settings

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import app.velora.launcher.R
import app.velora.launcher.core.model.AppEntry
import app.velora.launcher.core.model.TextSize
import app.velora.launcher.core.model.ThemeSettings

/** Diseño: modo zurdo y tamaño de texto. */
internal fun LazyListScope.layoutSection(theme: ThemeSettings, actions: SettingsActions) {
    item(key = "layout") { SectionTitle(R.string.settings_layout) }
    item(key = "left-handed") {
        SwitchOption(
            title = stringResource(R.string.settings_left_handed),
            subtitle = stringResource(R.string.settings_left_handed_summary),
            checked = theme.leftHanded,
            onChange = actions::onLeftHanded,
        )
    }
    item(key = "full-list") {
        SwitchOption(
            title = stringResource(R.string.settings_full_list),
            subtitle = stringResource(R.string.settings_full_list_summary),
            checked = theme.fullList,
            onChange = actions::onFullList,
        )
    }
    item(key = "text-size") { SectionTitle(R.string.settings_text_size) }
    items(TextSize.entries.size, key = { "text-${TextSize.entries[it]}" }) { index ->
        val size = TextSize.entries[index]
        RadioOption(stringResource(textSizeLabel(size)), theme.textSize == size, { actions.onTextSize(size) })
    }
}

/** Favoritos en su orden, con flechas para subir y bajar (también accesibles con TalkBack). */
internal fun LazyListScope.favoritesSection(favorites: List<AppEntry>, actions: SettingsActions) {
    item(key = "favorites") { SectionTitle(R.string.settings_favorites) }
    if (favorites.isEmpty()) {
        item(key = "favorites-empty") { PlainRow(stringResource(R.string.settings_favorites_empty)) }
    }
    items(favorites.size, key = { "favorite-${favorites[it].key.stableId}" }) { index ->
        val app = favorites[index]
        ListItem(
            headlineContent = { Text(app.label) },
            trailingContent = {
                MoveButtons(
                    label = app.label,
                    canMoveUp = index > 0,
                    canMoveDown = index < favorites.lastIndex,
                    onMove = { offset -> actions.onMoveFavorite(app, offset) },
                )
            },
            colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.background),
        )
    }
}

@Composable
private fun SwitchOption(title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = { Text(subtitle) },
        trailingContent = { Switch(checked = checked, onCheckedChange = null) },
        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.background),
        modifier = Modifier.toggleable(value = checked, role = Role.Switch, onValueChange = onChange),
    )
}

@Composable
private fun MoveButtons(label: String, canMoveUp: Boolean, canMoveDown: Boolean, onMove: (Int) -> Unit) {
    val up = stringResource(R.string.settings_move_up, label)
    val down = stringResource(R.string.settings_move_down, label)
    Row {
        IconButton(
            onClick = { onMove(-1) },
            enabled = canMoveUp,
            modifier = Modifier.semantics {
                contentDescription =
                    up
            },
        ) {
            Text("↑", style = MaterialTheme.typography.titleMedium)
        }
        IconButton(
            onClick = { onMove(1) },
            enabled = canMoveDown,
            modifier = Modifier.semantics {
                contentDescription =
                    down
            },
        ) {
            Text("↓", style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
private fun PlainRow(text: String) {
    ListItem(
        headlineContent = { Text(text) },
        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.background),
    )
}

private fun textSizeLabel(size: TextSize): Int = when (size) {
    TextSize.Small -> R.string.text_size_small
    TextSize.Normal -> R.string.text_size_normal
    TextSize.Large -> R.string.text_size_large
}
