package app.velora.launcher.feature.home.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Hora en palabras y lectura de la hora para los relojes Analógico y Palabras. */
class ClockWordsTest {

    @Test
    fun `la hora se lee en 24 y 12 horas`() {
        assertEquals(7 to 58, clockParts("07:58"))
        assertEquals(19 to 5, clockParts("19:05"))
        assertEquals(7 to 58, clockParts("7:58 p. m."))
    }

    @Test
    fun `sin numeros no hay hora que leer`() {
        assertNull(clockParts("--"))
        assertNull(clockParts(""))
    }

    @Test
    fun `en punto y media cuentan desde la hora actual`() {
        assertEquals(7 to 0, wordsIndex(7, 0))
        assertEquals(7 to 0, wordsIndex(7, 2))
        assertEquals(7 to 6, wordsIndex(7, 30))
        assertEquals(7 to 3, wordsIndex(19, 15))
    }

    @Test
    fun `pasada la media se cuenta hacia la hora siguiente`() {
        // "veinticinco para las ocho"
        assertEquals(8 to 7, wordsIndex(7, 33))
        // "cuarto para las doce"
        assertEquals(0 to 9, wordsIndex(23, 45))
    }

    @Test
    fun `casi en punto redondea a la hora siguiente`() {
        assertEquals(8 to 0, wordsIndex(7, 58))
        assertEquals(0 to 0, wordsIndex(11, 58))
    }
}
