package app.velora.launcher.core.model

import androidx.compose.runtime.Immutable

/** Fuente de toda la interfaz (ADR-016). */
enum class FontChoice { PlusJakartaSans, Figtree, Fraunces, Quicksand, System }

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

/** Tema elegido por el usuario. Los valores por defecto son el aspecto de fábrica. */
@Immutable
data class ThemeSettings(
    val font: FontChoice = FontChoice.PlusJakartaSans,
    val clock: ClockStyle = ClockStyle.Classic,
    val icons: IconStyle = IconStyle.Circle,
)
