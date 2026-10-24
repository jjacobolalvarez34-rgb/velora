package app.velora.launcher.core.designsystem.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import app.velora.launcher.core.designsystem.R

// Manrope variable (eje wght 200–800), empaquetada y recortada a latino (ADR-015).
private fun manrope(weight: FontWeight) = Font(
    resId = R.font.manrope,
    weight = weight,
    variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight)),
)

val Manrope = FontFamily(
    manrope(FontWeight.ExtraLight),
    manrope(FontWeight.Light),
    manrope(FontWeight.Normal),
    manrope(FontWeight.Medium),
    manrope(FontWeight.SemiBold),
    manrope(FontWeight.Bold),
)

/**
 * Escala tipográfica de ESTILO §3. Los tamaños en `sp` respetan el tamaño de fuente del sistema.
 */
@Immutable
data class VeloraTypography(
    val clock: TextStyle = style(56, 64, FontWeight.ExtraLight),
    val date: TextStyle = style(16, 22, FontWeight.Normal),
    val favorite: TextStyle = style(22, 28, FontWeight.Normal),
    val appRow: TextStyle = style(18, 24, FontWeight.Normal),
    val sectionHeader: TextStyle = style(13, 16, FontWeight.SemiBold),
    val letter: TextStyle = style(13, 16, FontWeight.Medium),
    val letterActive: TextStyle = style(28, 32, FontWeight.SemiBold),
    val caption: TextStyle = style(14, 20, FontWeight.Normal),
    /** "Velora" en la landing. */
    val brandTitle: TextStyle = style(44, 52, FontWeight.Light).copy(letterSpacing = 2.sp),
)

private fun style(size: Int, lineHeight: Int, weight: FontWeight) = TextStyle(
    fontFamily = Manrope,
    fontSize = size.sp,
    lineHeight = lineHeight.sp,
    fontWeight = weight,
)

/** Tipografía de Material 3 con Manrope, para pantallas estándar (ajustes, diálogos, menús). */
internal val VeloraMaterialTypography: Typography = Typography().run {
    copy(
        displayLarge = displayLarge.copy(fontFamily = Manrope),
        displayMedium = displayMedium.copy(fontFamily = Manrope),
        displaySmall = displaySmall.copy(fontFamily = Manrope),
        headlineLarge = headlineLarge.copy(fontFamily = Manrope),
        headlineMedium = headlineMedium.copy(fontFamily = Manrope),
        headlineSmall = headlineSmall.copy(fontFamily = Manrope),
        titleLarge = titleLarge.copy(fontFamily = Manrope),
        titleMedium = titleMedium.copy(fontFamily = Manrope),
        titleSmall = titleSmall.copy(fontFamily = Manrope),
        bodyLarge = bodyLarge.copy(fontFamily = Manrope),
        bodyMedium = bodyMedium.copy(fontFamily = Manrope),
        bodySmall = bodySmall.copy(fontFamily = Manrope),
        labelLarge = labelLarge.copy(fontFamily = Manrope),
        labelMedium = labelMedium.copy(fontFamily = Manrope),
        labelSmall = labelSmall.copy(fontFamily = Manrope),
    )
}
