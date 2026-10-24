package app.velora.launcher.feature.apps

import androidx.compose.runtime.Stable
import androidx.compose.ui.geometry.Rect
import app.velora.launcher.core.model.AppEntry

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
}
