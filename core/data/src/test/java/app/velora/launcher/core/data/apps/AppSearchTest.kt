package app.velora.launcher.core.data.apps

import app.velora.launcher.core.model.AppEntry
import app.velora.launcher.core.model.AppKey
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AppSearchTest {

    private val es = Locale.forLanguageTag("es-CO")

    private fun apps(vararg labels: String) = labels.map { label ->
        AppEntry(
            key = AppKey("pkg.$label", "Main", 0),
            label = label,
            section = AppSorting.sectionOf(label, es),
            searchKey = AppSorting.searchKeyOf(label, es),
            isWorkProfile = false,
            canUninstall = true,
        )
    }

    @Test
    fun `buscar sin tildes encuentra Cámara`() {
        val result = AppSearch.filter(apps("Calculadora", "Cámara", "Reloj"), "camara", es)

        assertEquals(listOf("Cámara"), result.map { it.label })
    }

    @Test
    fun `primero lo que empieza por la búsqueda, luego palabras y luego el resto`() {
        val result = AppSearch.filter(apps("Google Maps", "Mapas", "Bitmaps", "Reloj"), "map", es)

        assertEquals(listOf("Mapas", "Google Maps", "Bitmaps"), result.map { it.label })
    }

    @Test
    fun `una búsqueda vacía no devuelve nada`() {
        assertTrue(AppSearch.filter(apps("Reloj"), "   ", es).isEmpty())
    }

    @Test
    fun `filtrar 300 apps es rápido`() {
        val many = apps(*Array(300) { "Aplicación número $it" })
        repeat(20) { AppSearch.filter(many, "numero 1", es) } // calentar la JVM

        val start = System.nanoTime()
        AppSearch.filter(many, "numero 1", es)
        val millis = (System.nanoTime() - start) / 1_000_000

        assertTrue("tardó $millis ms", millis < 16)
    }
}
