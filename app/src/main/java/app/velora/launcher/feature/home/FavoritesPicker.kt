package app.velora.launcher.feature.home

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import app.velora.launcher.R
import app.velora.launcher.core.data.favorites.FavoritesRepository
import app.velora.launcher.core.designsystem.theme.Sizes
import app.velora.launcher.core.designsystem.theme.Spacing
import app.velora.launcher.core.model.AppEntry
import app.velora.launcher.feature.apps.AppIcon

/**
 * Elegir favoritos la primera vez (PRODUCTO, MVP): aparece una sola vez si el inicio está vacío.
 * Se marcan con un toque (máximo [FavoritesRepository.MAX_FAVORITES]) y "Listo" los agrega en ese
 * orden. "Ahora no" lo cierra para siempre (luego se agregan desde la lista).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoritesPickerSheet(apps: List<AppEntry>, onDone: (List<AppEntry>) -> Unit, onSkip: () -> Unit) {
    val chosen = remember { mutableStateListOf<AppEntry>() }
    ModalBottomSheet(
        onDismissRequest = onSkip,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        PickerHeader(chosen.size)
        LazyColumn(Modifier.fillMaxWidth().heightIn(max = ListMaxHeight)) {
            items(apps, key = { it.key.stableId }) { app ->
                PickRow(app, checked = app in chosen) { on ->
                    if (!on) {
                        chosen.remove(app)
                    } else if (chosen.size <
                        FavoritesRepository.MAX_FAVORITES
                    ) {
                        chosen.add(app)
                    }
                }
            }
        }
        Column(Modifier.fillMaxWidth().padding(Spacing.l)) {
            Button(onClick = {
                onDone(chosen.toList())
            }, enabled = chosen.isNotEmpty(), modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.pick_favorites_done))
            }
            TextButton(onClick = onSkip, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.welcome_not_now))
            }
        }
    }
}

@Composable
private fun PickerHeader(count: Int) {
    Column(Modifier.padding(horizontal = Spacing.l)) {
        Text(
            stringResource(R.string.pick_favorites_title),
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.semantics { heading() },
        )
        Text(
            stringResource(R.string.pick_favorites_body, count, FavoritesRepository.MAX_FAVORITES),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = Spacing.xs, bottom = Spacing.s),
        )
    }
}

@Composable
private fun PickRow(app: AppEntry, checked: Boolean, onChange: (Boolean) -> Unit) {
    ListItem(
        headlineContent = { Text(app.label) },
        leadingContent = { AppIcon(key = app.key, size = Sizes.listIcon) },
        trailingContent = { Checkbox(checked = checked, onCheckedChange = null) },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = Modifier.toggleable(value = checked, role = Role.Checkbox, onValueChange = onChange),
    )
}

private val ListMaxHeight = 420.dp
