package app.velora.launcher.core.data.log

import android.util.Log

/**
 * Único punto de logs (REGLAS §8). No hace nada salvo que [enabled] sea true, lo que solo
 * ocurre en debug. Nunca loguear contenido de notificaciones, contactos ni datos de uso.
 */
object Logger {
    @Volatile
    var enabled: Boolean = false

    fun d(tag: String, message: () -> String) {
        if (enabled) Log.d(tag, message())
    }

    fun w(tag: String, throwable: Throwable? = null, message: () -> String) {
        if (enabled) Log.w(tag, message(), throwable)
    }
}
