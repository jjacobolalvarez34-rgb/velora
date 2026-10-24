package app.velora.launcher.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import app.velora.launcher.core.data.clock.ClockRepository
import app.velora.launcher.di.AppContainer
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class HomeViewModel(clockRepository: ClockRepository, private val clockFormatter: ClockFormatter) : ViewModel() {

    val uiState: StateFlow<HomeUiState> = clockRepository.clock
        .map { HomeUiState(clock = clockFormatter.format(it)) }
        .stateIn(
            scope = viewModelScope,
            // El reloj deja de escuchar cuando el inicio no está visible (RENDIMIENTO §6).
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
            initialValue = HomeUiState(clock = clockFormatter.format(clockRepository.current())),
        )

    companion object {
        private const val STOP_TIMEOUT_MS = 5_000L

        fun factory(container: AppContainer) = viewModelFactory {
            initializer { HomeViewModel(container.clockRepository, LocalizedClockFormatter()) }
        }
    }
}
