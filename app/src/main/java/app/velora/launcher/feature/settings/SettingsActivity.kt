package app.velora.launcher.feature.settings

import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import app.velora.launcher.R
import app.velora.launcher.core.data.log.Logger
import app.velora.launcher.core.designsystem.theme.VeloraTheme

/** Ajustes de Velora. Fuera de `HomeActivity` (ADR-003) y abrible desde cualquier launcher. */
class SettingsActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            VeloraTheme {
                SettingsScreen(onChooseHomeApp = ::openHomeAppSettings)
            }
        }
    }

    private fun openHomeAppSettings() {
        // Algunos fabricantes no tienen la pantalla de "app de inicio": se cae a Ajustes generales.
        val opened = tryStart(Intent(Settings.ACTION_HOME_SETTINGS)) || tryStart(Intent(Settings.ACTION_SETTINGS))
        if (!opened) {
            Toast.makeText(this, R.string.error_open_system_settings, Toast.LENGTH_SHORT).show()
        }
    }

    private fun tryStart(intent: Intent): Boolean = try {
        startActivity(intent)
        true
    } catch (e: ActivityNotFoundException) {
        Logger.w(TAG, e) { "No se pudo abrir ${intent.action}" }
        false
    } catch (e: SecurityException) {
        Logger.w(TAG, e) { "Sin permiso para abrir ${intent.action}" }
        false
    }

    private companion object {
        const val TAG = "SettingsActivity"
    }
}
