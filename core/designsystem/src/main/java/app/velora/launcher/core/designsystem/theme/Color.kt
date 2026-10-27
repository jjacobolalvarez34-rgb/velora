package app.velora.launcher.core.designsystem.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

// Único archivo con colores literales (ESTILO §2).
// Paleta de respaldo "Velora" para Android 8–11: azul-pizarra neutro.

private val Slate10 = Color(0xFF0E1419)
private val Slate30 = Color(0xFF2A3540)
private val Slate40 = Color(0xFF3C4A57)
private val Slate60 = Color(0xFF6F8091)
private val Slate80 = Color(0xFFB5C3D0)
private val Slate90 = Color(0xFFD9E3EC)
private val Slate95 = Color(0xFFEDF2F6)
private val Slate99 = Color(0xFFF9FBFC)

private val Steel40 = Color(0xFF3E6382)
private val Steel80 = Color(0xFFA6C8E6)
private val Steel90 = Color(0xFFCFE4F7)
private val Steel20 = Color(0xFF0D3350)

internal val VeloraLightColors: ColorScheme = lightColorScheme(
    primary = Steel40,
    onPrimary = Slate99,
    primaryContainer = Steel90,
    onPrimaryContainer = Steel20,
    background = Slate99,
    onBackground = Slate10,
    surface = Slate99,
    onSurface = Slate10,
    surfaceVariant = Slate90,
    onSurfaceVariant = Slate40,
    outline = Slate60,
)

internal val VeloraDarkColors: ColorScheme = darkColorScheme(
    primary = Steel80,
    onPrimary = Steel20,
    primaryContainer = Steel40,
    onPrimaryContainer = Steel90,
    background = Slate10,
    onBackground = Slate95,
    surface = Slate10,
    onSurface = Slate95,
    surfaceVariant = Slate30,
    onSurfaceVariant = Slate80,
    outline = Slate60,
)

/**
 * Colores para lo que se dibuja sobre el fondo (el wallpaper del usuario o el de un tema).
 * No dependen del modo claro/oscuro del sistema: dependen del fondo (ADR-018).
 */
@Immutable
data class OnWallpaperColors(
    val content: Color,
    val contentVariant: Color,
    val scrimTop: Color,
    val scrimBottom: Color,
    /** Velo de la lista completa: oscurece todo el wallpaper. */
    val drawerScrim: Color,
    /** Fondo del buscador y de elementos elevados sobre el velo. */
    val surface: Color,
    /** Único acento sobre el fondo (burbuja del alfabeto, resaltados). */
    val accent: Color,
    /** Íconos minimalistas: círculo relleno… */
    val iconCircle: Color = Color(0xF0F2F2F7),
    /** …y el trazo del ícono en línea encima. */
    val iconGlyph: Color = Color(0xFF26232E),
    /** Texto sobre el acento (la letra dentro de la burbuja). */
    val onAccent: Color = Slate10,
    /** Sombra suave del texto: legible sobre cualquier fondo sin un velo pesado. */
    val textShadow: Color = Color.Transparent,
    /** Fondo claro con texto oscuro: los íconos de las barras del sistema van oscuros. */
    val isLight: Boolean = false,
    /** Sombra dura (sin desenfoque, corrida en diagonal): letras pixel nítidas, como en un juego. */
    val crispShadow: Boolean = false,
    /** Brillo de color alrededor del texto (Neón); si es transparente, la sombra normal. */
    val textGlow: Color = Color.Transparent,
)

internal val DefaultOnWallpaperColors = OnWallpaperColors(
    content = Color(0xFFFFFFFF),
    contentVariant = Color(0xE0FFFFFF),
    scrimTop = Color(0x47000000),
    scrimBottom = Color(0x6B000000),
    drawerScrim = Color(0xC7000000),
    surface = Color(0x24FFFFFF),
    // Blanco neutro: sobre cualquier wallpaper, como la burbuja y los íconos en línea de referencia.
    accent = Color(0xFFF4F4F4),
    textShadow = Color(0x8C000000),
)

