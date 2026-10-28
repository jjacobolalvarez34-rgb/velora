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
        textShadow = Color(0x73000000),
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

    /** Mármol blanco: tinta grafito cálida sobre piedra clara. */
    val marble = OnWallpaperColors(
        content = Color(0xFF262421),
        contentVariant = Color(0xB3262421),
        scrimTop = Color.Transparent,
        scrimBottom = Color.Transparent,
        drawerScrim = Color(0xF2F4F2ED),
        surface = Color(0x14262421),
        accent = Color(0xFF3A3632),
        onAccent = Color(0xFFF4F2ED),
        isLight = true,
        iconCircle = Color(0xFF262421),
        iconGlyph = Color(0xFFF4F2ED),
    )

    /** Mármol negro: hueso sobre piedra oscura, acento dorado como las vetas. */
    val blackMarble = OnWallpaperColors(
        content = Color(0xFFEDE3CF),
        contentVariant = Color(0xB3EDE3CF),
        scrimTop = Color.Transparent,
        scrimBottom = Color.Transparent,
        drawerScrim = Color(0xF20A0A09),
        surface = Color(0x1FEDE3CF),
        accent = Color(0xFFD9B872),
        onAccent = Color(0xFF0F0E0C),
        textShadow = Color(0xB3000000),
        iconCircle = Color(0xFF221F1B),
        iconGlyph = Color(0xFFD9B872),
    )

    /** Grafito: blanco sobre carbón; el más sobrio. */
    val graphite = OnWallpaperColors(
        content = Color(0xFFFFFFFF),
        contentVariant = Color(0xB8FFFFFF),
        scrimTop = Color.Transparent,
        scrimBottom = Color.Transparent,
        drawerScrim = Color(0xF20E0F10),
        surface = Color(0x1FFFFFFF),
        accent = Color(0xFFF2F2F2),
        onAccent = Color(0xFF161718),
        textShadow = Color(0x59000000),
        iconCircle = Color(0xFFE9E9EC),
        iconGlyph = Color(0xFF161718),
    )
}

/** Colores de las ilustraciones de fondo: pocas formas, mucho aire. */
internal object ArtColors {
    /** Velo sobre una foto propia en los temas de texto oscuro. */
    val lightVeil = Color(0x8CFFFFFF)

    // Noche
    val nightBase = Color(0xFF050507)
    val nightHaze = Color(0xFF0C0D13)
    val nightGlow = Color(0xFF1B1F2C)

    // Papel
    val paperBase = Color(0xFFF3EEE5)
    val paperWarm = Color(0xFFEDE4D3)
    val paperFiber = Color(0xFFD9CDB7)
    val paperFiberLight = Color(0xFFFCFAF4)
    val paperEdge = Color(0xFFE5DAC7)

    // Sakura
    val sakuraSkyTop = Color(0xFFFFF5F7)
    val sakuraSkyBottom = Color(0xFFF6D6E2)
    val sakuraPeach = Color(0xFFFFE3D8)
    val sakuraRose = Color(0xFFF4C6D7)
    val sakuraLilac = Color(0xFFECDAF2)
    val sakuraMoon = Color(0xB3FFFFFF)
    val sakuraMoonHalo = Color(0x40FFFFFF)
    val sakuraPetal = Color(0x8CF0A3B8)

    // Atardecer lofi
    val sunsetTop = Color(0xFFFCE5D0)
    val sunsetMid = Color(0xFFF9C3A7)
    val sunsetLow = Color(0xFFF2A3AD)
    val sunsetSun = Color(0xFFFFF4E2)
    val sunsetCloud = Color(0xFFFDD8C9)
    val sunsetCityFar = Color(0xFFE99CAB)
    val sunsetCityNear = Color(0xFFD6849D)
    val sunsetWindow = Color(0xFFFFE3B0)

    // Océano visto desde arriba
    val oceanDeep = Color(0xFF04303C)
    val oceanMid = Color(0xFF0A6B76)
    val oceanShallow = Color(0xFF2FB1AD)
    val oceanGlow = Color(0xFF79DCD2)
    val oceanFoam = Color(0xFFF1FBF8)

    // Nieve
    val snowBase = Color(0xFFFAFAFA)
    val snowSky = Color(0xFFEEF2F7)
    val snowShade = Color(0xFFE3E9EF)

