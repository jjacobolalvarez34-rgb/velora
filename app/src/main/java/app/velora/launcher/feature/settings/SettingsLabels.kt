package app.velora.launcher.feature.settings

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import app.velora.launcher.R
import app.velora.launcher.core.model.ClockStyle
import app.velora.launcher.core.model.FontChoice
import app.velora.launcher.core.model.IconStyle

// Textos de las opciones de Ajustes → Tema: (título, descripción).

@Composable
internal fun fontName(font: FontChoice): String = when (font) {
    FontChoice.PlusJakartaSans -> stringResource(R.string.font_plus_jakarta_sans)
    FontChoice.Figtree -> stringResource(R.string.font_figtree)
    FontChoice.Fraunces -> stringResource(R.string.font_fraunces)
    FontChoice.Quicksand -> stringResource(R.string.font_quicksand)
    FontChoice.Nunito -> stringResource(R.string.font_nunito)
    FontChoice.Pixel -> stringResource(R.string.font_pixel)
    FontChoice.Cinzel -> stringResource(R.string.font_cinzel)
    FontChoice.System -> stringResource(R.string.font_system)
}

internal fun clockLabels(style: ClockStyle): Pair<Int, Int> = when (style) {
    ClockStyle.Classic -> R.string.clock_classic to R.string.clock_classic_summary
    ClockStyle.Centered -> R.string.clock_centered to R.string.clock_centered_summary
    ClockStyle.Day -> R.string.clock_day to R.string.clock_day_summary
    ClockStyle.Stacked -> R.string.clock_stacked to R.string.clock_stacked_summary
    ClockStyle.Banner -> R.string.clock_banner to R.string.clock_banner_summary
    ClockStyle.Duo -> R.string.clock_duo to R.string.clock_duo_summary
}

internal fun iconLabels(style: IconStyle): Pair<Int, Int> = when (style) {
    IconStyle.Circle -> R.string.icons_circle to R.string.icons_circle_summary
    IconStyle.System -> R.string.icons_system to R.string.icons_system_summary
    IconStyle.Minimal -> R.string.icons_minimal to R.string.icons_minimal_summary
    IconStyle.Pack -> R.string.icons_pack to R.string.icons_pack_summary
    IconStyle.None -> R.string.icons_none to R.string.icons_none_summary
}
