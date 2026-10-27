package app.velora.launcher.feature.folders

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import app.velora.launcher.R
import app.velora.launcher.core.data.favorites.FoldersRepository
import app.velora.launcher.core.designsystem.theme.Sizes
import app.velora.launcher.core.designsystem.theme.Spacing
import app.velora.launcher.core.model.AppEntry
import app.velora.launcher.core.model.AppFolder
import app.velora.launcher.feature.apps.AppActions
import app.velora.launcher.feature.apps.AppIcon

/** Una carpeta abierta: sus apps (tocar abre, la X la saca), renombrar y eliminar. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FolderSheet(folder: AppFolder, apps: AppActions, actions: FolderActions, onDismiss: () -> Unit) {
    var renaming by remember { mutableStateOf(false) }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState()) {
        Column(Modifier.padding(bottom = Spacing.l)) {
            FolderHeader(
                name = folder.name,
                onRename = { renaming = !renaming },
                onDelete = {
                    onDismiss()
                    actions.delete(folder)
                },
            )
            if (renaming) {
                NameField(initial = folder.name, label = R.string.folder_rename) { name ->
                    actions.rename(folder, name)
                    renaming = false
                }
            }
            if (folder.apps.isEmpty()) EmptyFolderHint()
            LazyColumn {
                items(folder.apps, key = { it.key.stableId }) { app ->
                    FolderAppRow(
                        app = app,
                        onOpen = {
                            onDismiss()
                            apps.open(app, null)
                        },
                        onRemove = { actions.remove(folder, app) },
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyFolderHint() {
    Text(
        stringResource(R.string.folder_empty),
        style = MaterialTheme.typography.bodyMedium,
        modifier = Modifier.padding(horizontal = Spacing.l, vertical = Spacing.m),
    )
}

@Composable
private fun FolderAppRow(app: AppEntry, onOpen: () -> Unit, onRemove: () -> Unit) {
    ListItem(
        headlineContent = { Text(app.label) },
        leadingContent = { AppIcon(key = app.key, size = Sizes.favoriteIcon) },
        trailingContent = {
            IconButton(onClick = onRemove) {
                Icon(Icons.Outlined.Clear, contentDescription = stringResource(R.string.folder_remove_app, app.label))
            }
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = Modifier.clickable(onClick = onOpen),
    )
}

@Composable
private fun FolderHeader(name: String, onRename: () -> Unit, onDelete: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(start = Spacing.l, end = Spacing.s),
    ) {
        Text(
            name,
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier
                .weight(1f)
                .semantics { heading() },
        )
        IconButton(onClick = onRename) {
            Icon(Icons.Outlined.Edit, contentDescription = stringResource(R.string.folder_rename))
        }
        IconButton(onClick = onDelete) {
            Icon(Icons.Outlined.Delete, contentDescription = stringResource(R.string.folder_delete))
        }
    }
}

/** "Añadir a carpeta" desde el panel de una app: elegir una o crear una nueva con esa app. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddToFolderSheet(app: AppEntry, folders: List<AppFolder>, actions: FolderActions, onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState()) {
        Column(Modifier.padding(bottom = Spacing.l)) {
            Text(
                stringResource(R.string.folder_add_title, app.label),
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier
                    .padding(horizontal = Spacing.l, vertical = Spacing.s)
                    .semantics { heading() },
            )
            folders.forEach { folder ->
                val inside = folder.apps.any { it.key == app.key }
                ListItem(
                    headlineContent = { Text(folder.name) },
                    supportingContent = if (inside) {
                        { Text(stringResource(R.string.folder_already_in)) }
                    } else {
                        null
                    },
                    leadingContent = { FolderMosaic(folder.apps) },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    modifier = Modifier.clickable(enabled = !inside) {
                        onDismiss()
                        actions.add(folder, app)
                    },
                )
            }
            NameField(initial = "", label = R.string.folder_new) { name ->
                onDismiss()
                actions.create(name, app)
            }
        }
    }
}

@Composable
private fun NameField(initial: String, label: Int, onSave: (String) -> Unit) {
    var text by remember { mutableStateOf(initial) }
    val save = { if (text.isNotBlank()) onSave(text) }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.l, vertical = Spacing.s),
    ) {
        OutlinedTextField(
            value = text,
            onValueChange = { text = it.take(FoldersRepository.MAX_NAME) },
            label = { Text(stringResource(label)) },
            leadingIcon = { Icon(Icons.Outlined.Add, contentDescription = null) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { save() }),
            modifier = Modifier.weight(1f),
        )
        TextButton(onClick = save) { Text(stringResource(R.string.folder_save)) }
    }
}
