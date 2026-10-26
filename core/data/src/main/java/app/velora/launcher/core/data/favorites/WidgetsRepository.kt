package app.velora.launcher.core.data.favorites

import kotlinx.coroutines.flow.Flow

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

    companion object {
        const val MIN_HEIGHT_DP = 64
        const val MAX_HEIGHT_DP = 360
        const val HEIGHT_STEP_DP = 40
    }
}
