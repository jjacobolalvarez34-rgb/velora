package app.velora.launcher.feature.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
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

// Secciones Tema, Reloj y Letra de Personalizar (ADR-028).

/** Temas listos: un toque cambia fondo, colores, letra, reloj e íconos (en el borrador). */
@Composable
internal fun ThemeTab(look: ThemeSettings, edit: ThemeEdit) {
    PanelList {
        sectionHint(R.string.personalize_hint_theme)
        groupTitle("presets", R.string.personalize_group_presets)
        optionGrid("presets", ThemePreset.entries, columns = PRESET_COLUMNS) { preset ->
            OptionCard(
                label = stringResource(presetName(preset)),
                selected = look.preset == preset,
                onClick = { edit { it.withPreset(preset) } },
                aspect = PRESET_ASPECT,
            ) {
                PresetThumb(preset)
            }
        }
    }
}

/** Cada reloj dibujado de verdad sobre el fondo elegido, en miniatura y del mismo tamaño. */
@Composable
internal fun ClockTab(state: PersonalizeState, preview: ClockPreview, edit: ThemeEdit) {
    val look = state.look
    PanelList {
        sectionHint(R.string.personalize_hint_clock)
        groupTitle("clocks", R.string.personalize_group_clock)
        optionGrid("clocks", ClockStyle.entries, columns = CLOCK_COLUMNS) { style ->
            OptionCard(
                label = stringResource(clockLabels(style).first),
                selected = look.clock == style,
                onClick = { edit { it.copy(clock = style) } },
                aspect = CLOCK_ASPECT,
                thumbColor = Color.Transparent,
            ) {
                ClockThumb(state, preview, style)
            }
        }
    }
}

@Composable
private fun ClockThumb(state: PersonalizeState, preview: ClockPreview, style: ClockStyle) {
    val look = state.look
    VeloraTheme(font = look.font, preset = look.preset, reducedMotion = true, accent = look.userAccent()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.CenterStart) {
            ThemeBackground(look.preset, Modifier.fillMaxSize(), state.photo)
            // El reloj se maqueta a tamaño de teléfono y se achica: se ve igual que en el inicio.
            Box(Modifier.fitInside(ClockCardWidth, ClockCardHeight), contentAlignment = Alignment.CenterStart) {
                ClockHeader(preview.clock, style, preview.battery, Modifier.padding(Spacing.l))
            }
        }
    }
}

/** Tamaño del texto arriba y cada letra escrita con ella misma. */
@Composable
internal fun FontTab(look: ThemeSettings, edit: ThemeEdit) {
    PanelList {
        sectionHint(R.string.personalize_hint_font)
        groupTitle("size", R.string.personalize_group_text_size)
        item(key = "size-row") { TextSizeRow(look.textSize) { size -> edit { it.copy(textSize = size) } } }
        groupTitle("fonts", R.string.personalize_group_font)
        optionGrid("fonts", FontChoice.entries, columns = FONT_COLUMNS) { font ->
            OptionCard(
                label = fontName(font),
                selected = look.font == font,
                onClick = { edit { it.copy(font = font) } },
                aspect = FONT_ASPECT,
            ) {
                Text(
                    FONT_SAMPLE,
                    style = MaterialTheme.typography.headlineMedium
                        .copy(fontFamily = fontFamilyOf(font), fontSize = 28.sp),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Clip,
                )
            }
        }
    }
}

@Composable
private fun TextSizeRow(selected: TextSize, onSelect: (TextSize) -> Unit) {
    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth().padding(horizontal = Spacing.m)) {
        TextSize.entries.forEachIndexed { index, size ->
            SegmentedButton(
                selected = selected == size,
                onClick = { onSelect(size) },
                shape = SegmentedButtonDefaults.itemShape(index, TextSize.entries.size),
            ) { Text(stringResource(textSizeLabel(size)), maxLines = 1) }
        }
    }
}

/** Muestra de cada letra: letras y números, para ver el carácter de la fuente. */
private const val FONT_SAMPLE = "Aa 9:41"
private const val PRESET_COLUMNS = 3
private const val CLOCK_COLUMNS = 2
private const val FONT_COLUMNS = 2

/** Miniatura de tema con forma de pantalla de teléfono. */
private const val PRESET_ASPECT = 0.56f
private const val FONT_ASPECT = 2f
private val ClockCardWidth = 360.dp
private val ClockCardHeight = 280.dp

/** Las miniaturas de reloj tienen la misma proporción que el reloj maquetado. */
private const val CLOCK_ASPECT = 360f / 280f
