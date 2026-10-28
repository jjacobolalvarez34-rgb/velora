package app.velora.launcher.feature.settings

import androidx.annotation.StringRes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.velora.launcher.R
import app.velora.launcher.core.designsystem.theme.Spacing
import app.velora.launcher.core.designsystem.theme.VeloraTheme
import app.velora.launcher.core.model.AppEntry
import app.velora.launcher.core.model.IconColor
import app.velora.launcher.core.model.IconOptions
import app.velora.launcher.core.model.IconPack
import app.velora.launcher.core.model.IconShape
import app.velora.launcher.core.model.IconStyle
import app.velora.launcher.core.model.ThemeSettings
import app.velora.launcher.feature.apps.AppIcon
import app.velora.launcher.feature.apps.LocalIconOptions
import app.velora.launcher.feature.apps.LocalIconStyle

/** Un pack de íconos instalado desde la Play Store (formato común de launchers). */
@Immutable
data class InstalledIconPack(val packageName: String, val label: String)

/**
 * Pestaña Íconos (ADR-028): primero el tipo (originales, del sistema, de Velora, un pack instalado
 * o sin íconos); según el tipo, su diseño, forma, color y grosor. Cada opción se ve con tus apps.
 */
@Composable
internal fun IconsTab(
    theme: ThemeSettings,
    favorites: List<AppEntry>,
    actions: SettingsActions,
    iconPacks: List<InstalledIconPack>,
) {
    val options = theme.custom.icons
    val samples = favorites.take(SAMPLE_ICONS)
    val change = { block: (IconOptions) -> IconOptions -> actions.onCustom { it.copy(icons = block(it.icons)) } }
    // Las muestras se dibujan con los colores del tema elegido, como en el inicio.
    VeloraTheme(font = theme.font, preset = theme.preset, reducedMotion = true, accent = theme.userAccent()) {
        LazyColumn(Modifier.fillMaxSize().tabContentPadding(), contentPadding = PaddingValues(vertical = Spacing.m)) {
            item(key = "hint") { TabHint(R.string.personalize_icons_hint) }
            choiceRow("type", R.string.personalize_icons_type, IconStyle.entries) { style ->
                OptionCard(stringResource(iconLabels(style).first), theme.icons == style, {
                    actions.onIconStyle(style)
                }) {
                    IconSamples(samples, style, options)
                }
            }
            if (theme.icons == IconStyle.Minimal) veloraOptions(options, samples, change)
            if (theme.icons == IconStyle.Circle || theme.icons == IconStyle.Minimal) shapeRow(options, change)
            if (theme.icons == IconStyle.Pack) packList(options, iconPacks, change)
        }
    }
}

private fun LazyListScope.veloraOptions(
    options: IconOptions,
    samples: List<AppEntry>,
    change: ((IconOptions) -> IconOptions) -> Unit,
) {
    choiceRow("pack", R.string.personalize_icons_design, IconPack.entries) { pack ->
        OptionCard(stringResource(packLabel(pack)), options.pack == pack, { change { it.copy(pack = pack) } }) {
            IconSamples(samples, IconStyle.Minimal, options.copy(pack = pack))
        }
    }
    choiceRow("color", R.string.personalize_icons_color, IconColor.entries) { color ->
        OptionCard(stringResource(colorLabel(color)), options.color == color, { change { it.copy(color = color) } }) {
            IconSamples(samples, IconStyle.Minimal, options.copy(color = color))
        }
    }
    if (options.pack != IconPack.Dots) {
        item(key = "weight") {
            LabeledSlider(
                R.string.personalize_icons_weight,
                options.weight,
                IconOptions.MIN_WEIGHT..IconOptions.MAX_WEIGHT,
            ) { w ->
                change { it.copy(weight = w) }
            }
        }
    }
    if (options.pack == IconPack.Line) {
        item(key = "soft-fill") {
            SwitchOption(
                title = stringResource(R.string.personalize_icons_soft_fill),
                subtitle = stringResource(R.string.personalize_icons_soft_fill_summary),
                checked = options.softFill,
                onChange = { on -> change { it.copy(softFill = on) } },
            )
        }
    }
}

