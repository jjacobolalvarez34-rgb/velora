package app.velora.launcher.feature.settings

import androidx.annotation.StringRes
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.layout.layout
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.velora.launcher.core.designsystem.theme.Spacing
import app.velora.launcher.core.designsystem.theme.VeloraTheme
import kotlin.math.min
import kotlin.math.roundToInt

// Piezas comunes de Personalizar: tarjetas de opción, rejillas, títulos de grupo y el escalado
// de las miniaturas. Todas las secciones se arman con lo mismo, así se ven iguales.

/**
 * Dibuja el contenido a tamaño real ([width]×[height]) y lo achica para que quepa en el espacio
 * disponible, sin re-maquetarlo: miniaturas fieles del inicio o de un reloj. El tamaño final es
 * el del contenido escalado, sin bordes vacíos.
 */
internal fun Modifier.fitInside(width: Dp, height: Dp): Modifier = layout { measurable, constraints ->
    val fullWidth = width.roundToPx()
    val fullHeight = height.roundToPx()
    val byWidth = if (constraints.hasBoundedWidth) constraints.maxWidth / fullWidth.toFloat() else 1f
    val byHeight = if (constraints.hasBoundedHeight) constraints.maxHeight / fullHeight.toFloat() else byWidth
    val scale = min(byWidth, byHeight)
    val placeable = measurable.measure(Constraints.fixed(fullWidth, fullHeight))
    val shownWidth = (fullWidth * scale).roundToInt().coerceIn(constraints.minWidth, constraints.maxWidth)
    val shownHeight = (fullHeight * scale).roundToInt()
        .coerceIn(constraints.minHeight, if (constraints.hasBoundedHeight) constraints.maxHeight else Int.MAX_VALUE)
    layout(shownWidth, shownHeight) {
        placeable.placeWithLayer(0, 0) {
            scaleX = scale
            scaleY = scale
            transformOrigin = TransformOrigin(0f, 0f)
        }
    }
}

/**
 * Tarjeta de opción, igual en todas las secciones: una miniatura con la proporción [aspect]
 * (ancho/alto), el nombre en una línea debajo y, si está elegida, borde del color primario y una
 * marca. En una rejilla todas miden lo mismo.
 */
@Composable
internal fun OptionCard(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    aspect: Float = DEFAULT_ASPECT,
    thumbColor: Color = MaterialTheme.colorScheme.surfaceContainerHighest,
    content: @Composable BoxScope.() -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val motion = VeloraTheme.motion
    val border by animateColorAsState(if (selected) scheme.primary else Color.Transparent, motion.effectFast(), "ring")
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.selectable(selected = selected, role = Role.RadioButton, onClick = onClick),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(aspect)
                .border(SelectedRing, border, ThumbShape)
                .padding(RingGap)
                .clip(InnerShape)
                .background(thumbColor),
        ) {
            content()
            if (selected) CheckBadge(Modifier.align(Alignment.TopEnd).padding(Spacing.xs))
        }
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (selected) scheme.primary else scheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth().padding(top = Spacing.xs + RingGap),
        )
    }
}

/** Marca de "elegida": círculo primario con un check. */
@Composable
internal fun CheckBadge(modifier: Modifier = Modifier) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.size(BadgeSize).background(MaterialTheme.colorScheme.primary, CircleShape),
    ) {
        Icon(
            Icons.Outlined.Check,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.size(BadgeIcon),
        )
    }
}

/** La lista de una sección: márgenes iguales y aire al final para la barra de navegación. */
@Composable
internal fun PanelList(content: LazyListScope.() -> Unit) {
    val bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    LazyColumn(
        contentPadding = PaddingValues(top = Spacing.xs, bottom = bottom + Spacing.l),
        modifier = Modifier.fillMaxSize(),
        content = content,
    )
}

/** La línea de ayuda de la sección: una sola, corta. */
internal fun LazyListScope.sectionHint(@StringRes text: Int) {
    item(key = "hint") {
        Text(
            stringResource(text),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = Spacing.m, vertical = Spacing.s),
        )
    }
}

/** Título de grupo, el mismo en todas las secciones. */
internal fun LazyListScope.groupTitle(key: String, @StringRes title: Int) {
    item(key = "$key-title") { GroupTitle(title) }
}

@Composable
internal fun GroupTitle(@StringRes title: Int, modifier: Modifier = Modifier) {
    Text(
        stringResource(title),
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = modifier
            .padding(start = Spacing.m, end = Spacing.m, top = Spacing.m, bottom = Spacing.s)
            .semantics { heading() },
    )
}

/**
 * Rejilla de [columns] columnas dentro de una lista: cada fila reparte el ancho en partes iguales
 * (las tarjetas miden lo mismo y quedan alineadas, también en la última fila).
 */
internal fun <T> LazyListScope.optionGrid(key: String, entries: List<T>, columns: Int, cell: @Composable (T) -> Unit) {
    val rows = entries.chunked(columns)
    items(rows.size, key = { "$key-row-$it" }) { index ->
        Row(
            horizontalArrangement = Arrangement.spacedBy(GridGap),
            modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.m).padding(bottom = Spacing.m),
        ) {
            val row = rows[index]
            row.forEach { entry -> Box(Modifier.weight(1f)) { cell(entry) } }
            repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
        }
    }
}

/** Separación vertical entre grupos. */
internal fun LazyListScope.groupGap(key: String) {
    item(key = "$key-gap") { Spacer(Modifier.height(Spacing.s)) }
}

/** Proporción por defecto de una miniatura (un poco más ancha que alta). */
internal const val DEFAULT_ASPECT = 1.25f
internal val GridGap = 12.dp
private val SelectedRing = 2.dp
private val RingGap = 3.dp
private val ThumbShape = RoundedCornerShape(18.dp)
private val InnerShape = RoundedCornerShape(15.dp)
private val BadgeSize = 20.dp
private val BadgeIcon = 14.dp
