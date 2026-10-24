package app.velora.launcher.core.data.apps

import app.velora.launcher.core.model.AppEntry
import app.velora.launcher.core.model.AppKey
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Test

class AppSortingTest {

    private val es = Locale.forLanguageTag("es-CO")
    private val en = Locale.ENGLISH

    private fun app(label: String, locale: Locale = es) = AppEntry(
        key = AppKey("pkg.$label", "Main", 0),
        label = label,
        section = AppSorting.sectionOf(label, locale),
        searchKey = AppSorting.searchKeyOf(label, locale),
        isWorkProfile = false,
        canUninstall = true,
    )

    @Test
    fun `B-009 orden en español con tildes, ñ, números y emojis`() {
        val labels = listOf("Zoom", "ñequi", "Banco", "😀 App", "álbum", "Nequi", "123 Juego")

        val sorted = AppSorting.sorted(labels.map { app(it) }, es).map { it.label }

        // Los símbolos van primero; entre ellos el orden lo decide el Collator de la plataforma.
        assertEquals(setOf("123 Juego", "😀 App"), sorted.take(2).toSet())
        assertEquals(listOf("álbum", "Banco", "Nequi", "ñequi", "Zoom"), sorted.drop(2))
    }

    @Test
    fun `B-009 secciones normalizadas sin tildes y con Ñ propia en español`() {
        assertEquals("A", AppSorting.sectionOf("álbum", es))
        assertEquals("Ñ", AppSorting.sectionOf("ñequi", es))
        assertEquals("N", AppSorting.sectionOf("Nequi", es))
        assertEquals("#", AppSorting.sectionOf("123 Juego", es))
        assertEquals("#", AppSorting.sectionOf("😀 App", es))
        assertEquals("E", AppSorting.sectionOf("  éxito", es))
    }

    @Test
    fun `fuera del español la ñ va con la N`() {
        assertEquals("N", AppSorting.sectionOf("ñandu", en))
    }

    @Test
    fun `minúsculas y mayúsculas se ordenan juntas`() {
        val sorted = AppSorting.sorted(listOf(app("banco"), app("Avianca"), app("Banco B")), es).map { it.label }

        assertEquals(listOf("Avianca", "banco", "Banco B"), sorted)
    }

    @Test
    fun `la clave de búsqueda quita tildes y mayúsculas`() {
        assertEquals("camara", AppSorting.searchKeyOf("Cámara", es))
        assertEquals("nequi", AppSorting.searchKeyOf("Ñequi", es))
    }
}
