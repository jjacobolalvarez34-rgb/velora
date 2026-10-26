package app.velora.launcher.feature.settings

import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.velora.launcher.R
import app.velora.launcher.VeloraApplication
import app.velora.launcher.core.data.log.Logger
import app.velora.launcher.core.designsystem.theme.VeloraTheme
import app.velora.launcher.core.model.AppEntry
import app.velora.launcher.core.model.ClockStyle
import app.velora.launcher.core.model.FontChoice
import app.velora.launcher.core.model.IconStyle
import app.velora.launcher.core.model.TextSize
import app.velora.launcher.core.model.ThemePreset

/** Ajustes de Velora. Fuera de `HomeActivity` (ADR-003); se abre manteniendo presionado el inicio. */
class SettingsActivity : ComponentActivity() {

    private val viewModel: SettingsViewModel by viewModels {
        SettingsViewModel.factory((application as VeloraApplication).container)
    }

    private val actions = object : SettingsActions {
        override fun onFont(font: FontChoice) = viewModel.onFont(font)
        override fun onClockStyle(style: ClockStyle) = viewModel.onClockStyle(style)
        override fun onIconStyle(style: IconStyle) = viewModel.onIconStyle(style)
        override fun onChooseHomeApp() = openHomeAppSettings()
        override fun onUnhide(app: AppEntry) = viewModel.onUnhide(app)
        override fun onMoveFavorite(app: AppEntry, offset: Int) = viewModel.onMoveFavorite(app, offset)
        override fun onTextSize(size: TextSize) = viewModel.onTextSize(size)
        override fun onLeftHanded(enabled: Boolean) = viewModel.onLeftHanded(enabled)
        override fun onFullList(enabled: Boolean) = viewModel.onFullList(enabled)
        override fun onPreset(preset: ThemePreset) = viewModel.onPreset(preset)
        override fun onWidgetHeight(appWidgetId: Int, heightDp: Int) = viewModel.onWidgetHeight(appWidgetId, heightDp)
        override fun onRemoveWidget(appWidgetId: Int) = viewModel.onRemoveWidget(appWidgetId)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            val theme = viewModel.theme.collectAsStateWithLifecycle().value ?: return@setContent
            val preview by viewModel.preview.collectAsStateWithLifecycle()
            val hidden by viewModel.hidden.collectAsStateWithLifecycle()
            val favorites by viewModel.favoriteApps.collectAsStateWithLifecycle()
            val widgets by viewModel.widgets.collectAsStateWithLifecycle()
            // La pantalla usa la fuente elegida: se ve el cambio al instante.
            VeloraTheme(font = theme.font, preset = theme.preset) {
                SettingsScreen(
                    theme = theme,
                    preview = preview,
                    lists = SettingsLists(favorites = favorites, hidden = hidden, widgets = widgets),
                    actions = actions,
                )
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
