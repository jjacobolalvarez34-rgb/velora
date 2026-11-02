package app.velora.launcher.feature.home.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.velora.launcher.R
import app.velora.launcher.core.designsystem.theme.Spacing
import app.velora.launcher.core.designsystem.theme.VeloraTheme
import app.velora.launcher.feature.home.ClockText
import kotlin.math.cos
import kotlin.math.sin

// Relojes Vertical, Contorno, Minimal, Analógico y Palabras (ESTILO §7.1).

/** Horas sobre minutos en una columna angosta; una línea fina y el día con la fecha al lado. */
@Composable
internal fun VerticalClock(clock: ClockText, battery: Int?, modifier: Modifier) {
    val colors = VeloraTheme.onWallpaper
    val (hours, minutes) = splitTime(clock.time)
    val digits = VeloraTheme.typography.clock.copy(fontSize = 60.sp, lineHeight = 60.sp)
    val hasDetail = clock.weekday.isNotEmpty() || clock.dayMonth.isNotEmpty()
    Row(modifier.fillMaxWidth().height(IntrinsicSize.Min), verticalAlignment = Alignment.CenterVertically) {
        Column {
            Text(text = hours, style = digits, color = colors.content)
            Text(text = minutes, style = digits, color = colors.content)
        }
        if (hasDetail) {
            Spacer(Modifier.width(Spacing.m))
            Box(
                Modifier
                    .width(DividerWidth)
                    .fillMaxHeight()
                    .padding(vertical = Spacing.s)
                    .background(colors.contentVariant),
            )
            Spacer(Modifier.width(Spacing.m))
            Column {
                Text(text = clock.weekday, style = VeloraTheme.typography.weekday, color = colors.content)
                Spacer(Modifier.height(Spacing.xs))
                Text(
                    text = withBattery(clock.dayMonth, battery),
                    style = VeloraTheme.typography.date,
                    color = colors.contentVariant,
                )
            }
        }
    }
}

/** Dígitos grandes y huecos: solo el contorno, con un trazo fino del color del texto. */
@Composable
internal fun OutlineClock(clock: ClockText, modifier: Modifier) {
    val colors = VeloraTheme.onWallpaper
    val stroke = with(LocalDensity.current) { OutlineStroke.toPx() }
    val style = VeloraTheme.typography.clock
        .copy(fontSize = 104.sp, lineHeight = 108.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-1).sp)
        .merge(TextStyle(drawStyle = Stroke(width = stroke)))
    Column(modifier.fillMaxWidth()) {
        Text(text = clock.time, style = style, color = colors.content, maxLines = 1)
        if (clock.date.isNotEmpty()) {
            Text(text = clock.date, style = VeloraTheme.typography.date, color = colors.contentVariant)
        }
    }
}

/** Solo la hora, pequeña y muy espaciada; debajo, la fecha en mayúsculas aún más discreta. */
@Composable
internal fun MinimalClock(clock: ClockText, modifier: Modifier) {
    val colors = VeloraTheme.onWallpaper
    val time = VeloraTheme.typography.clock
        .copy(fontSize = 30.sp, lineHeight = 36.sp, letterSpacing = 10.sp, fontWeight = FontWeight.Light)
    val detail = VeloraTheme.typography.caption.copy(fontSize = 11.sp, letterSpacing = 4.sp)
    Column(modifier.fillMaxWidth()) {
        Text(text = clock.time, style = time, color = colors.content, maxLines = 1)
        if (clock.dayMonth.isNotEmpty()) {
            Spacer(Modifier.height(Spacing.xs))
            Text(text = clock.dayMonth.uppercase(), style = detail, color = colors.contentVariant, maxLines = 1)
        }
    }
}

/** Esfera fina con agujas y sin números; el día y la fecha al lado. */
@Composable
internal fun AnalogClock(clock: ClockText, battery: Int?, modifier: Modifier) {
    val colors = VeloraTheme.onWallpaper
    val parts = clockParts(clock.time)
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        if (parts == null) {
            Text(text = clock.time, style = VeloraTheme.typography.clock, color = colors.content)
        } else {
            val ring = colors.contentVariant
            val hand = colors.content
            val accent = colors.accent
            Canvas(Modifier.size(DialSize).semantics { contentDescription = clock.time }) {
                drawDial(parts.first, parts.second, DialColors(ring, hand, accent))
            }
        }
        Spacer(Modifier.width(Spacing.l))
        Column {
            if (clock.weekday.isNotEmpty()) {
                Text(text = clock.weekday, style = VeloraTheme.typography.weekday, color = colors.content)
                Spacer(Modifier.height(Spacing.xs))
            }
            Text(
                text = withBattery(clock.dayMonth, battery),
                style = VeloraTheme.typography.date,
                color = colors.contentVariant,
            )
        }
    }
}

/** Colores de la esfera: aro y marcas, agujas, y el punto del centro. */
private class DialColors(val ring: Color, val hand: Color, val center: Color)

