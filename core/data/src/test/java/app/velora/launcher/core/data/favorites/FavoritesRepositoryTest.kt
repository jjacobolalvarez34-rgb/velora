package app.velora.launcher.core.data.favorites

import app.velora.launcher.core.model.AppEntry
import app.velora.launcher.core.model.AppKey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class FavoritesRepositoryTest {

    private class FakeDao : FavoritesDao {
        val rows = MutableStateFlow<List<FavoriteEntity>>(emptyList())
        override fun observe() = rows.map { list -> list.sortedBy { it.position } }
        override suspend fun count() = rows.value.size
        override suspend fun maxPosition() = rows.value.maxOfOrNull { it.position } ?: -1
        override suspend fun insert(favorite: FavoriteEntity) {
            rows.value += favorite
        }
        override suspend fun delete(stableId: String) {
            rows.value = rows.value.filterNot { it.stableId == stableId }
        }
        override suspend fun setPosition(stableId: String, position: Int) {
            rows.value = rows.value.map { if (it.stableId == stableId) it.copy(position = position) else it }
        }
    }

    private fun app(label: String) = AppEntry(
        key = AppKey("pkg.$label", "Main", 0),
        label = label,
        section = label.take(1),
        searchKey = label.lowercase(),
        isWorkProfile = false,
        canUninstall = true,
    )

    private val apps = listOf(app("A"), app("B"), app("C"))
    private val repository = FavoritesRepository(FakeDao(), MutableStateFlow(apps))

    private suspend fun order() = repository.favorites.first().map { it.label }

    @Test
    fun `subir y bajar cambia el orden de los favoritos`() = runTest {
        apps.forEach { repository.add(it.key) }

        repository.move(apps[2].key, -1)
        assertEquals(listOf("A", "C", "B"), order())

        repository.move(apps[0].key, 1)
        assertEquals(listOf("C", "A", "B"), order())
    }

    @Test
    fun `en los extremos no hace nada`() = runTest {
        apps.forEach { repository.add(it.key) }

        repository.move(apps[0].key, -1)
        repository.move(apps[2].key, 1)

        assertEquals(listOf("A", "B", "C"), order())
    }
}
