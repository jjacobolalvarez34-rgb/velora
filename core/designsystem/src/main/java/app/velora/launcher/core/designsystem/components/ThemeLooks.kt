package app.velora.launcher.core.designsystem.components

import androidx.compose.ui.draw.CacheDrawScope
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import app.velora.launcher.core.designsystem.theme.ArtColors
import app.velora.launcher.core.model.ThemePreset

/**
 * Cómo se dibuja el fondo de un tema, en capas: el degradado [top]→[bottom] mientras se genera el
 * [art] procedural (recibe alto/ancho), encima los detalles vectoriales nítidos ([overlay]) y al
 * final el grano de película con opacidad [grain] (0 = sin grano).
 */
internal class ThemeLook(
    val top: Color,
    val bottom: Color,
    val art: ((aspect: Float) -> PixelArt)? = null,
    val overlay: (CacheDrawScope.() -> DrawScope.() -> Unit)? = null,
    val grain: Float = 0f,
)

/** Qué dibuja cada tema; "Tu fondo" no está: deja ver el wallpaper. */
internal val ThemeLooks: Map<ThemePreset, ThemeLook> = mapOf(
    ThemePreset.Night to ThemeLook(ArtColors.nightBase, ArtColors.nightBase, { nightArt(it) }, grain = 0.05f),
    ThemePreset.Paper to ThemeLook(ArtColors.paperBase, ArtColors.paperWarm, { paperArt() }, grain = 0.05f),
    ThemePreset.Sakura to ThemeLook(
        ArtColors.sakuraSkyTop,
        ArtColors.sakuraSkyBottom,
        { sakuraArt() },
        { sakuraOverlay() },
        0.04f,
    ),
    ThemePreset.Forest to ThemeLook(ArtColors.forestTop, ArtColors.forestRidge4, { forestArt() }, grain = 0.06f),
    ThemePreset.Aurora to ThemeLook(
        ArtColors.auroraTop,
        ArtColors.auroraBottom,
        { auroraArt() },
        { auroraStars() },
        0.05f,
    ),
    ThemePreset.PixelNight to ThemeLook(ArtColors.pixelTop, ArtColors.pixelBottom, overlay = { pixelNight() }),
    ThemePreset.Ash to ThemeLook(ArtColors.ashTop, ArtColors.ashBottom, overlay = { ash() }, grain = 0.06f),
    ThemePreset.Sunset to ThemeLook(ArtColors.sunsetTop, ArtColors.sunsetLow, { sunsetArt(it) }, grain = 0.06f),
    ThemePreset.Ocean to ThemeLook(ArtColors.oceanDeep, ArtColors.oceanMid, { oceanArt() }, grain = 0.04f),
    ThemePreset.Snow to ThemeLook(ArtColors.snowSky, ArtColors.snowBase, { snowArt() }, grain = 0.035f),
    ThemePreset.Coffee to ThemeLook(ArtColors.coffeeTop, ArtColors.coffeeBottom, { coffeeArt(it) }, grain = 0.06f),
    ThemePreset.Galaxy to ThemeLook(
        ArtColors.galaxyBase,
        ArtColors.galaxyDeep,
        { galaxyArt() },
        { galaxyOverlay() },
        0.04f,
    ),
    ThemePreset.Neon to ThemeLook(ArtColors.neonTop, ArtColors.neonBottom, { neonArt() }, { neonOverlay() }, 0.04f),
    ThemePreset.Matcha to ThemeLook(ArtColors.matchaBase, ArtColors.matchaLeaf, { matchaArt() }, grain = 0.045f),
    ThemePreset.Lavender to ThemeLook(
        ArtColors.lavenderBase,
        ArtColors.lavenderBlue,
        { lavenderArt() },
        grain = 0.045f,
    ),
    ThemePreset.Marble to ThemeLook(ArtColors.marbleBase, ArtColors.marbleCloud, { marbleArt() }, grain = 0.035f),
    ThemePreset.BlackMarble to ThemeLook(
        ArtColors.blackMarbleBase,
        ArtColors.blackMarbleCloud,
        { blackMarbleArt() },
        grain = 0.05f,
    ),
    ThemePreset.Graphite to ThemeLook(
        ArtColors.graphiteTop,
        ArtColors.graphiteBottom,
        { graphiteArt(it) },
        grain = 0.08f,
    ),
)
