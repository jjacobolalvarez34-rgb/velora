package app.velora.launcher.feature.home

import android.content.res.Configuration
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import app.velora.launcher.R
import app.velora.launcher.core.designsystem.motion.StaggeredEntrance
import app.velora.launcher.core.designsystem.theme.Spacing
import app.velora.launcher.core.designsystem.theme.VeloraTheme
import app.velora.launcher.feature.apps.AppActions
import app.velora.launcher.feature.apps.AppRow
import app.velora.launcher.feature.apps.AppRowStyle
import app.velora.launcher.feature.apps.PreviewAppActions
import app.velora.launcher.feature.apps.previewApp
import app.velora.launcher.feature.drawer.WaveAlphabetWidth
import app.velora.launcher.feature.home.components.ClockHeader

/**
 * Inicio: reloj arriba y favoritos anclados abajo, en la zona del pulgar (ESTILO §4).
 * Los gestos (deslizar para la lista, mantener para ajustes) los maneja `LauncherScreen`.
 */
@Composable
fun HomeScreen(uiState: HomeUiState, actions: AppActions, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(start = Spacing.l, top = Spacing.xl, end = Spacing.l + WaveAlphabetWidth, bottom = Spacing.l),
    ) {
        StaggeredEntrance(index = 0) {
            ClockHeader(clock = uiState.clock, style = uiState.clockStyle, battery = uiState.battery)
        }
        Spacer(Modifier.weight(1f))
        // Agregar o quitar un favorito reacomoda la columna con un resorte, sin saltos.
        Column(
            verticalArrangement = Arrangement.spacedBy(Spacing.xs),
            modifier = Modifier.animateContentSize(VeloraTheme.motion.spatialDefault()),
        ) {
            if (uiState.favorites.isEmpty()) {
                StaggeredEntrance(index = 1) {
                    Text(
                        text = stringResource(R.string.home_favorites_empty),
                        style = VeloraTheme.typography.caption,
                        color = VeloraTheme.onWallpaper.contentVariant,
                    )
                }
            }
            uiState.favorites.forEachIndexed { index, app ->
                key(app.key.stableId) {
                    StaggeredEntrance(index = index + 1) {
                        AppRow(app = app, isFavorite = true, style = AppRowStyle.Favorite, actions = actions)
                    }
                }
            }
        }
    }
}

@Preview(name = "Claro", heightDp = 720)
@Preview(name = "Oscuro", heightDp = 720, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun HomeScreenPreview() {
    VeloraTheme(reducedMotion = true) {
        HomeScreen(
            uiState = HomeUiState(
                clock = ClockText(time = "7:05", date = "Jueves, 23 de octubre"),
                favorites = listOf("Teléfono", "Mensajes", "Cámara", "Spotify").map(::previewApp),
            ),
            actions = PreviewAppActions,
            modifier = Modifier.background(MaterialTheme.colorScheme.primaryContainer),
        )
    }
}

@Preview(name = "Sin favoritos", heightDp = 720)
@Composable
private fun HomeScreenEmptyPreview() {
    VeloraTheme(reducedMotion = true) {
        HomeScreen(
            uiState = HomeUiState(clock = ClockText(time = "7:05", date = "Jueves, 23 de octubre")),
            actions = PreviewAppActions,
            modifier = Modifier.background(MaterialTheme.colorScheme.primaryContainer),
        )
    }
}
