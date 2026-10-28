package app.velora.launcher.feature.settings

import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.velora.launcher.R
import app.velora.launcher.core.designsystem.theme.Spacing
import app.velora.launcher.core.designsystem.theme.VeloraTheme
import app.velora.launcher.core.model.AppEntry
import app.velora.launcher.core.model.ThemeSettings

/** Las pestañas de Personalizar, en el orden en que se suele armar un inicio. */
enum class PersonalizeTab(@StringRes val label: Int) {
    Theme(R.string.personalize_tab_theme),
    Background(R.string.personalize_tab_background),
    Clock(R.string.personalize_tab_clock),
    Font(R.string.personalize_tab_font),
    Icons(R.string.personalize_tab_icons),
    Color(R.string.personalize_tab_color),
    Elements(R.string.personalize_tab_elements),
}

/**
 * Personalizar (ADR-028): arriba, el inicio en miniatura que cambia al instante; debajo, una
 * pestaña por cosa (tema, fondo, reloj, letra, íconos, color, elementos). Todas usan las mismas
 * tarjetas, así se aprende una vez.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersonalizeScreen(
    theme: ThemeSettings,
    preview: ClockPreview,
    photo: ImageBitmap?,
    favorites: List<AppEntry>,
    actions: SettingsActions,
    features: FeatureActions,
    iconPacks: List<InstalledIconPack>,
    onBack: () -> Unit,
) {
    var tab by rememberSaveable { mutableStateOf(PersonalizeTab.Theme) }
    val motion = VeloraTheme.motion
    Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
        PageTitle(stringResource(R.string.personalize_title), onBack)
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            PhonePreview(theme, preview, photo, favorites, scale = PREVIEW_SCALE)
        }
        Spacer(Modifier.height(Spacing.s))
        PrimaryScrollableTabRow(selectedTabIndex = tab.ordinal, edgePadding = Spacing.m) {
            PersonalizeTab.entries.forEach { entry ->
                Tab(selected = tab == entry, onClick = { tab = entry }, text = { Text(stringResource(entry.label)) })
            }
        }
        AnimatedContent(
            targetState = tab,
            transitionSpec = { fadeIn(motion.effectDefault()) togetherWith fadeOut(motion.effectFast()) },
            label = "personalize-tab",
            modifier = Modifier.weight(1f),
        ) { current ->
            PersonalizeTabContent(current, theme, preview, favorites, actions, features, iconPacks)
        }
    }
}

@Composable
private fun PersonalizeTabContent(
    tab: PersonalizeTab,
    theme: ThemeSettings,
    preview: ClockPreview,
    favorites: List<AppEntry>,
    actions: SettingsActions,
    features: FeatureActions,
    iconPacks: List<InstalledIconPack>,
) {
    when (tab) {
        PersonalizeTab.Theme -> ThemeTab(theme, actions)
        PersonalizeTab.Background -> BackgroundTab(theme, features)
        PersonalizeTab.Clock -> ClockTab(theme, preview, actions)
        PersonalizeTab.Font -> FontTab(theme, actions)
        PersonalizeTab.Icons -> IconsTab(theme, favorites, actions, iconPacks)
        PersonalizeTab.Color -> ColorTab(theme, actions)
        PersonalizeTab.Elements -> ElementsTab(theme, actions)
    }
}

/** Espacio bajo cada pestaña: lo justo para no quedar debajo de la barra de navegación. */
@Composable
internal fun Modifier.tabContentPadding() = windowInsetsPadding(WindowInsets.navigationBars)

/**
 * Tarjeta de opción, la misma en todas las pestañas: contenido arriba, nombre abajo, borde del
 * color primario cuando está elegida.
 */
@Composable
internal fun OptionCard(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val border = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.xs),
        modifier = modifier
            .clip(CardShape)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .border(SELECTED_BORDER, border, CardShape)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .padding(Spacing.s),
    ) {
        Box(Modifier.clip(InnerShape), contentAlignment = Alignment.Center) { content() }
        Text(
            label,
            style = MaterialTheme.typography.labelLarge,
            textAlign = TextAlign.Center,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
        )
    }
}

/** Dibuja [content] a tamaño real ([width]×[height]) y lo achica: miniaturas fieles. */
@Composable
internal fun ScaledBox(width: Dp, height: Dp, scale: Float, content: @Composable () -> Unit) {
    Box(Modifier.size(width * scale, height * scale), contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .requiredSize(width, height)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                },
        ) { content() }
    }
}

private const val PREVIEW_SCALE = 0.42f
private val SELECTED_BORDER = 3.dp
private val CardShape = RoundedCornerShape(20.dp)
private val InnerShape = RoundedCornerShape(14.dp)
