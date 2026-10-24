package app.velora.launcher.feature.home

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.onLongClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.velora.launcher.R
import app.velora.launcher.core.designsystem.components.WallpaperScrim
import app.velora.launcher.core.designsystem.motion.StaggeredEntrance
import app.velora.launcher.core.designsystem.theme.Spacing
import app.velora.launcher.core.designsystem.theme.VeloraTheme
import app.velora.launcher.feature.home.components.ClockHeader

@Composable
fun HomeRoute(viewModel: HomeViewModel, onOpenSettings: () -> Unit, modifier: Modifier = Modifier) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    HomeScreen(uiState = uiState, onOpenSettings = onOpenSettings, modifier = modifier)
}

/**
 * Inicio: reloj arriba; el tercio inferior queda para los favoritos (Fase 1).
 * Mantener presionado el fondo abre los ajustes de Velora.
 */
@Composable
fun HomeScreen(uiState: HomeUiState, onOpenSettings: () -> Unit, modifier: Modifier = Modifier) {
    val haptics = LocalHapticFeedback.current
    val openSettingsLabel = stringResource(R.string.home_open_settings)
    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(onOpenSettings) {
                detectTapGestures(
                    onLongPress = {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        onOpenSettings()
                    },
                )
            }
            .semantics {
                onLongClick(label = openSettingsLabel) {
                    onOpenSettings()
                    true
                }
            },
    ) {
        WallpaperScrim()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(horizontal = Spacing.l, vertical = Spacing.xl),
        ) {
            StaggeredEntrance(index = 0) {
                ClockHeader(clock = uiState.clock)
            }
        }
    }
}

@Preview(name = "Claro")
@Preview(name = "Oscuro", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun HomeScreenPreview() {
    VeloraTheme(reducedMotion = true) {
        HomeScreen(
            uiState = HomeUiState(clock = ClockText(time = "7:05", date = "Jueves, 23 de octubre")),
            onOpenSettings = {},
            modifier = Modifier.background(MaterialTheme.colorScheme.primaryContainer),
        )
    }
}
