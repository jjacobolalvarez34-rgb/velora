package app.velora.launcher.core.data.prefs

import app.velora.launcher.core.model.ClockStyle
import app.velora.launcher.core.model.FontChoice
import app.velora.launcher.core.model.IconStyle
import app.velora.launcher.core.model.TextSize
import app.velora.launcher.core.model.ThemePreset
import app.velora.launcher.core.model.ThemeSettings
import org.junit.Assert.assertEquals
import org.junit.Test

class ThemeDecodingTest {

    @Test
    fun `sin preferencias guardadas se usa el aspecto de fábrica`() {
        assertEquals(ThemeSettings(), StoredTheme().decode())
    }

    @Test
    fun `lee lo que se guardó`() {
        assertEquals(
            ThemeSettings(FontChoice.Fraunces, ClockStyle.Day, IconStyle.Minimal),
            StoredTheme("Fraunces", "Day", "Minimal").decode(),
        )
    }

    @Test
    fun `tamaño de texto y modo zurdo`() {
        val theme = StoredTheme(textSize = "Large", leftHanded = true).decode()

        assertEquals(TextSize.Large, theme.textSize)
        assertEquals(true, theme.leftHanded)
    }

    @Test
    fun `lee el tema completo guardado`() {
        assertEquals(ThemePreset.Sakura, StoredTheme(preset = "Sakura").decode().preset)
    }

    @Test
    fun `cada tema recomienda fuente, reloj e íconos y se marca a sí mismo`() {
        val pixel = ThemePreset.PixelNight.recommended

        assertEquals(FontChoice.Pixel, pixel.font)
        assertEquals(ThemePreset.PixelNight, pixel.preset)
        ThemePreset.entries.forEach { assertEquals(it, it.recommended.preset) }
    }

    @Test
    fun `foto propia, notificaciones y doble toque`() {
        val theme = StoredTheme(
            photoBackground = "/data/fondo.jpg",
            notifications = true,
            doubleTapLock = true,
        ).decode()

        assertEquals("/data/fondo.jpg", theme.photoBackground)
        assertEquals(true, theme.notifications)
        assertEquals(true, theme.doubleTapLock)
    }

    @Test
    fun `un valor desconocido de otra versión no rompe el arranque`() {
        assertEquals(ThemeSettings(), StoredTheme("Manrope", "Retro", "Square").decode())
    }
}
