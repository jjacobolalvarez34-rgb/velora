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
    /** Único acento sobre el fondo (íconos minimalistas, burbuja del alfabeto). */
    val accent: Color,
    /** Texto sobre el acento (la letra dentro de la burbuja). */
    val onAccent: Color = Slate10,
    /** Sombra suave del texto: legible sobre cualquier fondo sin un velo pesado. */
    val textShadow: Color = Color.Transparent,
    /** Fondo claro con texto oscuro: los íconos de las barras del sistema van oscuros. */
    val isLight: Boolean = false,
)

internal val DefaultOnWallpaperColors = OnWallpaperColors(
    content = Color(0xFFFFFFFF),
    contentVariant = Color(0xE0FFFFFF),
    scrimTop = Color(0x47000000),
    scrimBottom = Color(0x6B000000),
    drawerScrim = Color(0xC7000000),
    surface = Color(0x24FFFFFF),
    accent = Steel80,
    textShadow = Color(0x8C000000),
)

/** Colores de cada tema (ADR-018): texto, acento y velos sobre su fondo dibujado. */
internal object PresetColors {
    val sakura = OnWallpaperColors(
        content = Color(0xFF2A2238),
        contentVariant = Color(0xB82A2238),
        scrimTop = Color.Transparent,
        scrimBottom = Color.Transparent,
        drawerScrim = Color(0xE6FFF6F8),
        surface = Color(0x1A2A2238),
        accent = Color(0xFFD9577A),
        onAccent = Color(0xFFFFFFFF),
        isLight = true,
    )
    val forest = OnWallpaperColors(
        content = Color(0xFFF1F7E8),
        contentVariant = Color(0xBFF1F7E8),
        scrimTop = Color.Transparent,
        scrimBottom = Color(0x330B2224),
        drawerScrim = Color(0xEB0F2A2C),
        surface = Color(0x1FF1F7E8),
        accent = Color(0xFFC4E04B),
        onAccent = Color(0xFF0F2A2C),
        textShadow = Color(0x59000000),
    )
    val pixelNight = OnWallpaperColors(
        content = Color(0xFFF2E9FF),
        contentVariant = Color(0xBFF2E9FF),
        scrimTop = Color.Transparent,
        scrimBottom = Color(0x400B0620),
        drawerScrim = Color(0xEB0B0620),
        surface = Color(0x1FF2E9FF),
        accent = Color(0xFFC86BFA),
        onAccent = Color(0xFF0B0620),
        textShadow = Color(0x99000000),
    )
    val aurora = OnWallpaperColors(
        content = Color(0xFFFFFFFF),
        contentVariant = Color(0xC7FFFFFF),
        scrimTop = Color.Transparent,
        scrimBottom = Color(0x40070B1F),
        drawerScrim = Color(0xEB070B1F),
        surface = Color(0x1FFFFFFF),
        accent = Color(0xFF7DE3D1),
        onAccent = Color(0xFF070B1F),
        textShadow = Color(0x80000000),
    )
}

/** Colores de las ilustraciones de fondo de cada tema. */
internal object ArtColors {
    val sakuraSkyTop = Color(0xFFFDF3F6)
    val sakuraSkyBottom = Color(0xFFF6D6E2)
    val sakuraMoon = Color(0xE6FFFFFF)
    val sakuraMountain = Color(0xFFC3D9F1)
    val sakuraMountainShade = Color(0xFFA9C5E7)
    val sakuraSnow = Color(0xFFF7FBFF)
    val sakuraBranch = Color(0xFF3A2E44)
    val sakuraPetal = Color(0xFFF3A3BB)
    val sakuraPetalLight = Color(0xFFF9C9D7)

    val forestBackground = Color(0xFF0F2A2C)
    val forestWaveLight = Color(0xFFD3E23A)
    val forestWaveDark = Color(0xFF5FB043)
    val forestDot = Color(0x40C4E04B)
    val forestStripe = Color(0x80C4E04B)

    val pixelTop = Color(0xFF0B0620)
    val pixelBottom = Color(0xFF241046)
    val pixelNebula = Color(0x807A1FA2)
    val pixelStar = Color(0xFFEBDDFF)
    val pixelPlanetLight = Color(0xFF8E6BE8)
    val pixelPlanetDark = Color(0xFF3B2A78)
    val pixelRing = Color(0xFFC86BFA)

    val auroraTop = Color(0xFF070B1F)
    val auroraBottom = Color(0xFF0E1838)
    val auroraTeal = Color(0x732DD4BF)
    val auroraViolet = Color(0x668B5CF6)
    val auroraBlue = Color(0x593B82F6)
    val auroraStar = Color(0x99FFFFFF)
}

/** Colores de marca (landing), tomados del logo: cinta plateada sobre negro. */
object BrandColors {
    val background = Color(0xFF000000)
    val silver = Color(0xFFE8EAED)
    val silverMuted = Color(0xA6E8EAED)
}