/** Colores de cada tema (ADR-018): texto, acento y velos sobre su fondo dibujado. */
internal object PresetColors {
    val night = OnWallpaperColors(
        content = Color(0xFFF2F2F2),
        contentVariant = Color(0xB3F2F2F2),
        scrimTop = Color.Transparent,
        scrimBottom = Color.Transparent,
        drawerScrim = Color(0xF2050507),
        surface = Color(0x1FFFFFFF),
        accent = Color(0xFFF2F2F2),
        iconCircle = Color(0xFFE9E9EC),
        iconGlyph = Color(0xFF111114),
    )
    val paper = OnWallpaperColors(
        content = Color(0xFF2B2620),
        contentVariant = Color(0xB32B2620),
        scrimTop = Color.Transparent,
        scrimBottom = Color.Transparent,
        drawerScrim = Color(0xF2F3EEE5),
        surface = Color(0x142B2620),
        accent = Color(0xFF2B2620),
        onAccent = Color(0xFFF3EEE5),
        isLight = true,
        iconCircle = Color(0xFF2B2620),
        iconGlyph = Color(0xFFF3EEE5),
    )
    val sakura = OnWallpaperColors(
        content = Color(0xFF2E2433),
        contentVariant = Color(0xB82E2433),
        scrimTop = Color.Transparent,
        scrimBottom = Color.Transparent,
        drawerScrim = Color(0xEBFFF6F8),
        surface = Color(0x1A2E2433),
        accent = Color(0xFFC75B7A),
        onAccent = Color(0xFFFFFFFF),
        isLight = true,
        iconCircle = Color(0xFFC75B7A),
        iconGlyph = Color(0xFFFFFFFF),
    )
    val forest = OnWallpaperColors(
        content = Color(0xFFEFF5EA),
        contentVariant = Color(0xBFEFF5EA),
        scrimTop = Color.Transparent,
        scrimBottom = Color.Transparent,
        drawerScrim = Color(0xF00E2321),
        surface = Color(0x1FEFF5EA),
        accent = Color(0xFFD7E8B0),
        onAccent = Color(0xFF0E2321),
        textShadow = Color(0x40000000),
        iconCircle = Color(0xFFD7E8B0),
        iconGlyph = Color(0xFF0E2321),
    )
    val aurora = OnWallpaperColors(
        content = Color(0xFFFFFFFF),
        contentVariant = Color(0xC7FFFFFF),
        scrimTop = Color.Transparent,
        scrimBottom = Color.Transparent,
        drawerScrim = Color(0xEB070B1F),
        surface = Color(0x1FFFFFFF),
        accent = Color(0xFFFFFFFF),
        textShadow = Color(0x66000000),
        iconCircle = Color(0xFFE6E9FF),
        iconGlyph = Color(0xFF1B2150),
    )

    /** Atardecer: tinta ciruela sobre durazno; texto oscuro. */
    val sunset = OnWallpaperColors(
        content = Color(0xFF3A1F2B),
        contentVariant = Color(0xB83A1F2B),
        scrimTop = Color.Transparent,
        scrimBottom = Color.Transparent,
        drawerScrim = Color(0xEBFFE9DC),
        surface = Color(0x1A3A1F2B),
        accent = Color(0xFF3A1F2B),
        onAccent = Color(0xFFFFE9DC),
        isLight = true,
        iconCircle = Color(0xFF3A1F2B),
        iconGlyph = Color(0xFFFFE3D3),
    )

    /** Océano: espuma sobre azul petróleo. */
    val ocean = OnWallpaperColors(
        content = Color(0xFFE6F4F3),
        contentVariant = Color(0xBFE6F4F3),
        scrimTop = Color.Transparent,
        scrimBottom = Color.Transparent,
        drawerScrim = Color(0xF0062227),
        surface = Color(0x1FE6F4F3),
        accent = Color(0xFFB8E3DE),
        onAccent = Color(0xFF062227),
        textShadow = Color(0x40000000),
        iconCircle = Color(0xFFB8E3DE),
        iconGlyph = Color(0xFF062227),
    )

    /** Nieve: carbón sobre blanco. */
    val snow = OnWallpaperColors(
        content = Color(0xFF202124),
        contentVariant = Color(0xA6202124),
        scrimTop = Color.Transparent,
        scrimBottom = Color.Transparent,
        drawerScrim = Color(0xF2FAFAFA),
        surface = Color(0x0F202124),
        accent = Color(0xFF202124),
        onAccent = Color(0xFFFAFAFA),
        isLight = true,
        iconCircle = Color(0xFFEDEDEF),
        iconGlyph = Color(0xFF202124),
    )

