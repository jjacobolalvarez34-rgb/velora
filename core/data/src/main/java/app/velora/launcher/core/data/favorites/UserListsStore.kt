package app.velora.launcher.core.data.favorites

import android.content.Context
import androidx.room.withTransaction
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

    fun folders(visible: StateFlow<List<AppEntry>>) = FoldersRepository(database.folders(), visible)

    /** Todo o nada: restaurar un respaldo no deja las listas a medias si algo falla (ADR-035). */
    suspend fun <R> transaction(block: suspend () -> R): R = database.withTransaction(block)

    /** [visible] = apps instaladas sin las ocultas. */
    fun favorites(visible: StateFlow<List<AppEntry>>) = FavoritesRepository(database.favorites(), visible)
}
