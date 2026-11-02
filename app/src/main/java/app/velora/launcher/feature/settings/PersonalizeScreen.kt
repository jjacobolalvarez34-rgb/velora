package app.velora.launcher.feature.settings

import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.List
import androidx.compose.material.icons.outlined.AccountBox
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Face
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import app.velora.launcher.R
import app.velora.launcher.core.designsystem.theme.Spacing
import app.velora.launcher.core.designsystem.theme.VeloraTheme
import app.velora.launcher.core.model.AppEntry
import app.velora.launcher.core.model.ThemeSettings
import app.velora.launcher.feature.home.ClockText

/** Las secciones de Personalizar, de la más usada a la más fina. */
enum class PersonalizeTab(@StringRes val label: Int) {
    Theme(R.string.personalize_tab_theme),
    Background(R.string.personalize_tab_background),
    Clock(R.string.personalize_tab_clock),
    Icons(R.string.personalize_tab_icons),
    Color(R.string.personalize_tab_color),
    Font(R.string.personalize_tab_font),
    Elements(R.string.personalize_tab_elements),
}

/** El ícono de cada pastilla; Color no lleva: muestra un punto del color elegido. */
private fun tabIcon(tab: PersonalizeTab): ImageVector? = when (tab) {
    PersonalizeTab.Theme -> Icons.Outlined.Star
    PersonalizeTab.Background -> Icons.Outlined.AccountBox
    PersonalizeTab.Clock -> Icons.Outlined.DateRange
    PersonalizeTab.Icons -> Icons.Outlined.Face
    PersonalizeTab.Color -> null
    PersonalizeTab.Font -> Icons.Outlined.Edit
    PersonalizeTab.Elements -> Icons.AutoMirrored.Outlined.List
}

/** Un cambio al borrador: `onEdit { it.copy(font = font) }`. */
typealias ThemeEdit = ((ThemeSettings) -> ThemeSettings) -> Unit

/**
 * Lo que muestra Personalizar.
 *
 * @property look el borrador si hay cambios, o lo guardado.
 * @property photo la foto de [look], ya cargada.
 * @property history fondos anteriores (rutas), del más nuevo al más viejo.
 * @property photoInUse la foto que usa el inicio ahora: no se puede borrar.
 */
@Immutable
data class PersonalizeState(
    val look: ThemeSettings,
    val hasChanges: Boolean = false,
    val photo: ImageBitmap? = null,
    val history: List<String> = emptyList(),
    val photoInUse: String? = null,
)

/** Lo que Personalizar puede pedir. Todo cambia el borrador; solo [onSave] toca el inicio. */
interface PersonalizeActions {
    fun onEdit(change: (ThemeSettings) -> ThemeSettings)
    fun onPickPhoto()
    fun onDeletePhoto(path: String)
    fun onSave()
    fun onDiscard()
}

/**
 * Personalizar (ADR-028): arriba la vista previa del inicio en un marco de teléfono; debajo, un
 * panel con las secciones en una fila de pastillas y sus opciones. Los cambios se ven al instante
 * en la vista previa y se aplican al inicio solo con "Guardar"; salir con cambios pregunta.
 */
@Composable
fun PersonalizeScreen(
    state: PersonalizeState,
    preview: ClockPreview,
    favorites: List<AppEntry>,
    iconPacks: List<InstalledIconPack>,
    actions: PersonalizeActions,
    onBack: () -> Unit,
) {
    var tab by rememberSaveable { mutableStateOf(PersonalizeTab.Theme) }
    var asking by rememberSaveable { mutableStateOf(false) }
    val leave: () -> Unit = { if (state.hasChanges) asking = true else onBack() }
    BackHandler(enabled = state.hasChanges) { asking = true }
    Column(
        Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)),
    ) {
        PersonalizeTopBar(state.hasChanges, onBack = leave, onDiscard = actions::onDiscard, onSave = actions::onSave)
        // El tamaño de la vista previa depende solo de la pantalla: cambiar de sección no la mueve.
        Box(Modifier.fillMaxWidth().weight(PREVIEW_WEIGHT).padding(Spacing.m), contentAlignment = Alignment.Center) {
            PhonePreview(state.look, preview, state.photo, favorites)
        }
        Column(
            Modifier
                .fillMaxWidth()
                .weight(PANEL_WEIGHT)
                .clip(PanelShape)
                .background(MaterialTheme.colorScheme.surfaceContainer),
        ) {
            SectionPills(tab, state.look, onSelect = { tab = it })
            SectionContent(tab, Modifier.weight(1f)) { current ->
                TabContent(current, state, preview, favorites, iconPacks, actions)
            }
        }
    }
    if (asking) UnsavedChangesDialog(actions, onLeave = onBack, onClose = { asking = false })
}

/** El contenido de la sección elegida; al cambiar, uno se desvanece y entra el otro. */
@Composable
private fun SectionContent(
    tab: PersonalizeTab,
    modifier: Modifier = Modifier,
    content: @Composable (PersonalizeTab) -> Unit,
) {
    val motion = VeloraTheme.motion
    AnimatedContent(
        targetState = tab,
        transitionSpec = { fadeIn(motion.effectDefault()) togetherWith fadeOut(motion.effectFast()) },
        label = "personalize-tab",
        modifier = modifier,
    ) { current -> content(current) }
}

@Composable
private fun TabContent(
    tab: PersonalizeTab,
    state: PersonalizeState,
    preview: ClockPreview,
    favorites: List<AppEntry>,
    iconPacks: List<InstalledIconPack>,
    actions: PersonalizeActions,
) {
    val look = state.look
    val edit: ThemeEdit = actions::onEdit
    when (tab) {
        PersonalizeTab.Theme -> ThemeTab(look, edit)
        PersonalizeTab.Background -> BackgroundTab(state, actions)
        PersonalizeTab.Clock -> ClockTab(state, preview, edit)
        PersonalizeTab.Icons -> IconsTab(look, favorites, iconPacks, edit)
        PersonalizeTab.Color -> ColorTab(look, edit)
        PersonalizeTab.Font -> FontTab(look, edit)
        PersonalizeTab.Elements -> ElementsTab(look, edit)
    }
}

