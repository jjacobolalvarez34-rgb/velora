package app.velora.launcher.core.data.usage

import android.annotation.SuppressLint
import android.app.AppOpsManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.Process
import app.velora.launcher.core.data.log.Logger
import java.util.Calendar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Tiempo de uso de hoy por app (ADR-024). Lo da el sistema con el acceso especial "Acceso a
 * datos de uso", que el usuario concede en Ajustes; Velora solo lo lee al abrir el panel de una
 * app, no lo guarda ni lo envía.
 */
class UsageRepository(context: Context) {

    private val appContext = context.applicationContext

    /** Lee AppOps: llamarlo fuera del hilo principal. */
    fun hasAccess(): Boolean {
        val ops = appContext.getSystemService(AppOpsManager::class.java) ?: return false

        @Suppress("DEPRECATION")
        val mode = ops.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), appContext.packageName)
        return mode == AppOpsManager.MODE_ALLOWED
    }

    /** Milisegundos en primer plano hoy, o null si no hay acceso o la app no se usó. */
    // El acceso se concede como acceso especial y se comprueba arriba con AppOps (hasAccess).
    @SuppressLint("MissingPermission")
    suspend fun todayMs(packageName: String): Long? = withContext(Dispatchers.IO) {
        if (!hasAccess()) return@withContext null
        val manager = appContext.getSystemService(UsageStatsManager::class.java) ?: return@withContext null
        val start = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        runCatching {
            manager.queryAndAggregateUsageStats(start, System.currentTimeMillis())[packageName]
                ?.totalTimeInForeground
                ?.takeIf { it > 0 }
        }.onFailure { Logger.w(TAG, it) { "queryAndAggregateUsageStats" } }.getOrNull()
    }

    private companion object {
        const val TAG = "UsageRepository"
    }
}
