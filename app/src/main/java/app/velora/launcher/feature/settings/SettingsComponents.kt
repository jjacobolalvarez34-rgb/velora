package app.velora.launcher.feature.settings

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import app.velora.launcher.R
import app.velora.launcher.core.designsystem.theme.Spacing

// Piezas de Ajustes que comparten todas las páginas: títulos, tarjetas, opciones y el menú agrupado.

/** Una página de Ajustes con su título, botón atrás y su lista. */
@Composable
internal fun SettingsListPage(title: Int, onBack: () -> Unit, content: LazyListScope.() -> Unit) {
    LazyColumn(contentPadding = WindowInsets.safeDrawing.asPaddingValues(), modifier = Modifier.fillMaxSize()) {
        item(key = "page-title") { PageTitle(stringResource(title), onBack) }
        content()
    }
}

/** Título con flecha atrás, igual en todas las páginas. */
@Composable
internal fun PageTitle(text: String, onBack: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(start = Spacing.xs, end = Spacing.m, top = Spacing.m, bottom = Spacing.s),
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = stringResource(R.string.settings_back))
        }
        Text(
            text = text,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.semantics { heading() },
        )
    }
}

/** El título grande del menú de Ajustes. */
@Composable
internal fun Title(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.headlineMedium,
        color = MaterialTheme.colorScheme.onBackground,
        modifier = Modifier
            .padding(start = Spacing.l, end = Spacing.m, top = Spacing.l, bottom = Spacing.m)
            .semantics { heading() },
    )
}

/** Tarjeta de ajustes: esquinas redondeadas y un poco de aire entre una y otra. */
internal fun Modifier.settingsCard() = padding(horizontal = Spacing.m, vertical = 3.dp).clip(SettingsCardShape)

@Composable
internal fun cardColors() = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)

/** Ícono de una opción dentro de un círculo suave; [tinted] lo pinta con el color primario. */
@Composable
internal fun IconCircle(icon: ImageVector, tinted: Boolean = false) {
    val scheme = MaterialTheme.colorScheme
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(IconCircleSize)
            .background(
                if (tinted) scheme.primaryContainer else scheme.onSurface.copy(alpha = ICON_CIRCLE_ALPHA),
                CircleShape,
            ),
    ) {
        Icon(icon, contentDescription = null, tint = if (tinted) scheme.onPrimaryContainer else scheme.onSurface)
    }
}

@Composable
internal fun SectionTitle(@StringRes title: Int) {
    Text(
        text = stringResource(title),
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier
            .padding(start = Spacing.m, end = Spacing.m, top = Spacing.l, bottom = Spacing.xs)
            .semantics { heading() },
    )
}

@Composable
internal fun RadioOption(
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
    subtitle: String? = null,
    titleStyle: TextStyle = MaterialTheme.typography.bodyLarge,
) {
    ListItem(
        headlineContent = { Text(title, style = titleStyle) },
        supportingContent = subtitle?.let { { Text(it) } },
        trailingContent = { RadioButton(selected = selected, onClick = null) },
        colors = cardColors(),
        modifier = Modifier.settingsCard().selectable(selected = selected, role = Role.RadioButton, onClick = onClick),
    )
}

@Composable
internal fun Option(title: String, subtitle: String, onClick: () -> Unit, icon: ImageVector? = null) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = { Text(subtitle) },
        leadingContent = icon?.let { { IconCircle(it) } },
        colors = cardColors(),
        modifier = Modifier.settingsCard().clickable(onClick = onClick),
    )
}

/** Una fila del menú principal de Ajustes. */
@Immutable
internal data class MenuEntry(val title: String, val subtitle: String, val icon: ImageVector, val onClick: () -> Unit)

/**
 * Un grupo del menú principal: su título y sus filas dentro de una sola tarjeta, separadas por
 * líneas finas (más ordenado que una tarjeta por fila).
 */
@Composable
internal fun MenuGroup(@StringRes title: Int, entries: List<MenuEntry>) {
    Column(Modifier.padding(bottom = Spacing.s)) {
        Text(
            text = stringResource(title),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .padding(start = Spacing.l, end = Spacing.m, top = Spacing.l, bottom = Spacing.s)
                .semantics { heading() },
        )
        Column(
            Modifier
                .padding(horizontal = Spacing.m)
                .fillMaxWidth()
                .clip(SettingsCardShape)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
        ) {
            entries.forEachIndexed { index, entry ->
                if (index > 0) {
                    HorizontalDivider(
                        modifier = Modifier.padding(start = DividerInset),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = DIVIDER_ALPHA),
                    )
                }
                MenuRow(entry)
            }
        }
    }
}

@Composable
private fun MenuRow(entry: MenuEntry) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = entry.onClick)
            .padding(horizontal = Spacing.m, vertical = MenuRowPadding),
    ) {
        IconCircle(entry.icon, tinted = true)
        Spacer(Modifier.width(Spacing.m))
        Column(Modifier.weight(1f)) {
            Text(entry.title, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
            Text(
                entry.subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Icon(
            Icons.AutoMirrored.Outlined.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private val SettingsCardShape = RoundedCornerShape(20.dp)
private val IconCircleSize = 40.dp
private const val ICON_CIRCLE_ALPHA = 0.1f
private const val DIVIDER_ALPHA = 0.6f

/** Las líneas empiezan donde empieza el texto (pasado el ícono). */
private val DividerInset = 72.dp
private val MenuRowPadding = 14.dp
