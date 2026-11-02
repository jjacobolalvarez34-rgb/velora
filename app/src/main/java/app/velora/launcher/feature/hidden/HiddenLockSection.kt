package app.velora.launcher.feature.hidden

import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import app.velora.launcher.R
import app.velora.launcher.feature.settings.Option
import app.velora.launcher.feature.settings.SectionTitle

/** Lo que la sección del PIN puede pedir (lo implementa [HiddenViewModel]). */
@Stable
interface HiddenLockActions {
    /** Escribir el PIN para ver las ocultas. */
    fun onUnlock()
    fun onSetPin()
    fun onChangePin()
    fun onRemovePin()
    fun onPinEntered(pin: String)
    fun onPinCancel()

    /** Al salir de la pantalla: la próxima vez se vuelve a pedir. */
    fun onRelock()
}

/**
 * Ajustes → Listas: poner, cambiar o quitar el PIN de las ocultas (ADR-034) y, si hay PIN y aún
 * no se escribió, una tarjeta para desbloquear en lugar de la lista. Mostrar la lista de ocultas
 * (`hiddenSection`) solo si [HiddenUiState.contentVisible]. La pantalla de PIN la dibuja
 * [HiddenPinHost], fuera de la lista.
 */
fun LazyListScope.hiddenLockSection(state: HiddenUiState, actions: HiddenLockActions) {
    if (!state.loaded) return
    item(key = "hidden-lock") { SectionTitle(R.string.hidden_lock_section) }
    if (!state.hasPin) {
        item(key = "hidden-lock-set") {
            Option(
                title = stringResource(R.string.hidden_lock_set),
                subtitle = stringResource(R.string.hidden_lock_set_summary),
                onClick = actions::onSetPin,
                icon = Icons.Outlined.Lock,
            )
        }
        return
    }
    if (!state.unlocked) {
        item(key = "hidden-lock-unlock") {
            Option(
                title = stringResource(R.string.hidden_lock_unlock),
                subtitle = stringResource(R.string.hidden_lock_unlock_summary),
                onClick = actions::onUnlock,
                icon = Icons.Outlined.Lock,
            )
        }
    }
    pinOptions(actions)
}

/** Con PIN puesto: cambiarlo o quitarlo (los dos piden antes el PIN actual). */
private fun LazyListScope.pinOptions(actions: HiddenLockActions) {
    item(key = "hidden-lock-change") {
        Option(
            title = stringResource(R.string.hidden_lock_change),
            subtitle = stringResource(R.string.hidden_lock_on_summary),
            onClick = actions::onChangePin,
            icon = Icons.Outlined.Edit,
        )
    }
    item(key = "hidden-lock-remove") {
        Option(
            title = stringResource(R.string.hidden_lock_remove),
            subtitle = stringResource(R.string.hidden_lock_remove_summary),
            onClick = actions::onRemovePin,
            icon = Icons.Outlined.Delete,
        )
    }
}

/**
 * La pantalla de PIN de Ajustes (es un diálogo: da igual dónde se ponga, pero fuera de la lista
 * para que no desaparezca al desplazar). Al salir de la app las ocultas se vuelven a bloquear.
 */
@Composable
fun HiddenPinHost(state: HiddenUiState, actions: HiddenLockActions) {
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) { actions.onRelock() }
    state.pin?.let { PinDialog(it, onSubmit = actions::onPinEntered, onCancel = actions::onPinCancel) }
}

/** Sin PIN ni acciones: valor por defecto de `SettingsScreen` y de las previews. */
object NoHiddenLockActions : HiddenLockActions {
    override fun onUnlock() = Unit
    override fun onSetPin() = Unit
    override fun onChangePin() = Unit
    override fun onRemovePin() = Unit
    override fun onPinEntered(pin: String) = Unit
    override fun onPinCancel() = Unit
    override fun onRelock() = Unit
}
