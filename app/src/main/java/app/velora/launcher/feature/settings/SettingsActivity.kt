package app.velora.launcher.feature.settings

import android.Manifest
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
import androidx.compose.runtime.CompositionLocalProvider
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
import app.velora.launcher.core.model.Customization
import app.velora.launcher.core.model.FontChoice
import app.velora.launcher.core.model.IconStyle
import app.velora.launcher.core.model.TextSize
import app.velora.launcher.core.model.ThemePreset
import app.velora.launcher.feature.apps.LocalIconOptions
import app.velora.launcher.feature.apps.LocalIconRepository
import app.velora.launcher.feature.apps.LocalIconStyle
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

    private val container get() = (application as VeloraApplication).container

    private val widgetHost get() = container.widgetHost

    /** Packs de íconos instalados (formato común de launchers), para elegir uno en Personalizar. */
    private var iconPacks by mutableStateOf<List<InstalledIconPack>>(emptyList())

    private var status by mutableStateOf(FeatureStatus(lockSupported = LockAccessibilityService.isSupported))

    /** Selector de fotos del sistema: no pide permisos de almacenamiento (ADR-022). */
    private val pickPhoto = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let(::importPhoto)
    }

    /** El permiso de contactos se pide solo después del aviso destacado (ADR-025). */
    private val askContacts = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        status = status.copy(contactsPermission = granted)
        // Si ya lo negó dos veces el sistema no vuelve a preguntar: se abre la ficha de la app.
        if (!granted && !shouldShowRequestPermissionRationale(Manifest.permission.READ_CONTACTS)) {
            openSystemScreen(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", packageName, null))
        }
    }

    private val sharing = ThemeSharing(
        activity = this,
        current = { viewModel.theme.value },
        apply = { theme, photo -> viewModel.onSharedTheme(theme, photo) },
    )

    private val actions = object : SettingsActions {
        override fun onFont(font: FontChoice) = viewModel.onPrefs { setFont(font) }
        override fun onClockStyle(style: ClockStyle) = viewModel.onPrefs { setClockStyle(style) }
        override fun onIconStyle(style: IconStyle) = viewModel.onPrefs { setIconStyle(style) }
        override fun onCustom(change: (Customization) -> Customization) = viewModel.onPrefs { updateCustom(change) }
        override fun onChooseHomeApp() = openHomeAppSettings()
        override fun onUnhide(app: AppEntry) = viewModel.onUnhide(app)
        override fun onMoveFavorite(app: AppEntry, offset: Int) = viewModel.onMoveFavorite(app, offset)
        override fun onTextSize(size: TextSize) = viewModel.onPrefs { setTextSize(size) }
        override fun onLeftHanded(enabled: Boolean) = viewModel.onPrefs { setLeftHanded(enabled) }
        override fun onFullList(enabled: Boolean) = viewModel.onPrefs { setFullList(enabled) }
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

        override fun onExportTheme() = sharing.export()

        override fun onImportTheme() = sharing.import()

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
            viewModel.onFeature(Feature.DoubleTapLock, enabled)
            if (enabled && !status.lockService) onLockAccess()
        }

        override fun onUsageTime(enabled: Boolean) {
            viewModel.onFeature(Feature.UsageTime, enabled)
            if (enabled && !status.usageAccess) onUsageAccess()
        }

        override fun onUsageAccess() {
            openSystemScreen(Settings.ACTION_USAGE_ACCESS_SETTINGS)
        }

        override fun onContactsSearch(enabled: Boolean) {
            viewModel.onFeature(Feature.ContactsSearch, enabled)
            if (enabled && !status.contactsPermission) askContacts.launch(Manifest.permission.READ_CONTACTS)
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
            // La pantalla usa la fuente elegida: se ve el cambio al instante. Los íconos de la vista
            // previa salen del mismo repositorio que el inicio.
            VeloraTheme(font = theme.font, preset = theme.preset, accent = theme.userAccent()) {
                CompositionLocalProvider(
                    LocalIconRepository provides container.iconRepository,
                    LocalIconStyle provides theme.icons,
                    LocalIconOptions provides theme.custom.icons,
                ) {
                    SettingsScreen(
                        theme = theme,
                        preview = preview,
                        lists = SettingsLists(favorites = favorites, hidden = hidden, widgets = widgets),
                        actions = actions,
                        features = features,
                        status = status,
                        photo = photo,
                        iconPacks = iconPacks,
                    )
                }
            }
        }
    }

    /** Al volver de la pantalla del sistema se mira si el usuario dio (o quitó) el acceso. */
    override fun onResume() {
        super.onResume()
        // Al volver de la Play Store puede haber un pack nuevo.
        lifecycleScope.launch {
            iconPacks =
                container.iconPackRepository.installedPacks().map { InstalledIconPack(it.packageName, it.label) }
        }
        lifecycleScope.launch {
            status = withContext(Dispatchers.IO) {
                FeatureStatus(
                    notificationAccess =
                        packageName in NotificationManagerCompat.getEnabledListenerPackages(this@SettingsActivity),
                    lockService = LockAccessibilityService.isEnabled(this@SettingsActivity),
                    lockSupported = LockAccessibilityService.isSupported,
                    usageAccess = container.usageRepository.hasAccess(),
                    contactsPermission = container.contactsRepository.hasPermission(),
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

    private fun openSystemScreen(action: String, data: Uri? = null) {
        if (!tryStart(Intent(action, data)) && !tryStart(Intent(Settings.ACTION_SETTINGS))) {
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
