package app.velora.launcher.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.velora.launcher.R
import app.velora.launcher.core.designsystem.components.ThemeBackground
import app.velora.launcher.core.designsystem.theme.Spacing
import app.velora.launcher.core.designsystem.theme.VeloraTheme
import app.velora.launcher.core.designsystem.theme.fontFamilyOf
import app.velora.launcher.core.model.ClockStyle
import app.velora.launcher.core.model.FontChoice
import app.velora.launcher.core.model.TextSize
import app.velora.launcher.core.model.ThemePreset
import app.velora.launcher.core.model.ThemeSettings
import app.velora.launcher.feature.home.components.ClockHeader

// Pestañas Tema, Fondo, Reloj y Letra de Personalizar (ADR-028).

private val GridPadding = PaddingValues(horizontal = Spacing.m, vertical = Spacing.m)

/** Temas listos: un toque aplica todo (fondo, colores, letra, reloj, íconos). */
@Composable
internal fun ThemeTab(theme: ThemeSettings, actions: SettingsActions) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(PresetCellWidth),
        contentPadding = GridPadding,
        horizontalArrangement = Arrangement.spacedBy(Spacing.s),
        verticalArrangement = Arrangement.spacedBy(Spacing.m),
        modifier = Modifier.fillMaxSize().tabContentPadding(),
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) { TabHint(R.string.personalize_theme_hint) }
        items(ThemePreset.entries, key = { it.name }) { preset ->
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                PresetCard(preset, selected = preset == theme.preset, onClick = { actions.onPreset(preset) })
                Text(
                    stringResource(presetName(preset)),
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(top = Spacing.xs),
                )
            }
        }
    }
}

/** Foto propia encima del tema o el fondo del teléfono. */
@Composable
internal fun BackgroundTab(theme: ThemeSettings, features: FeatureActions) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(vertical = Spacing.m).tabContentPadding(),
    ) {
        TabHint(R.string.personalize_background_hint)
        Option(
            title = stringResource(
                if (theme.photoBackground ==
                    null
                ) {
                    R.string.settings_photo_pick
                } else {
                    R.string.settings_photo_change
                },
            ),
            subtitle = stringResource(R.string.settings_photo_summary),
            onClick = features::onPickPhoto,
            icon = Icons.Outlined.Edit,
        )
        if (theme.photoBackground != null) {
            Option(
                title = stringResource(R.string.settings_photo_remove),
                subtitle = stringResource(R.string.settings_photo_remove_summary),
                onClick = features::onRemovePhoto,
                icon = Icons.Outlined.Delete,
            )
        }
        Option(
            title = stringResource(R.string.settings_system_wallpaper),
            subtitle = stringResource(R.string.settings_system_wallpaper_summary),
            onClick = features::onSystemWallpaper,
            icon = Icons.Outlined.Settings,
        )
    }
}

/** Cada reloj dibujado de verdad sobre el fondo del tema, en miniatura. */
@Composable
internal fun ClockTab(theme: ThemeSettings, preview: ClockPreview, actions: SettingsActions) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = GridPadding,
        horizontalArrangement = Arrangement.spacedBy(Spacing.s),
        verticalArrangement = Arrangement.spacedBy(Spacing.s),
        modifier = Modifier.fillMaxSize().tabContentPadding(),
    ) {
        items(ClockStyle.entries, key = { it.name }) { style ->
            OptionCard(
                label = stringResource(clockLabels(style).first),
                selected = theme.clock == style,
                onClick = { actions.onClockStyle(style) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                ScaledBox(ClockCardWidth, ClockCardHeight, CLOCK_SCALE) {
                    VeloraTheme(
                        font = theme.font,
                        preset = theme.preset,
                        reducedMotion = true,
                        accent = theme.userAccent(),
                    ) {
                        Box(Modifier.fillMaxSize()) {
                            ThemeBackground(theme.preset, Modifier.fillMaxSize())
                            ClockHeader(preview.clock, style, preview.battery, Modifier.padding(Spacing.l))
                        }
                    }
                }
            }
        }
    }
}

/** Tamaño del texto arriba y cada letra escrita con ella misma. */
@Composable
internal fun FontTab(theme: ThemeSettings, actions: SettingsActions) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = GridPadding,
        horizontalArrangement = Arrangement.spacedBy(Spacing.s),
        verticalArrangement = Arrangement.spacedBy(Spacing.s),
        modifier = Modifier.fillMaxSize().tabContentPadding(),
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) { TextSizeRow(theme.textSize, actions::onTextSize) }
        items(FontChoice.entries, key = { it.name }) { font ->
            OptionCard(
                label = fontName(font),
                selected = theme.font == font,
                onClick = { actions.onFont(font) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    "Aa 9:41",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontFamily = fontFamilyOf(font),
                        fontSize = 30.sp,
                    ),
                    modifier = Modifier.padding(vertical = Spacing.s),
                )
            }
        }
    }
}

@Composable
private fun TextSizeRow(selected: TextSize, onSelect: (TextSize) -> Unit) {
    Column(Modifier.padding(bottom = Spacing.s)) {
        Text(stringResource(R.string.settings_text_size), style = MaterialTheme.typography.titleSmall)
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth().padding(top = Spacing.xs)) {
            TextSize.entries.forEachIndexed { index, size ->
                SegmentedButton(
                    selected = selected == size,
                    onClick = { onSelect(size) },
                    shape = SegmentedButtonDefaults.itemShape(index, TextSize.entries.size),
                ) { Text(stringResource(textSizeLabel(size))) }
            }
        }
    }
}

/** Una frase corta al principio de cada pestaña: qué hace y cómo se usa. */
@Composable
internal fun TabHint(text: Int) {
    Text(
        stringResource(text),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = Spacing.m, vertical = Spacing.xs),
    )
}

private val PresetCellWidth = 104.dp
private val ClockCardWidth = 360.dp
private val ClockCardHeight = 240.dp
private const val CLOCK_SCALE = 0.42f
