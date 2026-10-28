package app.velora.launcher.feature.settings

import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import app.velora.launcher.R
import app.velora.launcher.core.designsystem.components.ThemeBackground
import app.velora.launcher.core.designsystem.motion.VeloraMotion
import app.velora.launcher.core.designsystem.theme.Shapes
import app.velora.launcher.core.designsystem.theme.Spacing
import app.velora.launcher.core.designsystem.theme.VeloraTheme
import app.velora.launcher.core.designsystem.theme.fontFamilyOf
import app.velora.launcher.core.model.AppEntry
import app.velora.launcher.core.model.ClockStyle
import app.velora.launcher.core.model.Customization
import app.velora.launcher.core.model.FontChoice
import app.velora.launcher.core.model.IconStyle
import app.velora.launcher.core.model.TextSize
import app.velora.launcher.core.model.ThemePreset
import app.velora.launcher.core.model.ThemeSettings
import app.velora.launcher.feature.home.ClockText
import app.velora.launcher.feature.home.components.ClockHeader

/** Lo que la pantalla de ajustes puede pedir. */
interface SettingsActions {
    fun onFont(font: FontChoice)
    fun onClockStyle(style: ClockStyle)
    fun onIconStyle(style: IconStyle)
    fun onChooseHomeApp()
    fun onUnhide(app: AppEntry)
    fun onMoveFavorite(app: AppEntry, offset: Int)
    fun onTextSize(size: TextSize)
    fun onLeftHanded(enabled: Boolean)
    fun onFullList(enabled: Boolean)
    fun onPreset(preset: ThemePreset)
    fun onWidgetHeight(appWidgetId: Int, heightDp: Int)
    fun onRemoveWidget(appWidgetId: Int)
    fun onMoveWidget(appWidgetId: Int, offset: Int)

    /** Íconos, color del tema, elementos visibles (ADR-028). */
    fun onCustom(change: (Customization) -> Customization)
}

/** Listas de apps que se editan en Ajustes. */
@Immutable
data class SettingsLists(
    val favorites: List<AppEntry> = emptyList(),
    val hidden: List<AppEntry> = emptyList(),
    val widgets: List<WidgetSetting> = emptyList(),
)

/** Un widget del inicio tal como se edita en Ajustes. */
@Immutable
data class WidgetSetting(val appWidgetId: Int, val label: String, val heightDp: Int)

/** Páginas de Ajustes: un menú corto y cada tema en su propia pantalla, siempre igual. */
enum class SettingsPage { Main, Personalize, Layout, Lists, Features }

/**
 * Ajustes (ADR-028): arriba "Personalizar" con una vista previa en vivo; debajo, pocas
 * categorías. Cada página entra deslizándose y "atrás" vuelve al menú.
 */
@Composable
fun SettingsScreen(
    theme: ThemeSettings,
    preview: ClockPreview,
    lists: SettingsLists,
    actions: SettingsActions,
    modifier: Modifier = Modifier,
    features: FeatureActions = PreviewFeatureActions,
    status: FeatureStatus = FeatureStatus(),
    photo: ImageBitmap? = null,
    iconPacks: List<InstalledIconPack> = emptyList(),
) {
    var page by rememberSaveable { mutableStateOf(SettingsPage.Main) }
    BackHandler(enabled = page != SettingsPage.Main) { page = SettingsPage.Main }
    val motion = VeloraTheme.motion
    AnimatedContent(
        targetState = page,
        transitionSpec = { pageTransition(initialState, targetState, motion) },
        label = "settings-page",
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) { current ->
        val back = { page = SettingsPage.Main }
        when (current) {
            SettingsPage.Main -> MainPage(theme, preview, photo, lists, actions, features, onOpen = { page = it })

            SettingsPage.Personalize ->
                PersonalizeScreen(theme, preview, photo, lists.favorites, actions, features, iconPacks, back)

            SettingsPage.Layout -> SettingsListPage(R.string.settings_page_layout, back) {
                layoutSection(theme, actions)
            }

            SettingsPage.Lists -> SettingsListPage(R.string.settings_page_lists, back) {
                favoritesSection(lists.favorites, actions)
                widgetsSection(lists.widgets, actions)
                hiddenSection(lists.hidden, actions)
            }

            SettingsPage.Features -> SettingsListPage(R.string.settings_page_features, back) {
                featuresSection(theme, status, features)
            }
        }
    }
}

/** Hacia una página: entra desde la derecha; de vuelta al menú, al revés. */
private fun pageTransition(from: SettingsPage, to: SettingsPage, motion: VeloraMotion): ContentTransform {
    val forward = to != SettingsPage.Main
    val sign = if (forward) 1 else -1
    return (
        slideInHorizontally(motion.spatialDefault()) {
            sign * it / PAGE_SLIDE
        } + fadeIn(motion.effectDefault())
        ) togetherWith
        (slideOutHorizontally(motion.spatialDefault()) { -sign * it / PAGE_SLIDE } + fadeOut(motion.effectFast()))
}

private const val PAGE_SLIDE = 3

