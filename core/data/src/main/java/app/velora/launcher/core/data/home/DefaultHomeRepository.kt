package app.velora.launcher.core.data.home

import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

/** ¿Es Velora la app de inicio predeterminada? Alimenta la landing de bienvenida. */
interface DefaultHomeRepository {
    /** null mientras no se sabe (para no mostrar la landing por un frame). */
    val isDefault: StateFlow<Boolean?>

    /** "Ahora no" se recuerda mientras viva el proceso. */
    val welcomeDismissed: StateFlow<Boolean>

    fun dismissWelcome()

    suspend fun refresh()

    /**
     * Intent para pedir ser el inicio: diálogo del sistema en Android 10+
     * (`startActivityForResult`), pantalla de "App de inicio" antes.
     */
    fun requestIntent(): Intent
}

class SystemDefaultHomeRepository(private val context: Context) : DefaultHomeRepository {

    private val _isDefault = MutableStateFlow<Boolean?>(null)
    override val isDefault: StateFlow<Boolean?> = _isDefault.asStateFlow()

    private val _welcomeDismissed = MutableStateFlow(false)
    override val welcomeDismissed: StateFlow<Boolean> = _welcomeDismissed.asStateFlow()

    override fun dismissWelcome() {
        _welcomeDismissed.value = true
    }

    override suspend fun refresh() {
        _isDefault.value = withContext(Dispatchers.IO) { readIsDefault() }
    }

    override fun requestIntent(): Intent {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roles = context.getSystemService(RoleManager::class.java)
            if (roles.isRoleAvailable(RoleManager.ROLE_HOME) && !roles.isRoleHeld(RoleManager.ROLE_HOME)) {
                return roles.createRequestRoleIntent(RoleManager.ROLE_HOME)
            }
        }
        return Intent(Settings.ACTION_HOME_SETTINGS)
    }

    private fun readIsDefault(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return context.getSystemService(RoleManager::class.java).isRoleHeld(RoleManager.ROLE_HOME)
        }
        val home = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        val resolved = context.packageManager.resolveActivity(home, PackageManager.MATCH_DEFAULT_ONLY)
        return resolved?.activityInfo?.packageName == context.packageName
    }
}
