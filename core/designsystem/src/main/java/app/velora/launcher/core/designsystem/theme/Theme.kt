package app.velora.launcher.core.designsystem.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import app.velora.launcher.core.designsystem.motion.VeloraMotion
import app.velora.launcher.core.designsystem.motion.rememberReducedMotion
import app.velora.launcher.core.model.FontChoice
import app.velora.launcher.core.model.ThemeSettings

private val LocalVeloraTypography = staticCompositionLocalOf { veloraTypography(FontFamily.Default) }
private val LocalVeloraMotion = staticCompositionLocalOf { VeloraMotion(reduced = false) }
private val LocalOnWallpaperColors = staticCompositionLocalOf { DefaultOnWallpaperColors }

/**
 * Tema raíz. Material You en Android 12+; paleta "Velora" como respaldo (ESTILO §2).
 *
 * @param font fuente elegida en Ajustes → Tema (ADR-016).
 */
@Composable
fun VeloraTheme(
    font: FontChoice = ThemeSettings().font,
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    reducedMotion: Boolean = rememberReducedMotion(),
    content: @Composable () -> Unit,
) {
    val dynamic = dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val context = LocalContext.current
    val colorScheme = when {
        dynamic && darkTheme -> dynamicDarkColorScheme(context)
        dynamic -> dynamicLightColorScheme(context)
        darkTheme -> VeloraDarkColors
        else -> VeloraLightColors
    }
    // Sobre el wallpaper siempre hay velo oscuro: el acento es el tono claro, en ambos modos.
    val accent = if (dynamic) dynamicDarkColorScheme(context).primary else VeloraDarkColors.primary
    val onWallpaper = remember(accent) { DefaultOnWallpaperColors.copy(accent = accent) }
    val family = fontFamilyOf(font)
    val typography = remember(family) { veloraTypography(family) }
    val material = remember(family) { materialTypography(family) }
    val motion = remember(reducedMotion) { VeloraMotion(reducedMotion) }

    CompositionLocalProvider(
        LocalVeloraMotion provides motion,
        LocalVeloraTypography provides typography,
        LocalOnWallpaperColors provides onWallpaper,
    ) {
        MaterialTheme(colorScheme = colorScheme, typography = material, content = content)
    }
}

/** Acceso a los tokens propios de Velora (lo que Material 3 no cubre). */
object VeloraTheme {
    val typography: VeloraTypography
        @Composable @ReadOnlyComposable
        get() = LocalVeloraTypography.current

    val motion: VeloraMotion
        @Composable @ReadOnlyComposable
        get() = LocalVeloraMotion.current

    val onWallpaper: OnWallpaperColors
        @Composable @ReadOnlyComposable
        get() = LocalOnWallpaperColors.current
}
