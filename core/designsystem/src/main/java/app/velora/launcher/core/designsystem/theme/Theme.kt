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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import app.velora.launcher.core.designsystem.motion.VeloraMotion
import app.velora.launcher.core.designsystem.motion.rememberReducedMotion
import app.velora.launcher.core.model.FontChoice
import app.velora.launcher.core.model.ThemePreset
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
    preset: ThemePreset = ThemePreset.Wallpaper,
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
    val onWallpaper = onWallpaperColorsFor(preset)
    val family = fontFamilyOf(font)
    val typography = remember(family, onWallpaper) { veloraTypography(family, textShadowOf(onWallpaper)) }
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

/** Colores sobre el fondo de cada tema; con el wallpaper, blanco neutro (nunca el azul del sistema). */
private fun onWallpaperColorsFor(preset: ThemePreset): OnWallpaperColors = when (preset) {
    ThemePreset.Wallpaper -> DefaultOnWallpaperColors
    ThemePreset.Night -> PresetColors.night
    ThemePreset.Paper -> PresetColors.paper
    ThemePreset.Sakura -> PresetColors.sakura
    ThemePreset.Forest -> PresetColors.forest
    ThemePreset.Sunset -> PresetColors.sunset
    ThemePreset.Ocean -> PresetColors.ocean
    ThemePreset.Snow -> PresetColors.snow
    ThemePreset.Coffee -> PresetColors.coffee
    ThemePreset.Galaxy -> PresetColors.galaxy
    ThemePreset.Neon -> PresetColors.neon
    ThemePreset.Matcha -> PresetColors.matcha
    ThemePreset.Lavender -> PresetColors.lavender
    ThemePreset.PixelNight -> PresetColors.pixelNight
    ThemePreset.Ash -> PresetColors.ash
    ThemePreset.Aurora -> PresetColors.aurora
}

private const val SHADOW_BLUR = 8f
private const val SHADOW_DROP = 1.5f

/** Brillo de neón, sombra dura (pixel) o la sombra suave de siempre. */
private fun textShadowOf(colors: OnWallpaperColors): Shadow? = when {
    colors.textGlow.alpha > 0f -> Shadow(colors.textGlow, Offset.Zero, GLOW_BLUR)
    colors.textShadow.alpha == 0f -> null
    colors.crispShadow -> Shadow(colors.textShadow, Offset(CRISP_DROP, CRISP_DROP), 0f)
    else -> Shadow(colors.textShadow, Offset(0f, SHADOW_DROP), SHADOW_BLUR)
}

private const val GLOW_BLUR = 14f
private const val CRISP_DROP = 3f

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