private fun LazyListScope.shapeRow(options: IconOptions, change: ((IconOptions) -> IconOptions) -> Unit) {
    choiceRow("shape", R.string.personalize_icons_shape, IconShape.entries) { shape ->
        OptionCard(stringResource(shapeLabel(shape)), options.shape == shape, { change { it.copy(shape = shape) } }) {
            ShapeGlyph(shape)
        }
    }
}

private fun LazyListScope.packList(
    options: IconOptions,
    packs: List<InstalledIconPack>,
    change: ((IconOptions) -> IconOptions) -> Unit,
) {
    item(key = "packs-title") { SectionTitle(R.string.personalize_icons_packs) }
    if (packs.isEmpty()) item(key = "packs-empty") { TabHint(R.string.personalize_icons_packs_empty) }
    items(packs, key = { "pack-${it.packageName}" }) { pack ->
        RadioOption(
            title = pack.label,
            selected = options.thirdPartyPack == pack.packageName,
            onClick = { change { it.copy(thirdPartyPack = pack.packageName) } },
        )
    }
}

/** Título y una fila horizontal de tarjetas: el mismo patrón para cada opción. */
private fun <T> LazyListScope.choiceRow(
    key: String,
    @StringRes title: Int,
    entries: List<T>,
    card: @Composable (T) -> Unit,
) {
    item(key = "$key-title") { SectionTitle(title) }
    item(key = "$key-row") {
        LazyRow(
            contentPadding = PaddingValues(horizontal = Spacing.m),
            horizontalArrangement = Arrangement.spacedBy(Spacing.s),
        ) {
            items(entries.size) { card(entries[it]) }
        }
    }
}

/** Tres de tus apps dibujadas con ese estilo; sin favoritos, círculos de muestra. */
@Composable
private fun IconSamples(samples: List<AppEntry>, style: IconStyle, options: IconOptions) {
    CompositionLocalProvider(LocalIconStyle provides style, LocalIconOptions provides options) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
            modifier = Modifier
                .background(VeloraTheme.onWallpaper.drawerScrim)
                .padding(Spacing.s),
        ) {
            if (samples.isEmpty()) {
                repeat(SAMPLE_ICONS) {
                    Box(Modifier.size(SampleSize).background(VeloraTheme.onWallpaper.iconCircle, CircleShape))
                }
            } else if (style == IconStyle.None) {
                Text("Aa", style = VeloraTheme.typography.appRow, color = VeloraTheme.onWallpaper.content)
            } else {
                samples.forEach { AppIcon(key = it.key, size = SampleSize) }
            }
        }
    }
}

/** La silueta de cada forma, para elegir sin adivinar. */
@Composable
private fun ShapeGlyph(shape: IconShape) {
    val color = MaterialTheme.colorScheme.primary
    Canvas(Modifier.size(SampleSize).padding(2.dp)) {
        drawPath(shapePath(shape, size), color)
    }
}

@Composable
private fun LabeledSlider(
    @StringRes title: Int,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onChange: (Float) -> Unit,
) {
    Column(Modifier.padding(horizontal = Spacing.m, vertical = Spacing.s)) {
        Text(
            stringResource(title),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
        )
        Slider(value = value, onValueChange = onChange, valueRange = range, steps = WEIGHT_STEPS)
    }
}

private fun packLabel(pack: IconPack): Int = when (pack) {
    IconPack.Line -> R.string.icon_pack_line
    IconPack.Material -> R.string.icon_pack_material
    IconPack.Dots -> R.string.icon_pack_dots
    IconPack.Sticker -> R.string.icon_pack_sticker
    IconPack.Bright -> R.string.icon_pack_bright
    IconPack.Glass -> R.string.icon_pack_glass
    IconPack.Stellar -> R.string.icon_pack_stellar
    IconPack.Marble -> R.string.icon_pack_marble
}

private fun shapeLabel(shape: IconShape): Int = when (shape) {
    IconShape.Circle -> R.string.icon_shape_circle
    IconShape.Squircle -> R.string.icon_shape_squircle
    IconShape.Rounded -> R.string.icon_shape_rounded
    IconShape.Teardrop -> R.string.icon_shape_teardrop
}

private fun colorLabel(color: IconColor): Int = when (color) {
    IconColor.Theme -> R.string.icon_color_theme
    IconColor.App -> R.string.icon_color_app
    IconColor.Mono -> R.string.icon_color_mono
}

private const val SAMPLE_ICONS = 3
private const val WEIGHT_STEPS = 6
private val SampleSize = 30.dp
