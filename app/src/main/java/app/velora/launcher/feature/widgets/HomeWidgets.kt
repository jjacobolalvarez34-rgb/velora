package app.velora.launcher.feature.widgets

import android.view.View
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.DraggableState
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import app.velora.launcher.R
import app.velora.launcher.core.data.favorites.WidgetEntity
import app.velora.launcher.core.data.favorites.WidgetsRepository
import app.velora.launcher.core.designsystem.theme.Shapes
import app.velora.launcher.core.designsystem.theme.Spacing
import app.velora.launcher.core.designsystem.theme.VeloraTheme

/**
 * Los widgets del inicio, uno debajo del otro. Son `View`s del sistema dentro de Compose; si su
 * app ya no los ofrece, no se dibujan (y se limpian, B-019). Mantener presionado uno lo pone en
 * modo edición: se marca, aparece "Quitar widget", flechas para moverlo y una barra para cambiar
 * su alto arrastrando.
 */
@Composable
fun HomeWidgets(host: WidgetHost, widgets: List<WidgetEntity>, modifier: Modifier = Modifier) {
    var editing by rememberSaveable { mutableStateOf<Int?>(null) }
    Column(modifier = modifier.fillMaxWidth()) {
        widgets.forEachIndexed { index, widget ->
            key(widget.appWidgetId) {
                val isEditing = editing == widget.appWidgetId
                EditableWidget(
                    host = host,
                    widget = widget,
                    editing = isEditing,
                    onLongPress = { editing = widget.appWidgetId },
                )
                AnimatedVisibility(visible = isEditing) {
                    WidgetEditBar(
                        canMoveUp = index > 0,
                        canMoveDown = index < widgets.lastIndex,
                        onMove = { offset -> host.update { move(widget.appWidgetId, offset) } },
                        onRemove = {
                            editing = null
                            host.remove(widget.appWidgetId)
                        },
                        onDone = { editing = null },
                    )
                }
            }
        }
    }
}

@Composable
private fun EditableWidget(host: WidgetHost, widget: WidgetEntity, editing: Boolean, onLongPress: () -> Unit) {
    val density = LocalDensity.current
    // Alto mientras se arrastra; se guarda al soltar.
    var heightDp by remember(widget.heightDp) { mutableFloatStateOf(widget.heightDp.toFloat()) }
    val drag = rememberDraggableState { delta ->
        val next = heightDp + with(density) { delta.toDp().value }
        heightDp = next.coerceIn(WidgetsRepository.MIN_HEIGHT_DP.toFloat(), WidgetsRepository.MAX_HEIGHT_DP.toFloat())
    }
    val colors = VeloraTheme.onWallpaper
    Column(Modifier.padding(vertical = Spacing.xs)) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(heightDp.dp)
                .clip(Shapes.menu)
                .then(if (editing) Modifier.border(2.dp, colors.accent, Shapes.menu) else Modifier),
        ) {
            AndroidView(
                factory = { context -> host.createView(context, widget.appWidgetId) ?: View(context) },
                update = { view -> (view as? LongPressWidgetHostView)?.onLongPress = onLongPress },
                modifier = Modifier.fillMaxSize(),
            )
            // En edición el widget no recibe toques: así no se abre su app por error.
            if (editing) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(colors.accent.copy(alpha = EDIT_TINT))
                        .pointerInput(Unit) { awaitPointerEventScope { while (true) awaitPointerEvent() } },
                )
            }
        }
        if (editing) {
            ResizeHandle(drag, onStopped = { host.update { setHeight(widget.appWidgetId, heightDp.toInt()) } })
        }
    }
}

/** La barrita bajo el widget en edición: arrastrarla cambia el alto. */
@Composable
private fun ResizeHandle(drag: DraggableState, onStopped: () -> Unit) {
    val description = stringResource(R.string.widget_resize_handle)
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxWidth()
            .height(HandleTouchHeight)
            .draggable(state = drag, orientation = Orientation.Vertical, onDragStopped = { onStopped() })
            .semantics { contentDescription = description },
    ) {
        Box(Modifier.size(HandleWidth, HandleHeight).background(VeloraTheme.onWallpaper.accent, CircleShape))
    }
}

/** La barra de edición: "Quitar widget" bien visible, mover y listo. */
@Composable
private fun WidgetEditBar(
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onMove: (Int) -> Unit,
    onRemove: () -> Unit,
    onDone: () -> Unit,
) {
    val colors = VeloraTheme.onWallpaper
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.s),
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = Spacing.s)
            .background(colors.surface, Shapes.menu)
            .padding(horizontal = Spacing.s, vertical = Spacing.xs),
    ) {
        Button(
            onClick = onRemove,
            colors = ButtonDefaults.buttonColors(containerColor = RemoveRed, contentColor = Color.White),
        ) {
            Icon(Icons.Outlined.Delete, contentDescription = null)
            Spacer(Modifier.width(Spacing.xs))
            Text(stringResource(R.string.widget_remove))
        }
        FilledTonalIconButton(onClick = { onMove(-1) }, enabled = canMoveUp) {
            Icon(Icons.Outlined.KeyboardArrowUp, contentDescription = stringResource(R.string.widget_move_up))
        }
        FilledTonalIconButton(onClick = { onMove(1) }, enabled = canMoveDown) {
            Icon(Icons.Outlined.KeyboardArrowDown, contentDescription = stringResource(R.string.widget_move_down))
        }
        Spacer(Modifier.weight(1f))
        TextButton(onClick = onDone) { Text(stringResource(R.string.widget_done), color = colors.content) }
    }
}

private const val EDIT_TINT = 0.12f
private val RemoveRed = Color(0xFFD93A3A)
private val HandleTouchHeight = 28.dp
private val HandleWidth = 48.dp
private val HandleHeight = 5.dp
