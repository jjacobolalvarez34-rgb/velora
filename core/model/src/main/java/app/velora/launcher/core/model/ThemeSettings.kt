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

/** Tamaño del texto del inicio y la lista, encima del tamaño de fuente del sistema. */
enum class TextSize(val scale: Float) { Small(0.9f), Normal(1f), Large(1.15f) }

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
)
