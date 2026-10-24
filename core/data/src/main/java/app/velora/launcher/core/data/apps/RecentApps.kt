package app.velora.launcher.core.data.apps

import app.velora.launcher.core.model.AppEntry
import java.util.concurrent.TimeUnit

/** "Recién instaladas" (PRODUCTO, Fase 2 adelantada a pedido del autor). Kotlin puro. */
object RecentApps {

    val WINDOW_MS: Long = TimeUnit.DAYS.toMillis(14)
    const val MAX = 4

    /** Las instaladas en la ventana, de la más nueva a la más vieja; nunca más de [MAX]. */
    fun of(apps: List<AppEntry>, nowMs: Long): List<AppEntry> = apps
        .filter { it.installedAtMs in (nowMs - WINDOW_MS)..nowMs }
        .sortedByDescending { it.installedAtMs }
        .take(MAX)
}
