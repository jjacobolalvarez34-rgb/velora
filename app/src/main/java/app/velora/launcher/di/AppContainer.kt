package app.velora.launcher.di

import android.content.Context
import app.velora.launcher.core.data.apps.AppLauncher
import app.velora.launcher.core.data.apps.AppsRepository
import app.velora.launcher.core.data.apps.IconRepository
import app.velora.launcher.core.data.apps.SystemAppsRepository
import app.velora.launcher.core.data.clock.ClockRepository
import app.velora.launcher.core.data.clock.SystemClockRepository
import app.velora.launcher.core.data.favorites.FavoritesRepository
import app.velora.launcher.core.data.home.DefaultHomeRepository
import app.velora.launcher.core.data.home.SystemDefaultHomeRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/** DI manual (ADR-004). Todo `by lazy`; solo `applicationContext`. */
class AppContainer(context: Context) {

    private val appContext: Context = context.applicationContext

    /** Trabajo que debe sobrevivir a la pantalla, como mantener la lista de apps (ARQUITECTURA §7). */
    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val clockRepository: ClockRepository by lazy { SystemClockRepository(appContext) }

    private val systemApps: SystemAppsRepository by lazy {
        SystemAppsRepository(appContext, applicationScope, onPackageChanged = { iconRepository.evictPackage(it) })
    }

    val appsRepository: AppsRepository get() = systemApps

    val iconRepository: IconRepository by lazy { IconRepository(appContext, infoFor = { systemApps.infoFor(it) }) }

    val appLauncher: AppLauncher by lazy { AppLauncher(appContext, userFor = { systemApps.userFor(it) }) }

    val favoritesRepository: FavoritesRepository by lazy {
        FavoritesRepository.create(appContext, appsRepository.apps)
    }

    val defaultHomeRepository: DefaultHomeRepository by lazy { SystemDefaultHomeRepository(appContext) }
}
