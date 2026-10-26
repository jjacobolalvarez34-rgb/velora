package app.velora.launcher.core.data.apps

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GlyphMatcherTest {

    private val noCategory = -1

    @Test
    fun `las apps conocidas usan su logo`() {
        assertEquals("brand-whatsapp", GlyphMatcher.match("com.whatsapp", "WhatsApp", noCategory))
        assertEquals("crown", GlyphMatcher.match("com.supercell.clashroyale", "Clash Royale", 0))
    }

    @Test
    fun `por el nombre aunque tenga tildes o esté en español`() {
        assertEquals("camera", GlyphMatcher.match("com.miui.camera", "Cámara", noCategory))
        assertEquals("book", GlyphMatcher.match("com.sirma.mobile.bible.android", "Biblia", noCategory))
        assertEquals("lock", GlyphMatcher.match("com.miui.securitycore", "Bóveda de aplicaciones", noCategory))
        assertEquals("chess", GlyphMatcher.match("org.lichess.mobile", "Ajedrez", noCategory))
    }

    @Test
    fun `por la categoría que declara la app`() {
        assertEquals("device-gamepad-2", GlyphMatcher.match("com.example.x", "Aventura", 0))
    }

    @Test
    fun `sin pistas no inventa, se dibuja la inicial`() {
        assertNull(GlyphMatcher.match("com.example.atria", "Atria", noCategory))
    }

    @Test
    fun `todos los íconos elegidos existen en el archivo de trazos`() {
        val asset = File("src/main/assets/glyphs/tabler.tsv")
        val available = asset.readLines().map { it.substringBefore('\t') }.toSet()
        val probes = listOf(
            Triple("com.whatsapp", "", noCategory),
            Triple("x", "Cámara", noCategory),
            Triple("x", "Reloj", noCategory),
            Triple("x", "Calculadora", noCategory),
            Triple("x", "Archivos", noCategory),
            Triple("x", "Biblia", noCategory),
            Triple("x", "Aventura", 0),
            Triple("x", "Social", 4),
        )
        probes.forEach { (pkg, label, category) ->
            val name = GlyphMatcher.match(pkg, label, category)
            assertTrue("$name no está en tabler.tsv", name in available)
        }
    }
}
