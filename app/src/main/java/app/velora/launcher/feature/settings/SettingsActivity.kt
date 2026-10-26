package app.velora.launcher.feature.settings

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
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
import app.velora.launcher.services.LockAccessibilityService
import app.velora.launcher.services.VeloraNotificationListener
import app.velora.launcher.ui.BackgroundPhotos
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Ajustes de Velora. Fuera de `HomeActivity` (ADR-003); se abre manteniendo presionado el inicio. */
class SettingsActivity : ComponentActivity() {

    private val viewModel: SettingsViewModel by viewModels {
        SettingsViewModel.factory((application as VeloraApplication).container)
    }

    private val widgetHost get() = (application as VeloraApplication).container.widgetHost

    private var status by mutableStateOf(FeatureStatus(lockSupported = LockAccessibilityService.isSupported))

    /** Selector de fotos del sistema: no pide permisos de almacenamiento (ADR-022). */
    private val pickPhoto = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let(::importPhoto)
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
        override fun onWidgetHeight(appWidgetId: Int, heightDp: Int) =
            widgetHost.update { setHeight(appWidgetId, heightDp) }
        override fun onRemoveWidget(appWidgetId: Int) = widgetHost.remove(appWidgetId)
        override fun onMoveWidget(appWidgetId: Int, offset: Int) = widgetHost.update { move(appWidgetId, offset) }
    }

    private val features = object : FeatureActions {
        override fun onPickPhoto() {
            pickPhoto.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }

        override fun onRemovePhoto() {
            viewModel.onPhotoBackground(null)
            lifecycleScope.launch { BackgroundPhotos.delete(this@SettingsActivity) }
        }

        override fun onSystemWallpaper() {
            val chooser = Intent.createChooser(
                Intent(Intent.ACTION_SET_WALLPAPER),
                getString(R.string.settings_system_wallpaper),
            )
            if (!tryStart(
                    chooser,
                )
            ) {
                Toast.makeText(
                    this@SettingsActivity,
                    R.string.error_open_system_settings,
                    Toast.LENGTH_SHORT,
                ).show()
            }
        }

        override fun onNotifications(enabled: Boolean) {
            viewModel.onNotifications(enabled) {
                when {
                    !enabled -> VeloraNotificationListener.disconnect()
                    status.notificationAccess -> VeloraNotificationListener.reconnect(this@SettingsActivity)
                    else -> onNotificationAccess()
                }
            }
        }

        override fun onNotificationAccess() {
            openSystemScreen(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
        }

        override fun onDoubleTapLock(enabled: Boolean) {
            viewModel.onDoubleTapLock(enabled)
            if (enabled && !status.lockService) onLockAccess()
        }

        override fun onLockAccess() {
            openSystemScreen(Settings.ACTION_ACCESSIBILITY_SETTINGS)
        }
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
            val photo by produceState<ImageBitmap?>(null, theme.photoBackground) {
                value = theme.photoBackground?.let { BackgroundPhotos.load(it) }
            }
            // La pantalla usa la fuente elegida: se ve el cambio al instante.
            VeloraTheme(font = theme.font, preset = theme.preset) {
                SettingsScreen(
                    theme = theme,
                    preview = preview,
                    lists = SettingsLists(favorites = favorites, hidden = hidden, widgets = widgets),
                    actions = actions,
                    features = features,
                    status = status,
                    photo = photo,
                )
            }
        }
    }

    /** Al volver de la pantalla del sistema se mira si el usuario dio (o quitó) el acceso. */
    override fun onResume() {
        super.onResume()
        lifecycleScope.launch {
            status = withContext(Dispatchers.IO) {
                FeatureStatus(
                    notificationAccess =
                        packageName in NotificationManagerCompat.getEnabledListenerPackages(this@SettingsActivity),
                    lockService = LockAccessibilityService.isEnabled(this@SettingsActivity),
                    lockSupported = LockAccessibilityService.isSupported,
                )
            }
        }
    }

    private fun importPhoto(uri: Uri) {
        lifecycleScope.launch {
            val path = BackgroundPhotos.import(this@SettingsActivity, uri)
            if (path == null) {
                Toast.makeText(this@SettingsActivity, R.string.settings_photo_error, Toast.LENGTH_SHORT).show()
            } else {
                viewModel.onPhotoBackground(path)
            }
        }
    }

    private fun openSystemScreen(action: String) {
        if (!tryStart(Intent(action)) && !tryStart(Intent(Settings.ACTION_SETTINGS))) {
            Toast.makeText(this, R.string.error_open_system_settings, Toast.LENGTH_SHORT).show()
        }
    }

    private fun openHomeAppSettings() {
        // Algunos fabricantes no tienen la pantalla de "app de inicio": se cae a Ajustes generales.
        openSystemScreen(Settings.ACTION_HOME_SETTINGS)
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
