package app.velora.launcher.feature.home

import androidx.compose.runtime.Immutable
import app.velora.launcher.core.model.AppEntry
import app.velora.launcher.core.model.ClockStyle

@Immutable
data class HomeUiState(
    val clock: ClockText = ClockText(),
    val favorites: List<AppEntry> = emptyList(),
    /** Landing de bienvenida: solo si Velora no es el inicio y no se eligió "Ahora no". */
    val showWelcome: Boolean = false,
    val clockStyle: ClockStyle = ClockStyle.Classic,
    /** Batería en %, o null si no se sabe (los estilos que la muestran la ocultan). */
    val battery: Int? = null,
)

/** Hora y fecha ya formateadas según el idioma y la preferencia 12/24 h del sistema. */
@Immutable
data class ClockText(
    val time: String = "",
    val date: String = "",
    val weekdayShort: String = "",
    /** "LUNES": el día completo en mayúsculas, para el reloj Día. */
    val weekday: String = "",
    /** "26 de octubre": la fecha sin el día de la semana. */
    val dayMonth: String = "",
)

/** Avisos de un solo uso para la UI. */
sealed interface HomeEvent {
    data object FavoritesFull : HomeEvent

    /** Se ocultó una app: se avisa dónde recuperarla. */
    data class AppHidden(val label: String) : HomeEvent
}