    /** Café: crema sobre marrón tostado. */
    val coffee = OnWallpaperColors(
        content = Color(0xFFF1E4D3),
        contentVariant = Color(0xBFF1E4D3),
        scrimTop = Color.Transparent,
        scrimBottom = Color.Transparent,
        drawerScrim = Color(0xF21E140E),
        surface = Color(0x1FF1E4D3),
        accent = Color(0xFFE2BE91),
        onAccent = Color(0xFF1E140E),
        textShadow = Color(0x59000000),
        iconCircle = Color(0xFFF1E4D3),
        iconGlyph = Color(0xFF3B2A1E),
    )

    /** Ceniza: hueso y pergamino sobre negro cálido; brasa como acento. */
    val ash = OnWallpaperColors(
        content = Color(0xFFDCCFB6),
        contentVariant = Color(0xB3DCCFB6),
        scrimTop = Color.Transparent,
        scrimBottom = Color.Transparent,
        drawerScrim = Color(0xF20B0907),
        surface = Color(0x1FDCCFB6),
        accent = Color(0xFFE0893C),
        onAccent = Color(0xFF140D07),
        textShadow = Color(0xB3000000),
        iconCircle = Color(0xFF2A211A),
        iconGlyph = Color(0xFFDCCFB6),
    )
    val pixelNight = OnWallpaperColors(
        content = Color(0xFFFBF6FF),
        contentVariant = Color(0xFFD9C7F5),
        scrimTop = Color.Transparent,
        scrimBottom = Color.Transparent,
        drawerScrim = Color(0xF00B0620),
        surface = Color(0x29F2E9FF),
        accent = Color(0xFFFFD86B),
        onAccent = Color(0xFF1A0B33),
        textShadow = Color(0xFF140733),
        crispShadow = true,
        iconCircle = Color(0xFFF2E9FF),
        iconGlyph = Color(0xFF241046),
    )

    /** Galaxia: blanco estelar con sombra profunda, acento turquesa. */
    val galaxy = OnWallpaperColors(
        content = Color(0xFFFFFFFF),
        contentVariant = Color(0xD9E4ECFF),
        scrimTop = Color.Transparent,
        scrimBottom = Color.Transparent,
        drawerScrim = Color(0xF005060F),
        surface = Color(0x24FFFFFF),
        accent = Color(0xFF7FF0E0),
        onAccent = Color(0xFF05060F),
        textShadow = Color(0xE6000000),
        iconCircle = Color(0xFFEAF0FF),
        iconGlyph = Color(0xFF151A3A),
    )

    /** Neón: blanco con brillo magenta. */
    val neon = OnWallpaperColors(
        content = Color(0xFFFFFFFF),
        contentVariant = Color(0xFFFFC7F2),
        scrimTop = Color.Transparent,
        scrimBottom = Color.Transparent,
        drawerScrim = Color(0xF20A0614),
        surface = Color(0x24FF4FD8),
        accent = Color(0xFF3CF2FF),
        onAccent = Color(0xFF0A0614),
        textGlow = Color(0xCCFF2BD6),
        iconCircle = Color(0xFF3CF2FF),
        iconGlyph = Color(0xFF0A0614),
    )

    /** Matcha: verde bosque sobre crema verdosa. */
    val matcha = OnWallpaperColors(
        content = Color(0xFF24331F),
        contentVariant = Color(0xB824331F),
        scrimTop = Color.Transparent,
        scrimBottom = Color.Transparent,
        drawerScrim = Color(0xF2EEF2E1),
        surface = Color(0x1424331F),
        accent = Color(0xFF4F6B3A),
        onAccent = Color(0xFFF1F3E4),
        isLight = true,
        iconCircle = Color(0xFF4F6B3A),
        iconGlyph = Color(0xFFF1F3E4),
    )

    /** Lavanda: violeta profundo sobre lila. */
    val lavender = OnWallpaperColors(
        content = Color(0xFF2E2346),
        contentVariant = Color(0xB82E2346),
        scrimTop = Color.Transparent,
        scrimBottom = Color.Transparent,
        drawerScrim = Color(0xF2F1ECF9),
        surface = Color(0x142E2346),
        accent = Color(0xFF6D55A8),
        onAccent = Color(0xFFFFFFFF),
        isLight = true,
        iconCircle = Color(0xFF6D55A8),
        iconGlyph = Color(0xFFFFFFFF),
    )
}

