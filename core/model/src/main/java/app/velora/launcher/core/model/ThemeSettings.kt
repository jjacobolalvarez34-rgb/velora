package app.velora.launcher.core.model

import androidx.compose.runtime.Immutable

/** Fuente de toda la interfaz (ADR-016). */
enum class FontChoice { PlusJakartaSans, Figtree, Fraunces, Quicksand, Pixel, System }

/** Cómo se dibuja el reloj del inicio. */
enum class ClockStyle {
    /** Hora grande y fecha debajo, alineadas a la izquierda. */
    Classic,

    /** Hora, fecha y batería centradas. */
    Centered,

    /** El día en grande ("JUE") cortado por una franja con hora, fecha y batería. */
    Day,
}

/** Forma de los íconos de las apps. */
enum class IconStyle {
    /** Recortados en círculo; los antiguos van sobre un círculo neutro. */
    Circle,

    /** La forma que usa el sistema del teléfono. */
    System,

    /** Una sola tinta del color de acento (íconos temáticos de Android 13+). */
    Minimal,

    /** Solo texto: lo más minimalista y lo más rápido. */
    None,
}

/** Tamaño del texto del inicio y la lista, encima del tamaño de fuente del sistema. */
enum class TextSize(val scale: Float) { Small(0.9f), Normal(1f), Large(1.15f) }

/**
 * Tema completo (ADR-018): fondo dibujado por Velora, colores del texto y acento. Al elegirlo
 * también se aplican su fuente, reloj e íconos recomendados ([recommended]); luego cada uno se
 * puede cambiar por separado.
 */
enum class ThemePreset {
    /** El fondo de pantalla del usuario, con texto claro. */
    Wallpaper,

    /** Claro: cielo rosa, luna y pétalos. */
    Sakura,

    /** Verde azulado con una ola lima y trama de puntos. */
    Forest,

    /** Espacio morado con estrellas de píxeles y fuente pixel. */
    PixelNight,

    /** Azul noche con luces suaves. */
    Aurora,
    ;

    /** Fuente, reloj e íconos que mejor le quedan. */
    val recommended: ThemeSettings
        get() = when (this) {
            Wallpaper -> ThemeSettings()

            Sakura -> ThemeSettings(font = FontChoice.Figtree, clock = ClockStyle.Classic, icons = IconStyle.Circle)

            Forest -> ThemeSettings(font = FontChoice.Quicksand, clock = ClockStyle.Classic, icons = IconStyle.Minimal)

            PixelNight -> ThemeSettings(font = FontChoice.Pixel, clock = ClockStyle.Classic, icons = IconStyle.Circle)

            Aurora -> ThemeSettings(
                font = FontChoice.PlusJakartaSans,
                clock = ClockStyle.Day,
                icons = IconStyle.Minimal,
            )
        }.copy(preset = this)
}

/**
 * Apariencia elegida por el usuario. Los valores por defecto son el aspecto de fábrica.
 *
 * @property leftHanded modo zurdo: el inicio y la lista se espejan (alfabeto a la izquierda).
 * @property fullList lista completa con scroll; si es false (por defecto) se ve solo la letra elegida.
 */
@Immutable
data class ThemeSettings(
    val font: FontChoice = FontChoice.PlusJakartaSans,
    val clock: ClockStyle = ClockStyle.Classic,
    val icons: IconStyle = IconStyle.Circle,
    val textSize: TextSize = TextSize.Normal,
    val leftHanded: Boolean = false,
    val fullList: Boolean = false,
    val preset: ThemePreset = ThemePreset.Wallpaper,
)
