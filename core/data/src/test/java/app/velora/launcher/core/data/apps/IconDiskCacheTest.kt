package app.velora.launcher.core.data.apps

import java.io.File
import java.nio.file.Files
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class IconDiskCacheTest {

    private val dir: File = Files.createTempDirectory("icons").toFile()
    private val cache = IconDiskCache(dir)

    private fun icon(packageName: String) = File(dir, "${IconDiskCache.packagePrefix(packageName)}a_0_Circle_ff@96.png")
        .apply { writeText("png") }

    @Test
    fun `la primera vez solo guarda las versiones`() {
        val camera = icon("cam")

        assertTrue(cache.syncVersions(mapOf("cam" to "/data/app/v1")).isEmpty())
        assertTrue(camera.exists())
    }

    @Test
    fun `una app actualizada con Velora cerrado pierde su ícono viejo`() {
        cache.syncVersions(mapOf("cam" to "/data/app/v1", "clock" to "/data/app/c1"))
        val camera = icon("cam")
        val clock = icon("clock")

        val changed = cache.syncVersions(mapOf("cam" to "/data/app/v2", "clock" to "/data/app/c1"))

        assertEquals(setOf("cam"), changed)
        assertFalse(camera.exists())
        assertTrue(clock.exists())
    }

    @Test
    fun `una app desinstalada también se limpia`() {
        cache.syncVersions(mapOf("cam" to "/data/app/v1"))
        val camera = icon("cam")

        assertEquals(setOf("cam"), cache.syncVersions(emptyMap()))
        assertFalse(camera.exists())
    }

    @Test
    fun `borrar un paquete no toca otro que empieza igual`() {
        val cam = icon("cam")
        val camera = icon("camera")

        cache.deletePackage("cam")

        assertFalse(cam.exists())
        assertTrue(camera.exists())
    }
}