/** Colores de las ilustraciones de fondo: pocas formas, mucho aire. */
internal object ArtColors {
    /** Velo sobre una foto propia en los temas de texto oscuro. */
    val lightVeil = Color(0x8CFFFFFF)

    val nightBase = Color(0xFF050507)
    val nightGlow = Color(0x14FFFFFF)

    val paperBase = Color(0xFFF3EEE5)
    val paperShade = Color(0x0F2B2620)
    val paperLine = Color(0x0D2B2620)

    val sakuraSkyTop = Color(0xFFFFF6F8)
    val sakuraSkyBottom = Color(0xFFF7DCE6)
    val sakuraMoon = Color(0xB3FFFFFF)
    val sakuraPetal = Color(0x8CF0A3B8)

    val sunsetTop = Color(0xFFFFD9B8)
    val sunsetBottom = Color(0xFFF4A7B4)
    val sunsetSun = Color(0xB3FFF1E0)
    val sunsetHorizon = Color(0x593A1F2B)
    val oceanTop = Color(0xFF0B3A40)
    val oceanBottom = Color(0xFF062227)
    val oceanWave = Color(0x2EB8E3DE)
    val snowBase = Color(0xFFFAFAFA)
    val snowShade = Color(0x0D202124)
    val coffeeTop = Color(0xFF3B2A1E)
    val coffeeBottom = Color(0xFF1E140E)
    val coffeeGlow = Color(0x26E2BE91)
    val forestTop = Color(0xFF0E2321)
    val forestBottom = Color(0xFF173A33)
    val forestCurve = Color(0x33D7E8B0)

    val auroraTop = Color(0xFF070B1F)
    val auroraBottom = Color(0xFF0E1838)
    val auroraTeal = Color(0x592DD4BF)
    val auroraViolet = Color(0x4D8B5CF6)
    val auroraBlue = Color(0x403B82F6)

    val ashTop = Color(0xFF0B0907)
    val ashBottom = Color(0xFF1A130E)
    val ashGlow = Color(0x8CE0893C)
    val ashEmber = Color(0xFFF2A65A)
    val ashVignette = Color(0xCC000000)

    val pixelTop = Color(0xFF0B0620)
    val pixelMid = Color(0xFF1E0F45)
    val pixelBottom = Color(0xFF3A1A5E)
    val pixelStar = Color(0xFFFFF7E0)
    val pixelStarGlow = Color(0x99C9B3FF)
    val pixelNebula = Color(0x2EB58CFF)
    val pixelMoon = Color(0xFFFFE9A8)
    val pixelMoonGlow = Color(0x1AFFE9A8)
    val pixelCrater = Color(0xFFE5C77A)
    val pixelHillFar = Color(0xFF2A1450)
    val pixelHillNear = Color(0xFF160A30)
    val galaxyBase = Color(0xFF05060F)
    val galaxyMagenta = Color(0x66B23CFF)
    val galaxyTeal = Color(0x5527C4C9)
    val galaxyStar = Color(0xFFF4F7FF)
    val galaxyHalo = Color(0x80A9C8FF)
    val galaxyPlanetLight = Color(0xFFF2B07A)
    val galaxyPlanetDark = Color(0xFF5A2B5E)
    val galaxyRing = Color(0xB3FFE2C4)
    val neonTop = Color(0xFF0A0614)
    val neonBottom = Color(0xFF3B0D4F)
    val neonFloor = Color(0xFF0A0614)
    val neonSunTop = Color(0xFFFFD25A)
    val neonSunBottom = Color(0xFFFF2BD6)
    val neonGrid = Color(0xB3FF2BD6)
    val neonGlow = Color(0x553CF2FF)
    val neonHorizon = Color(0xFF3CF2FF)
    val matchaTop = Color(0xFFF1F3E4)
    val matchaBottom = Color(0xFFDCE5C3)
    val matchaSoft = Color(0x1F4F6B3A)
    val matchaLine = Color(0x664F6B3A)
    val lavenderTop = Color(0xFFF1ECF9)
    val lavenderBottom = Color(0xFFDCCFEF)
    val lavenderBlobA = Color(0x55B79CF0)
    val lavenderBlobB = Color(0x44F0A8D8)
}

/** Colores de marca (landing), tomados del logo: cinta plateada sobre negro. */
object BrandColors {
    val background = Color(0xFF000000)
    val silver = Color(0xFFE8EAED)
    val silverMuted = Color(0xA6E8EAED)
}