@Composable
private fun MainPage(
    theme: ThemeSettings,
    preview: ClockPreview,
    photo: ImageBitmap?,
    lists: SettingsLists,
    actions: SettingsActions,
    features: FeatureActions,
    onOpen: (SettingsPage) -> Unit,
) {
    LazyColumn(contentPadding = WindowInsets.safeDrawing.asPaddingValues(), modifier = Modifier.fillMaxSize()) {
        item(key = "title") { Title(stringResource(R.string.settings_title)) }
        item(key = "personalize") {
            PersonalizeHero(theme, preview, photo, lists.favorites, onClick = { onOpen(SettingsPage.Personalize) })
        }
        item(key = "categories") { SectionTitle(R.string.settings_more) }
        item(key = "layout") {
            Option(
                title = stringResource(R.string.settings_page_layout),
                subtitle = stringResource(R.string.settings_page_layout_summary),
                onClick = { onOpen(SettingsPage.Layout) },
                icon = Icons.Outlined.Menu,
            )
        }
        item(key = "lists") {
            Option(
                title = stringResource(R.string.settings_page_lists),
                subtitle = stringResource(R.string.settings_page_lists_summary),
                onClick = { onOpen(SettingsPage.Lists) },
                icon = Icons.Outlined.Star,
            )
        }
        item(key = "features") {
            Option(
                title = stringResource(R.string.settings_page_features),
                subtitle = stringResource(R.string.settings_page_features_summary),
                onClick = { onOpen(SettingsPage.Features) },
                icon = Icons.Outlined.Notifications,
            )
        }
        shareSection(features)
        generalSection(actions)
    }
}

private fun LazyListScope.generalSection(actions: SettingsActions) {
    item(key = "general") { SectionTitle(R.string.settings_general) }
    item(key = "home-app") {
        Option(
            title = stringResource(R.string.settings_home_app_title),
            subtitle = stringResource(R.string.settings_home_app_summary),
            onClick = actions::onChooseHomeApp,
            icon = Icons.Outlined.Home,
        )
    }
}

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

internal fun LazyListScope.hiddenSection(hidden: List<AppEntry>, actions: SettingsActions) {
    item(key = "hidden") { SectionTitle(R.string.settings_hidden) }
    if (hidden.isEmpty()) {
        item(key = "hidden-empty") {
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_hidden_empty)) },
                colors = cardColors(),
                modifier = Modifier.settingsCard(),
            )
        }
    }
    items(hidden.size, key = { "hidden-${hidden[it].key.stableId}" }) { index ->
        val app = hidden[index]
        ListItem(
            headlineContent = { Text(app.label) },
            trailingContent = {
                TextButton(onClick = { actions.onUnhide(app) }) { Text(stringResource(R.string.settings_unhide)) }
            },
            colors = cardColors(),
            modifier = Modifier.settingsCard(),
        )
    }
}

@Composable
private fun Title(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.headlineMedium,
        color = MaterialTheme.colorScheme.onBackground,
        modifier = Modifier
            .padding(horizontal = Spacing.m, vertical = Spacing.l)
            .semantics { heading() },
    )
}

/** Tarjeta de ajustes: esquinas redondeadas y un poco de aire entre una y otra. */
internal fun Modifier.settingsCard() = padding(horizontal = Spacing.m, vertical = 3.dp).clip(SettingsCardShape)

private val SettingsCardShape = RoundedCornerShape(20.dp)
private val IconCircleSize = 40.dp

@Composable
internal fun cardColors() = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)

/** Ícono de una opción dentro de un círculo suave, a la izquierda de la tarjeta. */
@Composable
internal fun IconCircle(icon: ImageVector) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(IconCircleSize)
            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f), CircleShape),
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface)
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

private object PreviewSettingsActions : SettingsActions {
    override fun onFont(font: FontChoice) = Unit
    override fun onClockStyle(style: ClockStyle) = Unit
    override fun onIconStyle(style: IconStyle) = Unit
    override fun onChooseHomeApp() = Unit
    override fun onUnhide(app: AppEntry) = Unit
    override fun onMoveFavorite(app: AppEntry, offset: Int) = Unit
    override fun onTextSize(size: TextSize) = Unit
    override fun onLeftHanded(enabled: Boolean) = Unit
    override fun onFullList(enabled: Boolean) = Unit
    override fun onPreset(preset: ThemePreset) = Unit
    override fun onWidgetHeight(appWidgetId: Int, heightDp: Int) = Unit
    override fun onRemoveWidget(appWidgetId: Int) = Unit
    override fun onMoveWidget(appWidgetId: Int, offset: Int) = Unit
    override fun onCustom(change: (Customization) -> Customization) = Unit
}

private object PreviewFeatureActions : FeatureActions {
    override fun onPickPhoto() = Unit
    override fun onRemovePhoto() = Unit
    override fun onExportTheme() = Unit
    override fun onImportTheme() = Unit
    override fun onSystemWallpaper() = Unit
    override fun onNotifications(enabled: Boolean) = Unit
    override fun onNotificationAccess() = Unit
    override fun onDoubleTapLock(enabled: Boolean) = Unit
    override fun onLockAccess() = Unit
    override fun onUsageTime(enabled: Boolean) = Unit
    override fun onUsageAccess() = Unit
    override fun onContactsSearch(enabled: Boolean) = Unit
}

@Preview(name = "Claro", heightDp = 1200)
@Preview(name = "Oscuro", heightDp = 1200, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun SettingsScreenPreview() {
    VeloraTheme(reducedMotion = true) {
        SettingsScreen(
            theme = ThemeSettings(clock = ClockStyle.Day),
            preview = ClockPreview(ClockText("7:05", "Jueves, 23 de octubre", "JUE"), battery = 82),
            lists = SettingsLists(),
            actions = PreviewSettingsActions,
        )
    }
}
