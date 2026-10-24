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
import app.velora.launcher.core.designsystem.motion.VeloraMotion
import app.velora.launcher.core.designsystem.motion.rememberReducedMotion

private val LocalVeloraTypography = staticCompositionLocalOf { VeloraTypography() }
private val LocalVeloraMotion = staticCompositionLocalOf { VeloraMotion(reduced = false) }
private val LocalOnWallpaperColors = staticCompositionLocalOf { DefaultOnWallpaperColors }

/**
 * Tema raíz. Material You en Android 12+; paleta "Velora" como respaldo (ESTILO §2).
 */
@Composable
fun VeloraTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    reducedMotion: Boolean = rememberReducedMotion(),
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> VeloraDarkColors

        else -> VeloraLightColors
    }
    val motion = remember(reducedMotion) { VeloraMotion(reducedMotion) }

    CompositionLocalProvider(
        LocalVeloraMotion provides motion,
    ) {
        MaterialTheme(colorScheme = colorScheme, content = content)
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
