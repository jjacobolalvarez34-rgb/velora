package app.velora.launcher.core.data.favorites

import app.velora.launcher.core.model.AppEntry
import app.velora.launcher.core.model.AppKey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class HiddenAppsRepositoryTest {

    private class FakeDao : HiddenAppsDao {
        val rows = MutableStateFlow<List<HiddenAppEntity>>(emptyList())
        override fun observe() = rows
        override suspend fun insert(app: HiddenAppEntity) {
            if (rows.value.none { it.stableId == app.stableId }) rows.value += app
        }
        override suspend fun delete(stableId: String) {
            rows.value = rows.value.filterNot { it.stableId == stableId }
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

    private val camera = app("Cámara")
    private val clock = app("Reloj")
    private val repository = HiddenAppsRepository(FakeDao(), MutableStateFlow(listOf(camera, clock)))

    @Test
    fun `una app oculta sale de las visibles y entra en las ocultas`() = runTest {
        repository.hide(camera.key)

        assertEquals(listOf(clock), repository.visibleApps.first())
        assertEquals(listOf(camera), repository.hiddenApps.first())
    }

    @Test
    fun `mostrarla de nuevo la devuelve a la lista`() = runTest {
        repository.hide(camera.key)
        repository.unhide(camera.key)

        assertEquals(listOf(camera, clock), repository.visibleApps.first())
    }
}
