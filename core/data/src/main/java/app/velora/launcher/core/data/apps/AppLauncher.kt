package app.velora.launcher.core.data.apps

import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.LauncherApps
import android.graphics.Rect
import android.net.Uri
import android.os.Bundle
import android.os.UserHandle
import android.os.UserManager
import app.velora.launcher.core.data.log.Logger
import app.velora.launcher.core.model.AppKey
import app.velora.launcher.core.model.AppShortcut

sealed interface LaunchResult {
    data object Launched : LaunchResult
    data object WorkProfilePaused : LaunchResult
    data object Failed : LaunchResult
}

/**
 * Abre apps y pantallas del sistema siempre vía `LauncherApps` (SEGURIDAD §3.1): respeta perfiles
 * y valida que el componente sea abrible. Nunca lanza: los errores vuelven como [LaunchResult].
 */
class AppLauncher(context: Context, private val userFor: (AppKey) -> UserHandle?) {
    private val launcherApps = context.getSystemService(LauncherApps::class.java)
    private val userManager = context.getSystemService(UserManager::class.java)

    fun launch(key: AppKey, sourceBounds: Rect?, options: Bundle?): LaunchResult = withUser(key) { user ->
        launcherApps.startMainActivity(key.component, user, sourceBounds, options)
    }

    fun launchShortcut(shortcut: AppShortcut, sourceBounds: Rect?, options: Bundle?): LaunchResult {
        val key = AppKey(shortcut.packageName, "", shortcut.userSerial)
        return withUser(key) { user ->
            launcherApps.startShortcut(shortcut.packageName, shortcut.id, sourceBounds, options, user)
        }
    }

    fun openAppInfo(key: AppKey, sourceBounds: Rect?): LaunchResult = withUser(key) { user ->
        launcherApps.startAppDetailsActivity(key.component, user, sourceBounds, null)
    }

    /** Intent del diálogo de desinstalación del sistema (requiere `REQUEST_DELETE_PACKAGES`). */
    fun uninstallIntent(key: AppKey): Intent? {
        val user = userFor(key) ?: return null
        return Intent(Intent.ACTION_DELETE, Uri.fromParts("package", key.packageName, null))
            .putExtra(Intent.EXTRA_USER, user)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    private inline fun withUser(key: AppKey, start: (UserHandle) -> Unit): LaunchResult {
        val user = userFor(key) ?: return LaunchResult.Failed
        if (userManager.isQuietModeEnabled(user)) return LaunchResult.WorkProfilePaused
        return try {
            start(user)
            LaunchResult.Launched
        } catch (e: ActivityNotFoundException) {
            failed(key, e)
        } catch (e: SecurityException) {
            failed(key, e)
        } catch (e: IllegalStateException) {
            failed(key, e)
        }
    }

    private fun failed(key: AppKey, e: Exception): LaunchResult {
        Logger.w(TAG, e) { "No se pudo abrir ${key.packageName}" }
        return LaunchResult.Failed
    }

    private val AppKey.component get() = ComponentName(packageName, className)

    private companion object {
        const val TAG = "AppLauncher"
    }
}
