package app.velora.launcher.feature.drawer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
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
