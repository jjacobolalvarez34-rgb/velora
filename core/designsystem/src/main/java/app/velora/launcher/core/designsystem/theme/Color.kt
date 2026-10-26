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
    /** Círculo translúcido detrás de los íconos minimalistas. */
    val iconBackdrop: Color = Color(0x2EFFFFFF),
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
        iconBackdrop = Color(0x26FFFFFF),
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
        iconBackdrop = Color(0x142B2620),
        isLight = true,
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
        iconBackdrop = Color(0x1AC75B7A),
        isLight = true,
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
        iconBackdrop = Color(0x26D7E8B0),
        textShadow = Color(0x40000000),
    )
    val aurora = OnWallpaperColors(
        content = Color(0xFFFFFFFF),
        contentVariant = Color(0xC7FFFFFF),
        scrimTop = Color.Transparent,
        scrimBottom = Color.Transparent,
        drawerScrim = Color(0xEB070B1F),
        surface = Color(0x1FFFFFFF),
        accent = Color(0xFFFFFFFF),
        iconBackdrop = Color(0x29FFFFFF),
        textShadow = Color(0x66000000),
    )
    val pixelNight = OnWallpaperColors(
        content = Color(0xFFF2E9FF),
        contentVariant = Color(0xBFF2E9FF),
        scrimTop = Color.Transparent,
        scrimBottom = Color.Transparent,
        drawerScrim = Color(0xEB0B0620),
        surface = Color(0x1FF2E9FF),
        accent = Color(0xFFF2E9FF),
        onAccent = Color(0xFF0B0620),
        iconBackdrop = Color(0x29F2E9FF),
        textShadow = Color(0x99000000),
    )
}

/** Colores de las ilustraciones de fondo: pocas formas, mucho aire. */
internal object ArtColors {
    val nightBase = Color(0xFF050507)
    val nightGlow = Color(0x14FFFFFF)

    val paperBase = Color(0xFFF3EEE5)
    val paperShade = Color(0x0F2B2620)
    val paperLine = Color(0x0D2B2620)

    val sakuraSkyTop = Color(0xFFFFF6F8)
    val sakuraSkyBottom = Color(0xFFF7DCE6)
    val sakuraMoon = Color(0xB3FFFFFF)
    val sakuraPetal = Color(0x8CF0A3B8)

    val forestTop = Color(0xFF0E2321)
    val forestBottom = Color(0xFF173A33)
    val forestCurve = Color(0x33D7E8B0)

    val auroraTop = Color(0xFF070B1F)
    val auroraBottom = Color(0xFF0E1838)
    val auroraTeal = Color(0x592DD4BF)
    val auroraViolet = Color(0x4D8B5CF6)
    val auroraBlue = Color(0x403B82F6)

    val pixelTop = Color(0xFF0B0620)
    val pixelBottom = Color(0xFF241046)
    val pixelStar = Color(0xFFEBDDFF)
}

/** Colores de marca (landing), tomados del logo: cinta plateada sobre negro. */
object BrandColors {
    val background = Color(0xFF000000)
    val silver = Color(0xFFE8EAED)
    val silverMuted = Color(0xA6E8EAED)
}
