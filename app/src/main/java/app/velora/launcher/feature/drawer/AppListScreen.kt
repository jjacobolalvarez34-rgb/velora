package app.velora.launcher.feature.drawer

import android.content.res.Configuration
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
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
import androidx.compose.ui.unit.sp
import app.velora.launcher.R
import app.velora.launcher.core.data.contacts.ContactResult
import app.velora.launcher.core.designsystem.motion.VeloraMotion
import app.velora.launcher.core.designsystem.theme.Sizes
import app.velora.launcher.core.designsystem.theme.Spacing
import app.velora.launcher.core.designsystem.theme.VeloraTheme
import app.velora.launcher.core.model.AppEntry
import app.velora.launcher.feature.apps.AppActions
import app.velora.launcher.feature.apps.AppRow
import app.velora.launcher.feature.apps.AppRowStyle
import app.velora.launcher.feature.apps.PreviewAppActions
import app.velora.launcher.feature.apps.previewApp

/**
 * La lista: por letra (por defecto), completa (opción), o buscando. Cada cambio se anima: entre
 * letras, las apps entran desde el lado hacia el que se movió el dedo.
 *
 * @param fullList lista completa con scroll en vez de "solo la letra elegida".
 * @param activeSection sección resaltada por el alfabeto en la lista completa; se lee al dibujar.
 */
@Composable
fun AppListScreen(
    uiState: DrawerUiState,
    listState: LazyListState,
    search: DrawerSearchState,
    actions: AppActions,
    nestedScrollConnection: NestedScrollConnection,
    fullList: Boolean,
    modifier: Modifier = Modifier,
    activeSection: () -> String? = { null },
) {
    val motion = VeloraTheme.motion
    val barState = rememberSearchBarVisibility(listState)
    val mode = drawerMode(search, fullList)
    val atTop by remember(listState) { derivedStateOf { listState.firstVisibleItemIndex == 0 } }
    Box(modifier = modifier.fillMaxSize()) {
        val padding = listPadding()
        val scroll = Modifier.listScroll(nestedScrollConnection, barState.observer)
        val letters = uiState.list.letters
        AnimatedContent(
            targetState = mode,
            transitionSpec = { modeTransition(initialState, targetState, letters, motion) },
            label = "drawer-mode",
        ) { current ->
            when (current) {
                DrawerMode.Search -> SearchPanel(uiState, padding, actions, scroll, exploring = false)
                DrawerMode.Explore -> SearchPanel(uiState, padding, actions, scroll, exploring = true)
                DrawerMode.Full -> FullList(uiState, listState, padding, actions, activeSection, scroll)
                is DrawerMode.Letter -> LetterPage(current.letter, uiState, padding, actions, scroll)
            }
        }
        AnimatedSearchBar(
            state = search,
            // El buscador solo va donde están las recientes (abrir deslizando, o arriba de la lista
            // completa); viendo una letra o bajando por la lista estorbaría.
            visible =
                search.active || mode == DrawerMode.Explore || (mode == DrawerMode.Full && atTop && barState.visible),
            onGo = { uiState.results?.firstOrNull()?.let { actions.open(it, null) } },
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

/** El cajón (cerrar arrastrando) va por fuera; el que esconde el buscador solo observa. */
private fun Modifier.listScroll(drawer: NestedScrollConnection, barObserver: NestedScrollConnection) =
    fillMaxSize().nestedScroll(drawer).nestedScroll(barObserver)

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

                is DrawerItem.Recent -> ListApp(item.entry, actions)

                is DrawerItem.App -> ListApp(
                    app = item.entry,
                    actions = actions,
                    dimmed = { activeSection().let { it != null && it != item.entry.section } },
                )
            }
        }
    }
}

/**
 * Buscando: sin texto, las recientes; con texto, los resultados con fundido y resorte.
 * [exploring] = se abrió sin letra: arriba, la pista para usar el alfabeto.
 */
@Composable
private fun SearchPanel(
    uiState: DrawerUiState,
    padding: PaddingValues,
    actions: AppActions,
    modifier: Modifier,
    exploring: Boolean,
) {
    val results = uiState.results
    LazyColumn(state = rememberLazyListState(), contentPadding = padding, modifier = modifier) {
        if (exploring) item(key = "pick-letter") { Hint(stringResource(R.string.drawer_pick_letter)) }
        when {
            results == null && uiState.recents.isEmpty() -> if (!exploring) {
                item(key = "hint") { Hint(stringResource(R.string.drawer_search_empty)) }
            }

            results == null -> {
                item(key = "recent-header") { SectionHeader(stringResource(R.string.drawer_recent)) }
                items(uiState.recents, key = { "recent:${it.key.stableId}" }, contentType = { "app" }) {
                    ListApp(it, actions, animatedItem())
                }
            }

            else -> searchResults(results, uiState, actions)
        }
    }
}

/** Calculadora arriba si es una cuenta, las apps encontradas y al final "Buscar en la web". */
private fun LazyListScope.searchResults(results: List<AppEntry>, uiState: DrawerUiState, actions: AppActions) {
    uiState.calculation?.let { result ->
        item(key = "calculation") {
            CalculatorRow(result, onCopy = { actions.copy(result) }, modifier = animatedItem())
        }
    }
    if (results.isEmpty() && uiState.calculation == null) {
        item(key = "empty") { Hint(stringResource(R.string.drawer_no_results, uiState.query)) }
    }
    items(results, key = { it.key.stableId }, contentType = { "app" }) {
        ListApp(it, actions, animatedItem())
    }
    items(uiState.contacts, key = { "contact-${it.id}" }, contentType = { "contact" }) { contact ->
        ContactRow(contact, onClick = { actions.openContact(contact.uri) }, modifier = animatedItem())
    }
    item(key = "web") {
        WebSearchRow(uiState.query, onClick = { actions.webSearch(uiState.query) }, modifier = animatedItem())
    }
}

/** Al filtrar, lo que entra aparece con un fundido y lo que queda se reacomoda con un resorte. */
@Composable
internal fun LazyItemScope.animatedItem(): Modifier {
    val motion = VeloraTheme.motion
    return Modifier.animateItem(
        fadeInSpec = motion.effectDefault(),
        placementSpec = motion.spatialDefault(),
        fadeOutSpec = motion.effectFast(),
    )
}

@Composable
internal fun ListApp(
    app: AppEntry,
    actions: AppActions,
    modifier: Modifier = Modifier,
    dimmed: () -> Boolean = { false },
) {
    AppRow(
        app = app,
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
            fullList = true,
            modifier = Modifier.background(VeloraTheme.onWallpaper.drawerScrim),
        )
    }
}
