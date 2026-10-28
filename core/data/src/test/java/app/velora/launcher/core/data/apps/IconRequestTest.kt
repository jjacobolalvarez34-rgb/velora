package app.velora.launcher.core.data.apps

import app.velora.launcher.core.model.AppKey
import app.velora.launcher.core.model.IconColor
import app.velora.launcher.core.model.IconOptions
import app.velora.launcher.core.model.IconPack
import app.velora.launcher.core.model.IconShape
import app.velora.launcher.core.model.IconStyle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class IconRequestTest {

    private companion object {
        const val BLACK = 0xFF000000.toInt()
    }

    private val key = AppKey("com.ejemplo", "com.ejemplo.Main", 0)

    private fun name(style: IconStyle, options: IconOptions = IconOptions(), accent: Int = 0) =
        IconRequest(key, 96, style, tint = BLACK, backdrop = -1, options = options, accent = accent).fileName

    @Test
    fun `cambiar cualquier opcion de Velora cambia la clave`() {
        val base = name(IconStyle.Minimal)
        val variants = listOf(
            name(IconStyle.Minimal, IconOptions(pack = IconPack.Marble)),
            name(IconStyle.Minimal, IconOptions(shape = IconShape.Squircle)),
            name(IconStyle.Minimal, IconOptions(color = IconColor.App)),
            name(IconStyle.Minimal, IconOptions(weight = 2.5f)),
            name(IconStyle.Minimal, IconOptions(softFill = true)),
            name(IconStyle.Minimal, accent = 0xFF00FF00.toInt()),
        )

        variants.forEach { assertNotEquals(base, it) }
        assertEquals(variants.size, variants.toSet().size)
    }

    @Test
    fun `la clave es estable y empieza por el paquete`() {
        val options = IconOptions(pack = IconPack.Stellar, shape = IconShape.Teardrop)

        assertEquals(name(IconStyle.Minimal, options), name(IconStyle.Minimal, options.copy()))
        assertTrue(name(IconStyle.Minimal).startsWith(IconDiskCache.packagePrefix("com.ejemplo")))
        assertTrue(name(IconStyle.Minimal).endsWith("@96.png"))
    }

    @Test
    fun `el recorte del original solo depende de la forma`() {
        assertEquals(name(IconStyle.Circle), name(IconStyle.Circle, IconOptions(pack = IconPack.Glass)))
        assertNotEquals(name(IconStyle.Circle), name(IconStyle.Circle, IconOptions(shape = IconShape.Rounded)))
        assertNotEquals(
            name(IconStyle.Pack, IconOptions(thirdPartyPack = "a.pack")),
            name(IconStyle.Pack, IconOptions(thirdPartyPack = "b.pack")),
        )
    }

    @Test
    fun `la clave no depende de hashCode de enums`() {
        assertEquals(
            "Line|Circle|Theme|2.0|false|4278190080|4294967295|0",
            variantKey(IconStyle.Minimal, IconOptions(), 0xFF000000.toInt(), -1, 0),
        )
    }
}
