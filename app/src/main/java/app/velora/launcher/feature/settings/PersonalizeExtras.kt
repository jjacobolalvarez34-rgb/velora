package app.velora.launcher.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import app.velora.launcher.R
import app.velora.launcher.core.designsystem.theme.Spacing
import app.velora.launcher.core.model.IconShape
import app.velora.launcher.core.model.ThemeSettings
import app.velora.launcher.core.model.VisibleElements
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sign
import kotlin.math.sin

// Pestañas Color y Elementos de Personalizar (ADR-028), y la silueta de las formas de ícono.

/** Colores del tema: el del tema elegido o uno de estos, pensados para leerse sobre cualquier fondo. */
private val Swatches = listOf(
    0xFFF4F4F4, 0xFFFFD86B, 0xFFFFB38A, 0xFFFF7A7A, 0xFFFF5FA2, 0xFFD18CFF,
    0xFF9A8CFF, 0xFF7FB8FF, 0xFF5FE0E8, 0xFF7FF0B0, 0xFFB8E07A, 0xFF1E1E24,
)

/** "Automático" (el del tema) y los colores fijos, en círculos grandes fáciles de tocar. */
@Composable
internal fun ColorTab(theme: ThemeSettings, actions: SettingsActions) {
    val current = theme.custom.accentColor
    LazyVerticalGrid(
        columns = GridCells.Adaptive(SwatchCell),
        contentPadding = PaddingValues(Spacing.m),
        horizontalArrangement = Arrangement.spacedBy(Spacing.s),
        verticalArrangement = Arrangement.spacedBy(Spacing.s),
        modifier = Modifier.fillMaxSize().tabContentPadding(),
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) { TabHint(R.string.personalize_color_hint) }
        item(key = "auto") {
            Swatch(null, current == null, stringResource(R.string.personalize_color_auto)) {
                actions.onCustom { it.copy(accentColor = null) }
            }
        }
        items(Swatches, key = { it }) { argb ->
            Swatch(Color(argb.toInt()), current == argb, colorName(argb)) {
                actions.onCustom { it.copy(accentColor = argb) }
            }
        }
    }
}

@Composable
private fun Swatch(color: Color?, selected: Boolean, label: String, onClick: () -> Unit) {
    val ring = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(SwatchSize)
                .border(3.dp, ring, CircleShape)
                .padding(5.dp)
                .background(color ?: MaterialTheme.colorScheme.surfaceContainerHighest, CircleShape)
                .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
                .semantics { contentDescription = label },
        ) {
            if (color == null) Text("A", style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
private fun colorName(argb: Long): String =
    stringResource(R.string.personalize_color_swatch, "#%06X".format(argb and 0xFFFFFF))

/** Qué se ve en el inicio. */
@Composable
internal fun ElementsTab(theme: ThemeSettings, actions: SettingsActions) {
    val visible = theme.custom.visible
    val set = { change: (
        VisibleElements,
    ) -> VisibleElements,
        ->
        actions.onCustom { it.copy(visible = change(it.visible)) }
    }
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(vertical = Spacing.m).tabContentPadding(),
    ) {
        TabHint(R.string.personalize_elements_hint)
        SwitchOption(
            stringResource(R.string.element_clock),
            stringResource(R.string.element_clock_summary),
            visible.clock,
            { on -> set { it.copy(clock = on) } },
            Icons.Outlined.DateRange,
        )
        SwitchOption(
            stringResource(R.string.element_date),
            stringResource(R.string.element_date_summary),
            visible.date,
            { on -> set { it.copy(date = on) } },
            Icons.Outlined.Info,
        )
        SwitchOption(
            stringResource(R.string.element_status_bar),
            stringResource(R.string.element_status_bar_summary),
            visible.statusBar,
            { on -> set { it.copy(statusBar = on) } },
            Icons.Outlined.Menu,
        )
        SwitchOption(
            stringResource(R.string.element_tips),
            stringResource(R.string.element_tips_summary),
            visible.tips,
            { on -> set { it.copy(tips = on) } },
            Icons.Outlined.Notifications,
        )
    }
}

/** Silueta de una forma de ícono en [size] (la misma geometría que usan los íconos). */
internal fun shapePath(shape: IconShape, size: Size): Path = when (shape) {
    IconShape.Circle -> Path().apply { addOval(Rect(0f, 0f, size.width, size.height)) }
    IconShape.Squircle -> squircle(size)
    IconShape.Rounded -> rounded(size, ROUNDED, ROUNDED, ROUNDED, ROUNDED)
    IconShape.Teardrop -> rounded(size, HALF, HALF, HALF, TEAR_CORNER)
}

private fun rounded(size: Size, tl: Float, tr: Float, br: Float, bl: Float) = Path().apply {
    val w = size.width
    val r = { f: Float -> CornerRadius(w * f) }
    addRoundRect(
        RoundRect(
            rect = Rect(0f, 0f, w, size.height),
            topLeft = r(tl),
            topRight = r(tr),
            bottomRight = r(br),
            bottomLeft = r(bl),
        ),
    )
}

/** Superelipse |x|^n + |y|^n = 1 con n = 4: el "squircle" de los íconos modernos. */
private fun squircle(size: Size) = Path().apply {
    val cx = size.width / 2f
    val cy = size.height / 2f
    for (step in 0..SQUIRCLE_STEPS) {
        val t = step / SQUIRCLE_STEPS.toFloat() * 2f * Math.PI.toFloat()
        val c = cos(t)
        val s = sin(t)
        val x = cx + cx * sign(c) * abs(c).pow(SQUIRCLE_EXP)
        val y = cy + cy * sign(s) * abs(s).pow(SQUIRCLE_EXP)
        if (step == 0) moveTo(x, y) else lineTo(x, y)
    }
    close()
}

private val SwatchCell = 64.dp
private val SwatchSize = 52.dp
private const val ROUNDED = 0.28f
private const val HALF = 0.5f
private const val TEAR_CORNER = 0.12f
private const val SQUIRCLE_STEPS = 64
private const val SQUIRCLE_EXP = 0.5f
