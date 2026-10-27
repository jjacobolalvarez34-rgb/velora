package app.velora.launcher.core.data.favorites

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import app.velora.launcher.core.model.AppEntry
import app.velora.launcher.core.model.AppFolder
import app.velora.launcher.core.model.AppKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine

/** Una carpeta del inicio ("Trabajo", "Bancos"…). */
@Entity(tableName = "folders")
data class FolderEntity(@PrimaryKey(autoGenerate = true) val id: Long = 0, val name: String, val position: Int)

/** Una app dentro de una carpeta. Como los favoritos: solo la identidad, nunca un `Intent`. */
@Entity(tableName = "folder_apps", primaryKeys = ["folderId", "stableId"])
data class FolderAppEntity(
    val folderId: Long,
    val stableId: String,
    val packageName: String,
    val className: String,
    val userSerial: Long,
)

@Dao
interface FoldersDao {
    @Query("SELECT * FROM folders ORDER BY position")
    fun observeFolders(): Flow<List<FolderEntity>>

    @Query("SELECT * FROM folder_apps")
    fun observeApps(): Flow<List<FolderAppEntity>>

    @Query("SELECT COALESCE(MAX(position), -1) FROM folders")
    suspend fun maxPosition(): Int

    @Insert
    suspend fun insertFolder(folder: FolderEntity): Long

    @Query("UPDATE folders SET name = :name WHERE id = :id")
    suspend fun rename(id: Long, name: String)

    @Query("DELETE FROM folders WHERE id = :id")
    suspend fun deleteFolder(id: Long)

    @Query("DELETE FROM folder_apps WHERE folderId = :id")
    suspend fun deleteFolderApps(id: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertApp(app: FolderAppEntity)

    @Query("DELETE FROM folder_apps WHERE folderId = :folderId AND stableId = :stableId")
    suspend fun deleteApp(folderId: Long, stableId: String)
}

/** Carpetas del inicio con sus apps instaladas, por nombre (Fase 3). */
class FoldersRepository(private val dao: FoldersDao, apps: StateFlow<List<AppEntry>>) {

    val folders: Flow<List<AppFolder>> =
        combine(dao.observeFolders(), dao.observeApps(), apps) { folders, members, installed ->
            val byId = installed.associateBy { it.key.stableId }
            val byFolder = members.groupBy { it.folderId }
            folders.map { folder ->
                val inside = byFolder[folder.id].orEmpty().mapNotNull { byId[it.stableId] }.sortedBy { it.searchKey }
                AppFolder(folder.id, folder.name, inside)
            }
        }

    /** Crea una carpeta y, si se da, le agrega la primera app. Devuelve su id. */
    suspend fun create(name: String, first: AppKey? = null): Long {
        val id = dao.insertFolder(FolderEntity(name = name.trim().take(MAX_NAME), position = dao.maxPosition() + 1))
        if (first != null) add(id, first)
        return id
    }

    suspend fun rename(id: Long, name: String) = dao.rename(id, name.trim().take(MAX_NAME))

    suspend fun delete(id: Long) {
        dao.deleteFolderApps(id)
        dao.deleteFolder(id)
    }

    suspend fun add(folderId: Long, key: AppKey) =
        dao.insertApp(FolderAppEntity(folderId, key.stableId, key.packageName, key.className, key.userSerial))

    suspend fun remove(folderId: Long, key: AppKey) = dao.deleteApp(folderId, key.stableId)

    companion object {
        const val MAX_NAME = 30
    }
}
