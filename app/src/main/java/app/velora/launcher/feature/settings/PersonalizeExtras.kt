package app.velora.launcher.feature.settings

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import app.velora.launcher.R
import app.velora.launcher.core.model.IconShape
import app.velora.launcher.core.model.ThemeSettings
import app.velora.launcher.core.model.VisibleElements
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sign
import kotlin.math.sin

// Secciones Color y Elementos de Personalizar (ADR-028), y la silueta de las formas de ícono.

/** Colores del tema: el del tema elegido o uno de estos, pensados para leerse sobre cualquier fondo. */
private val Swatches = listOf(
    0xFFF4F4F4, 0xFFFFD86B, 0xFFFFB38A, 0xFFFF7A7A, 0xFFFF5FA2, 0xFFD18CFF,
    0xFF9A8CFF, 0xFF7FB8FF, 0xFF5FE0E8, 0xFF7FF0B0, 0xFFB8E07A, 0xFF1E1E24,
)

/** "El del tema" arriba, y los colores fijos en círculos grandes, seis por fila. */
@Composable
internal fun ColorTab(look: ThemeSettings, edit: ThemeEdit) {
    val current = look.custom.accentColor
    val choose = { argb: Long? -> edit { it.copy(custom = it.custom.copy(accentColor = argb)) } }
    PanelList {
        sectionHint(R.string.personalize_hint_color)
        groupTitle("auto", R.string.personalize_group_auto_color)
        item(key = "auto") {
            RadioOption(
                title = stringResource(R.string.personalize_color_theme_title),
                subtitle = stringResource(R.string.personalize_color_theme_summary),
                selected = current == null,
                onClick = { choose(null) },
            )
        }
        groupTitle("colors", R.string.personalize_group_colors)
        optionGrid("colors", Swatches, columns = SWATCH_COLUMNS) { argb ->
            Swatch(Color(argb.toInt()), current == argb, colorName(argb)) { choose(argb) }
        }
    }
}

@Composable
private fun Swatch(color: Color, selected: Boolean, label: String, onClick: () -> Unit) {
    val ring = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent
    // La marca se ve sobre cualquier color: oscura en los claros y clara en los oscuros.
    val mark = if (color.luminance() > LIGHT_COLOR) Color.Black else Color.White
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .border(SwatchRing, ring, CircleShape)
            .padding(SwatchGap)
            .background(color, CircleShape)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .semantics { contentDescription = label },
    ) {
        if (selected) Icon(Icons.Outlined.Check, contentDescription = null, tint = mark)
    }
}

@Composable
private fun colorName(argb: Long): String =
    stringResource(R.string.personalize_color_swatch, "#%06X".format(argb and RGB_MASK))

/** Un interruptor de "qué se ve en el inicio": su texto, su ícono y qué campo cambia. */
private class ElementToggle(
    @StringRes val title: Int,
    @StringRes val summary: Int,
    val icon: ImageVector,
    val read: (VisibleElements) -> Boolean,
    val write: (VisibleElements, Boolean) -> VisibleElements,
)

private val ElementToggles = listOf(
    ElementToggle(
        R.string.element_clock,
        R.string.element_clock_summary,
        Icons.Outlined.DateRange,
        { it.clock },
        { v, on -> v.copy(clock = on) },
    ),
    ElementToggle(
        R.string.element_date,
        R.string.element_date_summary,
        Icons.Outlined.Info,
        { it.date },
        { v, on -> v.copy(date = on) },
    ),
    ElementToggle(
        R.string.element_status_bar,
        R.string.element_status_bar_summary,
        Icons.Outlined.Menu,
        { it.statusBar },
        { v, on -> v.copy(statusBar = on) },
    ),
    ElementToggle(
        R.string.element_tips,
        R.string.element_tips_summary,
        Icons.Outlined.Notifications,
        { it.tips },
        { v, on -> v.copy(tips = on) },
    ),
)

/** Qué se ve en el inicio: un interruptor por elemento. */
@Composable
internal fun ElementsTab(look: ThemeSettings, edit: ThemeEdit) {
    val visible = look.custom.visible
    PanelList {
        sectionHint(R.string.personalize_hint_elements)
        groupTitle("elements", R.string.personalize_group_elements)
        items(ElementToggles, key = { it.title }) { toggle ->
            SwitchOption(
                title = stringResource(toggle.title),
                subtitle = stringResource(toggle.summary),
                checked = toggle.read(visible),
                onChange = { on ->
                    edit { it.copy(custom = it.custom.copy(visible = toggle.write(it.custom.visible, on))) }
                },
                icon = toggle.icon,
            )
        }
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

private const val SWATCH_COLUMNS = 6

/** Luminancia desde la que un color se considera claro. */
private const val LIGHT_COLOR = 0.5f

/** Solo el color, sin la transparencia, para nombrarlo como #RRGGBB. */
private const val RGB_MASK = 0xFFFFFFL
private val SwatchRing = 2.dp
private val SwatchGap = 4.dp
private const val ROUNDED = 0.28f
private const val HALF = 0.5f
private const val TEAR_CORNER = 0.12f
private const val SQUIRCLE_STEPS = 64
private const val SQUIRCLE_EXP = 0.5f
