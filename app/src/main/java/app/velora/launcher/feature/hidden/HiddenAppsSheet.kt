package app.velora.launcher.feature.hidden

import android.content.res.Configuration
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.velora.launcher.R
import app.velora.launcher.core.data.hidden.PrivateSpaceState
import app.velora.launcher.core.designsystem.theme.Sizes
import app.velora.launcher.core.designsystem.theme.Spacing
import app.velora.launcher.core.designsystem.theme.VeloraTheme
import app.velora.launcher.core.model.AppEntry
import app.velora.launcher.core.model.AppKey
import app.velora.launcher.feature.apps.AppIcon

/**
 * Panel "Apps ocultas" (ADR-034), lo abre el gesto de dos dedos del inicio. Si hay PIN, primero
 * la pantalla de PIN y nada más: el panel no existe hasta escribirlo. Al cerrarse se vuelve a
 * bloquear. Abajo, el espacio privado de Android 15+ si existe (ADR-033).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HiddenAppsSheet(viewModel: HiddenViewModel, onOpenApp: (AppEntry) -> Unit, onDismiss: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(viewModel) { viewModel.onOpen() }
    val close = {
        viewModel.relock()
        onDismiss()
    }
    if (!state.contentVisible) {
        state.pin?.let { PinDialog(it, onSubmit = viewModel::onPinEntered, onCancel = close) }
        return
    }
    ModalBottomSheet(onDismissRequest = close, sheetState = rememberModalBottomSheetState()) {
        HiddenAppsContent(
            state = state,
            onOpen = { app ->
                close()
                onOpenApp(app)
            },
            onUnhide = viewModel::onUnhide,
            onPrivateSpace = viewModel::onPrivateSpace,
        )
    }
}

/** El contenido del panel, sin ViewModel (para previews y pruebas de UI). */
@Composable
fun HiddenAppsContent(
    state: HiddenUiState,
    onOpen: (AppEntry) -> Unit,
    onUnhide: (AppEntry) -> Unit,
    onPrivateSpace: (lock: Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(modifier = modifier.padding(bottom = Spacing.l)) {
        item(key = "title") {
            Text(
                stringResource(R.string.hidden_sheet_title),
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier
                    .padding(horizontal = Spacing.l, vertical = Spacing.s)
                    .semantics { heading() },
            )
        }
        if (state.apps.isEmpty()) {
            item(key = "empty") { Hint(stringResource(R.string.hidden_sheet_empty)) }
        }
        items(state.apps, key = { "hidden-${it.key.stableId}" }) { app ->
            HiddenAppRow(app, onOpen = { onOpen(app) }, onUnhide = { onUnhide(app) })
        }
        privateSpaceItems(state, onOpen, onPrivateSpace)
    }
}

private fun LazyListScope.privateSpaceItems(
    state: HiddenUiState,
    onOpen: (AppEntry) -> Unit,
    onPrivateSpace: (lock: Boolean) -> Unit,
) {
    if (state.privateSpace == PrivateSpaceState.Unavailable) return
    val locked = state.privateSpace == PrivateSpaceState.Locked
    item(key = "private-space") {
        ListItem(
            headlineContent = { Text(stringResource(R.string.private_space_title)) },
            supportingContent = {
                Text(stringResource(if (locked) R.string.private_space_locked else R.string.private_space_unlocked))
            },
            leadingContent = { Icon(Icons.Outlined.Lock, contentDescription = null) },
            trailingContent = {
                TextButton(onClick = { onPrivateSpace(!locked) }) {
                    Text(stringResource(if (locked) R.string.private_space_unlock else R.string.private_space_lock))
                }
            },
            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        )
    }
    if (!locked) {
        items(state.privateApps, key = { "private-${it.key.stableId}" }) { app ->
            HiddenAppRow(app, onOpen = { onOpen(app) }, onUnhide = null)
        }
    }
}

/** Tocar abre la app; mantener presionado o el botón la vuelven a mostrar en la lista. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HiddenAppRow(app: AppEntry, onOpen: () -> Unit, onUnhide: (() -> Unit)?) {
    val haptics = LocalHapticFeedback.current
    ListItem(
        headlineContent = { Text(app.label) },
        leadingContent = { AppIcon(key = app.key, size = Sizes.favoriteIcon) },
        trailingContent = onUnhide?.let { unhide ->
            {
                TextButton(onClick = unhide) { Text(stringResource(R.string.hidden_unhide)) }
            }
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = Modifier.combinedClickable(
            onClick = onOpen,
            onLongClickLabel = onUnhide?.let { stringResource(R.string.hidden_unhide) },
            onLongClick = onUnhide?.let { unhide ->
                {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    unhide()
                }
            },
        ),
    )
}

@Composable
private fun Hint(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = Spacing.l, vertical = Spacing.m),
    )
}

@Preview(name = "Claro")
@Preview(name = "Oscuro", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun HiddenAppsContentPreview() {
    val bank = AppEntry(AppKey("com.bank", "Main", 0), "Banco", "B", "banco", false, true)
    val diary = AppEntry(AppKey("com.diary", "Main", 11), "Diario", "D", "diario", false, true, isPrivate = true)
    VeloraTheme(reducedMotion = true) {
        Surface {
            HiddenAppsContent(
                state = HiddenUiState(
                    apps = listOf(bank),
                    privateApps = listOf(diary),
                    privateSpace = PrivateSpaceState.Unlocked,
                    loaded = true,
                ),
                onOpen = {},
                onUnhide = {},
                onPrivateSpace = {},
            )
        }
    }
}
