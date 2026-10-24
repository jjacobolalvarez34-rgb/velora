package app.velora.launcher.core.data.apps

import app.velora.launcher.core.model.AppEntry
import app.velora.launcher.core.model.AppKey
import java.util.concurrent.TimeUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RecentAppsTest {

    private val now = TimeUnit.DAYS.toMillis(100)

    private fun app(label: String, daysAgo: Long) = AppEntry(
        key = AppKey("pkg.$label", "Main", 0),
        label = label,
        section = label.take(1),
        searchKey = label.lowercase(),
        isWorkProfile = false,
        canUninstall = true,
        installedAtMs = now - TimeUnit.DAYS.toMillis(daysAgo),
    )

    @Test
    fun `las recientes van de la más nueva a la más vieja`() {
        val recents = RecentApps.of(listOf(app("Vieja", 10), app("Nueva", 1), app("Media", 5)), now)

        assertEquals(listOf("Nueva", "Media", "Vieja"), recents.map { it.label })
    }

    @Test
    fun `fuera de los 14 días no cuentan`() {
        assertTrue(RecentApps.of(listOf(app("Antigua", 30)), now).isEmpty())
    }

    @Test
    fun `nunca más de cuatro`() {
        val many = (1L..9L).map { app("App$it", it) }

        assertEquals(RecentApps.MAX, RecentApps.of(many, now).size)
    }

    @Test
    fun `sin fecha de instalación no aparece`() {
        val unknown = app("SinFecha", 0).copy(installedAtMs = 0L)

        assertTrue(RecentApps.of(listOf(unknown), now).isEmpty())
    }
}
