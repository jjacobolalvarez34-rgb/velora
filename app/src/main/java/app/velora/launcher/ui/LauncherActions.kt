package app.velora.launcher.ui

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.geometry.Rect
import app.velora.launcher.core.model.AppEntry
import app.velora.launcher.core.model.AppShortcut
import app.velora.launcher.core.model.ThemeSettings
import app.velora.launcher.di.AppContainer
import app.velora.launcher.feature.apps.AppActions
import app.velora.launcher.feature.apps.rememberAppOpener
import app.velora.launcher.feature.home.HomeViewModel
import kotlinx.coroutines.launch

/** Se pasó el límite de uso de hoy: qué app, cuánto lleva y cuál era el límite (ADR-024). */
@Immutable
internal data class LimitHit(val app: AppEntry, val usedMs: Long, val limitMinutes: Int, val bounds: Rect?)

/**
 * Una sola instancia de acciones para todas las filas (RENDIMIENTO §3.6). Abrir respeta el freno
 * de uso: si la app tiene límite y hoy ya se pasó, primero se pregunta.
 */
@Composable
internal fun rememberAppActions(
    container: AppContainer,
    homeViewModel: HomeViewModel,
    sheets: LauncherSheetsState,
    theme: ThemeSettings,
): AppActions {
    val opener = rememberAppOpener(container.appLauncher)
    val scope = rememberCoroutineScope()
    val current by rememberUpdatedState(theme)
    return remember(opener, homeViewModel, sheets, scope) {
        object : AppActions {
            override fun open(app: AppEntry, bounds: Rect?) {
                val limit = current.custom.usageLimits[app.key.packageName]?.takeIf { current.usageTime }
                if (limit == null) return opener.open(app, bounds)
                scope.launch {
                    val used = container.usageRepository.todayMs(app.key.packageName) ?: 0L
                    if (used >= limit * MS_PER_MINUTE) {
                        sheets.limitHit = LimitHit(app, used, limit, bounds)
                    } else {
                        opener.open(app, bounds)
                    }
                }
            }

            /** "Abrir igual" desde el aviso del freno de uso. */
            override fun openAnyway(app: AppEntry, bounds: Rect?) = opener.open(app, bounds)
            override fun openInfo(app: AppEntry, bounds: Rect?) = opener.openInfo(app, bounds)
            override fun uninstall(app: AppEntry) = opener.uninstall(app)
            override fun addFavorite(app: AppEntry) = homeViewModel.onAddFavorite(app.key)
            override fun removeFavorite(app: AppEntry) = homeViewModel.onRemoveFavorite(app.key)
            override fun hide(app: AppEntry) = homeViewModel.onHide(app)
            override fun openShortcut(shortcut: AppShortcut, bounds: Rect?) = opener.openShortcut(shortcut, bounds)
            override fun showMenu(app: AppEntry) {
                sheets.appMenu = app
            }

            override fun webSearch(query: String) = opener.webSearch(query)
            override fun copy(text: String) = opener.copy(text)
            override fun openContact(uri: Uri) = opener.openContact(uri)
        }
    }
}

private const val MS_PER_MINUTE = 60_000L
