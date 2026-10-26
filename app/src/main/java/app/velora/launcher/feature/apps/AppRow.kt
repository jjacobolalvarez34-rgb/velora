package app.velora.launcher.feature.apps

import android.content.res.Configuration
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import app.velora.launcher.R
import app.velora.launcher.core.designsystem.theme.Sizes
import app.velora.launcher.core.designsystem.theme.Spacing
import app.velora.launcher.core.designsystem.theme.VeloraTheme
import app.velora.launcher.core.model.AppEntry
import app.velora.launcher.core.model.AppKey
import app.velora.launcher.core.model.AppShortcut
import app.velora.launcher.core.model.IconStyle

/** Dónde se usa la fila: cambia tamaños y tipografía (ESTILO §3 y §4). */
enum class AppRowStyle { Favorite, List }

/** Guarda dónde quedó la fila sin provocar recomposiciones; se lee solo al tocar. */
private class RowPosition {
    var coordinates: LayoutCoordinates? = null

    fun bounds(): Rect? = coordinates?.takeIf { it.isAttached }?.boundsInRoot()
}

/**
 * Una app: ícono + nombre. Tocar la abre; mantener presionado abre su menú.
 *
 * @param dimmed se lee en la fase de dibujo (resaltar la letra del alfabeto sin recomponer).
 */
@Composable
fun AppRow(
    app: AppEntry,
    style: AppRowStyle,
    actions: AppActions,
    modifier: Modifier = Modifier,
    dimmed: () -> Boolean = { false },
) {
    val position = remember { RowPosition() }
    val interaction = remember { MutableInteractionSource() }
    val scale by pressScale(interaction)
    val workDescription = stringResource(R.string.app_work_profile_description, app.label)

    Box(modifier = modifier) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = style.height)
                .onPlaced { position.coordinates = it }
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    alpha = if (dimmed()) DIMMED_ALPHA else 1f
                }
                .combinedClickable(
                    interactionSource = interaction,
                    indication = null,
                    onClick = { actions.open(app, position.bounds()) },
                    onLongClick = { actions.showMenu(app) },
                )
                .workProfileSemantics(app.isWorkProfile, workDescription)
                .padding(vertical = Spacing.xs),
        ) {
            AppRowContent(app, style)
        }
    }
}

/** Escala a `PressScale` con `SpatialFast` mientras el dedo está encima (ESTILO §5.3). */
@Composable
private fun pressScale(interaction: MutableInteractionSource): State<Float> {
    val motion = VeloraTheme.motion
    val pressed by interaction.collectIsPressedAsState()
    return animateFloatAsState(if (pressed) motion.pressScale else 1f, motion.spatialFast(), label = "press")
}

@Composable
private fun AppRowContent(app: AppEntry, style: AppRowStyle) {
    if (LocalIconStyle.current != IconStyle.None) {
        AppIcon(key = app.key, size = style.iconSize)
        Spacer(Modifier.width(Spacing.m))
    }
    Text(
        text = app.label,
        style = style.textStyle,
        color = VeloraTheme.onWallpaper.content,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

/** El nombre ya lo lee TalkBack; en perfil de trabajo se aclara de cuál perfil es. */
private fun Modifier.workProfileSemantics(isWork: Boolean, description: String) =
    if (isWork) semantics { contentDescription = description } else this

private const val DIMMED_ALPHA = 0.35f

private val AppRowStyle.height: Dp
    get() = when (this) {
        AppRowStyle.Favorite -> Sizes.favoriteRowHeight
        AppRowStyle.List -> Sizes.appRowHeight
    }

private val AppRowStyle.iconSize: Dp
    get() = when (this) {
        AppRowStyle.Favorite -> Sizes.favoriteIcon
        AppRowStyle.List -> Sizes.listIcon
    }

private val AppRowStyle.textStyle: TextStyle
    @Composable get() = when (this) {
        AppRowStyle.Favorite -> VeloraTheme.typography.favorite
        AppRowStyle.List -> VeloraTheme.typography.appRow
    }

/** Acciones vacías para previews. */
internal object PreviewAppActions : AppActions {
    override fun open(app: AppEntry, bounds: Rect?) = Unit
    override fun openInfo(app: AppEntry, bounds: Rect?) = Unit
    override fun uninstall(app: AppEntry) = Unit
    override fun addFavorite(app: AppEntry) = Unit
    override fun removeFavorite(app: AppEntry) = Unit
    override fun hide(app: AppEntry) = Unit
    override fun openShortcut(shortcut: AppShortcut, bounds: Rect?) = Unit
    override fun showMenu(app: AppEntry) = Unit
    override fun webSearch(query: String) = Unit
    override fun copy(text: String) = Unit
}

internal fun previewApp(label: String) = AppEntry(
    key = AppKey("preview.$label", "Main", 0),
    label = label,
    section = label.take(1).uppercase(),
    searchKey = label.lowercase(),
    isWorkProfile = false,
    canUninstall = true,
)

@Preview(name = "Claro")
@Preview(name = "Oscuro", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun AppRowPreview() {
    VeloraTheme(reducedMotion = true) {
        Box(Modifier.background(VeloraTheme.onWallpaper.drawerScrim).padding(Spacing.m)) {
            AppRow(previewApp("Cámara"), style = AppRowStyle.List, actions = PreviewAppActions)
        }
    }
}
