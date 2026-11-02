package app.velora.launcher.feature.settings

import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.velora.launcher.R
import app.velora.launcher.core.designsystem.components.ThemeBackground
import app.velora.launcher.core.designsystem.theme.Spacing
import app.velora.launcher.core.designsystem.theme.VeloraTheme
import app.velora.launcher.ui.BackgroundPhotos

/** Qué se puede elegir como fondo: el del tema, una foto nueva o el fondo del teléfono. */
private enum class BackgroundChoice { Theme, Pick }

/**
 * Sección Fondo: el fondo del tema (quitar la foto) o elegir una foto nueva; debajo, los fondos
 * anteriores para volver a uno con un toque o borrarlo. La foto elegida va al borrador y se aplica
 * al guardar. Velora nunca cambia el fondo del teléfono (ADR-037).
 */
@Composable
internal fun BackgroundTab(state: PersonalizeState, actions: PersonalizeActions) {
    val look = state.look
    PanelList {
        sectionHint(R.string.personalize_hint_background)
        groupTitle("background", R.string.personalize_group_background)
        optionGrid("background", BackgroundChoice.entries, columns = BackgroundChoice.entries.size) { choice ->
            when (choice) {
                BackgroundChoice.Theme -> OptionCard(
                    label = stringResource(R.string.personalize_bg_theme),
                    selected = look.photoBackground == null,
                    onClick = { actions.onEdit { it.copy(photoBackground = null) } },
                    aspect = PHOTO_ASPECT,
                ) {
                    VeloraTheme(font = look.font, preset = look.preset, reducedMotion = true) {
                        ThemeBackground(look.preset, Modifier.fillMaxSize())
                    }
                }

                BackgroundChoice.Pick ->
                    ActionTile(R.string.personalize_bg_pick, Icons.Outlined.Add, actions::onPickPhoto)
            }
        }
        if (state.history.isNotEmpty()) {
            groupTitle("history", R.string.personalize_group_history)
            item(key = "history-row") { HistoryRow(state, actions) }
        }
    }
}

/** Una acción con forma de tarjeta (ícono grande al centro), del mismo tamaño que las opciones. */
@Composable
private fun ActionTile(@StringRes label: Int, icon: ImageVector, onClick: () -> Unit) {
    OptionCard(label = stringResource(label), selected = false, onClick = onClick, aspect = PHOTO_ASPECT) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
    }
}

/** Fondos anteriores en una fila: tocar uno lo pone en el borrador; la ✕ lo borra. */
@Composable
private fun HistoryRow(state: PersonalizeState, actions: PersonalizeActions) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = Spacing.m),
        horizontalArrangement = Arrangement.spacedBy(GridGap),
    ) {
        itemsIndexed(state.history, key = { _, path -> path }) { index, path ->
            Box(Modifier.width(HistoryWidth)) {
                OptionCard(
                    label = if (path == state.photoInUse) stringResource(R.string.personalize_bg_in_use) else "",
                    selected = path == state.look.photoBackground,
                    onClick = { actions.onEdit { it.copy(photoBackground = path) } },
                    aspect = PHOTO_ASPECT,
                ) {
                    HistoryThumb(path, stringResource(R.string.personalize_bg_photo, index + 1))
                }
                // La que usa el inicio ahora no se puede borrar.
                if (path != state.photoInUse) DeleteButton { actions.onDeletePhoto(path) }
            }
        }
    }
}

/** Miniatura cargada y reducida fuera del hilo principal; mientras tanto, el fondo de la tarjeta. */
@Composable
private fun HistoryThumb(path: String, description: String) {
    val maxSide = with(LocalDensity.current) { ThumbMaxSide.roundToPx() }
    val bitmap = produceState<ImageBitmap?>(null, path) { value = BackgroundPhotos.thumbnail(path, maxSide) }.value
    if (bitmap != null) {
        Image(
            bitmap = bitmap,
            contentDescription = description,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@Composable
private fun DeleteButton(onClick: () -> Unit) {
    IconButton(onClick = onClick, modifier = Modifier.size(DeleteTouch)) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(DeleteSize)
                .background(MaterialTheme.colorScheme.surfaceContainerHighest, CircleShape),
        ) {
            Icon(
                Icons.Outlined.Close,
                contentDescription = stringResource(R.string.personalize_bg_delete),
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(DeleteIcon),
            )
        }
    }
}

/** Las fotos tienen proporción de pantalla de teléfono. */
private const val PHOTO_ASPECT = 0.56f
private val HistoryWidth = 84.dp
private val ThumbMaxSide = 160.dp
private val DeleteTouch = 40.dp
private val DeleteSize = 24.dp
private val DeleteIcon = 16.dp
