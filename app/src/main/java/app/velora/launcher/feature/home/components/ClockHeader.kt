package app.velora.launcher.feature.home.components

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.tooling.preview.Preview
import app.velora.launcher.R
import app.velora.launcher.core.designsystem.theme.Spacing
import app.velora.launcher.core.designsystem.theme.VeloraTheme
import app.velora.launcher.core.model.ClockStyle
import app.velora.launcher.feature.home.ClockText

/**
 * Reloj del inicio. Sencillo a propósito: hora delgada y grande, fecha tranquila debajo
 * (ESTILO §7.1). Los estilos solo cambian la alineación y qué acompaña a la hora.
 */
@Composable
fun ClockHeader(clock: ClockText, style: ClockStyle, battery: Int?, modifier: Modifier = Modifier) {
    val merged = modifier.semantics(mergeDescendants = true) {}
    when (style) {
        ClockStyle.Classic -> ClockColumn(clock.time, clock.date, merged, Alignment.Start)

        ClockStyle.Centered -> ClockColumn(
            clock.time,
            withBattery(clock.date, battery),
            merged,
            Alignment.CenterHorizontally,
        )

        ClockStyle.Day -> DayClock(clock, battery, merged)
    }
}

/** "Lunes, 26 de octubre · 82 %". Sin batería conocida, se omite. */
@Composable
private fun withBattery(text: String, battery: Int?): String =
    battery?.let { "$text · ${stringResource(R.string.clock_battery, it)}" } ?: text

@Composable
private fun ClockColumn(time: String, detail: String, modifier: Modifier, alignment: Alignment.Horizontal) {
    val colors = VeloraTheme.onWallpaper
    Column(horizontalAlignment = alignment, modifier = modifier.fillMaxWidth()) {
        Text(text = time, style = VeloraTheme.typography.clock, color = colors.content)
        Text(text = detail, style = VeloraTheme.typography.date, color = colors.contentVariant)
    }
}

/** "LUNES" espaciado y discreto arriba; la hora; "26 de octubre · 82 %" debajo. */
@Composable
private fun DayClock(clock: ClockText, battery: Int?, modifier: Modifier) {
    val colors = VeloraTheme.onWallpaper
    Column(modifier = modifier.fillMaxWidth()) {
        Text(text = clock.weekday, style = VeloraTheme.typography.weekday, color = colors.contentVariant)
        Spacer(Modifier.height(Spacing.xs))
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
