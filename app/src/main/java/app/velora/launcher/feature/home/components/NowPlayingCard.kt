package app.velora.launcher.feature.home.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.velora.launcher.R
import app.velora.launcher.core.designsystem.theme.Shapes
import app.velora.launcher.core.designsystem.theme.Spacing
import app.velora.launcher.core.designsystem.theme.VeloraTheme
import app.velora.launcher.core.model.NowPlaying

/** Lo que el reproductor puede pedir. */
interface MediaActions {
    fun playPause()
    fun next()
    fun previous()
}

/**
 * Reproductor contextual (PRODUCTO, Fase 2): aparece solo cuando algo suena y se va igual,
 * creciendo en altura con un resorte (ESTILO §5.3).
 */
@Composable
fun NowPlayingCard(nowPlaying: NowPlaying?, actions: MediaActions, modifier: Modifier = Modifier) {
    val motion = VeloraTheme.motion
    AnimatedVisibility(
        visible = nowPlaying != null,
        enter = expandVertically(motion.spatialDefault()) + fadeIn(motion.effectDefault()),
        exit = shrinkVertically(motion.spatialDefault()) + fadeOut(motion.effectFast()),
        modifier = modifier,
    ) {
        nowPlaying?.let { Card(it, actions) }
    }
}

@Composable
private fun Card(nowPlaying: NowPlaying, actions: MediaActions) {
    val colors = VeloraTheme.onWallpaper
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .padding(vertical = Spacing.s)
            .fillMaxWidth()
            .background(colors.surface, Shapes.menu)
            .padding(horizontal = Spacing.m, vertical = Spacing.s),
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                nowPlaying.title,
                style = VeloraTheme.typography.appRow,
                color = colors.content,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            nowPlaying.artist?.let {
                Text(
                    it,
                    style = VeloraTheme.typography.caption,
                    color = colors.contentVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        MediaButton(MediaIcons.Previous, stringResource(R.string.media_previous), actions::previous)
        MediaButton(
            icon = if (nowPlaying.isPlaying) MediaIcons.Pause else Icons.Outlined.PlayArrow,
            description = stringResource(if (nowPlaying.isPlaying) R.string.media_pause else R.string.media_play),
            onClick = actions::playPause,
        )
        MediaButton(MediaIcons.Next, stringResource(R.string.media_next), actions::next)
    }
}

@Composable
private fun MediaButton(icon: ImageVector, description: String, onClick: () -> Unit) {
    IconButton(onClick = onClick) {
        Icon(icon, contentDescription = description, tint = VeloraTheme.onWallpaper.content)
    }
}

/** Pausa, anterior y siguiente: no vienen en `material-icons-core`, son formas simples. */
private object MediaIcons {
    val Pause: ImageVector = icon("pause") {
        moveTo(7f, 5f)
        lineTo(10f, 5f)
        lineTo(10f, 19f)
        lineTo(7f, 19f)
        close()
        moveTo(14f, 5f)
        lineTo(17f, 5f)
        lineTo(17f, 19f)
        lineTo(14f, 19f)
        close()
    }
    val Next: ImageVector = icon("next") {
        moveTo(6f, 6f)
        lineTo(14.5f, 12f)
        lineTo(6f, 18f)
        close()
        moveTo(16f, 6f)
        lineTo(18f, 6f)
        lineTo(18f, 18f)
        lineTo(16f, 18f)
        close()
    }
    val Previous: ImageVector = icon("previous") {
        moveTo(18f, 6f)
        lineTo(9.5f, 12f)
        lineTo(18f, 18f)
        close()
        moveTo(6f, 6f)
        lineTo(8f, 6f)
        lineTo(8f, 18f)
        lineTo(6f, 18f)
        close()
    }

    private fun icon(name: String, block: androidx.compose.ui.graphics.vector.PathBuilder.() -> Unit) =
        ImageVector.Builder(name, IconSize, IconSize, VIEWPORT, VIEWPORT)
            .path(fill = SolidColor(Color.Black), pathBuilder = block)
            .build()

    private const val VIEWPORT = 24f
    private val IconSize = 24.dp
}
