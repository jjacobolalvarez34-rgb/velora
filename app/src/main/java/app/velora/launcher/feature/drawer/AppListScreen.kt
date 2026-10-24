package app.velora.launcher.feature.drawer

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import app.velora.launcher.R
import app.velora.launcher.core.designsystem.theme.Shapes
import app.velora.launcher.core.designsystem.theme.Sizes
import app.velora.launcher.core.designsystem.theme.Spacing
import app.velora.launcher.core.designsystem.theme.VeloraTheme
import app.velora.launcher.core.model.AppEntry
import app.velora.launcher.feature.apps.AppActions
import app.velora.launcher.feature.apps.AppRow
import app.velora.launcher.feature.apps.AppRowStyle
import app.velora.launcher.feature.apps.PreviewAppActions
import app.velora.launcher.feature.apps.previewApp

private val SearchBarHeight = Sizes.appRowHeight

/** Espacio que la lista deja abajo para que el buscador no tape la última app. */
private val SearchBarSpace: Dp = SearchBarHeight + Spacing.m * 2

/**
 * Lista completa: todas las apps por letra, o los resultados si hay búsqueda.
 *
 * @param activeSection sección resaltada por el alfabeto; se lee en la fase de dibujo.
 */
@Composable
fun AppListScreen(
    uiState: DrawerUiState,
    listState: LazyListState,
    searchState: TextFieldState,
    actions: AppActions,
    nestedScrollConnection: NestedScrollConnection,
    modifier: Modifier = Modifier,
    activeSection: () -> String? = { null },
) {
    Box(modifier = modifier.fillMaxSize()) {
        val padding = listPadding()
        val results = uiState.results
        if (results == null) {
            FullList(uiState, listState, padding, actions, activeSection, nestedScrollConnection)
        } else {
            SearchResults(results, uiState.query, uiState.favoriteIds, padding, actions, nestedScrollConnection)
        }
        SearchBar(
            state = searchState,
            onGo = { results?.firstOrNull()?.let { actions.open(it, null) } },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .windowInsetsPadding(
                    WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal),
                )
                .padding(horizontal = Spacing.l, vertical = Spacing.m),
        )
    }
}

@Composable
private fun FullList(
    uiState: DrawerUiState,
    listState: LazyListState,
    padding: PaddingValues,
    actions: AppActions,
    activeSection: () -> String?,
    nestedScrollConnection: NestedScrollConnection,
) {
    LazyColumn(
        state = listState,
        contentPadding = padding,
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(nestedScrollConnection),
    ) {
        items(
            items = uiState.list.items,
            key = { it.key },
            contentType = { if (it is DrawerItem.Header) "header" else "app" },
        ) { item ->
            when (item) {
                is DrawerItem.Header -> SectionHeader(item.letter)

                is DrawerItem.App -> AppRow(
                    app = item.entry,
                    isFavorite = item.entry.key.stableId in uiState.favoriteIds,
                    style = AppRowStyle.List,
                    actions = actions,
                    dimmed = { activeSection().let { it != null && it != item.entry.section } },
                )
            }
        }
    }
}

@Composable
private fun SearchResults(
    results: List<AppEntry>,
    query: String,
    favoriteIds: Set<String>,
    padding: PaddingValues,
    actions: AppActions,
    nestedScrollConnection: NestedScrollConnection,
) {
    // Un estado propio: buscar no mueve la posición de la lista completa.
    val resultsState = rememberLazyListState()
    LazyColumn(
        state = resultsState,
        contentPadding = padding,
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(nestedScrollConnection),
    ) {
        if (results.isEmpty()) {
            item(key = "empty") {
                Text(
                    text = stringResource(R.string.drawer_no_results, query),
                    style = VeloraTheme.typography.caption,
                    color = VeloraTheme.onWallpaper.contentVariant,
                )
            }
        }
        items(items = results, key = { it.key.stableId }, contentType = { "app" }) { app ->
            AppRow(
                app = app,
                isFavorite = app.key.stableId in favoriteIds,
                style = AppRowStyle.List,
                actions = actions,
            )
        }
    }
}

@Composable
private fun SectionHeader(letter: String) {
    Text(
        text = letter,
        style = VeloraTheme.typography.sectionHeader,
        color = VeloraTheme.onWallpaper.contentVariant,
        modifier = Modifier
            .padding(top = Spacing.l, bottom = Spacing.xs)
            .semantics { heading() },
    )
}

@Composable
private fun SearchBar(state: TextFieldState, onGo: () -> Unit, modifier: Modifier = Modifier) {
    val colors = VeloraTheme.onWallpaper
    val style = VeloraTheme.typography.appRow.copy(color = colors.content)
    BasicTextField(
        state = state,
        textStyle = style,
        cursorBrush = SolidColor(colors.content),
        lineLimits = TextFieldLineLimits.SingleLine,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go, autoCorrectEnabled = false),
        onKeyboardAction = { onGo() },
        modifier = modifier
            .fillMaxWidth()
            .height(SearchBarHeight)
            .background(colors.surface, Shapes.sheet),
        decorator = { field ->
            Box(contentAlignment = Alignment.CenterStart, modifier = Modifier.padding(horizontal = Spacing.l)) {
                if (state.text.isEmpty()) {
                    Text(stringResource(R.string.drawer_search_hint), style = style, color = colors.contentVariant)
                }
                field()
            }
        },
    )
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
            uiState = DrawerUiState(list = buildDrawerList(apps)),
            listState = rememberLazyListState(),
            searchState = rememberTextFieldState(),
            actions = PreviewAppActions,
            nestedScrollConnection = object : NestedScrollConnection {},
            modifier = Modifier.background(VeloraTheme.onWallpaper.drawerScrim),
        )
    }
}
