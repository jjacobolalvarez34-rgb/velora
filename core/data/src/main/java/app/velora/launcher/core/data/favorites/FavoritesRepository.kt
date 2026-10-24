package app.velora.launcher.core.data.favorites

import android.content.Context
import app.velora.launcher.core.model.AppEntry
import app.velora.launcher.core.model.AppKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

sealed interface AddFavoriteResult {
    data object Added : AddFavoriteResult
    data object Full : AddFavoriteResult
}

/** Favoritos del inicio (PRODUCTO: 8 sugeridos, máximo 12). */
class FavoritesRepository(private val dao: FavoritesDao, apps: StateFlow<List<AppEntry>>) {
    /** Ids de los favoritos, para marcar "Quitar de favoritos" en los menús. */
    val favoriteIds: Flow<Set<String>> = dao.observe().map { list -> list.mapTo(HashSet()) { it.stableId } }

    /** Favoritos en su orden, solo los que siguen instalados. */
    val favorites: Flow<List<AppEntry>> = combine(dao.observe(), apps) { favorites, installed ->
        val byId = installed.associateBy { it.key.stableId }
        favorites.mapNotNull { byId[it.stableId] }
    }

    suspend fun add(key: AppKey): AddFavoriteResult {
        if (dao.count() >= MAX_FAVORITES) return AddFavoriteResult.Full
        dao.insert(
            FavoriteEntity(
                stableId = key.stableId,
                packageName = key.packageName,
                className = key.className,
                userSerial = key.userSerial,
                position = dao.maxPosition() + 1,
            ),
        )
        return AddFavoriteResult.Added
    }

    suspend fun remove(key: AppKey) = dao.delete(key.stableId)

    companion object {
        const val MAX_FAVORITES = 12

        /** Crea el repositorio sobre la base de datos de Velora (Room queda dentro de `core:data`). */
        fun create(context: Context, apps: StateFlow<List<AppEntry>>): FavoritesRepository =
            FavoritesRepository(VeloraDatabase.create(context).favorites(), apps)
    }
}
