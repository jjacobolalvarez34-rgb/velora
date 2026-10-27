package app.velora.launcher.feature.widgets

import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import app.velora.launcher.R
import app.velora.launcher.core.designsystem.theme.Spacing
import app.velora.launcher.feature.appmenu.MenuItem

private val PreviewSize = 72.dp

/** Mantener presionado el inicio: agregar un widget o ir a los ajustes. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeMenuSheet(onAddWidget: () -> Unit, onOpenSettings: () -> Unit, onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState()) {
        Column(Modifier.padding(bottom = Spacing.l)) {
            MenuItem(Icons.Outlined.Add, stringResource(R.string.home_menu_add_widget), onDismiss, onAddWidget)
            MenuItem(
                Icons.Outlined.Settings,
                stringResource(R.string.settings_launcher_label),
                onDismiss,
                onOpenSettings,
            )
        }
    }
}

/** Todos los widgets instalados, por app, con su vista previa. Tocar uno lo agrega al inicio. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WidgetPickerSheet(host: WidgetHost, onDismiss: () -> Unit) {
    val activity = LocalActivity.current
    val options by produceState(emptyList<WidgetOption>(), host) { value = host.options() }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Text(
            text = stringResource(R.string.widget_picker_title),
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier
                .padding(horizontal = Spacing.l, vertical = Spacing.s)
                .semantics { heading() },
        )
        LazyColumn(Modifier.fillMaxWidth()) {
            items(options, key = {
                it.provider.provider.flattenToString() + it.provider.profile.hashCode()
            }) { option ->
                WidgetOptionRow(host, option) {
                    onDismiss()
                    activity?.let { host.beginAdd(it, option) }
                }
            }
        }
    }
}

@Composable
private fun WidgetOptionRow(host: WidgetHost, option: WidgetOption, onClick: () -> Unit) {
    val densityDpi = LocalContext.current.resources.displayMetrics.densityDpi
    val preview: ImageBitmap? by produceState<ImageBitmap?>(null, option) { value = host.preview(option, densityDpi) }
    ListItem(
        headlineContent = { Text(option.label) },
        supportingContent = { Text(option.appLabel) },
        leadingContent = {
            Box(Modifier.size(PreviewSize), contentAlignment = Alignment.Center) {
                preview?.let {
                    Image(
                        it,
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.size(PreviewSize),
                    )
                }
            }
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = Modifier.clickable(onClick = onClick),
    )
}
