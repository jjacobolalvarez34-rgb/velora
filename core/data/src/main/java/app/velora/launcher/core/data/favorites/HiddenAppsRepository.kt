package app.velora.launcher.core.data.favorites

import app.velora.launcher.core.model.AppEntry
import app.velora.launcher.core.model.AppKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/** Apps ocultas (PRODUCTO, MVP): fuera de la lista, la búsqueda y los favoritos. */
class HiddenAppsRepository(private val dao: HiddenAppsDao, apps: StateFlow<List<AppEntry>>) {

    private val hiddenIds: Flow<Set<String>> = dao.observe().map { list -> list.mapTo(HashSet()) { it.stableId } }

    /** Todas las apps instaladas menos las ocultas: lo que ve el resto de la app. */
    val visibleApps: Flow<List<AppEntry>> = combine(apps, hiddenIds) { installed, hidden ->
        if (hidden.isEmpty()) installed else installed.filterNot { it.key.stableId in hidden }
    }

    /** Las ocultas que siguen instaladas, para recuperarlas en Ajustes. */
    val hiddenApps: Flow<List<AppEntry>> = combine(apps, hiddenIds) { installed, hidden ->
        installed.filter { it.key.stableId in hidden }
    }

    suspend fun hide(key: AppKey) =
        dao.insert(HiddenAppEntity(key.stableId, key.packageName, key.className, key.userSerial))

    suspend fun unhide(key: AppKey) = dao.delete(key.stableId)

    /** Todas las ocultas, instaladas o no, para el respaldo (ADR-035). */
    suspend fun snapshot(): List<AppKey> =
        dao.observe().first().map { AppKey(it.packageName, it.className, it.userSerial) }

    /** Reemplaza las ocultas por [keys]. Llamar dentro de una transacción. */
    suspend fun restore(keys: List<AppKey>) {
        dao.clear()
        keys.distinctBy { it.stableId }.forEach { hide(it) }
    }
}
