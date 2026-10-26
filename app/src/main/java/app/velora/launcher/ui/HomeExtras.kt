package app.velora.launcher.ui

import android.content.Intent
import android.provider.Settings
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.velora.launcher.R
import app.velora.launcher.core.data.log.Logger
import app.velora.launcher.core.data.notifications.NotificationsRepository
import app.velora.launcher.di.AppContainer
import app.velora.launcher.feature.apps.LocalNotifications
import app.velora.launcher.feature.home.components.MediaActions
import app.velora.launcher.feature.home.components.NowPlayingCard
import app.velora.launcher.feature.widgets.HomeWidgets
import app.velora.launcher.services.LockAccessibilityService

// Lo que el inicio suma en 0.6.0: notificaciones, música, bloqueo con doble toque y foto de fondo.

/** Puntos de notificación para las filas de apps; sin la función activada, nada (ADR-023). */
@Composable
internal fun NotificationsProvider(
    repository: NotificationsRepository,
    enabled: Boolean,
    content: @Composable () -> Unit,
) {
    val byApp by repository.byApp.collectAsStateWithLifecycle()
    CompositionLocalProvider(LocalNotifications provides if (enabled) byApp else emptyMap(), content = content)
}

/** Bajo el reloj: la música que suena (si está activado) y los widgets del usuario. */
@Composable
internal fun HomeWidgetsSlot(container: AppContainer, showMusic: Boolean) {
    val repository = container.notificationsRepository
    val widgets by container.widgetHost.widgets.collectAsStateWithLifecycle()
    val nowPlaying by repository.nowPlaying.collectAsStateWithLifecycle()
    val media = remember(repository) { mediaActionsFor(repository) }
    NowPlayingCard(nowPlaying.takeIf { showMusic }, media)
    HomeWidgets(container.widgetHost, widgets)
}

/** Controles del reproductor: van a la sesión de música que sigue el servicio (ADR-023). */
private fun mediaActionsFor(repository: NotificationsRepository) = object : MediaActions {
    override fun playPause() {
        repository.mediaControls?.playPause()
    }

    override fun next() {
        repository.mediaControls?.next()
    }

    override fun previous() {
        repository.mediaControls?.previous()
    }
}

/**
 * Doble toque para bloquear (ADR-009). Si el sistema apagó el servicio (algunos fabricantes lo
 * hacen, B-002), se avisa y se abre Accesibilidad para volver a activarlo.
 */
@Composable
internal fun rememberDoubleTapLock(): () -> Unit {
    val context = LocalContext.current
    return remember(context) {
        {
            if (!LockAccessibilityService.lock()) {
                Toast.makeText(context, R.string.lock_service_needed, Toast.LENGTH_LONG).show()
                runCatching {
                    context.startActivity(
                        Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                    )
                }.onFailure { Logger.w("DoubleTapLock", it) { "Sin pantalla de accesibilidad" } }
            }
        }
    }
}

/** La foto propia del tema, leída fuera del hilo principal; null mientras carga o si no hay. */
@Composable
internal fun rememberPhoto(path: String?): ImageBitmap? {
    val photo by produceState<ImageBitmap?>(null, path) {
        value = path?.let { BackgroundPhotos.load(it) }
    }
    return photo
}
