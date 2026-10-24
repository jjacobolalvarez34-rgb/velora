package app.velora.launcher.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.systemGestures
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import app.velora.launcher.R
import app.velora.launcher.core.designsystem.components.WallpaperScrim
import app.velora.launcher.core.designsystem.theme.Sizes
import app.velora.launcher.core.designsystem.theme.Spacing
import app.velora.launcher.core.designsystem.theme.VeloraTheme
import app.velora.launcher.feature.apps.AppActions
import app.velora.launcher.feature.drawer.AppListScreen
import app.velora.launcher.feature.drawer.DrawerList
import app.velora.launcher.feature.drawer.DrawerSearchState
import app.velora.launcher.feature.drawer.DrawerUiState
import app.velora.launcher.feature.drawer.WaveAlphabet
import app.velora.launcher.feature.home.HomeScreen
import app.velora.launcher.feature.home.HomeUiState

/** Fracción de la altura de pantalla que hay que arrastrar para abrir la lista del todo. */
private const val DRAG_DISTANCE_FRACTION = 0.45f

/** El alfabeto ocupa la parte baja de la pantalla, donde llega el pulgar. */
private const val ALPHABET_HEIGHT_FRACTION = 0.72f

private val HomeLift = 48.dp
private val DrawerDrop = 96.dp

/** Espacio del buscador bajo el alfabeto. */
private val AlphabetBottomSpace = Sizes.appRowHeight + Spacing.m * 2

/**
 * Inicio y lista completa en una sola pantalla (ADR-003). Todo lo que cambia por frame
 * (progreso del cajón, letra activa) se lee en `graphicsLayer {}`: arrastrar no recompone.
 */
@Composable
fun LauncherScreen(
    home: HomeUiState,
    drawer: DrawerUiState,
    drawerState: DrawerState,
    listState: LazyListState,
    search: DrawerSearchState,
    actions: AppActions,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val motion = VeloraTheme.motion
    drawerState.spec = if (motion.reduced) null else motion.spatialDefault()
    val drawerVisible by remember(drawerState) { derivedStateOf { drawerState.isOpen } }
    var activeLetter by remember { mutableIntStateOf(-1) }
    val letters = drawer.list.letters

    Box(
        modifier = modifier
            .fillMaxSize()
            .onSizeChanged { drawerState.dragDistancePx = it.height * DRAG_DISTANCE_FRACTION },
    ) {
        WallpaperScrim()
        DrawerScrim(drawerState)
        HomeScreen(
            uiState = home,
            actions = actions,
            modifier = Modifier
                .homeLayer(drawerState)
                .homeGestures(drawerState, onOpenSettings),
        )
        if (drawerVisible) {
            AppListScreen(
                uiState = drawer,
                listState = listState,
                search = search,
                actions = actions,
                nestedScrollConnection = drawerState.nestedScroll,
                activeSection = { letters.getOrNull(activeLetter) },
                modifier = Modifier.drawerLayer(drawerState),
            )
        }
        // Buscando, el alfabeto sobra: se oculta para dejar la pantalla limpia.
        if (!search.active) {
            AlphabetOverlay(drawer.list, drawerState, listState, onActiveLetter = { activeLetter = it })
        }
    }
}

/** Con la lista abierta el wallpaper se oscurece del todo, para leer sin esfuerzo. */
@Composable
private fun DrawerScrim(drawerState: DrawerState) {
    Box(
        Modifier
            .fillMaxSize()
            .graphicsLayer { alpha = drawerState.progress }
            .background(VeloraTheme.onWallpaper.drawerScrim),
    )
}

/** El inicio se desvanece y sube un poco mientras se abre la lista. */
private fun Modifier.homeLayer(drawerState: DrawerState) = graphicsLayer {
    val p = drawerState.progress
    alpha = 1f - p
    translationY = -p * HomeLift.toPx()
}

/** La lista aparece y baja a su sitio siguiendo al dedo. */
private fun Modifier.drawerLayer(drawerState: DrawerState) = graphicsLayer {
    val p = drawerState.progress
    alpha = p
    translationY = (1f - p) * DrawerDrop.toPx()
}

@Composable
private fun BoxScope.AlphabetOverlay(
    list: DrawerList,
    drawerState: DrawerState,
    listState: LazyListState,
    onActiveLetter: (Int) -> Unit,
) {
    WaveAlphabet(
        letters = list.letters,
        onTouchStart = drawerState::open,
        onLetterChange = { index ->
            onActiveLetter(index)
            list.letterIndex.getOrNull(index)?.let(listState::requestScrollToItem)
        },
        modifier = Modifier
            .align(Alignment.BottomEnd)
            .fillMaxHeight(ALPHABET_HEIGHT_FRACTION)
            // Fuera de la zona del gesto "atrás" del sistema (B-013).
            .windowInsetsPadding(
                WindowInsets.systemGestures.only(WindowInsetsSides.End)
                    .union(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom)),
            )
            .padding(end = Spacing.xs, bottom = AlphabetBottomSpace),
    )
}

/** Deslizar hacia arriba abre la lista; mantener presionado abre los ajustes. */
@Composable
private fun Modifier.homeGestures(drawerState: DrawerState, onOpenSettings: () -> Unit): Modifier {
    val haptics = LocalHapticFeedback.current
    val openSettings = stringResource(R.string.home_open_settings)
    val openApps = stringResource(R.string.home_open_all_apps)
    val dragState = rememberDraggableState { delta -> drawerState.dragBy(delta) }
    return this
        .draggable(
            state = dragState,
            orientation = Orientation.Vertical,
            onDragStopped = { velocity -> drawerState.settle(velocity) },
        )
        .pointerInput(onOpenSettings) {
            detectTapGestures(
                onLongPress = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    onOpenSettings()
                },
            )
        }
        .semantics {
            customActions = listOf(
                CustomAccessibilityAction(openApps) {
                    drawerState.open()
                    true
                },
                CustomAccessibilityAction(openSettings) {
                    onOpenSettings()
                    true
                },
            )
        }
}
