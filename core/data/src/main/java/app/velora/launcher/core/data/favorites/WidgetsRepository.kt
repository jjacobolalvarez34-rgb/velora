package app.velora.launcher.core.data.favorites

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

/** Widgets del inicio en su orden (ADR-020). El `AppWidgetHost` vive en la app; aquí solo la lista. */
class WidgetsRepository(private val dao: WidgetsDao) {

    val widgets: Flow<List<WidgetEntity>> = dao.observe()

    suspend fun add(appWidgetId: Int, provider: String, heightDp: Int) {
        dao.insert(
            WidgetEntity(
                appWidgetId,
                provider,
                heightDp.coerceIn(MIN_HEIGHT_DP, MAX_HEIGHT_DP),
                dao.maxPosition() + 1,
            ),
        )
    }

    suspend fun setHeight(appWidgetId: Int, heightDp: Int) =
        dao.setHeight(appWidgetId, heightDp.coerceIn(MIN_HEIGHT_DP, MAX_HEIGHT_DP))

    suspend fun remove(appWidgetId: Int) = dao.delete(appWidgetId)

    /** Sube (offset -1) o baja (+1) un widget, renumerando todos para que no haya huecos. */
    suspend fun move(appWidgetId: Int, offset: Int) {
        val order = dao.observe().first().map { it.appWidgetId }.toMutableList()
        val from = order.indexOf(appWidgetId)
        val to = from + offset
        if (from < 0 || to !in order.indices) return
        order.add(to, order.removeAt(from))
        order.forEachIndexed { position, id -> dao.setPosition(id, position) }
    }

    /** Widgets en su orden para el respaldo: solo proveedor y alto sirven al restaurar (ADR-035). */
    suspend fun snapshot(): List<WidgetEntity> = dao.observe().first()

    companion object {
        const val MIN_HEIGHT_DP = 64
        const val MAX_HEIGHT_DP = 360
        const val HEIGHT_STEP_DP = 40
    }
}
