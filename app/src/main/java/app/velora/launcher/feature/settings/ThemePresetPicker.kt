package app.velora.launcher.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.velora.launcher.R
import app.velora.launcher.core.designsystem.components.ThemeBackground
import app.velora.launcher.core.designsystem.theme.Spacing
import app.velora.launcher.core.designsystem.theme.VeloraTheme
import app.velora.launcher.core.model.ThemePreset

private val CardWidth = 96.dp
private val CardHeight = 168.dp
private val CardShape = RoundedCornerShape(18.dp)
private val SelectedBorder = 3.dp
private const val MINI_ROWS = 3
private val MiniBarWidth = 44.dp
private val MiniBarStep = 8.dp
private val MiniBarHeight = 6.dp

/** Temas completos: cada tarjeta es una miniatura real del inicio con su fondo, fuente y colores. */
@Composable
fun ThemePresetPicker(selected: ThemePreset, onSelect: (ThemePreset) -> Unit, modifier: Modifier = Modifier) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = Spacing.m),
        horizontalArrangement = Arrangement.spacedBy(Spacing.m),
        modifier = modifier.fillMaxWidth(),
    ) {
        items(ThemePreset.entries, key = { it.name }) { preset ->
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                PresetCard(preset, selected = preset == selected, onClick = { onSelect(preset) })
                Spacer(Modifier.height(Spacing.s))
                Text(
                    text = stringResource(presetName(preset)),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                )
            }
        }
    }
}

@Composable
private fun PresetCard(preset: ThemePreset, selected: Boolean, onClick: () -> Unit) {
    val border = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent
    val scheme = MaterialTheme.colorScheme
    Box(
        modifier = Modifier
            .size(CardWidth, CardHeight)
            .clip(CardShape)
            .background(Brush.verticalGradient(listOf(scheme.primary, scheme.tertiary)))
            .border(SelectedBorder, border, CardShape)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick),
    ) {
        // Tema anidado: la miniatura usa los colores y la fuente de ese tema.
        VeloraTheme(font = preset.recommended.font, preset = preset, reducedMotion = true) {
            ThemeBackground(preset, Modifier.fillMaxSize())
            MiniHome()
        }
    }
}

/** Reloj y tres "apps" de mentira: lo justo para ver cómo se lee el tema. */
@Composable
private fun MiniHome() {
    val colors = VeloraTheme.onWallpaper
    val type = VeloraTheme.typography
    Column(Modifier.padding(Spacing.s)) {
        Text("9:41", style = type.clock.copy(fontSize = 24.sp, lineHeight = 26.sp), color = colors.content)
        Spacer(Modifier.weight(1f))
        repeat(MINI_ROWS) { row ->
            Box(
                Modifier
                    .padding(vertical = 3.dp)
                    .size(width = MiniBarWidth - MiniBarStep * row, height = MiniBarHeight)
                    .background(colors.contentVariant, RoundedCornerShape(3.dp)),
            )
        }
    }
}

internal fun presetName(preset: ThemePreset): Int = when (preset) {
    ThemePreset.Wallpaper -> R.string.theme_wallpaper
    ThemePreset.Night -> R.string.theme_night
    ThemePreset.Paper -> R.string.theme_paper
    ThemePreset.Sakura -> R.string.theme_sakura
    ThemePreset.Forest -> R.string.theme_forest
    ThemePreset.Sunset -> R.string.theme_sunset
    ThemePreset.Ocean -> R.string.theme_ocean
    ThemePreset.Snow -> R.string.theme_snow
    ThemePreset.Coffee -> R.string.theme_coffee
    ThemePreset.Galaxy -> R.string.theme_galaxy
    ThemePreset.Neon -> R.string.theme_neon
    ThemePreset.Matcha -> R.string.theme_matcha
    ThemePreset.Lavender -> R.string.theme_lavender
    ThemePreset.PixelNight -> R.string.theme_pixel_night
    ThemePreset.Ash -> R.string.theme_ash
    ThemePreset.Aurora -> R.string.theme_aurora
}
