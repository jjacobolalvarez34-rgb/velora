package app.velora.launcher.feature.welcome

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import app.velora.launcher.R
import app.velora.launcher.core.designsystem.motion.StaggeredEntrance
import app.velora.launcher.core.designsystem.theme.BrandColors
import app.velora.launcher.core.designsystem.theme.Shapes
import app.velora.launcher.core.designsystem.theme.Sizes
import app.velora.launcher.core.designsystem.theme.Spacing
import app.velora.launcher.core.designsystem.theme.VeloraTheme

private val LogoSize = 168.dp

/**
 * Bienvenida: se ve mientras Velora no sea el inicio predeterminado. Los botones quedan abajo,
 * al alcance del pulgar.
 */
@Composable
fun WelcomeScreen(onSetDefault: () -> Unit, onNotNow: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .fillMaxSize()
            .background(BrandColors.background)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = Spacing.l, vertical = Spacing.xl),
    ) {
        Spacer(Modifier.weight(1f))
        WelcomeIntro()
        Spacer(Modifier.height(Spacing.xl))
        StaggeredEntrance(index = 3) { WelcomePoints() }
        Spacer(Modifier.weight(1f))
        StaggeredEntrance(index = 4) { WelcomeActions(onSetDefault, onNotNow) }
    }
}

@Composable
private fun WelcomeIntro() {
    val type = VeloraTheme.typography
    StaggeredEntrance(index = 0) {
        AnimatedLogo(logoSize = LogoSize)
    }
    StaggeredEntrance(index = 1) {
        Text(
            text = stringResource(R.string.brand_name),
            style = type.brandTitle,
            color = BrandColors.silver,
            modifier = Modifier.semantics { heading() },
        )
    }
    StaggeredEntrance(index = 2) {
        Text(
            text = stringResource(R.string.welcome_tagline),
            style = type.date,
            color = BrandColors.silverMuted,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun WelcomePoints() {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
        listOf(R.string.welcome_point_list, R.string.welcome_point_alphabet, R.string.welcome_point_privacy)
            .forEach { point ->
                Text(
                    text = stringResource(point),
                    style = VeloraTheme.typography.caption,
                    color = BrandColors.silverMuted,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
    }
}

/** Abajo, al alcance del pulgar. */
@Composable
internal fun WelcomeActions(onSetDefault: () -> Unit, onNotNow: () -> Unit) {
    val type = VeloraTheme.typography
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Button(
            onClick = onSetDefault,
            shape = Shapes.sheet,
            colors = ButtonDefaults.buttonColors(
                containerColor = BrandColors.silver,
                contentColor = BrandColors.background,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = Sizes.appRowHeight),
        ) {
            Text(stringResource(R.string.welcome_set_default), style = type.appRow)
        }
        TextButton(onClick = onNotNow, modifier = Modifier.heightIn(min = Sizes.minTouchTarget)) {
            Text(stringResource(R.string.welcome_not_now), style = type.caption, color = BrandColors.silverMuted)
        }
    }
}

@Preview(name = "Claro", heightDp = 760)
@Preview(name = "Oscuro", heightDp = 760, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun WelcomeScreenPreview() {
    VeloraTheme(reducedMotion = true) {
        WelcomeScreen(onSetDefault = {}, onNotNow = {})
    }
}
