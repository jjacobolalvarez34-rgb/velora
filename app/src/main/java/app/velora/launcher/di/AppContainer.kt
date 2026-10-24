package app.velora.launcher.di

import android.content.Context
import app.velora.launcher.core.data.clock.ClockRepository
import app.velora.launcher.core.data.clock.SystemClockRepository

/** DI manual (ADR-004). Todo `by lazy`; solo `applicationContext`. */
class AppContainer(context: Context) {

    private val appContext: Context = context.applicationContext

    val clockRepository: ClockRepository by lazy { SystemClockRepository(appContext) }
}
