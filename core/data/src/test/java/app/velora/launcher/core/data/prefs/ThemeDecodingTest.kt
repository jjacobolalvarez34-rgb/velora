package app.velora.launcher.core.data.prefs

import app.velora.launcher.core.model.ClockStyle
import app.velora.launcher.core.model.FontChoice
import app.velora.launcher.core.model.IconStyle
import app.velora.launcher.core.model.ThemeSettings
import org.junit.Assert.assertEquals
import org.junit.Test

class ThemeDecodingTest {

    @Test
    fun `sin preferencias guardadas se usa el aspecto de fábrica`() {
        assertEquals(ThemeSettings(), decodeTheme(null, null, null))
    }

    @Test
    fun `lee lo que se guardó`() {
        assertEquals(
            ThemeSettings(FontChoice.Fraunces, ClockStyle.Day, IconStyle.Minimal),
            decodeTheme("Fraunces", "Day", "Minimal"),
        )
    }

    @Test
    fun `un valor desconocido de otra versión no rompe el arranque`() {
        assertEquals(ThemeSettings(), decodeTheme("Manrope", "Retro", "Square"))
    }
}
