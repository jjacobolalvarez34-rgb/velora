package app.velora.launcher.core.data.hidden

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PinSecurityTest {

    /** Pocas iteraciones: la prueba mide la lógica, no el costo. */
    private val stored = PinHasher.create("2580", iterations = TEST_ITERATIONS)

    @Test
    fun `el PIN correcto se verifica y nunca se guarda en claro`() {
        assertTrue(PinHasher.matches("2580", stored))
        assertFalse(PinHasher.encode(stored).contains("2580"))
    }

    @Test
    fun `un PIN incorrecto no se acepta`() {
        assertFalse(PinHasher.matches("2581", stored))
        assertFalse(PinHasher.matches("258", stored))
        assertFalse(PinHasher.matches("abcd", stored))
    }

    @Test
    fun `la misma clave con otra sal da otro hash`() {
        val other = PinHasher.create("2580", iterations = TEST_ITERATIONS)

        assertNotEquals(stored, other)
        assertTrue(PinHasher.matches("2580", other))
    }

    @Test
    fun `solo se aceptan de 4 a 8 digitos`() {
        assertTrue(PinHasher.isValid("1234"))
        assertTrue(PinHasher.isValid("12345678"))
        assertFalse(PinHasher.isValid("123"))
        assertFalse(PinHasher.isValid("123456789"))
        assertFalse(PinHasher.isValid("12a4"))
        assertFalse(PinHasher.isValid("١٢٣٤"))
    }

    @Test
    fun `lo guardado se lee igual y lo dañado no`() {
        assertEquals(stored, PinHasher.decode(PinHasher.encode(stored)))
        assertNull(PinHasher.decode("basura"))
        assertNull(PinHasher.decode("0:AA==:AA=="))
        assertNull(PinHasher.decode(null))
    }

    @Test
    fun `cinco fallos libres y luego espera`() {
        var attempts = PinAttempts()
        repeat(PinPolicy.FREE_ATTEMPTS - 1) { index ->
            val (result, next) = PinPolicy.check(stored, attempts, "0000", NOW)
            assertEquals(PinCheck.Wrong(PinPolicy.FREE_ATTEMPTS - index - 1, 0L), result)
            attempts = next
        }
        val (fifth, locked) = PinPolicy.check(stored, attempts, "0000", NOW)

        assertEquals(PinCheck.Wrong(0, PinPolicy.BASE_WAIT_MS), fifth)
        assertEquals(NOW + PinPolicy.BASE_WAIT_MS, locked.lockedUntilMs)
    }

    @Test
    fun `durante la espera ni el PIN correcto entra`() {
        val locked = PinAttempts(failures = 5, lockedUntilMs = NOW + 10_000)

        val (result, next) = PinPolicy.check(stored, locked, "2580", NOW)

        assertEquals(PinCheck.Wait(10_000), result)
        assertEquals(locked, next)
    }

    @Test
    fun `pasada la espera el PIN correcto entra y reinicia el contador`() {
        val locked = PinAttempts(failures = 5, lockedUntilMs = NOW + 10_000)

        val (result, next) = PinPolicy.check(stored, locked, "2580", NOW + 10_000)

        assertEquals(PinCheck.Correct, result)
        assertEquals(PinAttempts(), next)
    }

    @Test
    fun `la espera crece con cada fallo y tiene tope`() {
        assertEquals(0L, PinPolicy.waitAfter(4))
        assertEquals(30_000L, PinPolicy.waitAfter(5))
        assertEquals(60_000L, PinPolicy.waitAfter(6))
        assertEquals(120_000L, PinPolicy.waitAfter(7))
        assertEquals(PinPolicy.MAX_WAIT_MS, PinPolicy.waitAfter(50))
    }

    @Test
    fun `si el reloj se atrasa la espera no pasa del maximo`() {
        val farFuture = PinAttempts(failures = 9, lockedUntilMs = NOW + 365L * 24 * 3_600_000)

        assertEquals(PinPolicy.MAX_WAIT_MS, PinPolicy.remaining(farFuture, NOW))
    }

    private companion object {
        const val TEST_ITERATIONS = 1_000
        const val NOW = 1_700_000_000_000L
    }
}
