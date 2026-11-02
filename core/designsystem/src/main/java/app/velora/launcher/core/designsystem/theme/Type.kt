package app.velora.launcher.core.designsystem.theme

import androidx.annotation.FontRes
import androidx.compose.material3.Typography
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font as GoogleFontFace
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.unit.sp
import app.velora.launcher.core.designsystem.R
import app.velora.launcher.core.model.FontChoice

// Fuentes variables empaquetadas (ADR-016). Cada peso es una instancia del eje wght.
private val usedWeights = listOf(
    FontWeight.ExtraLight,
    FontWeight.Light,
    FontWeight.Normal,
    FontWeight.Medium,
    FontWeight.SemiBold,
    FontWeight.Bold,
)

private fun variableFamily(@FontRes resId: Int) = FontFamily(
    usedWeights.map { weight ->
        Font(
            resId = resId,
            weight = weight,
            variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight)),
        )
    },
)

private val Nunito = variableFamily(R.font.nunito)
private val PlusJakartaSans = variableFamily(R.font.plus_jakarta_sans)
private val Figtree = variableFamily(R.font.figtree)
private val Fraunces = variableFamily(R.font.fraunces)
private val Quicksand = variableFamily(R.font.quicksand)
private val Cinzel = variableFamily(R.font.cinzel)
private val Pixel = FontFamily(
    Font(R.font.silkscreen_regular, FontWeight.Normal),
    Font(R.font.silkscreen_bold, FontWeight.Bold),
)

fun fontFamilyOf(choice: FontChoice): FontFamily = when (choice) {
    FontChoice.Nunito -> Nunito
    FontChoice.PlusJakartaSans -> PlusJakartaSans
    FontChoice.Figtree -> Figtree
    FontChoice.Fraunces -> Fraunces
    FontChoice.Quicksand -> Quicksand
    FontChoice.Pixel -> Pixel
    FontChoice.Cinzel -> Cinzel
    FontChoice.System -> FontFamily.Default
    else -> downloadableFamily(choice)
}

/**
 * Fuentes que no van en el APK (ADR-031): las baja una sola vez el proveedor de fuentes de Google
 * Play (Velora no usa internet) y quedan en caché del sistema. Mientras llegan, o si el teléfono
 * no tiene Google Play, se ve la fuente del sistema.
 */
private val FontProvider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs,
)

private val downloadableNames = mapOf(
    FontChoice.PlayfairDisplay to "Playfair Display",
    FontChoice.SpaceGrotesk to "Space Grotesk",
    FontChoice.DmSerifDisplay to "DM Serif Display",
    FontChoice.JosefinSans to "Josefin Sans",
    FontChoice.Comfortaa to "Comfortaa",
    FontChoice.Caveat to "Caveat",
    FontChoice.BebasNeue to "Bebas Neue",
    FontChoice.Righteous to "Righteous",
)

private val downloadableFamilies = HashMap<FontChoice, FontFamily>()

private fun downloadableFamily(choice: FontChoice): FontFamily = downloadableFamilies.getOrPut(choice) {
    val font = GoogleFont(downloadableNames[choice] ?: return@getOrPut FontFamily.Default)
    FontFamily(
        usedWeights.map { weight ->
            GoogleFontFace(googleFont = font, fontProvider = FontProvider, weight = weight)
        },
    )
}

/**
 * Escala tipográfica de ESTILO §3. Los tamaños en `sp` respetan el tamaño de fuente del sistema.
 */
@Immutable
data class VeloraTypography(
    val clock: TextStyle,
    val clockDay: TextStyle,
    /** Día de la semana en mayúsculas espaciadas (reloj "Día"). */
    val weekday: TextStyle,
    val date: TextStyle,
    val favorite: TextStyle,
    val appRow: TextStyle,
    val sectionHeader: TextStyle,
    val letter: TextStyle,
    val letterActive: TextStyle,
    val caption: TextStyle,
    val brandTitle: TextStyle,
)

/**
 * @param shadow sombra suave del texto sobre el fondo (null en temas claros).
 */
fun veloraTypography(family: FontFamily, shadow: Shadow? = null): VeloraTypography {
    fun style(size: Int, lineHeight: Int, weight: FontWeight) = TextStyle(
        fontFamily = family,
        fontSize = size.sp,
        lineHeight = lineHeight.sp,
        fontWeight = weight,
        shadow = shadow,
    )
    return VeloraTypography(
        // Hora delgada y grande, fecha tranquila debajo: sencillo, sin adornos.
        clock = style(64, 70, FontWeight.ExtraLight),
        clockDay = style(120, 120, FontWeight.Bold).copy(letterSpacing = 4.sp),
        weekday = style(13, 18, FontWeight.SemiBold).copy(letterSpacing = 3.sp),
        date = style(18, 24, FontWeight.Normal),
        // Más finos y con un poco de aire: se leen igual y se ven más elegantes.
        favorite = style(19, 26, FontWeight.Normal).copy(letterSpacing = 0.2.sp),
        appRow = style(17, 22, FontWeight.Normal).copy(letterSpacing = 0.2.sp),
        sectionHeader = style(13, 16, FontWeight.SemiBold),
        letter = style(13, 16, FontWeight.Medium),
        letterActive = style(28, 32, FontWeight.SemiBold),
        caption = style(14, 20, FontWeight.Normal),
        brandTitle = style(44, 52, FontWeight.Light).copy(letterSpacing = 2.sp),
    )
}

/** Tipografía de Material 3 con la fuente elegida, para ajustes, diálogos y menús. */
internal fun materialTypography(family: FontFamily): Typography = Typography().run {
    copy(
        displayLarge = displayLarge.copy(fontFamily = family),
        displayMedium = displayMedium.copy(fontFamily = family),
        displaySmall = displaySmall.copy(fontFamily = family),
        headlineLarge = headlineLarge.copy(fontFamily = family),
        headlineMedium = headlineMedium.copy(fontFamily = family),
        headlineSmall = headlineSmall.copy(fontFamily = family),
        titleLarge = titleLarge.copy(fontFamily = family),
        titleMedium = titleMedium.copy(fontFamily = family),
        titleSmall = titleSmall.copy(fontFamily = family),
        bodyLarge = bodyLarge.copy(fontFamily = family),
        bodyMedium = bodyMedium.copy(fontFamily = family),
        bodySmall = bodySmall.copy(fontFamily = family),
        labelLarge = labelLarge.copy(fontFamily = family),
        labelMedium = labelMedium.copy(fontFamily = family),
        labelSmall = labelSmall.copy(fontFamily = family),
    )
}
