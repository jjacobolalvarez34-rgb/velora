package app.velora.launcher.ui

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect

/**
 * Cuenta las vueltas al inicio para repetir la entrada de los íconos (ESTILO §5.3): al volver de
 * otra app y al desbloquear el teléfono (`ACTION_USER_PRESENT`, sin permisos).
 */
@Composable
internal fun rememberEntranceGeneration(): Int {
    var generation by remember { mutableIntStateOf(0) }
    var away by remember { mutableIntStateOf(0) }
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) { away++ }
    LifecycleEventEffect(Lifecycle.Event.ON_START) {
        // El primer ON_START es el arranque: esa entrada ya la hace la composición.
        if (away > 0) generation++
    }
    val context = LocalContext.current
    DisposableEffect(context) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                generation++
            }
        }
        ContextCompat.registerReceiver(
            context,
            receiver,
            IntentFilter(Intent.ACTION_USER_PRESENT),
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
        onDispose { context.unregisterReceiver(receiver) }
    }
    return generation
}
