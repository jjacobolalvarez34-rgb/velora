package app.velora.launcher.core.data.notifications

import app.velora.launcher.core.model.AppNotification
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NotificationsRepositoryTest {

    private fun notification(pkg: String, title: String, at: Long, serial: Long = 0) =
        AppNotification(pkg, serial, count = 1, title = title, text = null, postedAtMs = at)

    @Test
    fun `por app se cuentan todas y se muestra la más reciente`() {
        val grouped = NotificationsRepository.group(
            listOf(
                notification("chat", "Hola", 1),
                notification("chat", "¿Vienes?", 5),
                notification("mail", "Factura", 3),
            ),
        )

        val chat = grouped.getValue(NotificationsRepository.keyOf("chat", 0))
        assertEquals(2, chat.count)
        assertEquals("¿Vienes?", chat.title)
        assertEquals(1, grouped.getValue(NotificationsRepository.keyOf("mail", 0)).count)
    }

    @Test
    fun `el mismo app en el perfil de trabajo va aparte`() {
        val grouped = NotificationsRepository.group(
            listOf(notification("chat", "a", 1), notification("chat", "b", 2, serial = 10)),
        )

        assertEquals(2, grouped.size)
    }

    @Test
    fun `desactivar la función vacía todo`() {
        val repository = NotificationsRepository()
        repository.publish(listOf(notification("chat", "Hola", 1)))

        repository.clear()

        assertTrue(repository.byApp.value.isEmpty())
    }
}
