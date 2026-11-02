package app.velora.launcher.core.data.apps

import app.velora.launcher.core.model.IconShape
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.pow
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class IconDesignMathTest {

    @Test
    fun `los puntos del squircle cumplen la superelipse`() {
        val points = IconShapeMath.superellipse(64)

        assertEquals(128, points.size)
        for (i in 0 until 64) {
            val x = abs(points[i * 2].toDouble())
            val y = abs(points[i * 2 + 1].toDouble())
            assertEquals(1.0, x.pow(4) + y.pow(4), 1e-4)
        }
        // Más "lleno" que un círculo en la diagonal.
        val diagonal = points[16]
        assertTrue(diagonal > 0.8f && diagonal < 0.85f)
    }

    @Test
    fun `la lagrima tiene tres esquinas redondas y una casi recta`() {
        val radii = IconShapeMath.cornerRadii(IconShape.Teardrop, 100f) ?: error("sin radios")

        assertEquals(8, radii.size)
        assertEquals(listOf(50f, 50f, 50f, 50f, 6f, 6f, 50f, 50f), radii.toList())
        assertArrayEquals(FloatArray(8) { 28f }, IconShapeMath.cornerRadii(IconShape.Rounded, 100f), 0.001f)
        assertNull(IconShapeMath.cornerRadii(IconShape.Circle, 100f))
        assertNull(IconShapeMath.cornerRadii(IconShape.Squircle, 100f))
    }

    @Test
    fun `color principal de un logo rojo sobre blanco es rojo`() {
        val red = 0xFFE53935.toInt()
        val white = 0xFFFFFFFF.toInt()
        val pixels = IntArray(100) { if (it < 20) red else white }

        assertEquals(red, IconColorMath.dominant(pixels))
    }

    @Test
    fun `un icono gris da gris y uno transparente no da nada`() {
        val grey = 0xFF808080.toInt()

        assertEquals(grey, IconColorMath.dominant(IntArray(10) { grey }))
        assertNull(IconColorMath.dominant(IntArray(10) { 0x10FF0000 }))
    }

    @Test
    fun `hsv ida y vuelta conserva el color`() {
        listOf(0xFFE53935, 0xFF1E88E5, 0xFF43A047, 0xFFFDD835, 0xFF8E24AA, 0xFF000000, 0xFFFFFFFF).forEach {
            val color = it.toInt()
            val (h, s, v) = IconColorMath.toHsv(color)
            assertEquals(color, IconColorMath.fromHsv(h, s, v))
        }
    }

    @Test
    fun `el glifo se lee sobre claro y oscuro`() {
        assertEquals(IconColorMath.INK, IconColorMath.onColor(0xFFFFF59D.toInt()))
        assertEquals(IconColorMath.WHITE, IconColorMath.onColor(0xFF1A237E.toInt()))
        val (_, s, v) = IconColorMath.toHsv(IconColorMath.vivid(0xFF6D7F99.toInt()))
        assertTrue(s >= 0.6f && v >= 0.95f)
    }

    @Test
    fun `los adornos son deterministas por app y caben en la forma`() {
        val seed = "com.ejemplo.app".hashCode()
        val stars = IconOrnaments.stars(seed)

        assertEquals(stars, IconOrnaments.stars(seed))
        assertNotEquals(stars, IconOrnaments.stars("otra.app".hashCode()))
        assertTrue(stars.size in IconOrnaments.MIN_STARS..IconOrnaments.MAX_STARS)
        stars.forEach {
            val distance = hypot(it.x - 0.5f, it.y - 0.5f)
            assertTrue(distance >= IconOrnaments.STAR_INNER - 1e-4f && distance <= IconOrnaments.STAR_OUTER + 1e-4f)
        }
        val veins = IconOrnaments.veins(seed)
        assertTrue(veins.size in 2..3)
        assertEquals(veins, IconOrnaments.veins(seed))
        veins.forEach { assertTrue(it.startY in 0f..1f && it.endY in 0f..1f) }
    }
}
