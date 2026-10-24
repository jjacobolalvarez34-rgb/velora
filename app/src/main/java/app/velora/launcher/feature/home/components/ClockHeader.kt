package app.velora.launcher.feature.home.components

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import app.velora.launcher.core.designsystem.theme.Spacing
import app.velora.launcher.core.designsystem.theme.VeloraTheme
import app.velora.launcher.feature.home.ClockText

@Composable
fun ClockHeader(clock: ClockText, modifier: Modifier = Modifier) {
    val colors = VeloraTheme.onWallpaper
    Column(modifier = modifier.semantics(mergeDescendants = true) {}) {
        Text(
            text = clock.time,
            style = VeloraTheme.typography.clock,
            color = colors.content,
        )
        Text(
            text = clock.date,
            style = VeloraTheme.typography.date,
            color = colors.contentVariant,
        )
    }
}

@Preview(name = "Claro")
@Preview(name = "Oscuro", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ClockHeaderPreview() {
    VeloraTheme(reducedMotion = true) {
        ClockHeader(
            clock = ClockText(time = "7:05", date = "Jueves, 23 de octubre"),
            modifier = Modifier
                .background(VeloraTheme.onWallpaper.scrimBottom)
                .padding(Spacing.m),
        )
    }
}
