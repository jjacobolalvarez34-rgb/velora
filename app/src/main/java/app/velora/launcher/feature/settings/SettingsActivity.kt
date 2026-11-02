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
import androidx.compose.runtime.Composable
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
import app.velora.launcher.core.model.ThemeSettings
import app.velora.launcher.feature.apps.LocalIconOptions
import app.velora.launcher.feature.apps.LocalIconRepository
import app.velora.launcher.feature.apps.LocalIconStyle
import app.velora.launcher.feature.hidden.HiddenPinHost
import app.velora.launcher.feature.hidden.HiddenViewModel
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

    private val hiddenViewModel: HiddenViewModel by viewModels { HiddenViewModel.factory(container) }

    /** Respaldo completo (ADR-035): crear antes de onStart, registra los resultados. */
    private val backupSharing = BackupSharing(
        activity = this,
        backup = { container.userBackup },
        photoPath = { viewModel.theme.value?.photoBackground },
    )

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

        /** Deja de usar la foto; el archivo queda en los fondos anteriores. */
        override fun onRemovePhoto() {
            viewModel.onPhotoBackground(null)
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

        override fun onOpenBattery() {
            openSystemScreen(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
        }

        override fun onContactsSearch(enabled: Boolean) {
            viewModel.onFeature(Feature.ContactsSearch, enabled)
            if (enabled && !status.contactsPermission) askContacts.launch(Manifest.permission.READ_CONTACTS)
        }

        override fun onLockAccess() {
            openSystemScreen(Settings.ACTION_ACCESSIBILITY_SETTINGS)
        }
    }

    /** Fondos anteriores (rutas), del más nuevo al más viejo. Se leen del disco fuera del hilo principal. */
    private var photoHistory by mutableStateOf<List<String>>(emptyList())

    /** Personalizar: todo va al borrador del ViewModel; solo "Guardar" cambia el inicio. */
    private val personalize = object : PersonalizeActions {
        override fun onEdit(change: (ThemeSettings) -> ThemeSettings) = viewModel.onEditDraft(change)

        override fun onPickPhoto() = features.onPickPhoto()

        override fun onDeletePhoto(path: String) {
            if (path == viewModel.theme.value?.photoBackground) return
            if (path == viewModel.draft.value?.edited?.photoBackground) {
                viewModel.onEditDraft { it.copy(photoBackground = null) }
            }
            lifecycleScope.launch {
                BackgroundPhotos.delete(this@SettingsActivity, path)
                refreshPhotoHistory()
            }
        }

        override fun onSystemWallpaper() = features.onSystemWallpaper()

        override fun onSave() = viewModel.onSaveDraft()

        override fun onDiscard() = viewModel.onDiscardDraft()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            val theme = viewModel.theme.collectAsStateWithLifecycle().value ?: return@setContent
            val draft by viewModel.draft.collectAsStateWithLifecycle()
            SettingsContent(theme, draft)
        }
    }

    @Composable
    private fun SettingsContent(theme: ThemeSettings, draft: ThemeDraft?) {
        val preview by viewModel.preview.collectAsStateWithLifecycle()
        val hidden by viewModel.hidden.collectAsStateWithLifecycle()
        val favorites by viewModel.favoriteApps.collectAsStateWithLifecycle()
        val widgets by viewModel.widgets.collectAsStateWithLifecycle()
        val hiddenState by hiddenViewModel.state.collectAsStateWithLifecycle()
        val look = draft?.edited ?: theme
        val photo by rememberPhoto(theme.photoBackground)
        val draftPhoto by rememberPhoto(look.photoBackground.takeIf { it != theme.photoBackground })
        // La pantalla usa la fuente y los colores que se están eligiendo: se ve el cambio al instante.
        // Los íconos de la vista previa salen del mismo repositorio que el inicio.
        VeloraTheme(font = look.font, preset = look.preset, accent = look.userAccent()) {
            CompositionLocalProvider(
                LocalIconRepository provides container.iconRepository,
                LocalIconStyle provides look.icons,
                LocalIconOptions provides look.custom.icons,
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
                    personalize = personalizeState(theme, draft, photo, draftPhoto),
                    personalizeActions = personalize,
                    hiddenLock = hiddenState,
                    hiddenLockActions = hiddenViewModel,
                    backup = backupSharing.ui,
                    backupActions = backupSharing,
                )
                HiddenPinHost(hiddenState, hiddenViewModel)
                BackupConfirmHost(backupSharing.ui, backupSharing)
            }
        }
    }

    /** Lo que muestra Personalizar: el borrador si lo hay, y la foto que corresponde a él. */
    private fun personalizeState(
        theme: ThemeSettings,
        draft: ThemeDraft?,
        photo: ImageBitmap?,
        draftPhoto: ImageBitmap?,
    ): PersonalizeState {
        val look = draft?.edited ?: theme
        return PersonalizeState(
            look = look,
            hasChanges = draft?.hasChanges == true,
            photo = if (look.photoBackground == theme.photoBackground) photo else draftPhoto,
            history = photoHistory,
            photoInUse = theme.photoBackground,
        )
    }

    /** La foto de [path], decodificada fuera del hilo principal (null mientras carga o sin foto). */
    @Composable
    private fun rememberPhoto(path: String?) = produceState<ImageBitmap?>(null, path) {
        value = path?.let { BackgroundPhotos.load(it) }
    }

    private suspend fun refreshPhotoHistory() {
        photoHistory = BackgroundPhotos.history(this)
    }

    /** Al volver de la pantalla del sistema se mira si el usuario dio (o quitó) el acceso. */
    override fun onResume() {
        super.onResume()
        // Al volver de la Play Store puede haber un pack nuevo.
        lifecycleScope.launch {
            iconPacks =
                container.iconPackRepository.installedPacks().map { InstalledIconPack(it.packageName, it.label) }
        }
        lifecycleScope.launch { refreshPhotoHistory() }
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

    /**
     * La foto elegida se copia y va al borrador de Personalizar (se aplica al guardar). Las fotos en
     * uso (la del inicio y la del borrador) nunca se borran al recortar el historial.
     */
    private fun importPhoto(uri: Uri) {
        val keep = setOfNotNull(viewModel.theme.value?.photoBackground, viewModel.draft.value?.edited?.photoBackground)
        lifecycleScope.launch {
            val path = BackgroundPhotos.import(this@SettingsActivity, uri, keep)
            if (path == null) {
                Toast.makeText(this@SettingsActivity, R.string.settings_photo_error, Toast.LENGTH_SHORT).show()
            } else {
                viewModel.onEditDraft { it.copy(photoBackground = path) }
            }
            refreshPhotoHistory()
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
