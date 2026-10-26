package app.velora.launcher.feature.drawer

import app.velora.launcher.core.model.AppEntry
import app.velora.launcher.core.model.AppKey
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DrawerListTest {

    private fun app(label: String, section: String) = AppEntry(
        key = AppKey("pkg.$label", "Main", 0),
        label = label,
        section = section,
        searchKey = label.lowercase(),
        isWorkProfile = false,
        canUninstall = true,
    )

    @Test
    fun `un encabezado antes de cada letra nueva`() {
        val list = buildDrawerList(
            listOf(app("123", "#"), app("Banco", "B"), app("Bolt", "B"), app("Nequi", "N"), app("ñequi", "Ñ")),
        )

        assertEquals(listOf("#", "B", "N", "Ñ"), list.letters)
        assertEquals(
            listOf(
                "header:#", "pkg.123", "header:B", "pkg.Banco", "pkg.Bolt",
                "header:N", "pkg.Nequi", "header:Ñ", "pkg.ñequi",
            ),
            list.items.map { it.key.substringBefore('/') },
        )
    }

    @Test
    fun `el alfabeto salta al encabezado de su letra`() {
        val list = buildDrawerList(listOf(app("Banco", "B"), app("Bolt", "B"), app("Nequi", "N")))

        assertEquals(listOf(0, 3), list.letterIndex)
        assertEquals(DrawerItem.Header("N"), list.items[list.letterIndex[1]])
    }

    @Test
    fun `las keys son únicas aunque la misma app esté en dos perfiles`() {
        val personal = app("Chrome", "C")
        val work = personal.copy(key = personal.key.copy(userSerial = 10), isWorkProfile = true)

        val keys = buildDrawerList(listOf(personal, work)).items.map { it.key }

        assertEquals(keys.size, keys.toSet().size)
    }

    @Test
    fun `las recientes van arriba con otra key y el alfabeto salta por encima`() {
        val banco = app("Banco", "B")
        val list = buildDrawerList(listOf(banco, app("Nequi", "N")), recents = listOf(banco))

        assertEquals(DrawerItem.RecentHeader, list.items[0])
        assertEquals("recent:pkg.Banco", list.items[1].key.substringBefore('/'))
        assertEquals(listOf(2, 4), list.letterIndex)
        assertEquals(list.items.size, list.items.map { it.key }.toSet().size)
    }

    @Test
    fun `cada letra tiene sus apps en orden`() {
        val list = buildDrawerList(listOf(app("Banco", "B"), app("Bolt", "B"), app("Nequi", "N")))

        assertEquals(listOf("Banco", "Bolt"), list.byLetter.getValue("B").map { it.label })
        assertEquals(listOf("Nequi"), list.byLetter.getValue("N").map { it.label })
    }

    @Test
    fun `sin apps no hay letras`() {
        assertTrue(buildDrawerList(emptyList()).letters.isEmpty())
    }

    @Test
    fun `con favoritos el alfabeto empieza con la estrella`() {
        val banco = app("Banco", "B")
        val list = buildDrawerList(listOf(banco, app("Nequi", "N")), favorites = listOf(banco))

        assertEquals(listOf(FAVORITES_SECTION, "B", "N"), list.letters)
        assertEquals(0, list.letterIndex.first())
        assertEquals(listOf(banco), list.byLetter[FAVORITES_SECTION])
        // La lista completa no agrega una sección de favoritos: la estrella solo salta arriba.
        assertEquals(listOf("header:B", "header:N"), list.items.filterIsInstance<DrawerItem.Header>().map { it.key })
    }
}
