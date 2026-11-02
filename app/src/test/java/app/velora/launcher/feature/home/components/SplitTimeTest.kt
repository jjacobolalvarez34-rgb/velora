package app.velora.launcher.feature.home.components

import org.junit.Assert.assertEquals
import org.junit.Test

class SplitTimeTest {

    @Test
    fun `la hora se separa en horas y minutos`() {
        assertEquals("07" to "58", splitTime("07:58"))
        assertEquals("7" to "58 p. m.", splitTime("7:58 p. m."))
    }

    @Test
    fun `sin dos puntos todo queda arriba`() {
        assertEquals("758" to "", splitTime("758"))
    }
}
