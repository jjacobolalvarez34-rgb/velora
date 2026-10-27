package app.velora.launcher.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.velora.launcher.core.data.notifications.NotificationsRepository
import app.velora.launcher.core.model.AppEntry
import app.velora.launcher.core.model.AppFolder
import app.velora.launcher.core.model.ThemeSettings
import app.velora.launcher.di.AppContainer
import app.velora.launcher.feature.appmenu.AppMenuExtras
import app.velora.launcher.feature.appmenu.AppMenuSheet
import app.velora.launcher.feature.apps.AppActions
import app.velora.launcher.feature.drawer.DrawerUiState
import app.velora.launcher.feature.folders.AddToFolderSheet
import app.velora.launcher.feature.folders.FolderActions
import app.velora.launcher.feature.folders.FolderSheet
import app.velora.launcher.feature.widgets.HomeMenuSheet
import app.velora.launcher.feature.widgets.WidgetPickerSheet
import kotlinx.coroutines.launch

/** Qué panel está abierto sobre el inicio. */
@Stable
internal class LauncherSheetsState {
    var appMenu by mutableStateOf<AppEntry?>(null)
    var homeMenu by mutableStateOf(false)
    var widgetPicker by mutableStateOf(false)
    var openFolder by mutableStateOf<Long?>(null)
    var addToFolder by mutableStateOf<AppEntry?>(null)
}

@Composable
internal fun rememberLauncherSheets() = remember { LauncherSheetsState() }

/** Paneles desde abajo: menú de una app, menú del inicio, widgets y carpetas. */
@Composable
internal fun LauncherSheets(
    sheets: LauncherSheetsState,
    container: AppContainer,
    drawer: DrawerUiState,
    actions: AppActions,
    onOpenSettings: () -> Unit,
    theme: ThemeSettings,
    folders: List<AppFolder>,
    folderActions: FolderActions,
) {
    sheets.appMenu?.let { app ->
        AppMenuSheet(
            app = app,
            isFavorite = app.key.stableId in drawer.favoriteIds,
            actions = actions,
            onOpenSettings = onOpenSettings,
            onDismiss = { sheets.appMenu = null },
            extras = rememberMenuExtras(container, theme, app) { sheets.addToFolder = app },
        )
    }
    if (sheets.homeMenu) {
        HomeMenuSheet(
            onAddWidget = { sheets.widgetPicker = true },
            onOpenSettings = onOpenSettings,
            onDismiss = { sheets.homeMenu = false },
        )
    }
    if (sheets.widgetPicker) {
        WidgetPickerSheet(container.widgetHost, onDismiss = { sheets.widgetPicker = false })
    }
    sheets.openFolder?.let { id ->
        folders.firstOrNull { it.id == id }?.let { folder ->
            FolderSheet(folder, actions, folderActions, onDismiss = { sheets.openFolder = null })
        }
    }
    sheets.addToFolder?.let { app ->
        AddToFolderSheet(app, folders, folderActions, onDismiss = { sheets.addToFolder = null })
    }
}

/** Notificaciones y tiempo de uso solo si el usuario los activó. */
@Composable
private fun rememberMenuExtras(
    container: AppContainer,
    theme: ThemeSettings,
    app: AppEntry,
    onAddToFolder: () -> Unit,
): AppMenuExtras {
    val repository = container.notificationsRepository
    val all by repository.byAppList.collectAsStateWithLifecycle()
    val notifications = if (theme.notifications) all[NotificationsRepository.keyOf(app.key)].orEmpty() else emptyList()
    return AppMenuExtras(
        notifications = notifications,
        notificationActions = repository.notificationActions.takeIf { theme.notifications },
        usage = container.usageRepository.takeIf { theme.usageTime },
        onAddToFolder = onAddToFolder,
    )
}

/** Escribir carpetas en el scope de la app: el cambio se guarda aunque se cierre el panel. */
@Composable
internal fun rememberFolderActions(container: AppContainer, sheets: LauncherSheetsState): FolderActions =
    remember(container, sheets) {
        val repository = container.foldersRepository
        val scope = container.applicationScope
        object : FolderActions {
            override fun open(folder: AppFolder) {
                sheets.openFolder = folder.id
            }

            override fun create(name: String, first: AppEntry?) {
                scope.launch { repository.create(name, first?.key) }
            }

            override fun rename(folder: AppFolder, name: String) {
                scope.launch { repository.rename(folder.id, name) }
            }

            override fun delete(folder: AppFolder) {
                scope.launch { repository.delete(folder.id) }
            }

            override fun add(folder: AppFolder, app: AppEntry) {
                scope.launch { repository.add(folder.id, app.key) }
            }

            override fun remove(folder: AppFolder, app: AppEntry) {
                scope.launch { repository.remove(folder.id, app.key) }
            }
        }
    }
