package app.velora.launcher.core.data.battery

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import androidx.core.content.ContextCompat
import kotlin.math.roundToInt
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.distinctUntilChanged

/** Nivel de batería en %, para los estilos de reloj que lo muestran. Sin permisos. */
interface BatteryRepository {
    /** Solo escucha mientras hay suscriptores; null si el sistema no lo informa. */
    val level: Flow<Int?>
}

class SystemBatteryRepository(private val context: Context) : BatteryRepository {

    override val level: Flow<Int?> = callbackFlow {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                trySend(percentOf(intent))
            }
        }
        // ACTION_BATTERY_CHANGED es "sticky": registrarse devuelve el último valor al instante.
        val sticky = ContextCompat.registerReceiver(
            context,
            receiver,
            IntentFilter(Intent.ACTION_BATTERY_CHANGED),
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
        sticky?.let { trySend(percentOf(it)) }
        awaitClose { context.unregisterReceiver(receiver) }
    }.conflate().distinctUntilChanged()

    private fun percentOf(intent: Intent): Int? {
        val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
        return if (level < 0 || scale <= 0) null else (level * PERCENT / scale.toFloat()).roundToInt()
    }

    private companion object {
        const val PERCENT = 100
    }
}
