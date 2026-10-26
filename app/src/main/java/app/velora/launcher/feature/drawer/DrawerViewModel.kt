package app.velora.launcher.feature.drawer

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import app.velora.launcher.core.data.apps.AppSearch
import app.velora.launcher.core.data.apps.RecentApps
import app.velora.launcher.core.data.favorites.FavoritesRepository
import app.velora.launcher.core.data.search.Calculator
import app.velora.launcher.core.model.AppEntry
import app.velora.launcher.di.AppContainer
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

@Immutable
data class DrawerUiState(
    val list: DrawerList = DrawerList(),
    val query: String = "",
    /** null cuando no se ha escrito nada. */
    val results: List<AppEntry>? = null,
    /** Resultado de la calculadora si lo escrito es una cuenta ("12*3" → "36"). */
    val calculation: String? = null,
    /** Recién instaladas: arriba de la lista y en el buscador vacío. */
    val recents: List<AppEntry> = emptyList(),
    val favoriteIds: Set<String> = emptySet(),
)

class DrawerViewModel(
    /** Instaladas menos ocultas. */
    apps: StateFlow<List<AppEntry>>,
    favorites: FavoritesRepository,
    private val nowMs: () -> Long = System::currentTimeMillis,
) : ViewModel() {

    private val query = MutableStateFlow("")

    // Ordenar, agrupar y filtrar en Dispatchers.Default (ARQUITECTURA §7), nunca en un composable.
    private val list = apps.map { installed ->
        val recents = RecentApps.of(installed, nowMs())
        buildDrawerList(installed, recents) to recents
    }.flowOn(Dispatchers.Default)

    private val results = combine(apps, query) { installed, q ->
        if (q.isBlank()) null else AppSearch.filter(installed, q, Locale.getDefault())
    }.flowOn(Dispatchers.Default)

    val uiState: StateFlow<DrawerUiState> =
        combine(list, query, results, favorites.favoriteIds) { (list, recents), q, results, favoriteIds ->
            DrawerUiState(
                list = list,
                query = q,
                results = results,
                calculation = Calculator.evaluate(q),
                recents = recents,
                favoriteIds = favoriteIds,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), DrawerUiState())

    fun onQueryChange(value: String) {
        query.value = value
    }

    companion object {
        private const val STOP_TIMEOUT_MS = 5_000L

        fun factory(container: AppContainer) = viewModelFactory {
            initializer { DrawerViewModel(container.visibleApps, container.favoritesRepository) }
        }
    }
}
