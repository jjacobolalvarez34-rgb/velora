package app.velora.launcher.feature.home

import androidx.compose.runtime.Immutable
import app.velora.launcher.core.model.AppEntry

@Immutable
data class HomeUiState(
    val clock: ClockText = ClockText(),
    val favorites: List<AppEntry> = emptyList(),
    /** Landing de bienvenida: solo si Velora no es el inicio y no se eligió "Ahora no". */
    val showWelcome: Boolean = false,
)

/** Hora y fecha ya formateadas según el idioma y la preferencia 12/24 h del sistema. */
@Immutable
data class ClockText(val time: String = "", val date: String = "")

/** Avisos de un solo uso para la UI. */
sealed interface HomeEvent {
    data object FavoritesFull : HomeEvent
}
