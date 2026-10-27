package app.velora.launcher.feature.drawer

import android.content.res.Configuration
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import app.velora.launcher.R
import app.velora.launcher.core.data.contacts.ContactResult
import app.velora.launcher.core.designsystem.motion.VeloraMotion
import app.velora.launcher.core.designsystem.theme.Sizes
import app.velora.launcher.core.designsystem.theme.Spacing
import app.velora.launcher.core.designsystem.theme.VeloraTheme
import app.velora.launcher.core.model.AppEntry
import app.velora.launcher.feature.apps.AppActions
import app.velora.launcher.feature.apps.AppRow
import app.velora.launcher.feature.apps.AppRowStyle
import app.velora.launcher.feature.apps.PreviewAppActions
import app.velora.launcher.feature.apps.previewApp

// Filas especiales del buscador: calculadora, contactos y "Buscar en la web".

@Composable
internal fun CalculatorRow(result: String, onCopy: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onCopy)
            .padding(vertical = Spacing.s),
    ) {
        Text(
            "= $result",
            style = VeloraTheme.typography.clock.copy(fontSize = 40.sp, lineHeight = 46.sp),
            color = VeloraTheme.onWallpaper.content,
        )
        Text(
            stringResource(R.string.calculator_copy),
            style = VeloraTheme.typography.caption,
            color = VeloraTheme.onWallpaper.contentVariant,
        )
    }
}

/** Un contacto: su inicial en un círculo y el nombre. Tocarlo abre la ficha en Contactos. */
@Composable
internal fun ContactRow(contact: ContactResult, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = VeloraTheme.onWallpaper
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = Sizes.appRowHeight)
            .clickable(onClick = onClick),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(Sizes.listIcon)
                .background(colors.iconCircle, CircleShape),
        ) {
            Text(contact.name.take(1).uppercase(), style = VeloraTheme.typography.caption, color = colors.iconGlyph)
        }
        Spacer(Modifier.width(Spacing.m))
        Column {
            Text(contact.name, style = VeloraTheme.typography.appRow, color = colors.content, maxLines = 1)
            Text(
                stringResource(R.string.contact_label),
                style = VeloraTheme.typography.caption,
                color = colors.contentVariant,
            )
        }
    }
}

@Composable
internal fun WebSearchRow(query: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = Sizes.appRowHeight)
            .clickable(onClick = onClick),
    ) {
        Icon(Icons.Outlined.Search, contentDescription = null, tint = VeloraTheme.onWallpaper.contentVariant)
        Spacer(Modifier.width(Spacing.m))
        Text(
            text = stringResource(R.string.web_search, query),
            style = VeloraTheme.typography.appRow,
            color = VeloraTheme.onWallpaper.contentVariant,
        )
    }
}
