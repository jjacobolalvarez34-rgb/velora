package app.velora.launcher.feature.folders

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.velora.launcher.R
import app.velora.launcher.core.designsystem.theme.Sizes
import app.velora.launcher.core.designsystem.theme.Spacing
import app.velora.launcher.core.designsystem.theme.VeloraTheme
import app.velora.launcher.core.model.AppEntry
import app.velora.launcher.core.model.AppFolder
import app.velora.launcher.feature.apps.AppIcon

/** Lo que se puede hacer con las carpetas (Fase 3). Las escrituras van a Room en segundo plano. */
@Stable
interface FolderActions {
    fun open(folder: AppFolder)
    fun create(name: String, first: AppEntry?)
    fun rename(folder: AppFolder, name: String)
    fun delete(folder: AppFolder)
    fun add(folder: AppFolder, app: AppEntry)
    fun remove(folder: AppFolder, app: AppEntry)
}

/**
 * Una carpeta en el inicio, igual de alta que un favorito: un mosaico con hasta cuatro íconos de
 * sus apps, el nombre y cuántas tiene. Tocarla abre su panel.
 */
@Composable
fun FolderRow(folder: AppFolder, onOpen: () -> Unit, modifier: Modifier = Modifier) {
    val colors = VeloraTheme.onWallpaper
    val count = pluralStringResource(R.plurals.folder_count, folder.apps.size, folder.apps.size)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = Sizes.favoriteRowHeight)
            .clickable(role = Role.Button, onClick = onOpen)
            .semantics { contentDescription = "${folder.name}, $count" },
    ) {
        FolderMosaic(folder.apps)
        Spacer(Modifier.width(Spacing.m))
        Column {
            Text(
                folder.name,
                style = VeloraTheme.typography.appRow,
                color = colors.content,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(count, style = VeloraTheme.typography.caption, color = colors.contentVariant)
        }
    }
}

/** Cuatro íconos chiquitos en un círculo suave: se reconoce la carpeta sin abrirla. */
@Composable
internal fun FolderMosaic(apps: List<AppEntry>, size: Dp = Sizes.favoriteIcon) {
    val cell = size * MOSAIC_CELL
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(size)
            .background(VeloraTheme.onWallpaper.surface, CircleShape),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
            apps.take(MOSAIC_APPS).chunked(2).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(1.dp)) {
                    row.forEach { AppIcon(key = it.key, size = cell) }
                }
            }
        }
    }
}

private const val MOSAIC_APPS = 4
private const val MOSAIC_CELL = 0.36f
