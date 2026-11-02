package app.velora.launcher.feature.settings

import android.content.res.Configuration
import androidx.activity.BackEventCompat
import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import app.velora.launcher.R
import app.velora.launcher.core.designsystem.motion.VeloraMotion
import app.velora.launcher.core.designsystem.theme.VeloraTheme
import app.velora.launcher.core.model.AppEntry
import app.velora.launcher.core.model.ClockStyle
import app.velora.launcher.core.model.Customization
import app.velora.launcher.core.model.FontChoice
import app.velora.launcher.core.model.IconStyle
import app.velora.launcher.core.model.TextSize
import app.velora.launcher.core.model.ThemePreset
import app.velora.launcher.core.model.ThemeSettings
import app.velora.launcher.feature.hidden.HiddenLockActions
import app.velora.launcher.feature.hidden.HiddenUiState
import app.velora.launcher.feature.hidden.NoHiddenLockActions
import app.velora.launcher.feature.hidden.hiddenLockSection
import app.velora.launcher.feature.home.ClockText

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
 * categorías agrupadas. Cada página entra deslizándose y "atrás" vuelve al menú. [theme] es lo
 * guardado; Personalizar muestra su borrador ([personalize]).
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
    personalize: PersonalizeState = PersonalizeState(theme, photo = photo),
    personalizeActions: PersonalizeActions = PreviewPersonalizeActions,
    hiddenLock: HiddenUiState = HiddenUiState(loaded = true),
    hiddenLockActions: HiddenLockActions = NoHiddenLockActions,
    backup: BackupUiState = BackupUiState(),
    backupActions: BackupActions = NoBackupActions,
) {
    var page by rememberSaveable { mutableStateOf(SettingsPage.Main) }
    val backProgress = predictiveBackToMenu(enabled = page != SettingsPage.Main) { page = SettingsPage.Main }
    // Al volver al menú se suelta el borrador de Personalizar: lo que no se guardó, no queda.
    LaunchedEffect(page) { if (page == SettingsPage.Main) personalizeActions.onDiscard() }
    val motion = VeloraTheme.motion
    AnimatedContent(
        targetState = page,
        transitionSpec = { pageTransition(targetState, motion) },
        label = "settings-page",
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .backPeek(backProgress),
    ) { current ->
        val back = { page = SettingsPage.Main }
        when (current) {
            SettingsPage.Main -> MainPage(theme, preview, photo, lists.favorites, actions, features) { page = it }

            SettingsPage.Personalize ->
                PersonalizeScreen(personalize, preview, lists.favorites, iconPacks, personalizeActions, back)

            SettingsPage.Layout -> SettingsListPage(R.string.settings_page_layout, back) {
                layoutSection(theme, actions)
            }

            SettingsPage.Lists -> SettingsListPage(R.string.settings_page_lists, back) {
                favoritesSection(lists.favorites, actions)
                widgetsSection(lists.widgets, actions)
                hiddenLockSection(hiddenLock, hiddenLockActions)
                if (hiddenLock.contentVisible) hiddenSection(lists.hidden, actions)
                backupSection(backup, backupActions)
            }

            SettingsPage.Features -> SettingsListPage(R.string.settings_page_features, back) {
                featuresSection(theme, status, features)
                deviceHelpSection(features::onOpenBattery)
            }
        }
    }
}

/**
 * Atrás predictivo (Android 13+, ADR-036): deslizar desde el borde va achicando y corriendo la
 * página con el dedo; al soltar vuelve al menú y, si se cancela, la página regresa a su sitio.
 * Devuelve el avance del gesto (0..1) y de qué lado viene (+1 izquierda, −1 derecha).
 */
@Composable
private fun predictiveBackToMenu(enabled: Boolean, onBack: () -> Unit): State<Offset> {
    val progress = remember { mutableStateOf(Offset.Zero) }
    PredictiveBackHandler(enabled = enabled) { events ->
        try {
            events.collect { event ->
                val side = if (event.swipeEdge == BackEventCompat.EDGE_LEFT) 1f else -1f
                progress.value = Offset(event.progress, side)
            }
            onBack()
        } finally {
            progress.value = Offset.Zero
        }
    }
    return progress
}

