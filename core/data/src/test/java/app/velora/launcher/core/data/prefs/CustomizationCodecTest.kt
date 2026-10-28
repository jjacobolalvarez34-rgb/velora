package app.velora.launcher.core.data.prefs

import app.velora.launcher.core.model.Customization
import app.velora.launcher.core.model.IconColor
import app.velora.launcher.core.model.IconOptions
import app.velora.launcher.core.model.IconPack
import app.velora.launcher.core.model.IconShape
import app.velora.launcher.core.model.VisibleElements
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CustomizationCodecTest {

    @Test
    fun `la personalización se guarda y se lee igual`() {
        val custom = Customization(
            icons = IconOptions(IconPack.Sticker, IconShape.Squircle, IconColor.App, 2.5f, true, "com.example.icons"),
            accentColor = 0xFFFF2BD6,
            visible = VisibleElements(clock = true, date = false, statusBar = false, tips = false),
            dismissedTips = setOf("theme", "favorites"),
            usageLimits = mapOf("com.zhiliaoapp.musically" to 60, "com.instagram.android" to 30),
        )

        assertEquals(custom, CustomizationCodec.decode(CustomizationCodec.encode(custom)))
    }

    @Test
    fun `vacío o desconocido vuelve a lo de fábrica`() {
        assertEquals(Customization(), CustomizationCodec.decode(null))
        assertEquals(Customization(), CustomizationCodec.decode("icons.pack=Inventado\nicons.weight=99x"))
    }

    @Test
    fun `el grosor se limita y un paquete raro se descarta`() {
        val custom = CustomizationCodec.decode("icons.weight=40\nicons.thirdParty=../../etc")

        assertEquals(IconOptions.MAX_WEIGHT, custom.icons.weight)
        assertNull(custom.icons.thirdPartyPack)
    }
}
