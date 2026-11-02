package app.velora.launcher.feature.hidden

import android.content.res.Configuration
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import app.velora.launcher.R
import app.velora.launcher.core.data.hidden.PinHasher
import app.velora.launcher.core.designsystem.theme.Spacing
import app.velora.launcher.core.designsystem.theme.VeloraTheme
import kotlinx.coroutines.delay

/** Para qué se pide el PIN. */
enum class PinStep { Unlock, VerifyToChange, VerifyToRemove, New, Confirm }

/** Por qué no se aceptó el último PIN. */
sealed interface PinError {
    data class Wrong(val attemptsLeft: Int) : PinError
    data object Mismatch : PinError
}

/** Estado de la pantalla de PIN; los dígitos escritos viven solo en la UI y nunca se guardan. */
@Immutable
data class PinPadState(
    val step: PinStep,
    val error: PinError? = null,
    /** Espera obligatoria tras demasiados fallos (ms); mientras tanto el teclado no responde. */
    val waitMs: Long = 0L,
    /** Verificando (el hash tarda unas décimas): no se aceptan más toques. */
    val busy: Boolean = false,
)

/** Pantalla de PIN a pantalla completa, encima de lo que haya (panel de ocultas o Ajustes). */
@Composable
fun PinDialog(state: PinPadState, onSubmit: (String) -> Unit, onCancel: () -> Unit) {
    Dialog(onDismissRequest = onCancel, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) {
            PinPad(state, onSubmit, onCancel)
        }
    }
}

/**
 * Teclado numérico grande, puntos en vez de dígitos, error con vibración suave y temblor, y
 * cuenta atrás si hay que esperar. "Quitar animaciones" deja el temblor quieto.
 */
@Composable
fun PinPad(state: PinPadState, onSubmit: (String) -> Unit, onCancel: () -> Unit, modifier: Modifier = Modifier) {
    var entered by remember(state.step) { mutableStateOf("") }
    val remaining = rememberCountdown(state.waitMs)
    val shake = rememberErrorShake(state)
    val enabled = !state.busy && remaining <= 0L
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier.fillMaxSize().padding(Spacing.l),
    ) {
        Text(
            stringResource(state.step.title()),
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(Spacing.l))
        PinDots(entered.length, Modifier.graphicsLayer { translationX = shake.value })
        PinMessage(state.error, remaining)
        Keypad(
            enabled = enabled,
            canSubmit = PinHasher.isValid(entered),
            onDigit = { digit -> if (entered.length < PinHasher.MAX_LENGTH) entered += digit },
            onDelete = { entered = entered.dropLast(1) },
            onSubmit = {
                onSubmit(entered)
                entered = ""
            },
        )
        TextButton(onClick = onCancel) { Text(stringResource(R.string.pin_cancel)) }
    }
}

/** Con cada error: vibración suave y un temblor de los puntos (quieto con "Quitar animaciones"). */
@Composable
private fun rememberErrorShake(state: PinPadState): Animatable<Float, AnimationVector1D> {
    val shake = remember { Animatable(0f) }
    val haptics = LocalHapticFeedback.current
    val motion = VeloraTheme.motion
    LaunchedEffect(state) {
        if (state.error != null && !state.busy) {
            haptics.performHapticFeedback(HapticFeedbackType.Reject)
            if (!motion.reduced) {
                shake.snapTo(SHAKE_PX)
                shake.animateTo(0f, motion.spatialFast())
            }
        }
    }
    return shake
}

@Composable
private fun rememberCountdown(waitMs: Long): Long {
    var remaining by remember(waitMs) { mutableLongStateOf(waitMs) }
    LaunchedEffect(waitMs) {
        while (remaining > 0L) {
            delay(TICK_MS)
            remaining = (remaining - TICK_MS).coerceAtLeast(0L)
        }
    }
    return remaining
}

@Composable
private fun PinDots(count: Int, modifier: Modifier = Modifier) {
    val description = pluralStringResource(R.plurals.pin_digits_entered, count, count)
    Row(
        horizontalArrangement = Arrangement.spacedBy(Spacing.m),
        modifier = modifier
            .height(DotSize)
            .semantics { contentDescription = description },
    ) {
        repeat(maxOf(count, PinHasher.MIN_LENGTH)) { index ->
            val filled = index < count
            Box(
                Modifier
                    .size(DotSize)
                    .background(
                        if (filled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                        CircleShape,
                    ),
            )
        }
    }
}

@Composable
private fun PinMessage(error: PinError?, remainingMs: Long) {
    val text = when {
        remainingMs > 0L -> stringResource(R.string.pin_wait, formatWait(remainingMs))

        error is PinError.Wrong && error.attemptsLeft > 0 ->
            pluralStringResource(R.plurals.pin_wrong_attempts, error.attemptsLeft, error.attemptsLeft)

        error is PinError.Wrong -> stringResource(R.string.pin_wrong)

        error == PinError.Mismatch -> stringResource(R.string.pin_mismatch)

        else -> ""
    }
    Text(
        text = text,
        color = MaterialTheme.colorScheme.error,
        style = MaterialTheme.typography.bodyMedium,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .padding(vertical = Spacing.m)
            .height(MessageHeight)
            .semantics { liveRegion = LiveRegionMode.Polite },
    )
}

@Composable
private fun Keypad(
    enabled: Boolean,
    canSubmit: Boolean,
    onDigit: (Char) -> Unit,
    onDelete: () -> Unit,
    onSubmit: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
        KeyRows.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.l)) {
                row.forEach { digit -> DigitKey(digit, enabled) { onDigit(digit) } }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.l)) {
            IconKey(Icons.Outlined.Clear, R.string.pin_delete, enabled, onDelete)
            DigitKey('0', enabled) { onDigit('0') }
            IconKey(Icons.Outlined.Check, R.string.pin_ok, enabled && canSubmit, onSubmit)
        }
    }
}

private val KeyRows = listOf(listOf('1', '2', '3'), listOf('4', '5', '6'), listOf('7', '8', '9'))

/** "1:05" para la cuenta atrás. */
internal fun formatWait(ms: Long): String {
    val seconds = (ms + TICK_MS - 1) / TICK_MS
    return "%d:%02d".format(seconds / SECONDS_PER_MINUTE, seconds % SECONDS_PER_MINUTE)
}

private fun PinStep.title() = when (this) {
    PinStep.Unlock -> R.string.pin_title_unlock
    PinStep.VerifyToChange, PinStep.VerifyToRemove -> R.string.pin_title_verify
    PinStep.New -> R.string.pin_title_new
    PinStep.Confirm -> R.string.pin_title_confirm
}

private const val TICK_MS = 1_000L
private const val SECONDS_PER_MINUTE = 60
private const val SHAKE_PX = 28f
private val DotSize = 14.dp
private val MessageHeight = 40.dp

@Preview(name = "Claro")
@Preview(name = "Oscuro", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PinPadPreview() {
    VeloraTheme(reducedMotion = true) {
        Surface {
            PinPad(PinPadState(PinStep.Unlock, error = PinError.Wrong(attemptsLeft = 2)), onSubmit = {}, onCancel = {})
        }
    }
}
