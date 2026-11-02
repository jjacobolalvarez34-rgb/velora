package app.velora.launcher.feature.home.components

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import app.velora.launcher.R
import app.velora.launcher.core.designsystem.theme.Spacing
import app.velora.launcher.core.designsystem.theme.VeloraTheme
import app.velora.launcher.core.model.ClockStyle
import app.velora.launcher.feature.home.ClockText

/**
 * Reloj del inicio. Sencillo a propósito: hora delgada y grande, fecha tranquila debajo
 * (ESTILO §7.1). Los estilos cambian la composición; [showDate] = false deja solo la hora
 * (elementos visibles, ADR-028).
 */
@Composable
fun ClockHeader(
    clock: ClockText,
    style: ClockStyle,
    battery: Int?,
    modifier: Modifier = Modifier,
    showDate: Boolean = true,
) {
    val merged = modifier.semantics(mergeDescendants = true) {}
    val shown = if (showDate) clock else clock.copy(date = "", dayMonth = "", weekday = "")
    ClockByStyle(shown, style, if (showDate) battery else null, merged)
}

@Composable
private fun ClockByStyle(clock: ClockText, style: ClockStyle, battery: Int?, merged: Modifier) {
    when (style) {
        ClockStyle.Classic -> ClockColumn(clock.time, clock.date, merged, Alignment.Start)

        ClockStyle.Centered -> ClockColumn(
            clock.time,
            withBattery(clock.date, battery),
            merged,
            Alignment.CenterHorizontally,
        )

        ClockStyle.Day -> DayClock(clock, battery, merged)

        ClockStyle.Stacked -> StackedClock(clock, merged)

        ClockStyle.Banner -> BannerClock(clock, battery, merged)

        ClockStyle.Duo -> DuoClock(clock, merged)

        ClockStyle.Vertical -> VerticalClock(clock, battery, merged)

        ClockStyle.Outline -> OutlineClock(clock, merged)

        ClockStyle.Minimal -> MinimalClock(clock, merged)

        ClockStyle.Analog -> AnalogClock(clock, battery, merged)

        ClockStyle.Words -> WordsClock(clock, merged)
    }
}

/** Horas arriba, minutos abajo, enormes y finos: el reloj es el cuadro. */
@Composable
private fun StackedClock(clock: ClockText, modifier: Modifier) {
    val colors = VeloraTheme.onWallpaper
    val (hours, minutes) = splitTime(clock.time)
    val big = VeloraTheme.typography.clock.copy(fontSize = 104.sp, lineHeight = 96.sp)
    Column(modifier = modifier.fillMaxWidth()) {
        Text(text = hours, style = big, color = colors.content)
        Text(text = minutes, style = big, color = colors.content)
        if (clock.date.isNotEmpty()) {
            Text(
                text = clock.date,
                style = VeloraTheme.typography.date,
                color = colors.contentVariant,
            )
        }
    }
}

/** El día en letras gigantes de lado a lado; hora y fecha chiquitas a los costados. */
@Composable
private fun BannerClock(clock: ClockText, battery: Int?, modifier: Modifier) {
    val colors = VeloraTheme.onWallpaper
    val small = VeloraTheme.typography.caption.copy(letterSpacing = 2.sp)
    Column(modifier = modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = clock.weekdayShort.ifEmpty { clock.time },
            style = VeloraTheme.typography.clock.copy(fontSize = 96.sp, letterSpacing = 18.sp, lineHeight = 100.sp),
            color = colors.content,
            maxLines = 1,
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(text = clock.time, style = small, color = colors.contentVariant)
            Text(text = clock.dayMonth.uppercase(), style = small, color = colors.contentVariant)
            battery?.let {
                Text(text = stringResource(R.string.clock_battery, it), style = small, color = colors.contentVariant)
            }
        }
    }
}

/** Horas del color del texto y minutos del acento, gruesos y juntos. */
@Composable
private fun DuoClock(clock: ClockText, modifier: Modifier) {
    val colors = VeloraTheme.onWallpaper
    val (hours, minutes) = splitTime(clock.time)
    val style = VeloraTheme.typography.clock.copy(
        fontSize = 80.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = (-2).sp,
    )
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = buildAnnotatedString {
                withStyle(SpanStyle(color = colors.content)) { append(hours) }
                withStyle(SpanStyle(color = colors.accent)) { append(minutes) }
            },
            style = style,
        )
        if (clock.date.isNotEmpty()) {
            Text(
                text = clock.date,
                style = VeloraTheme.typography.date,
                color = colors.contentVariant,
            )
        }
    }
}

