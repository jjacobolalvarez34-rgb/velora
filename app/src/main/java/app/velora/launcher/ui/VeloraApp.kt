package app.velora.launcher.ui

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import app.velora.launcher.core.designsystem.theme.VeloraTheme
import app.velora.launcher.di.AppContainer
import app.velora.launcher.feature.home.HomeRoute
import app.velora.launcher.feature.home.HomeViewModel

/** Raíz de Compose del inicio: tema + pantalla actual. */
@Composable
fun VeloraApp(container: AppContainer, onOpenSettings: () -> Unit) {
    VeloraTheme {
        HomeRoute(
            viewModel = viewModel(factory = HomeViewModel.factory(container)),
            onOpenSettings = onOpenSettings,
        )
    }
}
