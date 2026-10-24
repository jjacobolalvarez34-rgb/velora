package app.velora.launcher.core.data.apps

import android.content.ComponentName
import android.content.Context
import android.content.pm.LauncherApps
import android.content.pm.ShortcutInfo
import android.os.UserHandle
import app.velora.launcher.core.data.log.Logger
import app.velora.launcher.core.model.AppKey
import app.velora.launcher.core.model.AppShortcut
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Atajos de una app para su menú (B-018). Solo el launcher predeterminado puede leerlos: sin ese
 * permiso devuelve una lista vacía y el menú queda como siempre.
 */
class ShortcutsRepository(context: Context, private val userFor: (AppKey) -> UserHandle?) {

    private val launcherApps = context.getSystemService(LauncherApps::class.java)

    suspend fun shortcutsFor(key: AppKey): List<AppShortcut> = withContext(Dispatchers.IO) {
        val user = userFor(key)
        if (user == null || !hasPermission()) return@withContext emptyList()
        val query = LauncherApps.ShortcutQuery()
            .setPackage(key.packageName)
            .setActivity(ComponentName(key.packageName, key.className))
            .setQueryFlags(
                LauncherApps.ShortcutQuery.FLAG_MATCH_MANIFEST or LauncherApps.ShortcutQuery.FLAG_MATCH_DYNAMIC,
            )
        runCatching { launcherApps.getShortcuts(query, user).orEmpty() }
            .onFailure { Logger.w(TAG, it) { "Sin atajos para ${key.packageName}" } }
            .getOrDefault(emptyList())
            .filter { it.isEnabled }
            .sortedWith(compareBy<ShortcutInfo> { it.isDynamic }.thenBy { it.rank })
            .take(MAX_SHORTCUTS)
            .map { AppShortcut(it.id, it.`package`, key.userSerial, it.shortLabel?.toString().orEmpty()) }
            .filter { it.label.isNotBlank() }
    }

    private fun hasPermission(): Boolean = runCatching { launcherApps.hasShortcutHostPermission() }.getOrDefault(false)

    private companion object {
        const val TAG = "ShortcutsRepository"
        const val MAX_SHORTCUTS = 4
    }
}
