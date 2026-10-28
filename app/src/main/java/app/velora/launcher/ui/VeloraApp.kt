package app.velora.launcher.ui

import android.content.ActivityNotFoundException
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.PredictiveBackHandler
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
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import app.velora.launcher.R
import app.velora.launcher.core.data.apps.IconRequest
import app.velora.launcher.core.data.log.Logger
import app.velora.launcher.core.designsystem.motion.LocalEntranceGeneration
import app.velora.launcher.core.designsystem.theme.Sizes
import app.velora.launcher.core.designsystem.theme.VeloraTheme
import app.velora.launcher.core.model.AppEntry
import app.velora.launcher.core.model.AppKey
import app.velora.launcher.core.model.AppShortcut
import app.velora.launcher.core.model.IconOptions
import app.velora.launcher.core.model.IconStyle
import app.velora.launcher.core.model.ThemeSettings
import app.velora.launcher.di.AppContainer
import app.velora.launcher.feature.appmenu.LocalShortcutsRepository
import app.velora.launcher.feature.apps.AppActions
import app.velora.launcher.feature.apps.LocalIconOptions
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
import app.velora.launcher.feature.settings.userAccent
import app.velora.launcher.feature.welcome.OnboardingScreen
import app.velora.launcher.feature.welcome.WelcomeScreen
import kotlin.coroutines.cancellation.CancellationException
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
    VeloraTheme(font = theme.font, preset = theme.preset, accent = theme.userAccent()) {
        CompositionLocalProvider(
            // Tamaño de texto de Velora encima del tamaño de fuente del sistema.
            LocalDensity provides Density(density.density, density.fontScale * theme.textSize.scale),
            LocalIconRepository provides container.iconRepository,
            LocalIconStyle provides theme.icons,
            LocalIconOptions provides theme.custom.icons,
            LocalShortcutsRepository provides container.shortcutsRepository,
        ) {
            val home by homeViewModel.uiState.collectAsStateWithLifecycle()

            HomeEventMessages(homeViewModel)

            val onWelcome = !onboardingDone || home.showWelcome
            SystemBarsAppearance(
                lightBackground = VeloraTheme.onWallpaper.isLight && !onWelcome,
                showStatusBar = theme.custom.visible.statusBar || onWelcome,
            )
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
    val actions = rememberAppActions(container, homeViewModel, sheets, theme)
    val lock = rememberDoubleTapLock()
    val folders by container.foldersRepository.folders.collectAsStateWithLifecycle(emptyList())
    val folderActions = rememberFolderActions(container, sheets)
    DrawerEffects(drawerState, search, homePresses, drawerViewModel::onQueryChange)
    NotificationsProvider(container.notificationsRepository, enabled = theme.notifications) {
        CompositionLocalProvider(LocalEntranceGeneration provides rememberEntranceGeneration()) {
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
                folders = folders,
                onOpenFolder = folderActions::open,
                tips = { HomeTipsSlot(container, theme, home, homeViewModel, drawerState, sheets, onOpenSettings) },
            )
        }
    }
    LauncherSheets(sheets, container, drawer, actions, onOpenSettings, theme, folders, folderActions)
    IconPrefetch(container, home, drawer, theme.icons, theme.custom.icons)
}

/**
 * Prepara en segundo plano los íconos de favoritos y de toda la lista con el estilo actual:
 * al abrir la lista ya están dibujados (y en disco para el próximo arranque).
 */
@Composable
private fun IconPrefetch(
    container: AppContainer,
    home: HomeUiState,
    drawer: DrawerUiState,
    style: IconStyle,
    options: IconOptions,
) {
    if (style == IconStyle.None) return
    val colors = VeloraTheme.onWallpaper
    val tint = colors.iconGlyph.toArgb()
    val backdrop = colors.iconCircle.toArgb()
    val accent = colors.accent.toArgb()
    val density = LocalDensity.current
    val favoritePx = with(density) { Sizes.favoriteIcon.roundToPx() }
    val listPx = with(density) { Sizes.listIcon.roundToPx() }
    LaunchedEffect(home.favorites, drawer.list, style, options, tint, backdrop, accent, favoritePx, listPx) {
        fun request(key: AppKey, sizePx: Int) = IconRequest(key, sizePx, style, tint, backdrop, options, accent)
        val favorites = home.favorites.map { request(it.key, favoritePx) }
        val everything = drawer.list.byLetter.values.flatten().map { request(it.key, listPx) }
        container.iconRepository.prefetch(favorites + everything)
    }
}

/**
 * Íconos oscuros en las barras del sistema cuando el fondo del tema es claro (Sakura), y la barra
 * de estado oculta si así se eligió (elementos visibles, ADR-028; se asoma deslizando).
 */
@Composable
private fun SystemBarsAppearance(lightBackground: Boolean, showStatusBar: Boolean) {
    val view = LocalView.current
    val activity = LocalActivity.current
    LaunchedEffect(lightBackground, showStatusBar) {
        val window = activity?.window ?: return@LaunchedEffect
        WindowCompat.getInsetsController(window, view).apply {
            isAppearanceLightStatusBars = lightBackground
            isAppearanceLightNavigationBars = lightBackground
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            if (showStatusBar) {
                show(
                    WindowInsetsCompat.Type.statusBars(),
                )
            } else {
                hide(WindowInsetsCompat.Type.statusBars())
            }
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
    val searching = search.text.text.isNotEmpty() || search.focused
    // Primero se limpia el buscador o se guarda el teclado; con eso hecho, atrás cierra la lista.
    BackHandler(enabled = drawerState.isOpen && searching) {
        if (search.text.text.isNotEmpty()) search.clear() else focusManager.clearFocus()
    }
    PredictiveBackHandler(enabled = drawerState.isOpen && !searching) { events ->
        try {
            events.collect { event -> drawerState.peekBack(event.progress) }
            drawerState.close()
        } catch (e: CancellationException) {
            drawerState.open()
            throw e
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