/** Atrás y título; "Descartar" y "Guardar" aparecen solo cuando hay cambios. */
@Composable
private fun PersonalizeTopBar(hasChanges: Boolean, onBack: () -> Unit, onDiscard: () -> Unit, onSave: () -> Unit) {
    val motion = VeloraTheme.motion
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().height(TopBarHeight).padding(start = Spacing.xs, end = Spacing.m),
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = stringResource(R.string.settings_back))
        }
        Text(
            stringResource(R.string.personalize_title),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.weight(1f).semantics { heading() },
        )
        AnimatedVisibility(
            visible = hasChanges,
            enter = fadeIn(motion.effectDefault()),
            exit = fadeOut(motion.effectFast()),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onDiscard) { Text(stringResource(R.string.personalize_discard)) }
                Spacer(Modifier.width(Spacing.xs))
                Button(onClick = onSave) { Text(stringResource(R.string.personalize_save)) }
            }
        }
    }
}

/** Las secciones en una sola fila; la elegida, llena del color primario y siempre a la vista. */
@Composable
private fun SectionPills(selected: PersonalizeTab, look: ThemeSettings, onSelect: (PersonalizeTab) -> Unit) {
    val listState = rememberLazyListState()
    val reduced = VeloraTheme.motion.reduced
    LaunchedEffect(selected) {
        val index = (selected.ordinal - 1).coerceAtLeast(0)
        if (reduced) listState.scrollToItem(index) else listState.animateScrollToItem(index)
    }
    val dot = look.userAccent() ?: VeloraTheme.onWallpaper.accent
    LazyRow(
        state = listState,
        contentPadding = PaddingValues(start = Spacing.m, end = Spacing.m, top = Spacing.m, bottom = Spacing.xs),
        horizontalArrangement = Arrangement.spacedBy(Spacing.s),
        modifier = Modifier.fillMaxWidth(),
    ) {
        items(PersonalizeTab.entries, key = { it.name }) { entry ->
            Pill(entry, selected = entry == selected, dot = dot, onClick = { onSelect(entry) })
        }
    }
}

@Composable
private fun Pill(tab: PersonalizeTab, selected: Boolean, dot: Color, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val motion = VeloraTheme.motion
    val background by animateColorAsState(
        if (selected) scheme.primary else scheme.surfaceContainerHighest,
        motion.effectFast(),
        "pill",
    )
    val content = if (selected) scheme.onPrimary else scheme.onSurfaceVariant
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .height(PillHeight)
            .clip(CircleShape)
            .background(background)
            .selectable(selected = selected, role = Role.Tab, onClick = onClick)
            .padding(horizontal = PillPadding),
    ) {
        val icon = tabIcon(tab)
        if (icon == null) {
            Box(Modifier.size(DotSize).background(dot, CircleShape))
        } else {
            Icon(icon, contentDescription = null, tint = content, modifier = Modifier.size(PillIcon))
        }
        Spacer(Modifier.width(Spacing.s))
        Text(stringResource(tab.label), style = MaterialTheme.typography.labelLarge, color = content, maxLines = 1)
    }
}

/** Salir con cambios sin guardar: guardar, descartar (y en ambos casos salir) o seguir editando. */
@Composable
private fun UnsavedChangesDialog(actions: PersonalizeActions, onLeave: () -> Unit, onClose: () -> Unit) {
    val finish = { action: () -> Unit ->
        onClose()
        action()
        onLeave()
    }
    AlertDialog(
        onDismissRequest = onClose,
        title = { Text(stringResource(R.string.personalize_unsaved_title)) },
        text = { Text(stringResource(R.string.personalize_unsaved_text)) },
        confirmButton = {
            Button(onClick = { finish(actions::onSave) }) { Text(stringResource(R.string.personalize_save)) }
        },
        dismissButton = {
            Row {
                TextButton(onClick = onClose) { Text(stringResource(R.string.personalize_keep_editing)) }
                TextButton(onClick = { finish(actions::onDiscard) }) {
                    Text(stringResource(R.string.personalize_discard))
                }
            }
        },
    )
}

private const val PREVIEW_WEIGHT = 0.45f
private const val PANEL_WEIGHT = 0.55f
private val TopBarHeight = 64.dp
private val PanelShape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
private val PillHeight = 40.dp
private val PillPadding = 14.dp
private val PillIcon = 18.dp
private val DotSize = 14.dp

internal object PreviewPersonalizeActions : PersonalizeActions {
    override fun onEdit(change: (ThemeSettings) -> ThemeSettings) = Unit
    override fun onPickPhoto() = Unit
    override fun onDeletePhoto(path: String) = Unit
    override fun onSave() = Unit
    override fun onDiscard() = Unit
}

@Preview(name = "Claro", heightDp = 800)
@Preview(name = "Oscuro", heightDp = 800, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PersonalizeScreenPreview() {
    VeloraTheme(reducedMotion = true) {
        Box(Modifier.background(MaterialTheme.colorScheme.background)) {
            PersonalizeScreen(
                state = PersonalizeState(ThemeSettings(), hasChanges = true),
                preview = ClockPreview(ClockText("7:05", "Jueves, 23 de octubre", "JUE"), battery = 82),
                favorites = emptyList(),
                iconPacks = emptyList(),
                actions = PreviewPersonalizeActions,
                onBack = {},
            )
        }
    }
}
