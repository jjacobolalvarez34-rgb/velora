package app.velora.launcher.core.data.favorites

import app.velora.launcher.core.model.AppEntry
import app.velora.launcher.core.model.AppKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
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

    /** Sube (-1) o baja (+1) un favorito; en los extremos no hace nada. */
    suspend fun move(key: AppKey, offset: Int) {
        val order = dao.observe().first().map { it.stableId }.toMutableList()
        val from = order.indexOf(key.stableId)
        val to = from + offset
        if (from < 0 || to !in order.indices) return
        order.add(to, order.removeAt(from))
        order.forEachIndexed { position, id -> dao.setPosition(id, position) }
    }

    /** Favoritos en su orden, instalados o no, para el respaldo (ADR-035). */
    suspend fun snapshot(): List<AppKey> =
        dao.observe().first().map { AppKey(it.packageName, it.className, it.userSerial) }

    /** Reemplaza los favoritos por [keys], en ese orden. Llamar dentro de una transacción. */
    suspend fun restore(keys: List<AppKey>) {
        dao.clear()
        keys.distinctBy { it.stableId }.take(MAX_FAVORITES).forEachIndexed { position, key ->
            dao.insert(FavoriteEntity(key.stableId, key.packageName, key.className, key.userSerial, position))
        }
    }

    companion object {
        const val MAX_FAVORITES = 12
    }
}
