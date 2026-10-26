package app.velora.launcher.core.data.favorites

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class WidgetsRepositoryTest {

    private class FakeDao : WidgetsDao {
        val rows = MutableStateFlow<List<WidgetEntity>>(emptyList())
        override fun observe() = rows.map { list -> list.sortedBy { it.position } }
        override suspend fun maxPosition() = rows.value.maxOfOrNull { it.position } ?: -1
        override suspend fun insert(widget: WidgetEntity) {
            rows.value += widget
        }
        override suspend fun setHeight(appWidgetId: Int, heightDp: Int) {
            rows.value = rows.value.map { if (it.appWidgetId == appWidgetId) it.copy(heightDp = heightDp) else it }
        }
        override suspend fun setPosition(appWidgetId: Int, position: Int) {
            rows.value = rows.value.map { if (it.appWidgetId == appWidgetId) it.copy(position = position) else it }
        }
        override suspend fun delete(appWidgetId: Int) {
            rows.value = rows.value.filterNot { it.appWidgetId == appWidgetId }
        }
    }

    private val repository = WidgetsRepository(FakeDao())

    private suspend fun order() = repository.widgets.first().map { it.appWidgetId }

    @Test
    fun `subir y bajar cambia el orden de los widgets`() = runTest {
        listOf(1, 2, 3).forEach { repository.add(it, "provider", heightDp = 120) }

        repository.move(3, -1)
        assertEquals(listOf(1, 3, 2), order())

        repository.move(1, 1)
        assertEquals(listOf(3, 1, 2), order())
    }

    @Test
    fun `en los extremos no hace nada`() = runTest {
        listOf(1, 2).forEach { repository.add(it, "provider", heightDp = 120) }

        repository.move(1, -1)
        repository.move(2, 1)
        assertEquals(listOf(1, 2), order())
    }
}
