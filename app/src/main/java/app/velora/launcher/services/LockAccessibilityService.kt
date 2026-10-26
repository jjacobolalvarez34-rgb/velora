package app.velora.launcher.services

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.content.Intent
import android.os.Build
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityManager
import java.lang.ref.WeakReference

/**
 * Doble toque en el inicio para bloquear la pantalla (ADR-009, SEGURIDAD §3.3). Es **lo único**
 * que hace: no lee el contenido de la pantalla ni recibe eventos (`canRetrieveWindowContent=false`).
 * Opt-in con explicación previa; sin él, el doble toque no hace nada.
 */
class LockAccessibilityService : AccessibilityService() {

    override fun onServiceConnected() {
        current = WeakReference(this)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) = Unit

    override fun onInterrupt() = Unit

    override fun onUnbind(intent: Intent?): Boolean {
        current = null
        return super.onUnbind(intent)
    }

    companion object {
        private var current: WeakReference<LockAccessibilityService>? = null

        /** Bloquear el gesto existe desde Android 9. */
        val isSupported: Boolean get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.P

        /** true si se bloqueó; false si el servicio no está activo (hay que activarlo). */
        fun lock(): Boolean {
            if (!isSupported) return false
            val service = current?.get() ?: return false
            return service.performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN)
        }

        /** Los fabricantes a veces lo apagan solos (B-002): se comprueba al volver a Ajustes. */
        fun isEnabled(context: Context): Boolean {
            val manager = context.getSystemService(AccessibilityManager::class.java) ?: return false
            return manager.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK).any {
                it.resolveInfo.serviceInfo.packageName == context.packageName &&
                    it.resolveInfo.serviceInfo.name == LockAccessibilityService::class.java.name
            }
        }
    }
}
