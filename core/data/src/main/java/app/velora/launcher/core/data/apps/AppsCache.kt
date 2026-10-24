package app.velora.launcher.core.data.apps

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Transaction
import app.velora.launcher.core.model.AppEntry
import app.velora.launcher.core.model.AppKey
import java.util.Locale

/**
 * Última lista de apps vista, para que el arranque en frío la muestre sin esperar a
 * `PackageManager` (RENDIMIENTO §4). Se reconstruye sola: no se respalda (SEGURIDAD §4).
 */
interface AppsCache {
    suspend fun load(locale: Locale): List<AppEntry>

    suspend fun save(apps: List<AppEntry>)
}

@Entity(tableName = "cached_apps")
data class CachedAppEntity(
    @PrimaryKey val stableId: String,
    val packageName: String,
    val className: String,
    val userSerial: Long,
    val label: String,
    val isWorkProfile: Boolean,
    val canUninstall: Boolean,
    val installedAtMs: Long,
)

@Dao
abstract class CachedAppsDao {
    @Query("SELECT * FROM cached_apps")
    abstract suspend fun all(): List<CachedAppEntity>

    @Query("DELETE FROM cached_apps")
    abstract suspend fun clear()

    @Insert
    abstract suspend fun insertAll(apps: List<CachedAppEntity>)

    @Transaction
    open suspend fun replaceAll(apps: List<CachedAppEntity>) {
        clear()
        insertAll(apps)
    }
}

@Database(entities = [CachedAppEntity::class], version = 1, exportSchema = true)
abstract class AppsCacheDatabase : RoomDatabase() {
    abstract fun apps(): CachedAppsDao
}

class RoomAppsCache(context: Context) : AppsCache {

    private val dao by lazy {
        Room.databaseBuilder(context.applicationContext, AppsCacheDatabase::class.java, "apps_cache.db")
            // Es una caché: si cambia el esquema se borra y se vuelve a llenar.
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()
            .apps()
    }

    /** Las secciones y claves de búsqueda se recalculan con el idioma actual. */
    override suspend fun load(locale: Locale): List<AppEntry> =
        AppSorting.sorted(dao.all().map { it.toEntry(locale) }, locale)

    override suspend fun save(apps: List<AppEntry>) = dao.replaceAll(apps.map { it.toCached() })

    private fun CachedAppEntity.toEntry(locale: Locale) = AppEntry(
        key = AppKey(packageName, className, userSerial),
        label = label,
        section = AppSorting.sectionOf(label, locale),
        searchKey = AppSorting.searchKeyOf(label, locale),
        isWorkProfile = isWorkProfile,
        canUninstall = canUninstall,
        installedAtMs = installedAtMs,
    )

    private fun AppEntry.toCached() = CachedAppEntity(
        stableId = key.stableId,
        packageName = key.packageName,
        className = key.className,
        userSerial = key.userSerial,
        label = label,
        isWorkProfile = isWorkProfile,
        canUninstall = canUninstall,
        installedAtMs = installedAtMs,
    )
}
