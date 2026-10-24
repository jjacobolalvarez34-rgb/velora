package app.velora.launcher.feature.drawer

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.Dp
import app.velora.launcher.R
import app.velora.launcher.core.designsystem.theme.Shapes
import app.velora.launcher.core.designsystem.theme.Sizes
import app.velora.launcher.core.designsystem.theme.Spacing
import app.velora.launcher.core.designsystem.theme.VeloraTheme

/** Buscador abajo, en la zona del pulgar. Enter abre el primer resultado. */
@Composable
fun DrawerSearchBar(state: DrawerSearchState, onGo: () -> Unit, modifier: Modifier = Modifier) {
    val colors = VeloraTheme.onWallpaper
    val style = VeloraTheme.typography.appRow.copy(color = colors.content)
    BasicTextField(
        state = state.text,
        textStyle = style,
        cursorBrush = SolidColor(colors.content),
        lineLimits = TextFieldLineLimits.SingleLine,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go, autoCorrectEnabled = false),
        onKeyboardAction = { onGo() },
        modifier = modifier
            .fillMaxWidth()
            .height(Sizes.appRowHeight)
            .background(colors.surface, Shapes.sheet)
            .onFocusChanged { state.focused = it.isFocused },
        decorator = { field ->
            Box(contentAlignment = Alignment.CenterStart, modifier = Modifier.padding(horizontal = Spacing.l)) {
                if (state.text.text.isEmpty()) {
                    Text(stringResource(R.string.drawer_search_hint), style = style, color = colors.contentVariant)
                }
                field()
            }
        },
    )
}

/** Espacio que la lista deja abajo para que el buscador no tape la última app. */
internal val SearchBarSpace: Dp = Sizes.appRowHeight + Spacing.m * 2

/** Cuánto hay que arrastrar para que el buscador se esconda o vuelva. */
private const val BAR_TOGGLE_PX = 12f

/** El buscador abajo, sobre la barra de navegación; entra y sale deslizándose con un resorte. */
@Composable
internal fun AnimatedSearchBar(
    state: DrawerSearchState,
    visible: Boolean,
    onGo: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shown by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = VeloraTheme.motion.spatialDefault(),
        label = "search-bar",
    )
    DrawerSearchBar(
        state = state,
        onGo = onGo,
        modifier = modifier
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal))
            .padding(horizontal = Spacing.l, vertical = Spacing.m)
            .graphicsLayer {
                alpha = shown
                translationY = (1f - shown) * SearchBarSpace.toPx()
            },
    )
}

/** El buscador se ve arriba de la lista y al subir; se esconde al bajar (no tapa las apps). */
internal class SearchBarVisibility(private val listState: LazyListState) {
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

@Composable
internal fun rememberSearchBarVisibility(listState: LazyListState) =
    remember(listState) { SearchBarVisibility(listState) }
