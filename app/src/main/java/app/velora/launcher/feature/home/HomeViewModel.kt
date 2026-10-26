package app.velora.launcher.feature.home

import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import app.velora.launcher.core.data.apps.AppsRepository
import app.velora.launcher.core.data.battery.BatteryRepository
import app.velora.launcher.core.data.clock.ClockRepository
import app.velora.launcher.core.data.favorites.AddFavoriteResult
import app.velora.launcher.core.data.favorites.FavoritesRepository
import app.velora.launcher.core.data.favorites.HiddenAppsRepository
import app.velora.launcher.core.data.home.DefaultHomeRepository
import app.velora.launcher.core.data.prefs.PrefsRepository
import app.velora.launcher.core.model.AppEntry
import app.velora.launcher.core.model.AppKey
import app.velora.launcher.core.model.ThemeSettings
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
    private val hiddenAppsRepository: HiddenAppsRepository,
    private val prefsRepository: PrefsRepository,
    batteryRepository: BatteryRepository,
) : ViewModel() {

    /** null hasta leer las preferencias; false = primera vez: se muestra la introducción. */
    val onboardingDone: StateFlow<Boolean?> = prefsRepository.onboardingDone
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    /** null hasta leer las preferencias: así nunca se ve un frame con el tema equivocado. */
    val theme: StateFlow<ThemeSettings?> = prefsRepository.theme
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val _events = Channel<HomeEvent>(Channel.BUFFERED)
    val events: Flow<HomeEvent> = _events.receiveAsFlow()

    private val showWelcome = combine(defaultHomeRepository.isDefault, defaultHomeRepository.welcomeDismissed) {
            isDefault,
            dismissed,
        ->
        // Mientras no se sabe (null) se muestra el inicio: nunca un frame en blanco.
        isDefault == false && !dismissed
    }

    val uiState: StateFlow<HomeUiState> = combine(
        clockRepository.clock,
        favoritesRepository.favorites,
        showWelcome,
        prefsRepository.theme,
        batteryRepository.level,
    ) { clock, favorites, welcome, theme, battery ->
        HomeUiState(
            clock = clockFormatter.format(clock),
            favorites = favorites,
            showWelcome = welcome,
            clockStyle = theme.clock,
            battery = battery,
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

    /** Fin de la introducción. Con "Ahora no", tampoco se insiste con la bienvenida corta. */
    fun onOnboardingFinished(dismissWelcome: Boolean) {
        if (dismissWelcome) defaultHomeRepository.dismissWelcome()
        viewModelScope.launch { prefsRepository.setOnboardingDone() }
    }

    /** ¿Velora ya es el inicio? Para decir "Empezar" en vez de "Usar Velora como inicio". */
    val isDefaultHome: StateFlow<Boolean?> = defaultHomeRepository.isDefault

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

    fun onHide(app: AppEntry) {
        viewModelScope.launch {
            hiddenAppsRepository.hide(app.key)
            _events.send(HomeEvent.AppHidden(app.label))
        }
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
                    hiddenAppsRepository = container.hiddenAppsRepository,
                    prefsRepository = container.prefsRepository,
                    batteryRepository = container.batteryRepository,
                )
            }
        }
    }
}