/** La página sigue al dedo: se achica hasta un 10 % y se corre hacia el lado del gesto. */
private fun Modifier.backPeek(progress: State<Offset>) = graphicsLayer {
    val (p, side) = progress.value
    val scale = 1f - p * BACK_SCALE
    scaleX = scale
    scaleY = scale
    translationX = side * p * BACK_SHIFT.toPx()
    clip = p > 0f
    shape = RoundedCornerShape(BACK_CORNER * p)
}

private const val BACK_SCALE = 0.1f
private val BACK_SHIFT = 32.dp
private val BACK_CORNER = 32.dp

/** Hacia una página: entra desde la derecha; de vuelta al menú, al revés. */
private fun pageTransition(to: SettingsPage, motion: VeloraMotion): ContentTransform {
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

/** El menú: título, la tarjeta de Personalizar y tres grupos (inicio, compartir, general). */
@Composable
private fun MainPage(
    theme: ThemeSettings,
    preview: ClockPreview,
    photo: ImageBitmap?,
    favorites: List<AppEntry>,
    actions: SettingsActions,
    features: FeatureActions,
    onOpen: (SettingsPage) -> Unit,
) {
    LazyColumn(contentPadding = WindowInsets.safeDrawing.asPaddingValues(), modifier = Modifier.fillMaxSize()) {
        item(key = "title") { Title(stringResource(R.string.settings_title)) }
        item(key = "personalize") {
            PersonalizeHero(theme, preview, photo, favorites, onClick = { onOpen(SettingsPage.Personalize) })
        }
        item(key = "home-group") { MenuGroup(R.string.settings_more, homeEntries(onOpen)) }
        item(key = "share-group") { MenuGroup(R.string.settings_share_themes, shareEntries(features)) }
        item(key = "general-group") { MenuGroup(R.string.settings_general, generalEntries(actions)) }
    }
}

@Composable
private fun homeEntries(onOpen: (SettingsPage) -> Unit) = listOf(
    MenuEntry(
        stringResource(R.string.settings_page_layout),
        stringResource(R.string.settings_page_layout_summary),
        Icons.Outlined.Menu,
    ) { onOpen(SettingsPage.Layout) },
    MenuEntry(
        stringResource(R.string.settings_page_lists),
        stringResource(R.string.settings_page_lists_summary),
        Icons.Outlined.Star,
    ) { onOpen(SettingsPage.Lists) },
    MenuEntry(
        stringResource(R.string.settings_page_features),
        stringResource(R.string.settings_page_features_summary),
        Icons.Outlined.Notifications,
    ) { onOpen(SettingsPage.Features) },
)

/** Compartir temas de Velora con otros usuarios de Velora (ADR-027). */
@Composable
private fun shareEntries(features: FeatureActions) = listOf(
    MenuEntry(
        stringResource(R.string.settings_theme_export),
        stringResource(R.string.settings_theme_export_summary),
        Icons.Outlined.Share,
        features::onExportTheme,
    ),
    MenuEntry(
        stringResource(R.string.settings_theme_import),
        stringResource(R.string.settings_theme_import_summary),
        Icons.Outlined.Add,
        features::onImportTheme,
    ),
)

@Composable
private fun generalEntries(actions: SettingsActions) = listOf(
    MenuEntry(
        stringResource(R.string.settings_home_app_title),
        stringResource(R.string.settings_home_app_summary),
        Icons.Outlined.Home,
        actions::onChooseHomeApp,
    ),
)

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
    override fun onNotifications(enabled: Boolean) = Unit
    override fun onNotificationAccess() = Unit
    override fun onDoubleTapLock(enabled: Boolean) = Unit
    override fun onLockAccess() = Unit
    override fun onUsageTime(enabled: Boolean) = Unit
    override fun onUsageAccess() = Unit
    override fun onContactsSearch(enabled: Boolean) = Unit
    override fun onOpenBattery() = Unit
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
