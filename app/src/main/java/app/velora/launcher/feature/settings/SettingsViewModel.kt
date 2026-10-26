package app.velora.launcher.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import app.velora.launcher.core.data.battery.BatteryRepository
import app.velora.launcher.core.data.clock.ClockRepository
import app.velora.launcher.core.data.favorites.FavoritesRepository
import app.velora.launcher.core.data.favorites.HiddenAppsRepository
import app.velora.launcher.core.data.prefs.PrefsRepository
import app.velora.launcher.core.model.AppEntry
import app.velora.launcher.core.model.ClockStyle
import app.velora.launcher.core.model.FontChoice
import app.velora.launcher.core.model.IconStyle
import app.velora.launcher.core.model.TextSize
import app.velora.launcher.core.model.ThemePreset
import app.velora.launcher.core.model.ThemeSettings
import app.velora.launcher.di.AppContainer
import app.velora.launcher.feature.home.ClockFormatter
import app.velora.launcher.feature.home.ClockText
import app.velora.launcher.feature.home.LocalizedClockFormatter
import app.velora.launcher.feature.widgets.WidgetHost
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Vista previa del reloj con la hora y la batería reales. */
data class ClockPreview(val clock: ClockText = ClockText(), val battery: Int? = null)

class SettingsViewModel(
    private val prefs: PrefsRepository,
    private val hiddenApps: HiddenAppsRepository,
    private val favorites: FavoritesRepository,
    widgetHost: WidgetHost,
    clockRepository: ClockRepository,
    clockFormatter: ClockFormatter,
    batteryRepository: BatteryRepository,
) : ViewModel() {

    /** null hasta leer las preferencias. */
    val theme: StateFlow<ThemeSettings?> = prefs.theme.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val preview: StateFlow<ClockPreview> =
        combine(clockRepository.clock.map(clockFormatter::format), batteryRepository.level, ::ClockPreview)
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
                ClockPreview(clockFormatter.format(clockRepository.current())),
            )

    /** Apps ocultas que siguen instaladas, para volver a mostrarlas. */
    val hidden: StateFlow<List<AppEntry>> =
        hiddenApps.hiddenApps.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    fun onUnhide(app: AppEntry) = save { hiddenApps.unhide(app.key) }

    /** Favoritos en su orden, para subirlos o bajarlos. */
    val favoriteApps: StateFlow<List<AppEntry>> =
        favorites.favorites.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    fun onMoveFavorite(app: AppEntry, offset: Int) = save { favorites.move(app.key, offset) }

    fun onTextSize(size: TextSize) = save { prefs.setTextSize(size) }

    fun onLeftHanded(enabled: Boolean) = save { prefs.setLeftHanded(enabled) }

    fun onFullList(enabled: Boolean) = save { prefs.setFullList(enabled) }

    fun onPreset(preset: ThemePreset) = save { prefs.applyPreset(preset) }

    /**
     * Widgets del inicio con su nombre (leído de `PackageManager`, fuera del hilo principal).
     * Cambiarlos va directo a [WidgetHost], que ya guarda en segundo plano.
     */
    val widgets: StateFlow<List<WidgetSetting>> = widgetHost.widgets
        .map { list -> list.map { WidgetSetting(it.appWidgetId, widgetHost.labelOf(it.appWidgetId), it.heightDp) } }
        .flowOn(Dispatchers.IO)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    /** [then] corre después de guardar: el servicio lee la preferencia al conectarse. */
    fun onNotifications(enabled: Boolean, then: () -> Unit) = save {
        prefs.setNotifications(enabled)
        then()
    }

    fun onDoubleTapLock(enabled: Boolean) = save { prefs.setDoubleTapLock(enabled) }

    fun onPhotoBackground(path: String?) = save { prefs.setPhotoBackground(path) }

    fun onFont(font: FontChoice) = save { prefs.setFont(font) }

    fun onClockStyle(style: ClockStyle) = save { prefs.setClockStyle(style) }

    fun onIconStyle(style: IconStyle) = save { prefs.setIconStyle(style) }

    private fun save(block: suspend () -> Unit) {
        viewModelScope.launch { block() }
    }

    companion object {
        private const val STOP_TIMEOUT_MS = 5_000L

        fun factory(container: AppContainer) = viewModelFactory {
            initializer {
                SettingsViewModel(
                    prefs = container.prefsRepository,
                    hiddenApps = container.hiddenAppsRepository,
                    favorites = container.favoritesRepository,
                    widgetHost = container.widgetHost,
                    clockRepository = container.clockRepository,
                    clockFormatter = LocalizedClockFormatter(),
                    batteryRepository = container.batteryRepository,
                )
            }
        }
    }
}
