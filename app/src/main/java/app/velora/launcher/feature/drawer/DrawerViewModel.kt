package app.velora.launcher.feature.drawer

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import app.velora.launcher.core.data.apps.AppSearch
import app.velora.launcher.core.data.apps.AppsRepository
import app.velora.launcher.core.data.favorites.FavoritesRepository
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
    /** null cuando no se está buscando. */
    val results: List<AppEntry>? = null,
    val favoriteIds: Set<String> = emptySet(),
)

class DrawerViewModel(apps: AppsRepository, favorites: FavoritesRepository) : ViewModel() {

    private val query = MutableStateFlow("")

    // Ordenar, agrupar y filtrar en Dispatchers.Default (ARQUITECTURA §7), nunca en un composable.
    private val list = apps.apps.map(::buildDrawerList).flowOn(Dispatchers.Default)

    private val results = combine(apps.apps, query) { installed, q ->
        if (q.isBlank()) null else AppSearch.filter(installed, q, Locale.getDefault())
    }.flowOn(Dispatchers.Default)

    val uiState: StateFlow<DrawerUiState> =
        combine(list, query, results, favorites.favoriteIds) { list, q, results, favoriteIds ->
            DrawerUiState(list = list, query = q, results = results, favoriteIds = favoriteIds)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), DrawerUiState())

    fun onQueryChange(value: String) {
        query.value = value
    }

    companion object {
        private const val STOP_TIMEOUT_MS = 5_000L

        fun factory(container: AppContainer) = viewModelFactory {
            initializer { DrawerViewModel(container.appsRepository, container.favoritesRepository) }
        }
    }
}
