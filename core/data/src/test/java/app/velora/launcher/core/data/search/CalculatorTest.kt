package app.velora.launcher.core.data.search

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CalculatorTest {

    @Test
    fun `cuentas básicas con precedencia y paréntesis`() {
        assertEquals("36", Calculator.evaluate("12*3"))
        assertEquals("14", Calculator.evaluate("2+3*4"))
        assertEquals("5", Calculator.evaluate("(4+6)/2"))
        assertEquals("1024", Calculator.evaluate("2^10"))
    }

    @Test
    fun `acepta coma decimal, x y espacios`() {
        assertEquals("10", Calculator.evaluate("2,5 x 4"))
        assertEquals("0.5", Calculator.evaluate("1 ÷ 2"))
    }

    @Test
    fun `números negativos`() {
        assertEquals("-3", Calculator.evaluate("-5+2"))
    }

    @Test
    fun `sin operación o con letras sigue siendo una búsqueda de apps`() {
        assertNull(Calculator.evaluate("12"))
        assertNull(Calculator.evaluate("camara"))
        assertNull(Calculator.evaluate("2+"))
        assertNull(Calculator.evaluate("1/0"))
    }
}
