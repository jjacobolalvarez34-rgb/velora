package app.velora.launcher.ui

import android.content.ActivityNotFoundException
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.core.view.WindowCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import app.velora.launcher.R
import app.velora.launcher.core.data.apps.IconRequest
import app.velora.launcher.core.data.log.Logger
import app.velora.launcher.core.designsystem.theme.Sizes
import app.velora.launcher.core.designsystem.theme.VeloraTheme
import app.velora.launcher.core.model.AppEntry
import app.velora.launcher.core.model.AppShortcut
import app.velora.launcher.core.model.IconStyle
import app.velora.launcher.core.model.ThemeSettings
import app.velora.launcher.di.AppContainer
import app.velora.launcher.feature.appmenu.AppMenuSheet
import app.velora.launcher.feature.appmenu.LocalShortcutsRepository
import app.velora.launcher.feature.apps.AppActions
import app.velora.launcher.feature.apps.LocalIconRepository
import app.velora.launcher.feature.apps.LocalIconStyle
import app.velora.launcher.feature.apps.rememberAppOpener
import app.velora.launcher.feature.drawer.DrawerSearchState
import app.velora.launcher.feature.drawer.DrawerUiState
import app.velora.launcher.feature.drawer.DrawerViewModel
import app.velora.launcher.feature.drawer.rememberDrawerSearchState
import app.velora.launcher.feature.home.HomeEvent
import app.velora.launcher.feature.home.HomeUiState
import app.velora.launcher.feature.home.HomeViewModel
import app.velora.launcher.feature.welcome.OnboardingScreen
import app.velora.launcher.feature.welcome.WelcomeScreen
import app.velora.launcher.feature.widgets.HomeMenuSheet
import app.velora.launcher.feature.widgets.WidgetPickerSheet
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
    val homeViewModel: HomeViewModel = viewModel(factory = HomeViewModel.factory(container))
    val drawerViewModel: DrawerViewModel = viewModel(factory = DrawerViewModel.factory(container))
    // Hasta leer el tema (milisegundos) solo se ve el wallpaper: nunca la fuente equivocada.
    val theme = homeViewModel.theme.collectAsStateWithLifecycle().value ?: return
    val onboardingDone = homeViewModel.onboardingDone.collectAsStateWithLifecycle().value ?: return
    val density = LocalDensity.current
    VeloraTheme(font = theme.font, preset = theme.preset) {
        CompositionLocalProvider(
            // Tamaño de texto de Velora encima del tamaño de fuente del sistema.
            LocalDensity provides Density(density.density, density.fontScale * theme.textSize.scale),
            LocalIconRepository provides container.iconRepository,
            LocalIconStyle provides theme.icons,
            LocalShortcutsRepository provides container.shortcutsRepository,
        ) {
            val home by homeViewModel.uiState.collectAsStateWithLifecycle()

            HomeEventMessages(homeViewModel)

            val onWelcome = !onboardingDone || home.showWelcome
            SystemBarsAppearance(lightBackground = VeloraTheme.onWallpaper.isLight && !onWelcome)
            if (!onboardingDone) {
                WelcomeRoute(homeViewModel, intro = true)
            } else if (home.showWelcome) {
                WelcomeRoute(homeViewModel, intro = false)
            } else {
                // Modo zurdo: todo el inicio se espeja (alfabeto, márgenes, íconos, zona de gestos).
                val direction = if (theme.leftHanded) LayoutDirection.Rtl else LocalLayoutDirection.current
                CompositionLocalProvider(LocalLayoutDirection provides direction) {
                    LauncherRoute(
                        container,
                        homeViewModel,
                        drawerViewModel,
                        homePresses,
                        onOpenSettings,
                        theme,
                    )
                }
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
    theme: ThemeSettings,
) {
    val home by homeViewModel.uiState.collectAsStateWithLifecycle()
    val drawer by drawerViewModel.uiState.collectAsStateWithLifecycle()
    val drawerState = rememberDrawerState()
    val listState = rememberLazyListState()
    val search = rememberDrawerSearchState()
    val sheets = rememberLauncherSheets()
    val actions = rememberAppActions(container, homeViewModel, sheets)
    val lock = rememberDoubleTapLock()
    DrawerEffects(drawerState, search, homePresses, drawerViewModel::onQueryChange)
    NotificationsProvider(container.notificationsRepository, enabled = theme.notifications) {
        LauncherScreen(
            home = home,
            drawer = drawer,
            drawerState = drawerState,
            listState = listState,
            search = search,
            actions = actions,
            onOpenSettings = onOpenSettings,
            theme = theme,
            onHomeMenu = { sheets.homeMenu = true },
            widgets = { HomeWidgetsSlot(container, showMusic = theme.notifications) },
            photo = rememberPhoto(theme.photoBackground),
            onDoubleTap = lock.takeIf { theme.doubleTapLock },
        )
    }
    LauncherSheets(sheets, container, drawer, actions, onOpenSettings)
    IconPrefetch(container, home, drawer, theme.icons)
}

/** Qué panel está abierto sobre el inicio. */
@Stable
private class LauncherSheetsState {
    var appMenu by mutableStateOf<AppEntry?>(null)
    var homeMenu by mutableStateOf(false)
    var widgetPicker by mutableStateOf(false)
}

@Composable
private fun rememberLauncherSheets() = remember { LauncherSheetsState() }

@Composable
private fun rememberAppActions(
    container: AppContainer,
    homeViewModel: HomeViewModel,
    sheets: LauncherSheetsState,
): AppActions {
    val opener = rememberAppOpener(container.appLauncher)
    return remember(opener, homeViewModel, sheets) {
        object : AppActions {
            override fun open(app: AppEntry, bounds: Rect?) = opener.open(app, bounds)
            override fun openInfo(app: AppEntry, bounds: Rect?) = opener.openInfo(app, bounds)
            override fun uninstall(app: AppEntry) = opener.uninstall(app)
            override fun addFavorite(app: AppEntry) = homeViewModel.onAddFavorite(app.key)
            override fun removeFavorite(app: AppEntry) = homeViewModel.onRemoveFavorite(app.key)
            override fun hide(app: AppEntry) = homeViewModel.onHide(app)
            override fun openShortcut(shortcut: AppShortcut, bounds: Rect?) = opener.openShortcut(shortcut, bounds)
            override fun showMenu(app: AppEntry) {
                sheets.appMenu = app
            }
            override fun webSearch(query: String) = opener.webSearch(query)
            override fun copy(text: String) = opener.copy(text)
        }
    }
}

/** Paneles desde abajo: menú de una app, menú del inicio y selector de widgets. */
@Composable
private fun LauncherSheets(
    sheets: LauncherSheetsState,
    container: AppContainer,
    drawer: DrawerUiState,
    actions: AppActions,
    onOpenSettings: () -> Unit,
) {
    sheets.appMenu?.let { app ->
        AppMenuSheet(
            app = app,
            isFavorite = app.key.stableId in drawer.favoriteIds,
            actions = actions,
            onOpenSettings = onOpenSettings,
            onDismiss = { sheets.appMenu = null },
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
}

/**
 * Prepara en segundo plano los íconos de favoritos y de toda la lista con el estilo actual:
 * al abrir la lista ya están dibujados (y en disco para el próximo arranque).
 */
@Composable
private fun IconPrefetch(container: AppContainer, home: HomeUiState, drawer: DrawerUiState, style: IconStyle) {
    if (style == IconStyle.None) return
    val tint = VeloraTheme.onWallpaper.iconGlyph.toArgb()
    val backdrop = VeloraTheme.onWallpaper.iconCircle.toArgb()
    val density = LocalDensity.current
    val favoritePx = with(density) { Sizes.favoriteIcon.roundToPx() }
    val listPx = with(density) { Sizes.listIcon.roundToPx() }
    LaunchedEffect(home.favorites, drawer.list, style, tint, backdrop, favoritePx, listPx) {
        val favorites = home.favorites.map { IconRequest(it.key, favoritePx, style, tint, backdrop) }
        val everything = drawer.list.byLetter.values.flatten().map {
            IconRequest(it.key, listPx, style, tint, backdrop)
        }
        container.iconRepository.prefetch(favorites + everything)
    }
}

/** Íconos oscuros en las barras del sistema cuando el fondo del tema es claro (Sakura). */
@Composable
private fun SystemBarsAppearance(lightBackground: Boolean) {
    val view = LocalView.current
    val activity = LocalActivity.current
    LaunchedEffect(lightBackground) {
        val window = activity?.window ?: return@LaunchedEffect
        WindowCompat.getInsetsController(window, view).apply {
            isAppearanceLightStatusBars = lightBackground
            isAppearanceLightNavigationBars = lightBackground
        }
    }
}

/** Comportamiento del cajón que no es dibujo: búsqueda, atrás, Inicio y salir a otra app. */
@Composable
private fun DrawerEffects(
    drawerState: DrawerState,
    search: DrawerSearchState,
    homePresses: Flow<Boolean>,
    onQueryChange: (String) -> Unit,
) {
    val keyboard = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    // Escribir en el buscador filtra la lista.
    LaunchedEffect(search) {
        snapshotFlow { search.text.text.toString() }.collect(onQueryChange)
    }
    // Al cerrarse la lista del todo: buscador limpio y teclado guardado.
    LaunchedEffect(drawerState) {
        snapshotFlow { drawerState.isOpen }.distinctUntilChanged().filter { !it }.collect {
            search.clear()
            search.letter = null
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
        when {
            search.text.text.isNotEmpty() -> search.clear()
            search.focused -> focusManager.clearFocus()
            else -> drawerState.close()
        }
    }
}

@Composable
private fun WelcomeRoute(viewModel: HomeViewModel, intro: Boolean) {
    val context = LocalContext.current
    // El diálogo de "app de inicio" vuelve con un resultado; se vuelve a comprobar al regresar.
    val request = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        viewModel.onResume()
    }
    val setDefault = {
        if (intro) viewModel.onOnboardingFinished(dismissWelcome = false)
        try {
            request.launch(viewModel.defaultHomeRequest())
        } catch (e: ActivityNotFoundException) {
            Logger.w("WelcomeRoute", e) { "Sin pantalla para elegir la app de inicio" }
            Toast.makeText(context, R.string.error_open_system_settings, Toast.LENGTH_SHORT).show()
        }
    }
    val alreadyDefault = viewModel.isDefaultHome.collectAsStateWithLifecycle().value == true
    if (intro) {
        OnboardingScreen(
            onSetDefault = setDefault,
            onNotNow = { viewModel.onOnboardingFinished(dismissWelcome = true) },
            alreadyDefault = alreadyDefault,
        )
    } else {
        WelcomeScreen(onSetDefault = setDefault, onNotNow = viewModel::onWelcomeDismissed)
    }
}

/** Avisos de un solo uso y, al volver al frente, refrescar apps y app de inicio. */
@Composable
private fun HomeEventMessages(viewModel: HomeViewModel) {
    LifecycleResumeEffect(viewModel) {
        viewModel.onResume()
        onPauseOrDispose {}
    }
    val context = LocalContext.current
    val resources = LocalResources.current
    LaunchedEffect(viewModel, resources) {
        viewModel.events.collect { event ->
            val message = when (event) {
                HomeEvent.FavoritesFull -> resources.getString(R.string.favorites_full)
                is HomeEvent.AppHidden -> resources.getString(R.string.app_hidden, event.label)
            }
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }
}
