package app.velora.launcher

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.content.ContextCompat
import app.velora.launcher.feature.settings.SettingsActivity
import app.velora.launcher.ui.VeloraApp

/**
 * La pantalla de inicio. Única Activity del launcher (ADR-003); ciclo de vida en ARQUITECTURA §5.
 */
class HomeActivity : ComponentActivity() {

    // B-001: si otra app enciende la pantalla, la Activity puede no enterarse de que volvió al frente.
    private val userPresentReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) = refreshWindow()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // El contenido va sobre el wallpaper con velo oscuro: íconos de barras siempre claros.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)
        ContextCompat.registerReceiver(
            this,
            userPresentReceiver,
            IntentFilter(Intent.ACTION_USER_PRESENT),
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )

        val container = (application as VeloraApplication).container
        setContent {
            VeloraApp(
                container = container,
                onOpenSettings = ::openSettings,
            )
        }
    }

    override fun onDestroy() {
        unregisterReceiver(userPresentReceiver)
        super.onDestroy()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) refreshWindow()
    }

    override fun onTopResumedActivityChanged(isTopResumedActivity: Boolean) {
        super.onTopResumedActivityChanged(isTopResumedActivity)
        if (isTopResumedActivity) refreshWindow()
    }

    /** Fuerza un redibujado para no quedar con la UI congelada tras el keyguard (B-001). */
    private fun refreshWindow() {
        window.decorView.invalidate()
    }

    private fun openSettings() {
        startActivity(Intent(this, SettingsActivity::class.java))
    }
}
