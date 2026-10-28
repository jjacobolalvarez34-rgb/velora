package app.velora.launcher.core.data.apps

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AppFilterTest {

    private fun item(component: String, drawable: String) =
        XmlElement("item", mapOf("component" to component, "drawable" to drawable))

    @Test
    fun `lee los componentes y resuelve por paquete y clase`() {
        val filter = AppFilter.from(
            listOf(
                XmlElement("resources", emptyMap()),
                item("ComponentInfo{com.android.chrome/com.google.android.apps.chrome.Main}", "chrome"),
                item("ComponentInfo{org.telegram.messenger/org.telegram.ui.LaunchActivity}", "telegram"),
            ),
        )

        assertEquals("chrome", filter.drawableFor("com.android.chrome", "com.google.android.apps.chrome.Main"))
        assertEquals("telegram", filter.drawableFor("org.telegram.messenger", "otra.Clase"))
        assertNull(filter.drawableFor("com.desconocida", "X"))
        assertFalse(filter.canCompose)
    }

    @Test
    fun `clases relativas y espacios se normalizan`() {
        assertEquals("a.b" to "a.b.Main", AppFilter.parseComponent(" ComponentInfo{ a.b / .Main } "))
        assertEquals("a.b" to "c.D", AppFilter.parseComponent("ComponentInfo{a.b/c.D}"))
    }

    @Test
    fun `componentes rotos se ignoran sin fallar`() {
        assertNull(AppFilter.parseComponent("ComponentInfo{}"))
        assertNull(AppFilter.parseComponent("ComponentInfo{solo.paquete}"))
        assertNull(AppFilter.parseComponent("ComponentInfo{/Clase}"))
        assertNull(AppFilter.parseComponent("ComponentInfo{a/b/c}"))

        val filter = AppFilter.from(
            listOf(
                XmlElement("item", mapOf("component" to "basura")),
                item("ComponentInfo{a.b/a.b.Main}", "  "),
                XmlElement("calendar", mapOf("prefix" to "cal_")),
            ),
        )
        assertTrue(filter.components.isEmpty())
    }

    @Test
    fun `el primero de un componente repetido gana`() {
        val filter = AppFilter.from(
            listOf(item("ComponentInfo{a.b/a.b.Main}", "uno"), item("ComponentInfo{a.b/a.b.Main}", "dos")),
        )

        assertEquals("uno", filter.drawableFor("a.b", "a.b.Main"))
    }

    @Test
    fun `lee las piezas para componer`() {
        val filter = AppFilter.from(
            listOf(
                XmlElement("iconback", mapOf("img2" to "back2", "img1" to "back1")),
                XmlElement("iconmask", mapOf("img1" to "mask")),
                XmlElement("iconupon", mapOf("img1" to "upon")),
                XmlElement("scale", mapOf("factor" to "0.8")),
            ),
        )

        assertEquals(listOf("back1", "back2"), filter.backs)
        assertEquals("mask", filter.mask)
        assertEquals("upon", filter.upon)
        assertEquals(0.8f, filter.scale, 0.0001f)
        assertTrue(filter.canCompose)
        assertEquals(filter.backFor("com.x"), filter.backFor("com.x"))
    }

    @Test
    fun `una escala absurda se acota y una ilegible usa la de siempre`() {
        fun scaleOf(raw: String) = AppFilter.from(listOf(XmlElement("scale", mapOf("factor" to raw)))).scale

        assertEquals(1f, scaleOf("7"), 0f)
        assertEquals(0.1f, scaleOf("-3"), 0f)
        assertEquals(AppFilter.DEFAULT_SCALE, scaleOf("NaN"), 0f)
        assertEquals(AppFilter.DEFAULT_SCALE, scaleOf("grande"), 0f)
    }
}
