package app.velora.launcher.feature.drawer

import android.content.res.Configuration
import android.view.HapticFeedbackConstants
import android.view.accessibility.AccessibilityManager
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.CacheDrawScope
import androidx.compose.ui.draw.DrawResult
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import app.velora.launcher.R
import app.velora.launcher.core.data.apps.AppSorting
import app.velora.launcher.core.designsystem.theme.OnWallpaperColors
import app.velora.launcher.core.designsystem.theme.Sizes
import app.velora.launcher.core.designsystem.theme.VeloraTheme
import kotlin.math.exp
import kotlinx.coroutines.launch

/** Ancho de la columna de letras en reposo. */
val WaveAlphabetWidth = 28.dp

/** Cuánto se desplaza hacia el centro la letra bajo el dedo (A en ESTILO §6). */
private val WaveAmplitude = 56.dp

/** Ancho de la campana, en filas de letra (σ en ESTILO §6). */
private const val WAVE_SIGMA_ROWS = 3f

/** Escala extra de la letra bajo el dedo: 1 + 0.9 · g. */
private const val WAVE_SCALE_GAIN = 0.9f

private const val REST_ALPHA = 0.7f

/** La burbuja aparece en cuanto se toca y crece desde el 60 %. */
private const val BUBBLE_MIN_STRENGTH = 0.02f
private const val BUBBLE_START_SCALE = 0.6f
private val BubbleGap = 12.dp

/** Cada letra se centra en la mitad de su fila. */
private const val ROW_CENTER = 0.5f

/** Lo que cambia por frame mientras se arrastra; se lee solo al dibujar. */
@Stable
private class WaveState {
    val fingerY = mutableFloatStateOf(0f)

    /** 1 mientras se toca; vuelve a 0 con un resorte al soltar. */
    val intensity = Animatable(0f)
}

/**
 * El alfabeto ola (ESTILO §6, ADR-008): un único nodo de dibujo para todas las letras.
 * La posición del dedo se lee solo en la fase de dibujo, así arrastrar = 0 recomposiciones.
 *
 * @param onTouchStart al tocar (abre la lista si estaba cerrada).
 * @param onLetterChange índice de la letra bajo el dedo, o -1 al soltar.
 */
@Composable
fun WaveAlphabet(
    letters: List<String>,
    onTouchStart: () -> Unit,
    onLetterChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (letters.isEmpty()) return
    if (rememberTouchExplorationEnabled()) {
        AccessibleAlphabet(letters, onTouchStart, onLetterChange, modifier)
        return
    }
    val view = LocalView.current
    val scope = rememberCoroutineScope()
    val motion = VeloraTheme.motion
    val wave = remember { WaveState() }
    val touchStart by rememberUpdatedState(onTouchStart)
    val letterChange by rememberUpdatedState(onLetterChange)
    val draw = waveDrawing()

    Box(
        modifier = modifier
            .width(WaveAlphabetWidth)
            .pointerInput(letters) {
                letterGestures(
                    letterCount = letters.size,
                    wave = wave,
                    onDown = {
                        touchStart()
                        scope.launch { wave.intensity.snapTo(1f) }
                    },
                    onLetter = { index ->
                        if (index >= 0) view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                        letterChange(index)
                    },
                    onUp = { scope.launch { wave.intensity.animateTo(0f, motion.spatialFast()) } },
                )
            }
            .drawWithCache { with(draw) { drawLetters(letters, wave) } },
    )
}

/** Cómo se dibuja la ola con el tema actual. */
@Composable
private fun waveDrawing(): WaveDrawing {
    val type = VeloraTheme.typography
    return WaveDrawing(
        textMeasurer = rememberTextMeasurer(),
        style = type.letter,
        bubbleStyle = type.letterActive.copy(shadow = null),
        colors = VeloraTheme.onWallpaper,
        // La ola siempre se abre hacia el centro: a la izquierda, o a la derecha en modo zurdo.
        towardCenter = if (LocalLayoutDirection.current == LayoutDirection.Rtl) 1f else -1f,
        bubbleRadius = Sizes.letterBubble / 2,
    )
}

/** Sigue el dedo: avisa cada cambio de letra y -1 al soltar. */
private suspend fun PointerInputScope.letterGestures(
    letterCount: Int,
    wave: WaveState,
    onDown: () -> Unit,
    onLetter: (Int) -> Unit,
    onUp: () -> Unit,
) = awaitEachGesture {
    val down = awaitFirstDown()
    down.consume()
    onDown()
    var current = -1
    var change: PointerInputChange? = down
    while (change != null && change.pressed) {
        val y = change.position.y
        wave.fingerY.floatValue = y
        val index = (y / size.height * letterCount).toInt().coerceIn(0, letterCount - 1)
        if (index != current) {
            current = index
            onLetter(index)
        }
        change.consume()
        change = awaitPointerEvent().changes.firstOrNull { it.id == down.id }
    }
    onLetter(-1)
    onUp()
}

