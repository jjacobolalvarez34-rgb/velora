package app.velora.launcher.feature.home.components

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import app.velora.launcher.R
import app.velora.launcher.core.designsystem.theme.Spacing
import app.velora.launcher.core.designsystem.theme.VeloraTheme
import app.velora.launcher.core.model.ClockStyle
import app.velora.launcher.feature.home.ClockText

/** Reloj del inicio en el estilo elegido en Ajustes → Tema. */
@Composable
fun ClockHeader(clock: ClockText, style: ClockStyle, battery: Int?, modifier: Modifier = Modifier) {
    val merged = modifier.semantics(mergeDescendants = true) {}
    when (style) {
        ClockStyle.Classic -> ClassicClock(clock, merged)
        ClockStyle.Centered -> CenteredClock(clock, details(battery, clock.date), merged)
        ClockStyle.Day -> DayClock(clock, details(battery, clock.time, clock.date), merged)
    }
}

/** "Jueves, 23 de octubre · 82 %". Sin batería conocida, se omite. */
@Composable
private fun details(battery: Int?, vararg texts: String): String {
    val batteryText = battery?.let { stringResource(R.string.clock_battery, it) }
    return (texts.toList() + listOfNotNull(batteryText)).filter { it.isNotBlank() }.joinToString(" · ")
}

@Composable
private fun ClassicClock(clock: ClockText, modifier: Modifier) {
    val colors = VeloraTheme.onWallpaper
    Column(modifier = modifier) {
        Text(text = clock.time, style = VeloraTheme.typography.clock, color = colors.content)
        Text(text = clock.date, style = VeloraTheme.typography.date, color = colors.contentVariant)
    }
}

@Composable
private fun CenteredClock(clock: ClockText, details: String, modifier: Modifier) {
    val colors = VeloraTheme.onWallpaper
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = modifier.fillMaxWidth()) {
        Text(text = clock.time, style = VeloraTheme.typography.clock, color = colors.content)
        Text(text = details, style = VeloraTheme.typography.date, color = colors.contentVariant)
    }
}

/**
 * El día en grande, cortado por una franja con hora, fecha y batería. La franja borra los
 * píxeles del día que tiene debajo (capa aparte + `BlendMode.Clear`): se ve el wallpaper.
 */
@Composable
private fun DayClock(clock: ClockText, details: String, modifier: Modifier) {
    val colors = VeloraTheme.onWallpaper
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen },
    ) {
        Text(text = clock.weekdayShort, style = VeloraTheme.typography.clockDay, color = colors.content, maxLines = 1)
        Text(
            text = details,
            style = VeloraTheme.typography.caption,
            color = colors.content,
            textAlign = TextAlign.Center,
            maxLines = 1,
            modifier = Modifier
                .fillMaxWidth()
                .drawBehind { drawRect(Color.Transparent, blendMode = BlendMode.Clear) }
                .padding(vertical = Spacing.s),
        )
    }
}

private val previewClock = ClockText(time = "7:05", date = "Jueves, 23 de octubre", weekdayShort = "JUE")

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
