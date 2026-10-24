package app.velora.launcher.feature.drawer

import android.content.res.Configuration
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import app.velora.launcher.R
import app.velora.launcher.core.designsystem.theme.Sizes
import app.velora.launcher.core.designsystem.theme.Spacing
import app.velora.launcher.core.designsystem.theme.VeloraTheme
import app.velora.launcher.core.model.AppEntry
import app.velora.launcher.feature.apps.AppActions
import app.velora.launcher.feature.apps.AppRow
import app.velora.launcher.feature.apps.AppRowStyle
import app.velora.launcher.feature.apps.PreviewAppActions
import app.velora.launcher.feature.apps.previewApp

/** Espacio que la lista deja abajo para que el buscador no tape la última app. */
private val SearchBarSpace: Dp = Sizes.appRowHeight + Spacing.m * 2

/** Cuánto hay que arrastrar para que el buscador se esconda o vuelva. */
private const val BAR_TOGGLE_PX = 12f

/**
 * Lista completa o, buscando, las recientes / los resultados. El cambio entre ambas es un fundido.
 *
 * @param activeSection sección resaltada por el alfabeto; se lee en la fase de dibujo.
 */
@Composable
fun AppListScreen(
    uiState: DrawerUiState,
    listState: LazyListState,
    search: DrawerSearchState,
    actions: AppActions,
    nestedScrollConnection: NestedScrollConnection,
    modifier: Modifier = Modifier,
    activeSection: () -> String? = { null },
) {
    val motion = VeloraTheme.motion
    val barState = rememberSearchBarVisibility(listState)
    Box(modifier = modifier.fillMaxSize()) {
        val padding = listPadding()
        val scroll = Modifier.listScroll(nestedScrollConnection, barState.observer)
        AnimatedContent(
            targetState = search.active,
            transitionSpec = { fadeIn(motion.effectDefault()) togetherWith fadeOut(motion.effectFast()) },
            label = "search-mode",
        ) { searching ->
            if (searching) {
                SearchPanel(uiState, padding, actions, scroll)
            } else {
                FullList(uiState, listState, padding, actions, activeSection, scroll)
            }
        }
        val shown by animateFloatAsState(
            targetValue = if (search.active || barState.visible) 1f else 0f,
            animationSpec = motion.spatialDefault(),
            label = "search-bar",
        )
        DrawerSearchBar(
            state = search,
            onGo = { uiState.results?.firstOrNull()?.let { actions.open(it, null) } },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .windowInsetsPadding(
                    WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal),
                )
                .padding(horizontal = Spacing.l, vertical = Spacing.m)
                .graphicsLayer {
                    alpha = shown
                    translationY = (1f - shown) * SearchBarSpace.toPx()
                },
        )
    }
}

/** El buscador se ve arriba de la lista y al subir; se esconde al bajar (no tapa las apps). */
private class SearchBarVisibility(private val listState: LazyListState) {
    private var scrollingUp by mutableStateOf(true)
    private val atTop by derivedStateOf { listState.firstVisibleItemIndex == 0 }

    val visible: Boolean get() = atTop || scrollingUp

    val observer = object : NestedScrollConnection {
        override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
            if (available.y < -BAR_TOGGLE_PX) scrollingUp = false
            if (available.y > BAR_TOGGLE_PX) scrollingUp = true
            return Offset.Zero
        }
    }
}

/** El cajón (cerrar arrastrando) va por fuera; el que esconde el buscador solo observa. */
private fun Modifier.listScroll(drawer: NestedScrollConnection, barObserver: NestedScrollConnection) =
    fillMaxSize().nestedScroll(drawer).nestedScroll(barObserver)

@Composable
private fun rememberSearchBarVisibility(listState: LazyListState) =
    remember(listState) { SearchBarVisibility(listState) }

@Composable
private fun FullList(
    uiState: DrawerUiState,
    listState: LazyListState,
    padding: PaddingValues,
    actions: AppActions,
    activeSection: () -> String?,
    modifier: Modifier,
) {
    LazyColumn(state = listState, contentPadding = padding, modifier = modifier) {
        items(items = uiState.list.items, key = { it.key }, contentType = { it.contentType }) { item ->
            when (item) {
                DrawerItem.RecentHeader -> SectionHeader(stringResource(R.string.drawer_recent))

                is DrawerItem.Header -> SectionHeader(item.letter)

                is DrawerItem.Recent -> ListApp(item.entry, uiState, actions)

                is DrawerItem.App -> ListApp(
                    app = item.entry,
                    uiState = uiState,
                    actions = actions,
                    dimmed = { activeSection().let { it != null && it != item.entry.section } },
                )
            }
        }
    }
}