/** Dibujo de la ola: cada letra se desplaza y crece según una campana gaussiana alrededor del dedo. */
private class WaveDrawing(
    private val textMeasurer: TextMeasurer,
    private val style: TextStyle,
    private val bubbleStyle: TextStyle,
    private val colors: OnWallpaperColors,
    private val towardCenter: Float,
    private val bubbleRadius: Dp,
) {
    fun CacheDrawScope.drawLetters(letters: List<String>, wave: WaveState): DrawResult {
        val layouts = letters.map { textMeasurer.measure(it, style) }
        val bubbleLayouts = letters.map { textMeasurer.measure(it, bubbleStyle) }
        val radius = bubbleRadius.toPx()
        val gap = BubbleGap.toPx()
        val rowHeight = size.height / letters.size
        val amplitude = WaveAmplitude.toPx()
        val twoSigmaSq = 2f * WAVE_SIGMA_ROWS * WAVE_SIGMA_ROWS
        val rest = colors.contentVariant.copy(alpha = colors.contentVariant.alpha * REST_ALPHA)
        return onDrawBehind {
            val finger = wave.fingerY.floatValue
            val strength = wave.intensity.value
            for (i in layouts.indices) {
                val layout = layouts[i]
                val centerY = (i + ROW_CENTER) * rowHeight
                val rows = (finger - centerY) / rowHeight
                val g = strength * exp(-(rows * rows) / twoSigmaSq)
                val centerX = size.width / 2f + towardCenter * amplitude * g
                val letterScale = 1f + WAVE_SCALE_GAIN * g
                scale(letterScale, letterScale, pivot = Offset(centerX, centerY)) {
                    drawText(
                        textLayoutResult = layout,
                        color = lerp(rest, colors.content, g),
                        topLeft = Offset(centerX - layout.size.width / 2f, centerY - layout.size.height / 2f),
                    )
                }
            }
            if (strength > BUBBLE_MIN_STRENGTH) {
                val index = (finger / size.height * letters.size).toInt().coerceIn(0, letters.lastIndex)
                bubble(bubbleLayouts[index], finger, strength, radius, amplitude + gap)
            }
        }
    }

    /** Círculo del color de acento con la letra actual, junto a la cresta de la ola. */
    private fun DrawScope.bubble(
        layout: TextLayoutResult,
        fingerY: Float,
        strength: Float,
        radius: Float,
        offset: Float,
    ) {
        val r = radius * (BUBBLE_START_SCALE + (1f - BUBBLE_START_SCALE) * strength)
        val center = Offset(
            x = size.width / 2f + towardCenter * (offset + r),
            y = fingerY.coerceIn(r, size.height - r),
        )
        drawCircle(colors.accent.copy(alpha = strength), radius = r, center = center)
        drawText(
            textLayoutResult = layout,
            color = colors.onAccent.copy(alpha = strength),
            topLeft = Offset(center.x - layout.size.width / 2f, center.y - layout.size.height / 2f),
        )
    }
}

/** Con TalkBack el alfabeto es una columna de botones de letra (ESTILO §9). */
@Composable
private fun AccessibleAlphabet(
    letters: List<String>,
    onTouchStart: () -> Unit,
    onLetterChange: (Int) -> Unit,
    modifier: Modifier,
) {
    Column(modifier = modifier.width(WaveAlphabetWidth), horizontalAlignment = Alignment.CenterHorizontally) {
        letters.forEachIndexed { index, letter ->
            val description = if (letter == AppSorting.SYMBOLS_SECTION) {
                stringResource(R.string.alphabet_symbols)
            } else {
                stringResource(R.string.alphabet_letter, letter)
            }
            Text(
                text = letter,
                style = VeloraTheme.typography.letter,
                color = VeloraTheme.onWallpaper.contentVariant,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clickable {
                        onTouchStart()
                        onLetterChange(index)
                    }
                    .semantics { contentDescription = description },
            )
        }
    }
}

@Composable
private fun rememberTouchExplorationEnabled(): Boolean {
    val manager = LocalContext.current.getSystemService(AccessibilityManager::class.java)
    var enabled by remember { mutableStateOf(manager?.isTouchExplorationEnabled == true) }
    DisposableEffect(manager) {
        val listener = AccessibilityManager.TouchExplorationStateChangeListener { enabled = it }
        manager?.addTouchExplorationStateChangeListener(listener)
        onDispose { manager?.removeTouchExplorationStateChangeListener(listener) }
    }
    return enabled
}

@Preview(name = "Claro", heightDp = 520)
@Preview(name = "Oscuro", heightDp = 520, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun WaveAlphabetPreview() {
    VeloraTheme(reducedMotion = true) {
        Box(Modifier.background(VeloraTheme.onWallpaper.drawerScrim).height(520.dp).width(120.dp)) {
            WaveAlphabet(
                letters = listOf("#") + ('A'..'Z').map { it.toString() },
                onTouchStart = {},
                onLetterChange = {},
                modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight(),
            )
        }
    }
}
