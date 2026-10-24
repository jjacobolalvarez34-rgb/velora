package app.velora.launcher

import android.app.Application
import android.os.StrictMode
import app.velora.launcher.core.data.log.Logger
import app.velora.launcher.di.AppContainer

class VeloraApplication : Application() {

    /** Perezoso: nada se crea hasta que se usa (RENDIMIENTO §4). */
    val container: AppContainer by lazy { AppContainer(this) }

    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            Logger.enabled = true
            enableStrictMode()
        }
    }

    private fun enableStrictMode() {
        StrictMode.setThreadPolicy(
            StrictMode.ThreadPolicy.Builder()
                .detectAll()
                .penaltyLog()
                .build(),
        )
        StrictMode.setVmPolicy(
            StrictMode.VmPolicy.Builder()
                .detectLeakedClosableObjects()
                .detectLeakedRegistrationObjects()
                .detectActivityLeaks()
                .penaltyLog()
                .build(),
        )
    }
}
