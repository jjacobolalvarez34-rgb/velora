package app.velora.launcher.core.data.favorites

import android.content.Context
import app.velora.launcher.core.model.AppEntry
import kotlinx.coroutines.flow.StateFlow

/**
 * Listas del usuario sobre una sola base de datos (Room queda dentro de `core:data`).
 * La base se abre en su propio hilo en la primera consulta, no en el main thread.
 */
class UserListsStore(context: Context) {

    private val database by lazy { VeloraDatabase.create(context) }

    fun hiddenApps(installed: StateFlow<List<AppEntry>>) = HiddenAppsRepository(database.hiddenApps(), installed)

    fun widgets() = WidgetsRepository(database.widgets())

    /** [visible] = apps instaladas sin las ocultas. */
    fun favorites(visible: StateFlow<List<AppEntry>>) = FavoritesRepository(database.favorites(), visible)
}
