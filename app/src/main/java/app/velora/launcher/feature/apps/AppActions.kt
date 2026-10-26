package app.velora.launcher.feature.apps

import androidx.compose.runtime.Stable
import androidx.compose.ui.geometry.Rect
import app.velora.launcher.core.model.AppEntry
import app.velora.launcher.core.model.AppShortcut

/**
 * Todo lo que una fila puede pedir. Una sola instancia estable para toda la lista,
 * así las filas no reciben lambdas nuevas en cada recomposición (RENDIMIENTO §3.6).
 */
@Stable
interface AppActions {
    /** @param bounds rectángulo de la fila en coordenadas de la raíz, para animar desde ahí. */
    fun open(app: AppEntry, bounds: Rect?)
    fun openInfo(app: AppEntry, bounds: Rect?)
    fun uninstall(app: AppEntry)
    fun addFavorite(app: AppEntry)
    fun removeFavorite(app: AppEntry)
    fun hide(app: AppEntry)
    fun openShortcut(shortcut: AppShortcut, bounds: Rect?)

    /** Mantener presionada: abre el panel de la app. */
    fun showMenu(app: AppEntry)

    /** "Buscar en la web" desde el buscador (otra app hace la búsqueda; Velora no usa red). */
    fun webSearch(query: String)

    /** Copiar el resultado de la calculadora. */
    fun copy(text: String)
}
