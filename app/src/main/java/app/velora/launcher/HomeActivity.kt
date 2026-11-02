package app.velora.launcher

import android.content.BroadcastReceiver
import android.content.ComponentCallbacks2
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import app.velora.launcher.core.designsystem.theme.Sizes
import app.velora.launcher.feature.settings.SettingsActivity
import app.velora.launcher.ui.VeloraApp
import kotlin.math.roundToInt
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow

/**
 * La pantalla de inicio. Única Activity del launcher (ADR-003); ciclo de vida en ARQUITECTURA §5.
 * También es la entrada del ícono de la app (ADR-014).
 */
class HomeActivity : ComponentActivity() {

    private val container by lazy { (application as VeloraApplication).container }

    /** true = ya estábamos en el inicio y hay que animar el regreso al estado raíz. */
    private val homePresses = MutableSharedFlow<Boolean>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    // B-001: si otra app enciende la pantalla, la Activity puede no enterarse de que volvió al frente.
    private val userPresentReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) = refreshWindow()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // El contenido va sobre el wallpaper con velo oscuro: íconos de barras siempre claros.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)
        ContextCompat.registerReceiver(
            this,
            userPresentReceiver,
            IntentFilter(Intent.ACTION_USER_PRESENT),
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
        // B-021: "atrás" en el inicio no hace nada. Sin esto Android manda el launcher al fondo y lo
        // vuelve a traer (ON_STOP → ON_START), y la entrada de los íconos se repetía con cada toque.
        // Los BackHandler de Compose (lista abierta, buscador) se registran después y ganan.
        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() = Unit
            },
        )
        setContent {
            VeloraApp(container = container, homePresses = homePresses, onOpenSettings = ::openSettings)
        }
    }

    override fun onStart() {
        super.onStart()
        // Los widgets solo se actualizan con el inicio visible (batería, SEGURIDAD §3.4).
        container.widgetHost.startListening()
    }

    override fun onStop() {
        container.widgetHost.stopListening()
        super.onStop()
    }

    @Deprecated("El permiso y la configuración de widgets llegan por la API clásica de resultados")
    @Suppress("DEPRECATION")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (!container.widgetHost.onActivityResult(this, requestCode, resultCode)) {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // Si veníamos de otra app no se anima: solo se deja el estado raíz (ARQUITECTURA §5).
        homePresses.tryEmit(lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED))
    }

    override fun onDestroy() {
        unregisterReceiver(userPresentReceiver)
        super.onDestroy()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) refreshWindow()
    }

    override fun onTopResumedActivityChanged(isTopResumedActivity: Boolean) {
        super.onTopResumedActivityChanged(isTopResumedActivity)
        if (isTopResumedActivity) refreshWindow()
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        // Se sueltan los íconos de la lista; los de favoritos se quedan (RENDIMIENTO §5).
        if (level >= ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN) {
            val favoritePx = (Sizes.favoriteIcon.value * resources.displayMetrics.density).roundToInt()
            container.iconRepository.trimExcept(favoritePx)
        }
    }

    /** Fuerza un redibujado para no quedar con la UI congelada tras el keyguard (B-001). */
    private fun refreshWindow() {
        window.decorView.invalidate()
    }

    private fun openSettings() {
        startActivity(Intent(this, SettingsActivity::class.java))
    }
}
