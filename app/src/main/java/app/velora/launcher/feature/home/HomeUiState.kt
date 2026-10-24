package app.velora.launcher.feature.home

import androidx.compose.runtime.Immutable

@Immutable
data class HomeUiState(val clock: ClockText = ClockText())

/** Hora y fecha ya formateadas según el idioma y la preferencia 12/24 h del sistema. */
@Immutable
data class ClockText(val time: String = "", val date: String = "")
