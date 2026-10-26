package app.velora.launcher.services

import android.app.Notification
import android.content.ComponentName
import android.content.Context
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.Handler
import android.os.Looper
import android.os.UserManager
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import app.velora.launcher.VeloraApplication
import app.velora.launcher.core.data.log.Logger
import app.velora.launcher.core.data.notifications.MediaControls
import app.velora.launcher.core.data.notifications.NotificationsRepository
import app.velora.launcher.core.model.AppNotification
import app.velora.launcher.core.model.NowPlaying
import java.lang.ref.WeakReference
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Notificaciones y música en el inicio (ADR-023, SEGURIDAD §3.2). Opt-in: si la función está
 * apagada, se desconecta solo, y siempre *después* de conectar (B-003). Nada va a disco ni a logs.
 */
class VeloraNotificationListener : NotificationListenerService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val container get() = (application as VeloraApplication).container
    private val repository: NotificationsRepository get() = container.notificationsRepository
    private var publishJob: Job? = null
    private var media: MediaWatcher? = null

    override fun onListenerConnected() {
        current = WeakReference(this)
        scope.launch {
            if (!container.prefsRepository.theme.first().notifications) {
                requestUnbind()
                return@launch
            }
            publishAll()
            media = MediaWatcher(this@VeloraNotificationListener, repository).also { it.start() }
        }
    }

    override fun onListenerDisconnected() {
        media?.stop()
        media = null
        repository.clear()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) = schedulePublish()

    override fun onNotificationRemoved(sbn: StatusBarNotification) = schedulePublish()

    override fun onDestroy() {
        current = null
        media?.stop()
        scope.cancel()
        super.onDestroy()
    }

    /** Se juntan las ráfagas (~100 ms, RENDIMIENTO §6) y se publica el estado completo. */
    private fun schedulePublish() {
        publishJob?.cancel()
        publishJob = scope.launch {
            delay(DEBOUNCE_MS)
            publishAll()
        }
    }

    private fun publishAll() {
        val users = getSystemService(UserManager::class.java)
        val active = runCatching { activeNotifications }.getOrNull().orEmpty()
        repository.publish(active.filter(::isShown).map { it.toModel(users) })
    }

    private fun isShown(sbn: StatusBarNotification): Boolean {
        val n = sbn.notification
        return !sbn.isOngoing && n.flags and Notification.FLAG_GROUP_SUMMARY == 0 && sbn.packageName != packageName
    }

    private fun StatusBarNotification.toModel(users: UserManager): AppNotification {
        val extras = notification.extras
        // Notificación privada: solo se dice que hay algo, nunca qué (SEGURIDAD §3.2).
        val secret = notification.visibility == Notification.VISIBILITY_SECRET
        return AppNotification(
            packageName = packageName,
            userSerial = users.getSerialNumberForUser(user),
            count = 1,
            title = if (secret) null else extras.getCharSequence(Notification.EXTRA_TITLE)?.toString(),
            text = if (secret) null else extras.getCharSequence(Notification.EXTRA_TEXT)?.toString(),
            postedAtMs = postTime,
        )
    }

    companion object {
        private const val DEBOUNCE_MS = 100L
        private var current: WeakReference<VeloraNotificationListener>? = null

        /** Al apagar la función en Ajustes: dejar de recibir notificaciones ya. */
        fun disconnect() {
            current?.get()?.let { listener ->
                runCatching { listener.requestUnbind() }
                listener.repository.clear()
            }
        }

        /** Al encenderla con el acceso ya concedido: volver a conectarse sin esperar al sistema. */
        fun reconnect(context: Context) {
            runCatching { requestRebind(ComponentName(context, VeloraNotificationListener::class.java)) }
                .onFailure { Logger.w("NotificationListener", it) { "requestRebind" } }
        }
    }
}

/** Sigue la sesión de música activa y la expone como [NowPlaying] con controles. */
private class MediaWatcher(
    private val service: VeloraNotificationListener,
    private val repository: NotificationsRepository,
) {

    private val handler = Handler(Looper.getMainLooper())
    private val sessions = service.getSystemService(MediaSessionManager::class.java)
    private val component = ComponentName(service, VeloraNotificationListener::class.java)
    private var controller: MediaController? = null

    private val callback = object : MediaController.Callback() {
        override fun onPlaybackStateChanged(state: PlaybackState?) = publish()
        override fun onMetadataChanged(metadata: android.media.MediaMetadata?) = publish()
        override fun onSessionDestroyed() = choose(emptyList())
    }

    private val sessionsListener = MediaSessionManager.OnActiveSessionsChangedListener { choose(it.orEmpty()) }

    fun start() {
        runCatching {
            sessions.addOnActiveSessionsChangedListener(sessionsListener, component, handler)
            choose(sessions.getActiveSessions(component))
        }.onFailure { Logger.w(TAG, it) { "Sin acceso a las sesiones de música" } }
    }

    fun stop() {
        runCatching { sessions.removeOnActiveSessionsChangedListener(sessionsListener) }
        controller?.unregisterCallback(callback)
        controller = null
        repository.setNowPlaying(null)
    }

    /** La que está sonando, o si ninguna, la primera. */
    private fun choose(active: List<MediaController>) {
        val next = active.firstOrNull { it.playbackState?.state == PlaybackState.STATE_PLAYING } ?: active.firstOrNull()
        if (next?.sessionToken == controller?.sessionToken) return publish()
        controller?.unregisterCallback(callback)
        controller = next
        next?.registerCallback(callback, handler)
        repository.mediaControls = next?.let(::controlsFor)
        publish()
    }

    private fun publish() {
        val c = controller
        val title = c?.metadata?.getString(android.media.MediaMetadata.METADATA_KEY_TITLE)
        repository.setNowPlaying(
            if (c == null || title.isNullOrBlank()) {
                null
            } else {
                NowPlaying(
                    title = title,
                    artist = c.metadata?.getString(android.media.MediaMetadata.METADATA_KEY_ARTIST),
                    isPlaying = c.playbackState?.state == PlaybackState.STATE_PLAYING,
                    packageName = c.packageName,
                )
            },
        )
    }

    private fun controlsFor(c: MediaController) = object : MediaControls {
        override fun playPause() {
            if (c.playbackState?.state ==
                PlaybackState.STATE_PLAYING
            ) {
                c.transportControls.pause()
            } else {
                c.transportControls.play()
            }
        }

        override fun next() = c.transportControls.skipToNext()

        override fun previous() = c.transportControls.skipToPrevious()
    }

    private companion object {
        const val TAG = "MediaWatcher"
    }
}
