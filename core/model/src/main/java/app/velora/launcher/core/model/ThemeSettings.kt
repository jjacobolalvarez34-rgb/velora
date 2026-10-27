package app.velora.launcher.core.model

import androidx.compose.runtime.Immutable

/** Fuente de toda la interfaz (ADR-016). */
enum class FontChoice { Nunito, Quicksand, PlusJakartaSans, Figtree, Fraunces, Cinzel, Pixel, System }

/** Cómo se dibuja el reloj del inicio. */
enum class ClockStyle {
    /** Hora grande y fecha debajo, alineadas a la izquierda. */
    Classic,

    /** Hora, fecha y batería centradas. */
    Centered,

    /** El día en mayúsculas espaciadas arriba, la hora y la fecha debajo. */
    Day,
}

/** Forma de los íconos de las apps. */
enum class IconStyle {
    /** Recortados en círculo; los antiguos van sobre un círculo neutro. */
    Circle,

    /** La forma que usa el sistema del teléfono. */
    System,

    /** Logo en línea (ícono temático de Android 13+) sobre un círculo translúcido. */
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

    /** Negro puro con un brillo tenue. */
    Night,

    /** Crema con tinta oscura y fuente con serifa. */
    Paper,

    /** Claro: cielo rosa, luna y unos pétalos. */
    Sakura,

    /** Verde profundo con una curva suave. */
    Forest,

    /** Azul noche con luces suaves. */
    Aurora,

    /** Morado con estrellas de píxeles y fuente pixel. */
    PixelNight,

    /** Ceniza: fantasía oscura, hoguera y brasas, letras talladas. */
    Ash,

    /** Atardecer: cielo de durazno a rosa, un sol bajo y el horizonte. */
    Sunset,

    /** Océano: azul petróleo profundo con dos olas finas. */
    Ocean,

    /** Nieve: blanco casi puro, tinta gris carbón. El más minimalista. */
    Snow,

    /** Café: marrón tostado con crema, cálido y tranquilo. */
    Coffee,

    /** Galaxia: nebulosas, un brazo de estrellas y un planeta con anillo. */
    Galaxy,

    /** Neón: sol rayado y cuadrícula en perspectiva, texto que brilla. */
    Neon,

    /** Matcha: crema verdosa con un cuenco y una hoja; claro. */
    Matcha,

    /** Lavanda: lila con manchas de acuarela; claro. */
    Lavender,
    ;

    /** Fuente, reloj e íconos que mejor le quedan. */
    val recommended: ThemeSettings
        get() = when (this) {
            Wallpaper -> ThemeSettings()
            Night -> look(FontChoice.Nunito, ClockStyle.Classic, IconStyle.Minimal)
            Paper -> look(FontChoice.Fraunces, ClockStyle.Day, IconStyle.Minimal)
            Sakura -> look(FontChoice.Nunito, ClockStyle.Classic, IconStyle.Circle)
            Forest -> look(FontChoice.Quicksand, ClockStyle.Classic, IconStyle.Minimal)
            Aurora -> look(FontChoice.Nunito, ClockStyle.Centered, IconStyle.Minimal)
            PixelNight -> look(FontChoice.Pixel, ClockStyle.Classic, IconStyle.Circle)
            Ash -> look(FontChoice.Cinzel, ClockStyle.Day, IconStyle.Minimal)
            Sunset -> look(FontChoice.Fraunces, ClockStyle.Centered, IconStyle.Minimal)
            Ocean -> look(FontChoice.Figtree, ClockStyle.Classic, IconStyle.Minimal)
            Snow -> look(FontChoice.PlusJakartaSans, ClockStyle.Classic, IconStyle.Minimal)
            Coffee -> look(FontChoice.Fraunces, ClockStyle.Day, IconStyle.Minimal)
            Galaxy -> look(FontChoice.Figtree, ClockStyle.Centered, IconStyle.Minimal)
            Neon -> look(FontChoice.PlusJakartaSans, ClockStyle.Centered, IconStyle.Minimal)
            Matcha -> look(FontChoice.Quicksand, ClockStyle.Classic, IconStyle.Minimal)
            Lavender -> look(FontChoice.Nunito, ClockStyle.Day, IconStyle.Minimal)
        }.copy(preset = this)

    private fun look(font: FontChoice, clock: ClockStyle, icons: IconStyle) =
        ThemeSettings(font = font, clock = clock, icons = icons)
}

/**
 * Apariencia elegida por el usuario. Los valores por defecto son el aspecto de fábrica.
 *
 * @property leftHanded modo zurdo: el inicio y la lista se espejan (alfabeto a la izquierda).
 * @property fullList lista completa con scroll; si es false (por defecto) se ve solo la letra elegida.
 * @property photoBackground ruta de la foto propia que reemplaza el fondo del tema (ADR-022), o null.
 * @property notifications mostrar notificaciones en el inicio (opt-in, ADR-023).
 * @property doubleTapLock doble toque en el inicio para bloquear (opt-in, accesibilidad, ADR-009).
 * @property usageTime mostrar el tiempo de uso de hoy en el panel de cada app (opt-in, ADR-024).
 * @property contactsSearch buscar contactos en el buscador (opt-in, `READ_CONTACTS`, ADR-025).
 */
@Immutable
data class ThemeSettings(
    val font: FontChoice = FontChoice.Nunito,
    val clock: ClockStyle = ClockStyle.Classic,
    val icons: IconStyle = IconStyle.Circle,
    val textSize: TextSize = TextSize.Normal,
    val leftHanded: Boolean = false,
    val fullList: Boolean = false,
    val preset: ThemePreset = ThemePreset.Wallpaper,
    val photoBackground: String? = null,
    val notifications: Boolean = false,
    val doubleTapLock: Boolean = false,
    val usageTime: Boolean = false,
    val contactsSearch: Boolean = false,
)
