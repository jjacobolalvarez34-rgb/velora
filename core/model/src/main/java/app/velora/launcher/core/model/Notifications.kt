package app.velora.launcher.core.model

import androidx.compose.runtime.Immutable

/**
 * Lo pendiente de una app en la barra de notificaciones (ADR-023). Solo en memoria: nunca se
 * guarda en disco ni en logs (SEGURIDAD §3.2).
 *
 * @property title título de la más reciente, o null si es privada (se muestra "Nueva notificación").
 * @property key clave del sistema (`StatusBarNotification.key`), para abrirla o responderla.
 * @property canReply trae una respuesta rápida (`RemoteInput`).
 */
@Immutable
data class AppNotification(
    val packageName: String,
    val userSerial: Long,
    val count: Int,
    val title: String?,
    val text: String?,
    val postedAtMs: Long,
    val key: String = "",
    val canReply: Boolean = false,
)

/** Lo que suena ahora, para el reproductor del inicio. */
@Immutable
data class NowPlaying(val title: String, val artist: String?, val isPlaying: Boolean, val packageName: String)
