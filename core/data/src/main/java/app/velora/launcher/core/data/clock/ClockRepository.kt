package app.velora.launcher.core.data.clock

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.text.format.DateFormat
import androidx.core.content.ContextCompat
import app.velora.launcher.core.model.ClockState
import java.time.LocalDateTime
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate

/** Fuente de la hora para el inicio. */
interface ClockRepository {
    /**
     * Emite la hora al suscribirse y luego en cada minuto, cambio de hora, zona horaria
     * o formato 12/24 h. Solo escucha mientras hay suscriptores (RENDIMIENTO §6).
     */
    val clock: Flow<ClockState>

    /** Lectura inmediata, para que el primer frame ya muestre la hora. */
    fun current(): ClockState
}

class SystemClockRepository(private val context: Context) : ClockRepository {

    override fun current(): ClockState =
        ClockState(now = LocalDateTime.now(), is24Hour = DateFormat.is24HourFormat(context))

    override val clock: Flow<ClockState> = callbackFlow {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                trySend(current())
            }
        }
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_TIME_TICK)
            // También se envía al cambiar la preferencia 12/24 h.
            addAction(Intent.ACTION_TIME_CHANGED)
            addAction(Intent.ACTION_TIMEZONE_CHANGED)
        }
        ContextCompat.registerReceiver(context, receiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
        trySend(current())
        awaitClose { context.unregisterReceiver(receiver) }
    }.conflate()
}