private fun DrawScope.drawDial(hour: Int, minute: Int, colors: DialColors) {
    val radius = size.minDimension / 2f
    val ringWidth = RingWidth.toPx()
    drawCircle(colors.ring, radius = radius - ringWidth / 2f, style = Stroke(ringWidth))
    for (mark in 0 until HOURS_ON_DIAL) {
        val major = mark % QUARTER == 0
        drawCircle(
            colors.ring,
            radius = (if (major) MajorMark else MinorMark).toPx(),
            center = polar(radius * MARK_RADIUS, mark * DEGREES_PER_HOUR),
        )
    }
    val hourAngle = (hour % HOURS_ON_DIAL + minute / MINUTES_PER_HOUR) * DEGREES_PER_HOUR
    val minuteAngle = minute * DEGREES_PER_MINUTE
    drawLine(colors.hand, center, polar(radius * HOUR_HAND, hourAngle), HourHand.toPx(), StrokeCap.Round)
    drawLine(colors.hand, center, polar(radius * MINUTE_HAND, minuteAngle), MinuteHand.toPx(), StrokeCap.Round)
    drawCircle(colors.center, radius = CenterDot.toPx())
}

/** Punto a [length] del centro en el ángulo [degrees] (0 = las doce, en sentido horario). */
private fun DrawScope.polar(length: Float, degrees: Float): Offset {
    val radians = Math.toRadians(degrees.toDouble())
    return Offset(center.x + length * sin(radians).toFloat(), center.y - length * cos(radians).toFloat())
}

/**
 * La hora escrita ("Las siete y media", "Quarter past seven"), redondeada a cinco minutos. Las
 * frases salen de los recursos del idioma del teléfono; la hora va en el color del texto y el resto
 * más suave.
 */
@Composable
internal fun WordsClock(clock: ClockText, modifier: Modifier) {
    val colors = VeloraTheme.onWallpaper
    val parts = clockParts(clock.time)
    val hours = stringArrayResource(R.array.clock_words_hours)
    val patterns = stringArrayResource(R.array.clock_words_patterns)
    val words = VeloraTheme.typography.clock.copy(fontSize = 44.sp, lineHeight = 50.sp, fontWeight = FontWeight.Light)
    Column(modifier.fillMaxWidth()) {
        if (parts == null || hours.size < HOURS_ON_DIAL || patterns.size < HOURS_ON_DIAL) {
            Text(text = clock.time, style = VeloraTheme.typography.clock, color = colors.content)
        } else {
            val (hourIndex, phraseIndex) = wordsIndex(parts.first, parts.second)
            val pieces = patterns[phraseIndex].split(HOUR_SLOT, limit = 2)
            val before = pieces[0].replaceFirstChar { it.titlecase() }
            val after = pieces.getOrElse(1) { "" }
            // Si la frase empieza con la hora ("Las siete y media"), la mayúscula va en la hora.
            val hourWord = hours[hourIndex].let { word ->
                if (before.isEmpty()) word.replaceFirstChar { it.titlecase() } else word
            }
            Text(
                text = buildAnnotatedString {
                    withStyle(SpanStyle(color = colors.contentVariant)) { append(before) }
                    withStyle(SpanStyle(color = colors.content)) { append(hourWord) }
                    withStyle(SpanStyle(color = colors.contentVariant)) { append(after) }
                },
                style = words,
            )
        }
        if (clock.date.isNotEmpty()) {
            Spacer(Modifier.height(Spacing.s))
            Text(text = clock.date, style = VeloraTheme.typography.date, color = colors.contentVariant)
        }
    }
}

/** "07:58" → (7, 58); "7:58 p. m." → (7, 58). null si la hora no trae dígitos latinos. */
internal fun clockParts(time: String): Pair<Int, Int>? {
    val (hours, minutes) = splitTime(time)
    val hour = hours.trim().toIntOrNull()
    val minute = minutes.trim().takeWhile { it.isDigit() }.toIntOrNull()
    return if (hour == null || minute == null) null else hour to minute
}

/**
 * Para la hora en palabras: (hora de 0 a 11, frase de 0 a 11). Se redondea a cinco minutos; de "y
 * media" en adelante se cuenta hacia la hora siguiente ("veinte para las ocho").
 */
internal fun wordsIndex(hour: Int, minute: Int): Pair<Int, Int> {
    val steps = (minute + MINUTES_PER_STEP / 2) / MINUTES_PER_STEP
    val phrase = steps % HOURS_ON_DIAL
    val nextHour = steps >= HOURS_ON_DIAL || phrase > HALF_PAST
    return (hour + if (nextHour) 1 else 0) % HOURS_ON_DIAL to phrase
}

/** Dónde va la hora dentro de cada frase de `clock_words_patterns`. */
private const val HOUR_SLOT = "%1\$s"
private const val HOURS_ON_DIAL = 12
private const val QUARTER = 3
private const val HALF_PAST = 6
private const val MINUTES_PER_STEP = 5
private const val MINUTES_PER_HOUR = 60f
private const val DEGREES_PER_HOUR = 30f
private const val DEGREES_PER_MINUTE = 6f
private const val MARK_RADIUS = 0.82f
private const val HOUR_HAND = 0.5f
private const val MINUTE_HAND = 0.74f
private val DividerWidth = 1.dp
private val OutlineStroke = 1.5.dp
private val DialSize = 120.dp
private val RingWidth = 1.5.dp
private val MajorMark = 2.5.dp
private val MinorMark = 1.dp
private val HourHand = 3.5.dp
private val MinuteHand = 2.dp
private val CenterDot = 3.5.dp
