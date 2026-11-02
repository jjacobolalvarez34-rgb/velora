package app.velora.launcher.core.data.backup

import app.velora.launcher.core.data.favorites.FolderSnapshot
import app.velora.launcher.core.model.AppEntry
import app.velora.launcher.core.model.AppKey
import app.velora.launcher.core.model.ClockStyle
import app.velora.launcher.core.model.Customization
import app.velora.launcher.core.model.FontChoice
import app.velora.launcher.core.model.IconStyle
import app.velora.launcher.core.model.TextSize
import app.velora.launcher.core.model.ThemePreset

/**
 * Un widget en el respaldo: el id del sistema no se puede restaurar (cada widget necesita el
 * permiso del usuario otra vez), así que solo va quién lo provee y su alto para "volver a agregarlo".
 */
data class BackupWidget(val provider: String, val heightDp: Int)

/** Preferencias que viajan en el respaldo; lo que no venga (null) se deja como está. */
data class BackupPrefs(
    val preset: ThemePreset? = null,
    val font: FontChoice? = null,
    val clock: ClockStyle? = null,
    val icons: IconStyle? = null,
    val textSize: TextSize? = null,
    val leftHanded: Boolean? = null,
    val fullList: Boolean? = null,
    val custom: Customization? = null,
)

/**
 * Todo lo del usuario (ADR-035): listas, widgets por re-agregar, preferencias y, opcional, la
 * foto de fondo activa. Nunca notificaciones, PIN ni datos de otras apps.
 */
data class VeloraBackup(
    val favorites: List<AppKey> = emptyList(),
    val folders: List<FolderSnapshot> = emptyList(),
    val hidden: List<AppKey> = emptyList(),
    val widgets: List<BackupWidget> = emptyList(),
    val prefs: BackupPrefs = BackupPrefs(),
    /** JPEG del fondo propio, si el usuario eligió incluirlo. */
    val photo: ByteArray? = null,
) {
    override fun equals(other: Any?): Boolean = other is VeloraBackup &&
        favorites == other.favorites && folders == other.folders && hidden == other.hidden &&
        widgets == other.widgets && prefs == other.prefs && photo.sameBytes(other.photo)

    override fun hashCode(): Int =
        listOf(favorites, folders, hidden, widgets, prefs).hashCode() * HASH + (photo?.contentHashCode() ?: 0)

    private fun ByteArray?.sameBytes(other: ByteArray?) =
        if (this == null) other == null else other != null && contentEquals(other)

    private companion object {
        const val HASH = 31
    }
}

/**
 * Las apps del respaldo se buscan entre las instaladas: el número de perfil cambia de un teléfono
 * a otro, así que si no coincide exacto se usa la misma app del perfil personal. Si no está
 * instalada se guarda igual: aparecerá cuando se instale en el perfil personal.
 */
object BackupMatcher {

    fun resolve(keys: List<AppKey>, installed: List<AppEntry>): List<AppKey> {
        val byId = installed.associateBy { it.key.stableId }
        val byComponent = installed.sortedBy { if (it.isWorkProfile || it.isPrivate) 1 else 0 }
            .groupBy { "${it.key.packageName}/${it.key.className}" }
        return keys.map { key ->
            byId[key.stableId]?.key ?: byComponent["${key.packageName}/${key.className}"]?.first()?.key ?: key
        }.distinctBy { it.stableId }
    }
}
