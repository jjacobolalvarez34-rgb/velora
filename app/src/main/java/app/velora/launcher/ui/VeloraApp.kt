package app.velora.launcher.ui

import android.content.ActivityNotFoundException
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import app.velora.launcher.R
import app.velora.launcher.core.data.log.Logger
import app.velora.launcher.core.designsystem.theme.VeloraTheme
import app.velora.launcher.core.model.AppEntry
import app.velora.launcher.di.AppContainer
import app.velora.launcher.feature.apps.AppActions
import app.velora.launcher.feature.apps.LocalIconRepository
import app.velora.launcher.feature.apps.rememberAppOpener
import app.velora.launcher.feature.drawer.DrawerViewModel
import app.velora.launcher.feature.home.HomeEvent
import app.velora.launcher.feature.home.HomeViewModel
import app.velora.launcher.feature.welcome.WelcomeScreen
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter

/**
 * Raíz de Compose del inicio.
 *
 * @param homePresses cada vez que se pulsa Inicio estando en Velora; true = animar el regreso.
 */
@Composable
fun VeloraApp(container: AppContainer, homePresses: Flow<Boolean>, onOpenSettings: () -> Unit) {
    VeloraTheme {
        CompositionLocalProvider(LocalIconRepository provides container.iconRepository) {
            val homeViewModel: HomeViewModel = viewModel(factory = HomeViewModel.factory(container))
            val drawerViewModel: DrawerViewModel = viewModel(factory = DrawerViewModel.factory(container))
            val home by homeViewModel.uiState.collectAsStateWithLifecycle()

            LifecycleResumeEffect(homeViewModel) {
                homeViewModel.onResume()
                onPauseOrDispose {}
            }
            HomeEventMessages(homeViewModel)

            if (home.showWelcome) {
                WelcomeRoute(homeViewModel)
            } else {
                LauncherRoute(container, homeViewModel, drawerViewModel, homePresses, onOpenSettings)
            }
        }
    }
}

@Composable
private fun LauncherRoute(
    container: AppContainer,
    homeViewModel: HomeViewModel,
    drawerViewModel: DrawerViewModel,
    homePresses: Flow<Boolean>,
    onOpenSettings: () -> Unit,
) {
    val home by homeViewModel.uiState.collectAsStateWithLifecycle()
    val drawer by drawerViewModel.uiState.collectAsStateWithLifecycle()
    val drawerState = rememberDrawerState()
    val listState = rememberLazyListState()
    val searchState = rememberTextFieldState()
    val opener = rememberAppOpener(container.appLauncher)
    val actions = remember(opener, homeViewModel) {
        object : AppActions {
            override fun open(app: AppEntry, bounds: Rect?) = opener.open(app, bounds)
            override fun openInfo(app: AppEntry, bounds: Rect?) = opener.openInfo(app, bounds)
            override fun uninstall(app: AppEntry) = opener.uninstall(app)
            override fun addFavorite(app: AppEntry) = homeViewModel.onAddFavorite(app.key)
            override fun removeFavorite(app: AppEntry) = homeViewModel.onRemoveFavorite(app.key)
        }
    }
    DrawerEffects(drawerState, searchState, homePresses, drawerViewModel::onQueryChange)
    LauncherScreen(
        home = home,
        drawer = drawer,
        drawerState = drawerState,
        listState = listState,
        searchState = searchState,
        actions = actions,
        onOpenSettings = onOpenSettings,
    )
}

/** Comportamiento del cajón que no es dibujo: búsqueda, atrás, Inicio y salir a otra app. */
@Composable
private fun DrawerEffects(
    drawerState: DrawerState,
    searchState: TextFieldState,
    homePresses: Flow<Boolean>,
    onQueryChange: (String) -> Unit,
) {
    val keyboard = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    // Escribir en el buscador filtra la lista.
    LaunchedEffect(searchState) {
        snapshotFlow { searchState.text.toString() }.collect(onQueryChange)
    }
    // Al cerrarse la lista del todo: buscador limpio y teclado guardado.
    LaunchedEffect(drawerState) {
        snapshotFlow { drawerState.isOpen }.distinctUntilChanged().filter { !it }.collect {
            searchState.clearText()
            keyboard?.hide()
            focusManager.clearFocus()
        }
    }
    // Pulsar Inicio estando en Velora vuelve al estado raíz (ARQUITECTURA §5).
    LaunchedEffect(homePresses) {
        homePresses.collect { animate -> if (animate) drawerState.close() else drawerState.snapClosed() }
    }
    // Al irse a otra app el inicio queda listo, sin animación al volver (ESTILO §5.3).
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) { drawerState.snapClosed() }
    BackHandler(enabled = drawerState.isOpen) {
        if (searchState.text.isNotEmpty()) searchState.clearText() else drawerState.close()
    }
}

@Composable
private fun WelcomeRoute(viewModel: HomeViewModel) {
    val context = LocalContext.current
    // El diálogo de "app de inicio" vuelve con un resultado; se vuelve a comprobar al regresar.
    val request = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        viewModel.onResume()
    }
    WelcomeScreen(
        onSetDefault = {
            try {
                request.launch(viewModel.defaultHomeRequest())
            } catch (e: ActivityNotFoundException) {
                Logger.w("WelcomeRoute", e) { "Sin pantalla para elegir la app de inicio" }
                Toast.makeText(context, R.string.error_open_system_settings, Toast.LENGTH_SHORT).show()
            }
        },
        onNotNow = viewModel::onWelcomeDismissed,
    )
}

@Composable
private fun HomeEventMessages(viewModel: HomeViewModel) {
    val context = LocalContext.current
    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            val message = when (event) {
                HomeEvent.FavoritesFull -> R.string.favorites_full
            }
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }
}
