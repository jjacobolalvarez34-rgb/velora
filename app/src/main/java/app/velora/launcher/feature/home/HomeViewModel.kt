package app.velora.launcher.feature.home

import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import app.velora.launcher.core.data.apps.AppsRepository
import app.velora.launcher.core.data.clock.ClockRepository
import app.velora.launcher.core.data.favorites.AddFavoriteResult
import app.velora.launcher.core.data.favorites.FavoritesRepository
import app.velora.launcher.core.data.home.DefaultHomeRepository
import app.velora.launcher.core.model.AppKey
import app.velora.launcher.di.AppContainer
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HomeViewModel(
    clockRepository: ClockRepository,
    private val clockFormatter: ClockFormatter,
    private val appsRepository: AppsRepository,
    private val favoritesRepository: FavoritesRepository,
    private val defaultHomeRepository: DefaultHomeRepository,
) : ViewModel() {

    private val _events = Channel<HomeEvent>(Channel.BUFFERED)
    val events: Flow<HomeEvent> = _events.receiveAsFlow()

    val uiState: StateFlow<HomeUiState> = combine(
        clockRepository.clock,
        favoritesRepository.favorites,
        defaultHomeRepository.isDefault,
        defaultHomeRepository.welcomeDismissed,
    ) { clock, favorites, isDefault, dismissed ->
        HomeUiState(
            clock = clockFormatter.format(clock),
            favorites = favorites,
            // Mientras no se sabe (null) se muestra el inicio: nunca un frame en blanco.
            showWelcome = isDefault == false && !dismissed,
        )
    }.stateIn(
        scope = viewModelScope,
        // El reloj deja de escuchar cuando el inicio no está visible (RENDIMIENTO §6).
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
        initialValue = HomeUiState(clock = clockFormatter.format(clockRepository.current())),
    )

    /** Al volver al frente: por si cambió la app de inicio o se perdieron eventos de paquetes (B-008). */
    fun onResume() {
        appsRepository.refresh()
        viewModelScope.launch { defaultHomeRepository.refresh() }
    }

    fun onWelcomeDismissed() = defaultHomeRepository.dismissWelcome()

    /** Intent para pedir ser el inicio (diálogo del sistema o pantalla de "App de inicio"). */
    fun defaultHomeRequest(): Intent = defaultHomeRepository.requestIntent()

    fun onAddFavorite(key: AppKey) {
        viewModelScope.launch {
            if (favoritesRepository.add(key) == AddFavoriteResult.Full) _events.send(HomeEvent.FavoritesFull)
        }
    }

    fun onRemoveFavorite(key: AppKey) {
        viewModelScope.launch { favoritesRepository.remove(key) }
    }

    companion object {
        private const val STOP_TIMEOUT_MS = 5_000L

        fun factory(container: AppContainer) = viewModelFactory {
            initializer {
                HomeViewModel(
                    clockRepository = container.clockRepository,
                    clockFormatter = LocalizedClockFormatter(),
                    appsRepository = container.appsRepository,
                    favoritesRepository = container.favoritesRepository,
                    defaultHomeRepository = container.defaultHomeRepository,
                )
            }
        }
    }
}
