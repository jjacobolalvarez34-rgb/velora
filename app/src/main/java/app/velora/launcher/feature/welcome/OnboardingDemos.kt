package app.velora.launcher.feature.welcome

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.velora.launcher.core.designsystem.theme.BrandColors
import app.velora.launcher.core.designsystem.theme.VeloraTheme
import app.velora.launcher.core.designsystem.theme.fontFamilyOf
import app.velora.launcher.core.model.FontChoice
import kotlin.math.exp
import kotlinx.coroutines.delay

private val DemoSize = 200.dp
private const val REST_PROGRESS = 0.5f
private const val FONT_CYCLE_MS = 1400L
private val DemoLetters = ('A'..'J').map { it.toString() }

/** 0→1→0 en bucle para las demos; fijo a mitad del gesto si se quitaron las animaciones. */
@Composable
private fun demoProgress(): State<Float> {
    val motion = VeloraTheme.motion
    if (motion.reduced) return remember { mutableFloatStateOf(REST_PROGRESS) }
    return rememberInfiniteTransition(label = "demo").animateFloat(0f, 1f, motion.demoLoop(), label = "progress")
}

/** Un dedo sube por un teléfono y la lista sube detrás. */
@Composable
internal fun SwipeUpDemo() {
    val progress by demoProgress()
    Canvas(Modifier.size(DemoSize)) {
        val phone = Size(size.width * 0.55f, size.height)
        val left = (size.width - phone.width) / 2f
        val corner = CornerRadius(phone.width * 0.16f)
        drawRoundRect(BrandColors.silverMuted, Offset(left, 0f), phone, corner, style = Stroke(2.dp.toPx()))
        // La lista sube siguiendo al dedo.
        val sheetTop = phone.height * (1f - 0.7f * progress)
        repeat(4) { row ->
            val y = sheetTop + row * phone.height * 0.12f + phone.height * 0.06f
            if (y < phone.height - 8.dp.toPx()) bar(left + phone.width * 0.15f, y, phone.width * 0.7f)
        }
        val fingerY = phone.height * (0.9f - 0.55f * progress)
        drawCircle(BrandColors.silver, radius = 10.dp.toPx(), center = Offset(size.width / 2f, fingerY))
    }
}

/** Las letras se apartan en ola alrededor del dedo, como en el inicio. */
@Composable
internal fun WaveDemo() {
    val progress by demoProgress()
    val measurer = rememberTextMeasurer()
    val style = VeloraTheme.typography.letter.copy(fontSize = 16.sp, color = BrandColors.silver)
    // Se miden una vez; en cada frame solo se mueven.
    val layouts = remember(measurer, style) { DemoLetters.map { measurer.measure(it, style) } }
    Canvas(Modifier.size(DemoSize)) {
        val rowHeight = size.height / DemoLetters.size
        val fingerY = progress * size.height
        layouts.forEachIndexed { index, layout ->
            val centerY = (index + 0.5f) * rowHeight
            val rows = (fingerY - centerY) / rowHeight
            val g = exp(-(rows * rows) / 8f)
            val x = size.width * 0.8f - 40.dp.toPx() * g
            scale(1f + 0.9f * g, pivot = Offset(x, centerY)) {
                drawText(layout, topLeft = Offset(x - layout.size.width / 2f, centerY - layout.size.height / 2f))
            }
        }
        drawCircle(BrandColors.silverMuted, radius = 12.dp.toPx(), center = Offset(size.width * 0.93f, fingerY))
    }
}

/** Mantener presionada una fila: un anillo crece y aparece el menú. */
@Composable
internal fun LongPressDemo() {
    val progress by demoProgress()
    Canvas(Modifier.size(DemoSize)) {
        val rowY = size.height * 0.3f
        val iconCenter = Offset(size.width * 0.18f, rowY)
        drawCircle(BrandColors.silver, radius = 14.dp.toPx(), center = iconCenter)
        bar(size.width * 0.32f, rowY, size.width * 0.5f)
        drawCircle(
            color = BrandColors.silver.copy(alpha = 1f - progress),
            radius = (14 + 26 * progress).dp.toPx(),
            center = iconCenter,
            style = Stroke(2.dp.toPx()),
        )
        menu(alpha = ((progress - 0.4f) / 0.6f).coerceIn(0f, 1f), top = rowY + 28.dp.toPx())
    }
}

/** "Aa" cambia de fuente: cada quien elige la suya. */
@Composable
internal fun ThemeDemo() {
    val motion = VeloraTheme.motion
    var index by remember { mutableIntStateOf(0) }
    val fonts = FontChoice.entries
    LaunchedEffect(motion.reduced) {
        while (!motion.reduced) {
            delay(FONT_CYCLE_MS)
            index = (index + 1) % fonts.size
        }
    }
    Crossfade(targetState = fonts[index], animationSpec = motion.effectDefault(), label = "font") { font ->
        Text(
            text = "Aa",
            style = VeloraTheme.typography.clockDay.copy(fontFamily = fontFamilyOf(font)),
            color = BrandColors.silver,
        )
    }
}

private fun DrawScope.bar(x: Float, centerY: Float, width: Float) {
    val height = 10.dp.toPx()
    drawRoundRect(
        color = BrandColors.silverMuted,
        topLeft = Offset(x, centerY - height / 2f),
        size = Size(width, height),
        cornerRadius = CornerRadius(height / 2f),
    )
}

private fun DrawScope.menu(alpha: Float, top: Float) {
    if (alpha <= 0f) return
    val left = size.width * 0.3f
    val width = size.width * 0.6f
    drawRoundRect(
        color = BrandColors.silverMuted.copy(alpha = 0.35f * alpha),
        topLeft = Offset(left, top),
        size = Size(width, size.height * 0.45f),
        cornerRadius = CornerRadius(16.dp.toPx()),
    )
    repeat(3) { row ->
        val y = top + (row + 1) * size.height * 0.11f
        drawRoundRect(
            color = BrandColors.silver.copy(alpha = alpha),
            topLeft = Offset(left + 16.dp.toPx(), y - 4.dp.toPx()),
            size = Size(width * (0.75f - row * 0.12f), 8.dp.toPx()),
            cornerRadius = CornerRadius(4.dp.toPx()),
        )
    }
}
