package app.velora.launcher.feature.settings

import android.content.res.Configuration
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import app.velora.launcher.R
import app.velora.launcher.core.designsystem.components.ThemeBackground
import app.velora.launcher.core.designsystem.theme.Shapes
import app.velora.launcher.core.designsystem.theme.Spacing
import app.velora.launcher.core.designsystem.theme.VeloraTheme
import app.velora.launcher.core.designsystem.theme.fontFamilyOf
import app.velora.launcher.core.model.AppEntry
import app.velora.launcher.core.model.ClockStyle
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
}

/** Listas de apps que se editan en Ajustes. */
@Immutable
data class SettingsLists(val favorites: List<AppEntry> = emptyList(), val hidden: List<AppEntry> = emptyList())

@Composable
fun SettingsScreen(
    theme: ThemeSettings,
    preview: ClockPreview,
    lists: SettingsLists,
    actions: SettingsActions,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        contentPadding = WindowInsets.safeDrawing.asPaddingValues(),
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        item(key = "title") { Title(stringResource(R.string.settings_title)) }
        item(key = "preview") { ThemePreview(theme, preview) }
        item(key = "presets-title") { SectionTitle(R.string.settings_themes) }
        item(key = "presets") { ThemePresetPicker(selected = theme.preset, onSelect = actions::onPreset) }
        themeSection(theme, actions)
        layoutSection(theme, actions)
        favoritesSection(lists.favorites, actions)
        hiddenSection(lists.hidden, actions)
        item(key = "general") { SectionTitle(R.string.settings_general) }
        item(key = "home-app") {
            Option(
                title = stringResource(R.string.settings_home_app_title),
                subtitle = stringResource(R.string.settings_home_app_summary),
                onClick = actions::onChooseHomeApp,
            )
        }
    }
}

private fun LazyListScope.themeSection(theme: ThemeSettings, actions: SettingsActions) {
    item(key = "font") { SectionTitle(R.string.settings_font) }
    items(FontChoice.entries.size, key = { "font-${FontChoice.entries[it]}" }) { index ->
        val font = FontChoice.entries[index]
        RadioOption(
            title = fontName(font),
            selected = theme.font == font,
            onClick = { actions.onFont(font) },
            titleStyle = MaterialTheme.typography.titleMedium.copy(fontFamily = fontFamilyOf(font)),
        )
    }
    item(key = "clock") { SectionTitle(R.string.settings_clock) }
    items(ClockStyle.entries.size, key = { "clock-${ClockStyle.entries[it]}" }) { index ->
        val style = ClockStyle.entries[index]
        val (title, subtitle) = clockLabels(style)
        RadioOption(stringResource(title), theme.clock == style, {
            actions.onClockStyle(style)
        }, stringResource(subtitle))
    }
    item(key = "icons") { SectionTitle(R.string.settings_icons) }
    items(IconStyle.entries.size, key = { "icons-${IconStyle.entries[it]}" }) { index ->
        val style = IconStyle.entries[index]
        val (title, subtitle) = iconLabels(style)
        RadioOption(stringResource(title), theme.icons == style, {
            actions.onIconStyle(style)
        }, stringResource(subtitle))
    }
}

private fun LazyListScope.hiddenSection(hidden: List<AppEntry>, actions: SettingsActions) {
    item(key = "hidden") { SectionTitle(R.string.settings_hidden) }
    if (hidden.isEmpty()) {
        item(key = "hidden-empty") {
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_hidden_empty)) },
                colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.background),
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
            colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.background),
        )
    }
}

/** El reloj tal como se verá en el inicio, sobre un degradado del color del tema. */
@Composable
private fun ThemePreview(theme: ThemeSettings, preview: ClockPreview) {
    val scheme = MaterialTheme.colorScheme
    Box(
        modifier = Modifier
            .padding(horizontal = Spacing.m, vertical = Spacing.s)
            .fillMaxWidth()
            .clip(Shapes.sheet)
            .background(Brush.verticalGradient(listOf(scheme.primary, scheme.tertiary))),
    ) {
        // Mismo fondo y colores que el inicio con el tema elegido.
        VeloraTheme(font = theme.font, preset = theme.preset, reducedMotion = true) {
            ThemeBackground(theme.preset, Modifier.matchParentSize())
            ClockHeader(
                clock = preview.clock,
                style = theme.clock,
                battery = preview.battery,
                modifier = Modifier.padding(Spacing.l),
            )
        }
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
        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.background),
        modifier = Modifier.selectable(selected = selected, role = Role.RadioButton, onClick = onClick),
    )
}

@Composable
private fun Option(title: String, subtitle: String, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = { Text(subtitle) },
        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.background),
        modifier = Modifier.clickable(onClick = onClick),
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
