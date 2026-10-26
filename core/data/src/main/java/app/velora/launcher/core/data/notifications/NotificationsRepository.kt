package app.velora.launcher.core.data.notifications

import app.velora.launcher.core.model.AppKey
import app.velora.launcher.core.model.AppNotification
import app.velora.launcher.core.model.NowPlaying
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Controles del reproductor activo; los pone el servicio de notificaciones. */
interface MediaControls {
    fun playPause()
    fun next()
    fun previous()
}

/**
 * Notificaciones y música para el inicio (ADR-023). Todo en memoria: el servicio de
 * notificaciones publica aquí y la UI solo lee. Al desactivar la función se vacía.
 */
class NotificationsRepository {

    private val _byApp = MutableStateFlow<Map<String, AppNotification>>(emptyMap())

    /** Una entrada por app y perfil ([keyOf]): cuántas hay y la más reciente. */
    val byApp: StateFlow<Map<String, AppNotification>> = _byApp.asStateFlow()

    private val _nowPlaying = MutableStateFlow<NowPlaying?>(null)
    val nowPlaying: StateFlow<NowPlaying?> = _nowPlaying.asStateFlow()

    @Volatile
    var mediaControls: MediaControls? = null

    fun publish(notifications: List<AppNotification>) {
        _byApp.value = group(notifications)
    }

    fun setNowPlaying(value: NowPlaying?) {
        _nowPlaying.value = value
    }

    fun clear() {
        _byApp.value = emptyMap()
        _nowPlaying.value = null
        mediaControls = null
    }

    companion object {
        fun keyOf(packageName: String, userSerial: Long) = "$packageName#$userSerial"

        fun keyOf(key: AppKey) = keyOf(key.packageName, key.userSerial)

        /** Junta las de cada app: la cuenta total y el texto de la más reciente. */
        fun group(notifications: List<AppNotification>): Map<String, AppNotification> =
            notifications.groupBy { keyOf(it.packageName, it.userSerial) }.mapValues { (_, list) ->
                list.maxBy { it.postedAtMs }.copy(count = list.sumOf { it.count })
            }
    }
}
