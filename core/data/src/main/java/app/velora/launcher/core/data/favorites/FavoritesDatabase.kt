package app.velora.launcher.core.data.favorites

import android.content.Context
import androidx.room.AutoMigration
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

/**
 * Un favorito. Solo se guarda la identidad (componente + perfil), nunca un `Intent`:
 * siempre se resuelve vía `LauncherApps` (SEGURIDAD §7).
 */
@Entity(tableName = "favorites")
data class FavoriteEntity(
    @PrimaryKey val stableId: String,
    val packageName: String,
    val className: String,
    val userSerial: Long,
    val position: Int,
)

@Dao
interface FavoritesDao {
    @Query("SELECT * FROM favorites ORDER BY position")
    fun observe(): Flow<List<FavoriteEntity>>

    @Query("SELECT COUNT(*) FROM favorites")
    suspend fun count(): Int

    @Query("SELECT COALESCE(MAX(position), -1) FROM favorites")
    suspend fun maxPosition(): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(favorite: FavoriteEntity)

    @Query("DELETE FROM favorites WHERE stableId = :stableId")
    suspend fun delete(stableId: String)

    @Query("UPDATE favorites SET position = :position WHERE stableId = :stableId")
    suspend fun setPosition(stableId: String, position: Int)
}

/** Una app oculta: no sale en la lista ni en la búsqueda; se recupera en Ajustes. */
@Entity(tableName = "hidden_apps")
data class HiddenAppEntity(
    @PrimaryKey val stableId: String,
    val packageName: String,
    val className: String,
    val userSerial: Long,
)

@Dao
interface HiddenAppsDao {
    @Query("SELECT * FROM hidden_apps")
    fun observe(): Flow<List<HiddenAppEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(app: HiddenAppEntity)

    @Query("DELETE FROM hidden_apps WHERE stableId = :stableId")
    suspend fun delete(stableId: String)
}

/** Un widget del inicio: su id del sistema, quién lo provee, su alto y su orden (ADR-020). */
@Entity(tableName = "widgets")
data class WidgetEntity(
    @PrimaryKey val appWidgetId: Int,
    /** `ComponentName.flattenToString()` del proveedor. */
    val provider: String,
    val heightDp: Int,
    val position: Int,
)

@Dao
interface WidgetsDao {
    @Query("SELECT * FROM widgets ORDER BY position")
    fun observe(): Flow<List<WidgetEntity>>

    @Query("SELECT COALESCE(MAX(position), -1) FROM widgets")
    suspend fun maxPosition(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(widget: WidgetEntity)

    @Query("UPDATE widgets SET heightDp = :heightDp WHERE appWidgetId = :appWidgetId")
    suspend fun setHeight(appWidgetId: Int, heightDp: Int)

    @Query("UPDATE widgets SET position = :position WHERE appWidgetId = :appWidgetId")
    suspend fun setPosition(appWidgetId: Int, position: Int)

    @Query("DELETE FROM widgets WHERE appWidgetId = :appWidgetId")
    suspend fun delete(appWidgetId: Int)
}

/**
 * Base de datos que **sí** se respalda (favoritos, ocultas, widgets; luego carpetas). SEGURIDAD §4.
 * Versión 2: `hidden_apps`. Versión 3: `widgets`. Migraciones automáticas, sin perder nada.
 */
@Database(
    entities = [FavoriteEntity::class, HiddenAppEntity::class, WidgetEntity::class],
    version = 3,
    exportSchema = true,
    autoMigrations = [AutoMigration(from = 1, to = 2), AutoMigration(from = 2, to = 3)],
)
abstract class VeloraDatabase : RoomDatabase() {
    abstract fun favorites(): FavoritesDao

    abstract fun hiddenApps(): HiddenAppsDao

    abstract fun widgets(): WidgetsDao

    companion object {
        private const val NAME = "velora.db"

        /** Room abre el archivo en su propio hilo en la primera consulta, no en el main thread. */
        fun create(context: Context): VeloraDatabase =
            Room.databaseBuilder(context.applicationContext, VeloraDatabase::class.java, NAME).build()
    }
}
