package app.velora.launcher.feature.hidden

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import app.velora.launcher.core.data.favorites.HiddenAppsRepository
import app.velora.launcher.core.data.hidden.HiddenLock
import app.velora.launcher.core.data.hidden.PinCheck
import app.velora.launcher.core.data.hidden.PrivateSpaceRepository
import app.velora.launcher.core.data.hidden.PrivateSpaceState
import app.velora.launcher.core.model.AppEntry
import app.velora.launcher.di.AppContainer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Lo que muestran el panel de ocultas y la sección de Ajustes. */
@Immutable
data class HiddenUiState(
    val apps: List<AppEntry> = emptyList(),
    /** Apps del espacio privado, solo cuando está desbloqueado. */
    val privateApps: List<AppEntry> = emptyList(),
    val hasPin: Boolean = false,
    /** El PIN ya se pidió en esta visita (o no hay PIN). */
    val unlocked: Boolean = false,
    val privateSpace: PrivateSpaceState = PrivateSpaceState.Unavailable,
    /** Pantalla de PIN abierta, o null. */
    val pin: PinPadState? = null,
    /** false hasta leer si hay PIN: antes de eso no se muestra nada (ni un instante). */
    val loaded: Boolean = false,
) {
    /** Se pueden mostrar las ocultas: sin PIN, o con el PIN ya puesto. */
    val contentVisible: Boolean get() = loaded && (!hasPin || unlocked)
}

/**
 * Apps ocultas con PIN opcional y espacio privado (ADR-033, ADR-034). Lo usan el inicio (panel
 * con gesto) y Ajustes. El PIN nunca se guarda aquí: solo el que se está confirmando, en memoria.
 */
class HiddenViewModel(
    private val hidden: HiddenAppsRepository,
    private val lock: HiddenLock,
    private val privateSpace: PrivateSpaceRepository,
    visibleApps: StateFlow<List<AppEntry>>,
) : ViewModel(),
    HiddenLockActions {

    private val session = MutableStateFlow(Session())

    val state: StateFlow<HiddenUiState> = combine(
        hidden.hiddenApps,
        visibleApps.map { apps -> apps.filter { it.isPrivate } },
        lock.hasPin,
        privateSpace.state,
        session,
    ) { apps, privateApps, hasPin, space, current ->
        HiddenUiState(apps, privateApps, hasPin, current.unlocked, space, current.pin, loaded = true)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), HiddenUiState())

    /** Al abrir el panel: pide el PIN si hace falta y mira el espacio privado. */
    fun onOpen() {
        privateSpace.refresh()
        viewModelScope.launch {
            if (lock.hasPin.first() && !session.value.unlocked) startPin(PinStep.Unlock)
        }
    }

    /** Al cerrar el panel o salir de Ajustes: la próxima vez se vuelve a pedir el PIN. */
    fun relock() = session.update { Session() }

    override fun onRelock() = relock()

    fun onUnhide(app: AppEntry) {
        viewModelScope.launch { hidden.unhide(app.key) }
    }

    fun onPrivateSpace(lockIt: Boolean) {
        viewModelScope.launch { privateSpace.setLocked(lockIt) }
    }

    override fun onUnlock() = startPin(PinStep.Unlock)

    override fun onSetPin() = startPin(PinStep.New)

    override fun onChangePin() = startPin(PinStep.VerifyToChange)

    override fun onRemovePin() = startPin(PinStep.VerifyToRemove)

    override fun onPinCancel() = session.update { it.copy(pin = null, pendingPin = null) }

    override fun onPinEntered(pin: String) {
        val step = session.value.pin?.step ?: return
        setPad { it.copy(busy = true, error = null) }
        viewModelScope.launch {
            when (step) {
                PinStep.New -> onNewPin(pin)
                PinStep.Confirm -> onConfirmPin(pin)
                else -> onVerify(step, pin)
            }
        }
    }

    private suspend fun onVerify(step: PinStep, pin: String) {
        when (val result = lock.check(pin)) {
            PinCheck.Correct -> when (step) {
                PinStep.VerifyToChange -> setPad { PinPadState(PinStep.New) }

                PinStep.VerifyToRemove -> {
                    lock.clearPin()
                    session.update { Session() }
                }

                else -> session.update { Session(unlocked = true) }
            }

            is PinCheck.Wrong -> setPad {
                it.copy(busy = false, error = PinError.Wrong(result.attemptsLeft), waitMs = result.waitMs)
            }

            is PinCheck.Wait -> setPad { it.copy(busy = false, error = null, waitMs = result.remainingMs) }
        }
    }

    private fun onNewPin(pin: String) {
        session.update { it.copy(pendingPin = pin, pin = PinPadState(PinStep.Confirm)) }
    }

    private suspend fun onConfirmPin(pin: String) {
        val pending = session.value.pendingPin
        if (pending == null || pending != pin) {
            session.update { it.copy(pendingPin = null, pin = PinPadState(PinStep.New, error = PinError.Mismatch)) }
            return
        }
        lock.setPin(pin)
        session.update { Session(unlocked = true) }
    }

    private fun startPin(step: PinStep) {
        session.update { it.copy(pin = PinPadState(step), pendingPin = null) }
        // Si quedó una espera de antes (p. ej. tras cerrar el panel), se muestra desde el principio.
        if (step != PinStep.New) {
            viewModelScope.launch {
                val wait = lock.remainingWait()
                if (wait > 0) setPad { it.copy(waitMs = wait) }
            }
        }
    }

    private fun setPad(change: (PinPadState) -> PinPadState) =
        session.update { current -> current.pin?.let { current.copy(pin = change(it)) } ?: current }

    /** Estado de esta visita; [pendingPin] solo vive entre "nuevo PIN" y "confirmar". */
    private data class Session(
        val unlocked: Boolean = false,
        val pin: PinPadState? = null,
        val pendingPin: String? = null,
    )

    companion object {
        private const val STOP_TIMEOUT_MS = 5_000L

        fun factory(container: AppContainer) = viewModelFactory {
            initializer {
                HiddenViewModel(
                    hidden = container.hiddenAppsRepository,
                    lock = container.hiddenLock,
                    privateSpace = container.privateSpaceRepository,
                    visibleApps = container.visibleApps,
                )
            }
        }
    }
}
