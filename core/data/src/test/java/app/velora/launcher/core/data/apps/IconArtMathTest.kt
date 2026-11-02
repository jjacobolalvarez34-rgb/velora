package app.velora.launcher.core.data.apps

import app.velora.launcher.core.model.IconShape
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class IconArtMathTest {

    private companion object {
        const val OPAQUE = 0xFF000000.toInt()
        const val SEED = 0x5EED
    }

    @Test
    fun `el pixelado enciende solo las celdas cubiertas`() {
        // Lienzo de 32×32 con la cuadrícula de 16 en todo el lado: celdas de 2×2 px.
        val width = 32
        val pixels = IntArray(width * width) { i ->
            val x = i % width
            val y = i / width
            if (x in 8 until 16 && y in 4 until 6) OPAQUE else 0
        }

        val cells = PixelArt.cells(pixels, width, GridWindow(start = 0f, area = 32f))

        assertEquals(PixelArt.GRID * PixelArt.GRID, cells.size)
        val lit = cells.indices.filter { cells[it] }.map { it % PixelArt.GRID to it / PixelArt.GRID }
        assertEquals(listOf(4 to 2, 5 to 2, 6 to 2, 7 to 2), lit)
    }

    @Test
    fun `el pixelado respeta el umbral y el margen`() {
        val width = 20
        // Media opacidad en todo el lienzo: por encima de 0.4 se enciende, por debajo no.
        val half = IntArray(width * width) { 0x80000000.toInt() }
        assertTrue(PixelArt.cells(half, width, GridWindow(2f, 16f, grid = 4)).all { it })
        assertFalse(PixelArt.cells(half, width, GridWindow(2f, 16f, grid = 4), threshold = 0.6f).any { it })
        // Lo que cae fuera del cuadrado de la cuadrícula no cuenta.
        val border = IntArray(width * width) { i -> if (i % width < 2) OPAQUE else 0 }
        assertFalse(PixelArt.cells(border, width, GridWindow(2f, 16f, grid = 4)).any { it })
    }

    @Test
    fun `el borde de cada forma cae donde corresponde`() {
        assertEquals(1f, IconBlob.boundary(IconShape.Circle, 0.7), 1e-5f)
        // Squircle y redondeado llegan al lado en los ejes y van más allá del círculo en la diagonal.
        val diagonal = Math.PI / 4
        for (shape in listOf(IconShape.Squircle, IconShape.Rounded)) {
            assertEquals(1f, IconBlob.boundary(shape, 0.0), 1e-5f)
            assertEquals(1f, IconBlob.boundary(shape, Math.PI / 2), 1e-5f)
            assertTrue(IconBlob.boundary(shape, diagonal) in 1.05f..1.42f)
        }
        // Redondeado: arco de radio 0.56 centrado en (0.44, 0.44).
        assertEquals(0.44f * 1.41421f + 0.56f, IconBlob.boundary(IconShape.Rounded, diagonal), 1e-3f)
        // Lágrima: punta abajo a la derecha, redonda en las otras tres esquinas.
        assertTrue(IconBlob.boundary(IconShape.Teardrop, diagonal) > 1.3f)
        assertEquals(1f, IconBlob.boundary(IconShape.Teardrop, -diagonal), 1e-5f)
    }

    @Test
    fun `las manchas de acuarela son deterministas por app`() {
        val washes = IconBlob.washes(IconShape.Squircle, SEED, 64)
        val again = IconBlob.washes(IconShape.Squircle, SEED, 64)
        val other = IconBlob.washes(IconShape.Squircle, SEED + 1, 64)

        assertEquals(IconBlob.WASHES, washes.size)
        washes.zip(again).forEach { (a, b) ->
            assertArrayEquals(a.radii, b.radii, 0f)
            assertEquals(a.dx, b.dx, 0f)
        }
        assertNotEquals(washes[0].radii.toList(), other[0].radii.toList())
        // Las capas de una misma app no son iguales entre sí.
        assertNotEquals(washes[0].radii.toList(), washes[1].radii.toList())
    }

    @Test
    fun `las manchas de acuarela quedan dentro de la forma y del icono`() {
        for (shape in IconShape.entries) {
            for (seed in listOf(1, -7, SEED, "com.ejemplo.app".hashCode())) {
                IconBlob.washes(shape, seed, 72).forEach { assertWashFits(shape, it) }
            }
        }
    }

    private fun assertWashFits(shape: IconShape, wash: Wash) {
        assertTrue(abs(wash.dx) <= IconBlob.WASH_DRIFT && abs(wash.dy) <= IconBlob.WASH_DRIFT)
        var minRatio = 1f
        wash.radii.forEachIndexed { i, r ->
            val angle = 2 * Math.PI * i / wash.radii.size
            val edge = IconBlob.boundary(shape, angle)
            val ratio = r / edge
            assertTrue("se sale de la forma", ratio <= 1f + 1e-5f)
            assertTrue("entra demasiado", ratio >= 1f - IconBlob.ROUGHNESS - 1e-5f)
            minRatio = minOf(minRatio, ratio)
            val x = (cos(angle) * r * IconBlob.WASH_SCALE + wash.dx).toFloat()
            val y = (sin(angle) * r * IconBlob.WASH_SCALE + wash.dy).toFloat()
            assertTrue("se sale del ícono", abs(x) <= 1f && abs(y) <= 1f)
        }
        // Tiene ruido de verdad: no es la forma lisa.
        assertTrue(minRatio < 1f - IconBlob.ROUGHNESS / 4)
    }

    @Test
    fun `la paleta da glifos legibles`() {
        val pureBlue = 0xFF0000FF.toInt()
        assertTrue(IconColorMath.luminance(IconPalette.bright(pureBlue)) >= IconPalette.BRIGHT_MIN_LUMINANCE)
        // Ya legible: no cambia; ilegible: cambia de lado conservando el tono.
        val navy = 0xFF1A237E.toInt()
        assertEquals(navy, IconPalette.contrasting(navy, IconColorMath.WHITE))
        val fixed = IconPalette.contrasting(navy, IconColorMath.INK)
        assertTrue(IconPalette.isLight(fixed))
        val (navyHue) = IconColorMath.toHsv(navy)
        assertEquals(navyHue, IconColorMath.toHsv(fixed)[0], 12f)
        // El tono profundo se lee sobre el pastel del mismo color (contraste de gráficos ≥ 3:1).
        listOf(0xFFFDD835, 0xFF43A047, 0xFF8E24AA, 0xFF1E88E5, 0xFF808080, 0xFF1C1B20).forEach {
            val color = it.toInt()
            assertTrue(contrast(IconPalette.pastel(color), IconPalette.deep(color)) >= 3.0)
            assertFalse(IconPalette.isLight(IconPalette.deep(color)))
        }
    }

    private fun contrast(a: Int, b: Int): Double {
        val la = IconColorMath.luminance(a)
        val lb = IconColorMath.luminance(b)
        return (maxOf(la, lb) + 0.05) / (minOf(la, lb) + 0.05)
    }

    @Test
    fun `el degradado va de claro a profundo y el neon es luminoso`() {
        listOf(0xFFE53935, 0xFF1E88E5, 0xFF1C1B20, 0xFFF5F5F7).forEach {
            val (light, dark) = IconPalette.gradientStops(it.toInt())
            assertTrue(IconColorMath.luminance(light) > IconColorMath.luminance(dark))
            assertEquals(1f, IconColorMath.toHsv(IconPalette.neon(it.toInt()))[2], 0.01f)
        }
    }
}
