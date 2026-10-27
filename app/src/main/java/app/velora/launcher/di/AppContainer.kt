package app.velora.launcher.di

import android.content.Context
import app.velora.launcher.core.data.apps.AppLauncher
import app.velora.launcher.core.data.apps.AppsRepository
import app.velora.launcher.core.data.apps.IconRepository
import app.velora.launcher.core.data.apps.RoomAppsCache
import app.velora.launcher.core.data.apps.ShortcutsRepository
import app.velora.launcher.core.data.apps.SystemAppsRepository
import app.velora.launcher.core.data.battery.BatteryRepository
import app.velora.launcher.core.data.battery.SystemBatteryRepository
import app.velora.launcher.core.data.clock.ClockRepository
import app.velora.launcher.core.data.clock.SystemClockRepository
import app.velora.launcher.core.data.contacts.ContactsRepository
import app.velora.launcher.core.data.favorites.FavoritesRepository
import app.velora.launcher.core.data.favorites.FoldersRepository
import app.velora.launcher.core.data.favorites.HiddenAppsRepository
import app.velora.launcher.core.data.favorites.UserListsStore
import app.velora.launcher.core.data.home.DefaultHomeRepository
import app.velora.launcher.core.data.home.SystemDefaultHomeRepository
import app.velora.launcher.core.data.notifications.NotificationsRepository
import app.velora.launcher.core.data.prefs.DataStorePrefsRepository
import app.velora.launcher.core.data.prefs.PrefsRepository
import app.velora.launcher.core.data.usage.UsageRepository
import app.velora.launcher.core.model.AppEntry
import app.velora.launcher.feature.widgets.WidgetHost
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** DI manual (ADR-004). Todo `by lazy`; solo `applicationContext`. */
class AppContainer(context: Context) {

    private val appContext: Context = context.applicationContext

    /** Trabajo que debe sobrevivir a la pantalla, como mantener la lista de apps (ARQUITECTURA §7). */
    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val clockRepository: ClockRepository by lazy { SystemClockRepository(appContext) }

    private val systemApps: SystemAppsRepository by lazy {
        SystemAppsRepository(
            context = appContext,
            scope = applicationScope,
            cache = RoomAppsCache(appContext),
            onPackageChanged = { iconRepository.evictPackage(it) },
            onSystemLoad = { versions -> applicationScope.launch { iconRepository.syncVersions(versions) } },
        )
    }

    val appsRepository: AppsRepository get() = systemApps

    val iconRepository: IconRepository by lazy {
        IconRepository(appContext, applicationScope, infoFor = { systemApps.infoFor(it) })
    }

    val appLauncher: AppLauncher by lazy { AppLauncher(appContext, userFor = { systemApps.userFor(it) }) }

    private val userLists by lazy { UserListsStore(appContext) }

    val hiddenAppsRepository: HiddenAppsRepository by lazy { userLists.hiddenApps(appsRepository.apps) }

    /** Instaladas menos ocultas: lo que muestran la lista, la búsqueda y los favoritos. */
    val visibleApps: StateFlow<List<AppEntry>> by lazy {
        hiddenAppsRepository.visibleApps.stateIn(applicationScope, SharingStarted.Eagerly, emptyList())
    }

    val favoritesRepository: FavoritesRepository by lazy { userLists.favorites(visibleApps) }

    val foldersRepository: FoldersRepository by lazy { userLists.folders(visibleApps) }

    /** Tiempo de uso (ADR-024) y contactos (ADR-025): opt-in, sin leer nada hasta que se activan. */
    val usageRepository: UsageRepository by lazy { UsageRepository(appContext) }

    val contactsRepository: ContactsRepository by lazy { ContactsRepository(appContext) }

    val shortcutsRepository: ShortcutsRepository by lazy {
        ShortcutsRepository(appContext, userFor = { systemApps.userFor(it) })
    }

    val defaultHomeRepository: DefaultHomeRepository by lazy { SystemDefaultHomeRepository(appContext) }

    val prefsRepository: PrefsRepository by lazy { DataStorePrefsRepository(appContext) }

    val batteryRepository: BatteryRepository by lazy { SystemBatteryRepository(appContext) }

    /** Notificaciones y música, solo en memoria (ADR-023). */
    val notificationsRepository: NotificationsRepository by lazy { NotificationsRepository() }

    /** Un único `AppWidgetHost` por proceso (ADR-020). */
    val widgetHost: WidgetHost by lazy { WidgetHost(appContext, userLists.widgets(), applicationScope) }
}
