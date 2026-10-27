package app.velora.launcher.core.data.prefs

import app.velora.launcher.core.model.ClockStyle
import app.velora.launcher.core.model.FontChoice
import app.velora.launcher.core.model.IconStyle
import app.velora.launcher.core.model.ThemePreset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ThemeFileTest {

    @Test
    fun `un tema se exporta y se vuelve a leer igual`() {
        val theme =
            SharedTheme(ThemePreset.Galaxy, FontChoice.Cinzel, ClockStyle.Day, IconStyle.Minimal, byteArrayOf(1, 2, 3))

        assertEquals(theme, ThemeFile.decode(ThemeFile.encode(theme)))
    }

    @Test
    fun `lo desconocido se ignora sin romper`() {
        val text = "${ThemeFile.HEADER}\npreset=Neon\nfont=Inexistente\nicons=Minimal\nscript=rm -rf\nphoto=%%%"

        val theme = ThemeFile.decode(text)

        assertEquals(ThemePreset.Neon, theme?.preset)
        assertNull(theme?.font)
        assertEquals(IconStyle.Minimal, theme?.icons)
        assertNull(theme?.photo)
    }

    @Test
    fun `un archivo ajeno o sin tema se rechaza`() {
        assertNull(ThemeFile.decode("{\"theme\": \"otro launcher\"}"))
        assertNull(ThemeFile.decode("${ThemeFile.HEADER}\nfont=Nunito"))
    }
}