    // Café
    val coffeeTop = Color(0xFF3B2A1E)
    val coffeeBottom = Color(0xFF1A110C)
    val coffeeGlow = Color(0xFFE2BE91)
    val coffeeCrema = Color(0xFF5A3F2C)

    // Bosque con niebla
    val forestTop = Color(0xFF0B1E1C)
    val forestMist = Color(0xFF52766C)
    val forestRidge1 = Color(0xFF35564D)
    val forestRidge2 = Color(0xFF26443D)
    val forestRidge3 = Color(0xFF18302A)
    val forestRidge4 = Color(0xFF0C1D19)

    // Aurora
    val auroraTop = Color(0xFF040816)
    val auroraBottom = Color(0xFF0C1A35)
    val auroraGreen = Color(0xFF3BF0A6)
    val auroraTeal = Color(0xFF2DD4BF)
    val auroraViolet = Color(0xFF8B5CF6)
    val auroraStar = Color(0xFFEAF2FF)

    // Ceniza
    val ashTop = Color(0xFF0B0907)
    val ashBottom = Color(0xFF1A130E)
    val ashGlow = Color(0x8CE0893C)
    val ashEmber = Color(0xFFF2A65A)
    val ashVignette = Color(0xCC000000)

    // Noche pixel
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

    // Galaxia
    val galaxyBase = Color(0xFF04050D)
    val galaxyDeep = Color(0xFF0A0D22)
    val galaxyMagenta = Color(0xFFB23CFF)
    val galaxyTeal = Color(0xFF27C4C9)
    val galaxyCore = Color(0xFFFFD9F0)
    val galaxyStar = Color(0xFFF4F7FF)
    val galaxyHalo = Color(0x80A9C8FF)
    val galaxyPlanetLight = Color(0xFFF2B07A)
    val galaxyPlanetDark = Color(0xFF5A2B5E)
    val galaxyRing = Color(0xB3FFE2C4)

    // Neón
    val neonTop = Color(0xFF0A0614)
    val neonBottom = Color(0xFF3B0D4F)
    val neonHaze = Color(0xFFFF2BD6)
    val neonSunTop = Color(0xFFFFD25A)
    val neonSunBottom = Color(0xFFFF2BD6)
    val neonGrid = Color(0xB3FF2BD6)
    val neonGlow = Color(0x553CF2FF)
    val neonHorizon = Color(0xFF3CF2FF)

    // Matcha (degradado de malla)
    val matchaBase = Color(0xFFEEF1E0)
    val matchaTea = Color(0xFFDCE7BD)
    val matchaLeaf = Color(0xFFC8DAA2)
    val matchaMilk = Color(0xFFF5F2DE)

    // Lavanda (degradado de malla)
    val lavenderBase = Color(0xFFEEE8F8)
    val lavenderViolet = Color(0xFFCEB9F5)
    val lavenderPink = Color(0xFFF5C8E1)
    val lavenderBlue = Color(0xFFC0CBF6)
    val lavenderMist = Color(0xFFF8EEF8)

    // Mármol blanco
    val marbleBase = Color(0xFFF4F2ED)
    val marbleCloud = Color(0xFFE2DED7)
    val marbleVein = Color(0xFF8E8983)
    val marbleFine = Color(0xFFB8B2AA)

    // Mármol negro
    val blackMarbleBase = Color(0xFF0A0A09)
    val blackMarbleCloud = Color(0xFF1C1A17)
    val blackMarbleSmoke = Color(0xFF2A2723)
    val blackMarbleGold = Color(0xFFC9A35A)
    val blackMarbleGlint = Color(0xFFF0D48F)
    val blackMarbleHair = Color(0xFF6E685E)

    // Grafito
    val graphiteTop = Color(0xFF232427)
    val graphiteBottom = Color(0xFF0E0F10)
    val graphiteLight = Color(0xFF4A4D52)
}

/** Colores de marca (landing), tomados del logo: cinta plateada sobre negro. */
object BrandColors {
    val background = Color(0xFF000000)
    val silver = Color(0xFFE8EAED)
    val silverMuted = Color(0xA6E8EAED)

    /** Segundo tono de la marca: la cinta pasa de plata a lila. */
    val lilac = Color(0xFFB9A8FF)

    /** Halo detrás del logo y luces del landing. */
    val glow = Color(0x40A99BFF)

    /** Luces de fondo del landing, muy tenues sobre negro. */
    val auroraA = Color(0x337C6CFF)
    val auroraB = Color(0x2639C6D6)
}
