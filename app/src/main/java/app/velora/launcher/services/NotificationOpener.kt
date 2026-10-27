package app.velora.launcher.services

import android.app.ActivityOptions
import android.app.Notification
import android.app.PendingIntent
import android.app.RemoteInput
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.service.notification.StatusBarNotification
import app.velora.launcher.core.data.log.Logger
import app.velora.launcher.core.data.notifications.NotificationActions

/**
 * Abre, responde o descarta notificaciones desde el panel de una app (ADR-023). Usa solo las
 * `PendingIntent` y `RemoteInput` de la propia notificación (SEGURIDAD §3.6); el texto escrito
 * se entrega a la app y no se guarda.
 */
internal class NotificationOpener(
    private val context: Context,
    private val find: (String) -> StatusBarNotification?,
    private val cancel: (String) -> Unit,
) : NotificationActions {

    override fun open(key: String): Boolean {
        val sbn = find(key) ?: return false
        val intent = sbn.notification.contentIntent ?: return false
        val sent = send(intent, null)
        if (sent && sbn.notification.flags and Notification.FLAG_AUTO_CANCEL != 0) cancel(key)
        return sent
    }

    override fun reply(key: String, text: String): Boolean {
        val action = find(key)?.let(::replyAction) ?: return false
        val inputs = action.remoteInputs ?: return false
        val results = Bundle().apply { inputs.forEach { putCharSequence(it.resultKey, text) } }
        val fill = Intent().also { RemoteInput.addResultsToIntent(inputs, it, results) }
        return send(action.actionIntent, fill)
    }

    override fun dismiss(key: String) = cancel(key)

    /**
     * Desde Android 14 quien envía la `PendingIntent` (el inicio, visible) tiene que permitir que
     * la app abra su pantalla; si no, el sistema la bloquea.
     */
    private fun send(intent: PendingIntent, fill: Intent?): Boolean = runCatching {
        val options = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ActivityOptions.makeBasic()
                .setPendingIntentBackgroundActivityStartMode(ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED)
                .toBundle()
        } else {
            null
        }
        intent.send(context, 0, fill, null, null, null, options)
        true
    }.onFailure { Logger.w(TAG, it) { "No se pudo enviar la notificación" } }.getOrDefault(false)

    companion object {
        private const val TAG = "NotificationOpener"

        /** La acción con texto libre (no las de opciones fijas). */
        fun replyAction(sbn: StatusBarNotification): Notification.Action? =
            sbn.notification.actions?.firstOrNull { action ->
                action.remoteInputs?.any { it.allowFreeFormInput } == true
            }
    }
}
