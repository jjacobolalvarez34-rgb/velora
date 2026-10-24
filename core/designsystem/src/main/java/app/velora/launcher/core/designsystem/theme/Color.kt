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
 * Colores para lo que se dibuja directamente sobre el fondo de pantalla del usuario.
 * No dependen del tema claro/oscuro: dependen del velo (scrim) que garantiza el contraste.
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
    /** Único acento sobre el wallpaper (íconos minimalistas, letra activa): tono claro del tema. */
    val accent: Color,
)

internal val DefaultOnWallpaperColors = OnWallpaperColors(
    content = Color(0xFFFFFFFF),
    contentVariant = Color(0xD9FFFFFF),
    scrimTop = Color(0x66000000),
    scrimBottom = Color(0x8C000000),
    drawerScrim = Color(0xC7000000),
    surface = Color(0x24FFFFFF),
    accent = Steel80,
)

/** Colores de marca (landing), tomados del logo: cinta plateada sobre negro. */
object BrandColors {
    val background = Color(0xFF000000)
    val silver = Color(0xFFE8EAED)
    val silverMuted = Color(0xA6E8EAED)
}