/** Buscando: sin texto, las recientes; con texto, los resultados con fundido y resorte. */
@Composable
private fun SearchPanel(uiState: DrawerUiState, padding: PaddingValues, actions: AppActions, modifier: Modifier) {
    val results = uiState.results
    LazyColumn(state = rememberLazyListState(), contentPadding = padding, modifier = modifier) {
        when {
            results == null && uiState.recents.isEmpty() -> item(key = "hint") {
                Hint(stringResource(R.string.drawer_search_empty))
            }

            results == null -> {
                item(key = "recent-header") { SectionHeader(stringResource(R.string.drawer_recent)) }
                items(uiState.recents, key = { "recent:${it.key.stableId}" }, contentType = { "app" }) {
                    ListApp(it, uiState, actions, animatedItem())
                }
            }

            results.isEmpty() -> item(key = "empty") {
                Hint(stringResource(R.string.drawer_no_results, uiState.query))
            }

            else -> items(results, key = { it.key.stableId }, contentType = { "app" }) {
                ListApp(it, uiState, actions, animatedItem())
            }
        }
    }
}

/** Al filtrar, lo que entra aparece con un fundido y lo que queda se reacomoda con un resorte. */
@Composable
private fun LazyItemScope.animatedItem(): Modifier {
    val motion = VeloraTheme.motion
    return Modifier.animateItem(
        fadeInSpec = motion.effectDefault(),
        placementSpec = motion.spatialDefault(),
        fadeOutSpec = motion.effectFast(),
    )
}

@Composable
private fun ListApp(
    app: AppEntry,
    uiState: DrawerUiState,
    actions: AppActions,
    modifier: Modifier = Modifier,
    dimmed: () -> Boolean = { false },
) {
    AppRow(
        app = app,
        isFavorite = app.key.stableId in uiState.favoriteIds,
        style = AppRowStyle.List,
        actions = actions,
        dimmed = dimmed,
        modifier = modifier,
    )
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        style = VeloraTheme.typography.sectionHeader,
        color = VeloraTheme.onWallpaper.contentVariant,
        modifier = Modifier
            .padding(top = Spacing.l, bottom = Spacing.xs)
            .semantics { heading() },
    )
}

@Composable
private fun Hint(text: String) {
    Text(
        text = text,
        style = VeloraTheme.typography.caption,
        color = VeloraTheme.onWallpaper.contentVariant,
        modifier = Modifier.padding(top = Spacing.l),
    )
}

private val DrawerItem.contentType: String
    get() = when (this) {
        DrawerItem.RecentHeader, is DrawerItem.Header -> "header"
        is DrawerItem.Recent, is DrawerItem.App -> "app"
    }

@Composable
private fun listPadding(): PaddingValues {
    val insets = WindowInsets.safeDrawing.asPaddingValues()
    val direction = LocalLayoutDirection.current
    return PaddingValues(
        start = insets.calculateStartPadding(direction) + Spacing.l,
        top = insets.calculateTopPadding() + Spacing.xl,
        // A la derecha queda el alfabeto.
        end = insets.calculateEndPadding(direction) + WaveAlphabetWidth + Spacing.l,
        bottom = insets.calculateBottomPadding() + SearchBarSpace,
    )
}

@Preview(name = "Claro", heightDp = 640)
@Preview(name = "Oscuro", heightDp = 640, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun AppListScreenPreview() {
    val apps = listOf("Banco", "Calculadora", "Cámara", "Chrome", "Reloj", "Spotify").map(::previewApp)
    VeloraTheme(reducedMotion = true) {
        AppListScreen(
            uiState = DrawerUiState(list = buildDrawerList(apps, recents = apps.take(1)), recents = apps.take(1)),
            listState = rememberLazyListState(),
            search = rememberDrawerSearchState(),
            actions = PreviewAppActions,
            nestedScrollConnection = object : NestedScrollConnection {},
            modifier = Modifier.background(VeloraTheme.onWallpaper.drawerScrim),
        )
    }
}
