package app.velora.launcher.ui

import android.content.ActivityNotFoundException
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.velora.launcher.core.data.log.Logger
import app.velora.launcher.core.model.ThemePreset
import app.velora.launcher.core.model.ThemeSettings
import app.velora.launcher.di.AppContainer
import app.velora.launcher.feature.home.HomeUiState
import app.velora.launcher.feature.home.HomeViewModel
import app.velora.launcher.feature.home.components.HomeTip
import app.velora.launcher.feature.home.components.HomeTips
import kotlinx.coroutines.launch

/**
 * Los consejos del inicio que todavía tienen sentido: lo que ya hiciste (o quitaste) no vuelve.
 * Tocar uno te lleva a hacerlo y lo marca como hecho.
 */
@Composable
internal fun HomeTipsSlot(
    container: AppContainer,
    theme: ThemeSettings,
    home: HomeUiState,
    homeViewModel: HomeViewModel,
    drawerState: DrawerState,
    sheets: LauncherSheetsState,
    onOpenSettings: () -> Unit,
) {
    val isDefault by homeViewModel.isDefaultHome.collectAsStateWithLifecycle()
    val tips = pendingTips(theme, home, isDefault)
    if (tips.isEmpty()) return
    val chooseHome = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        homeViewModel.onResume()
    }
    val done = { tip: HomeTip ->
        container.applicationScope.launch {
            container.prefsRepository.updateCustom { it.copy(dismissedTips = it.dismissedTips + tip.id) }
        }
        Unit
    }
    HomeTips(
        tips = tips,
        onOpen = { tip ->
            done(tip)
            when (tip) {
                HomeTip.SetDefault -> runCatching { chooseHome.launch(homeViewModel.defaultHomeRequest()) }
                    .onFailure {
                        Logger.w("HomeTips", it as? ActivityNotFoundException ?: it) { "Sin pantalla de inicio" }
                    }

                HomeTip.Favorites, HomeTip.Alphabet -> drawerState.open()

                HomeTip.Theme, HomeTip.Notifications -> onOpenSettings()

                HomeTip.Widgets -> sheets.widgetPicker = true
            }
        },
        onDismiss = done,
    )
}

private fun pendingTips(theme: ThemeSettings, home: HomeUiState, isDefault: Boolean?): List<HomeTip> {
    if (!theme.custom.visible.tips) return emptyList()
    val dismissed = theme.custom.dismissedTips
    return buildList {
        if (isDefault == false) add(HomeTip.SetDefault)
        if (home.favorites.isEmpty()) add(HomeTip.Favorites)
        if (theme.preset == ThemePreset.Wallpaper && theme.photoBackground == null) add(HomeTip.Theme)
        add(HomeTip.Alphabet)
        if (!theme.notifications) add(HomeTip.Notifications)
        add(HomeTip.Widgets)
    }.filter { it.id !in dismissed }
}
