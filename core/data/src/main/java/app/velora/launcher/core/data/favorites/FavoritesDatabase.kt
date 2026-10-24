package app.velora.launcher.core.data.favorites

import android.content.Context
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
}

/** Base de datos que **sí** se respalda (favoritos; luego ocultos y carpetas). SEGURIDAD §4. */
@Database(entities = [FavoriteEntity::class], version = 1, exportSchema = true)
abstract class VeloraDatabase : RoomDatabase() {
    abstract fun favorites(): FavoritesDao

    companion object {
        private const val NAME = "velora.db"

        /** Room abre el archivo en su propio hilo en la primera consulta, no en el main thread. */
        fun create(context: Context): VeloraDatabase =
            Room.databaseBuilder(context.applicationContext, VeloraDatabase::class.java, NAME).build()
    }
}