/** "07:58" → ("07", "58"); "7:58 p. m." → ("7", "58 p. m."). Sin dos puntos, todo arriba. */
internal fun splitTime(time: String): Pair<String, String> {
    val colon = time.indexOf(':')
    return if (colon < 0) time to "" else time.substring(0, colon) to time.substring(colon + 1)
}

/** "Lunes, 26 de octubre · 82 %". Sin batería conocida, se omite. */
@Composable
internal fun withBattery(text: String, battery: Int?): String =
    battery?.let { if (text.isEmpty()) "" else "$text · ${stringResource(R.string.clock_battery, it)}" } ?: text

@Composable
private fun ClockColumn(time: String, detail: String, modifier: Modifier, alignment: Alignment.Horizontal) {
    val colors = VeloraTheme.onWallpaper
    Column(horizontalAlignment = alignment, modifier = modifier.fillMaxWidth()) {
        Text(text = time, style = VeloraTheme.typography.clock, color = colors.content)
        if (detail.isNotEmpty()) Text(text = detail, style = VeloraTheme.typography.date, color = colors.contentVariant)
    }
}

/** "LUNES" espaciado y discreto arriba; la hora; "26 de octubre · 82 %" debajo. */
@Composable
private fun DayClock(clock: ClockText, battery: Int?, modifier: Modifier) {
    val colors = VeloraTheme.onWallpaper
    Column(modifier = modifier.fillMaxWidth()) {
        if (clock.weekday.isNotEmpty()) {
            Text(text = clock.weekday, style = VeloraTheme.typography.weekday, color = colors.contentVariant)
            Spacer(Modifier.height(Spacing.xs))
        }
        Text(text = clock.time, style = VeloraTheme.typography.clock, color = colors.content)
        Text(
            text = withBattery(clock.dayMonth, battery),
            style = VeloraTheme.typography.date,
            color = colors.contentVariant,
        )
    }
}

private val previewClock = ClockText(
    time = "07:58",
    date = "Lunes, 26 de octubre",
    weekdayShort = "LUN",
    weekday = "LUNES",
    dayMonth = "26 de octubre",
)

@Preview(name = "Clásico")
@Preview(name = "Clásico oscuro", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ClassicPreview() = ClockPreview(ClockStyle.Classic)

@Preview(name = "Centrado", widthDp = 360)
@Composable
private fun CenteredPreview() = ClockPreview(ClockStyle.Centered)

@Preview(name = "Día", widthDp = 360)
@Composable
private fun DayPreview() = ClockPreview(ClockStyle.Day)

@Preview(name = "Apilado", widthDp = 360)
@Composable
private fun StackedPreview() = ClockPreview(ClockStyle.Stacked)

@Preview(name = "Banda", widthDp = 360)
@Composable
private fun BannerPreview() = ClockPreview(ClockStyle.Banner)

@Preview(name = "Dúo", widthDp = 360)
@Composable
private fun DuoPreview() = ClockPreview(ClockStyle.Duo)

@Preview(name = "Vertical", widthDp = 360)
@Composable
private fun VerticalPreview() = ClockPreview(ClockStyle.Vertical)

@Preview(name = "Contorno", widthDp = 360)
@Composable
private fun OutlinePreview() = ClockPreview(ClockStyle.Outline)

@Preview(name = "Minimal", widthDp = 360)
@Composable
private fun MinimalPreview() = ClockPreview(ClockStyle.Minimal)

@Preview(name = "Analógico", widthDp = 360)
@Preview(name = "Analógico oscuro", widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun AnalogPreview() = ClockPreview(ClockStyle.Analog)

@Preview(name = "Palabras", widthDp = 360)
@Composable
private fun WordsPreview() = ClockPreview(ClockStyle.Words)

@Composable
private fun ClockPreview(style: ClockStyle) {
    VeloraTheme(reducedMotion = true) {
        ClockHeader(
            clock = previewClock,
            style = style,
            battery = 82,
            modifier = Modifier
                .background(VeloraTheme.onWallpaper.scrimBottom)
                .padding(Spacing.m),
        )
    }
}
