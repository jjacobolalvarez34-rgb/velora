package app.velora.launcher.core.data.backup

import app.velora.launcher.core.data.favorites.FavoritesRepository
import app.velora.launcher.core.data.favorites.FoldersRepository
import app.velora.launcher.core.data.favorites.HiddenAppsRepository
import app.velora.launcher.core.data.favorites.UserListsStore
import app.velora.launcher.core.data.favorites.WidgetsRepository
import app.velora.launcher.core.data.prefs.PrefsRepository
import app.velora.launcher.core.model.AppEntry
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first

/**
 * Arma y restaura el respaldo completo (ADR-035) con los repositorios de siempre. Restaurar
 * reemplaza favoritos, carpetas y ocultas en una sola transacción; las preferencias se aplican
 * después. Los widgets no se restauran solos: se devuelven para que el usuario los vuelva a agregar.
 */
class UserBackup(
    private val store: UserListsStore,
    private val favorites: FavoritesRepository,
    private val folders: FoldersRepository,
    private val hidden: HiddenAppsRepository,
    private val widgets: WidgetsRepository,
    private val prefs: PrefsRepository,
    /** Todas las instaladas (también las ocultas), para reconocer las apps en otro teléfono. */
    private val installed: StateFlow<List<AppEntry>>,
) {

    /** El respaldo de ahora; [photo] es la foto de fondo activa si el usuario quiere incluirla. */
    suspend fun create(photo: ByteArray?): VeloraBackup {
        val theme = prefs.theme.first()
        return VeloraBackup(
            favorites = favorites.snapshot(),
            folders = folders.snapshot(),
            hidden = hidden.snapshot(),
            widgets = widgets.snapshot().map { BackupWidget(it.provider, it.heightDp) },
            prefs = BackupPrefs(
                preset = theme.preset,
                font = theme.font,
                clock = theme.clock,
                icons = theme.icons,
                textSize = theme.textSize,
                leftHanded = theme.leftHanded,
                fullList = theme.fullList,
                custom = theme.custom,
            ),
            photo = photo,
        )
    }

    /**
     * Aplica [backup]. [photoPath] es la foto ya importada al almacenamiento privado (o null para
     * dejar el fondo actual). Devuelve los widgets que hay que volver a agregar a mano.
     */
    suspend fun restore(backup: VeloraBackup, photoPath: String?): List<BackupWidget> {
        val apps = installed.value
        store.transaction {
            favorites.restore(BackupMatcher.resolve(backup.favorites, apps))
            hidden.restore(BackupMatcher.resolve(backup.hidden, apps))
            folders.restore(backup.folders.map { it.copy(apps = BackupMatcher.resolve(it.apps, apps)) })
        }
        restorePrefs(backup.prefs)
        photoPath?.let { prefs.setPhotoBackground(it) }
        return backup.widgets
    }

    private suspend fun restorePrefs(saved: BackupPrefs) {
        // El tema primero: también pone su fuente, reloj e íconos; luego se aplica lo elegido.
        saved.preset?.let { prefs.applyPreset(it) }
        saved.font?.let { prefs.setFont(it) }
        saved.clock?.let { prefs.setClockStyle(it) }
        saved.icons?.let { prefs.setIconStyle(it) }
        saved.textSize?.let { prefs.setTextSize(it) }
        saved.leftHanded?.let { prefs.setLeftHanded(it) }
        saved.fullList?.let { prefs.setFullList(it) }
        saved.custom?.let { custom -> prefs.updateCustom { custom } }
    }
}
