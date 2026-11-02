package app.velora.launcher

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Velora solo cambia Velora (ADR-037): nunca el fondo, los íconos, los temas ni los ajustes del
 * teléfono. Al desinstalarla, el teléfono queda como estaba. Esta prueba falla si algún archivo
 * del código usa una API que escriba en el sistema.
 */
class SystemUntouchedTest {

    private val forbidden = listOf(
        "WallpaperManager.getInstance(" to listOf("setBitmap", "setStream", "setResource", "clear(", "clearWallpaper"),
        "ACTION_SET_WALLPAPER" to emptyList(),
        "SET_WALLPAPER" to emptyList(),
        "Settings.System.put" to emptyList(),
        "Settings.Secure.put" to emptyList(),
        "Settings.Global.put" to emptyList(),
        "WRITE_SETTINGS" to emptyList(),
    )

    private val sources = listOf(File("src/main"), File("../core/data/src/main"), File("../core/designsystem/src/main"))
        .flatMap { root -> root.walkTopDown().filter { it.extension in setOf("kt", "xml") }.toList() }

    @Test
    fun `hay código que revisar`() {
        assertTrue("No se encontraron fuentes: ¿cambió la carpeta de trabajo de las pruebas?", sources.size > 50)
    }

    @Test
    fun `ningún archivo escribe en el fondo, los temas ni los ajustes del sistema`() {
        val offenders = sources.mapNotNull { file ->
            val text = file.readText()
            val hit = forbidden.firstOrNull { (api, writes) ->
                if (writes.isEmpty()) api in text else api in text && writes.any { ".$it" in text }
            }
            hit?.let { "${file.path}: ${it.first}" }
        }
        assertTrue("Velora no debe tocar el sistema:\n" + offenders.joinToString("\n"), offenders.isEmpty())
    }
}
