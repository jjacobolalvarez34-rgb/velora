package app.velora.launcher.feature.settings

import androidx.annotation.StringRes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.velora.launcher.R
import app.velora.launcher.core.designsystem.components.ThemeBackground
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

/** Un cambio a las opciones de íconos del borrador. */
private typealias IconChange = ((IconOptions) -> IconOptions) -> Unit

/**
 * Sección Íconos (ADR-028): primero el tipo (originales, del sistema, de Velora, un pack instalado
 * o sin íconos); según el tipo, su diseño, forma, color y grosor. Cada opción se ve con tus apps
 * sobre el fondo del tema.
 */
@Composable
internal fun IconsTab(
    look: ThemeSettings,
    favorites: List<AppEntry>,
    iconPacks: List<InstalledIconPack>,
    edit: ThemeEdit,
) {
    val options = look.custom.icons
    val samples = favorites.take(SAMPLE_ICONS)
    val change: IconChange = { block -> edit { it.copy(custom = it.custom.copy(icons = block(it.custom.icons))) } }
    // Las muestras se dibujan con los colores del tema elegido, como en el inicio.
    VeloraTheme(font = look.font, preset = look.preset, reducedMotion = true, accent = look.userAccent()) {
        PanelList {
            sectionHint(R.string.personalize_hint_icons)
            groupTitle("type", R.string.personalize_icons_type)
            optionGrid("type", IconStyle.entries, columns = ICON_COLUMNS) { style ->
                OptionCard(stringResource(iconLabels(style).first), look.icons == style, {
                    edit { it.copy(icons = style) }
                }) {
                    IconSamples(look, samples, style, options)
                }
            }
            if (look.icons == IconStyle.Minimal) designGrid(look, options, samples, change)
            if (look.icons == IconStyle.Circle || look.icons == IconStyle.Minimal) shapeGrid(options, change)
            if (look.icons == IconStyle.Minimal) finishOptions(look, options, samples, change)
            if (look.icons == IconStyle.Pack) packList(options, iconPacks, change)
        }
    }
}

/** Los diseños de Velora, lo primero que se cambia de los íconos propios. */
private fun LazyListScope.designGrid(
    look: ThemeSettings,
    options: IconOptions,
    samples: List<AppEntry>,
    change: IconChange,
) {
    groupTitle("pack", R.string.personalize_icons_design)
    optionGrid("pack", IconPack.entries, columns = ICON_COLUMNS) { pack ->
        OptionCard(stringResource(packLabel(pack)), options.pack == pack, { change { it.copy(pack = pack) } }) {
            IconSamples(look, samples, IconStyle.Minimal, options.copy(pack = pack))
        }
    }
}

/** Color, grosor y relleno: los detalles finos, al final. */
private fun LazyListScope.finishOptions(
    look: ThemeSettings,
    options: IconOptions,
    samples: List<AppEntry>,
    change: IconChange,
) {
    groupTitle("color", R.string.personalize_icons_color)
    optionGrid("color", IconColor.entries, columns = ICON_COLUMNS) { color ->
        OptionCard(stringResource(colorLabel(color)), options.color == color, { change { it.copy(color = color) } }) {
            IconSamples(look, samples, IconStyle.Minimal, options.copy(color = color))
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

private fun LazyListScope.shapeGrid(options: IconOptions, change: IconChange) {
    groupTitle("shape", R.string.personalize_icons_shape)
    optionGrid("shape", IconShape.entries, columns = SHAPE_COLUMNS) { shape ->
        OptionCard(
            stringResource(shapeLabel(shape)),
            options.shape == shape,
            { change { it.copy(shape = shape) } },
            aspect = 1f,
        ) {
            ShapeGlyph(shape)
        }
    }
}

private fun LazyListScope.packList(options: IconOptions, packs: List<InstalledIconPack>, change: IconChange) {
    groupTitle("packs", R.string.personalize_icons_packs)
    if (packs.isEmpty()) sectionHint(R.string.personalize_icons_packs_empty)
    items(packs, key = { "pack-${it.packageName}" }) { pack ->
        RadioOption(
            title = pack.label,
            selected = options.thirdPartyPack == pack.packageName,
            onClick = { change { it.copy(thirdPartyPack = pack.packageName) } },
        )
    }
}

/** Tres de tus apps dibujadas con ese estilo sobre el fondo del tema; sin favoritos, círculos. */
@Composable
private fun IconSamples(look: ThemeSettings, samples: List<AppEntry>, style: IconStyle, options: IconOptions) {
    CompositionLocalProvider(LocalIconStyle provides style, LocalIconOptions provides options) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            ThemeBackground(look.preset, Modifier.fillMaxSize())
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                if (samples.isEmpty()) {
                    repeat(SAMPLE_ICONS) {
                        Box(Modifier.size(SampleSize).background(VeloraTheme.onWallpaper.iconCircle, CircleShape))
                    }
                } else if (style == IconStyle.None) {
                    Text(NO_ICON_SAMPLE, style = VeloraTheme.typography.appRow, color = VeloraTheme.onWallpaper.content)
                } else {
                    samples.forEach { AppIcon(key = it.key, size = SampleSize) }
                }
            }
        }
    }
}

/** La silueta de cada forma, para elegir sin adivinar. */
@Composable
private fun ShapeGlyph(shape: IconShape) {
    val color = MaterialTheme.colorScheme.primary
    Canvas(Modifier.size(ShapeSize)) {
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
    Column {
        GroupTitle(title)
        Slider(
            value = value,
            onValueChange = onChange,
            valueRange = range,
            steps = WEIGHT_STEPS,
            modifier = Modifier.padding(horizontal = Spacing.m),
        )
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
    IconPack.Neon -> R.string.icon_pack_neon
    IconPack.Duotone -> R.string.icon_pack_duotone
    IconPack.Pixel -> R.string.icon_pack_pixel
    IconPack.Gradient -> R.string.icon_pack_gradient
    IconPack.Outline -> R.string.icon_pack_outline
    IconPack.Watercolor -> R.string.icon_pack_watercolor
    IconPack.Paper -> R.string.icon_pack_paper
    IconPack.Clay -> R.string.icon_pack_clay
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
private const val ICON_COLUMNS = 3
private const val SHAPE_COLUMNS = 4

/** Con "Sin íconos" la muestra es solo texto. */
private const val NO_ICON_SAMPLE = "Aa"
private val SampleSize = 26.dp
private val ShapeSize = 34.dp
