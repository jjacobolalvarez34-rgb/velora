package app.velora.launcher.core.model

import androidx.compose.runtime.Immutable
import java.time.LocalDateTime

/**
 * Hora actual tal como la debe mostrar el inicio.
 *
 * @property is24Hour preferencia 12/24 h del sistema en el momento de la lectura.
 */
@Immutable
data class ClockState(val now: LocalDateTime, val is24Hour: Boolean)
