package app.velora.launcher.feature.home.components

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import app.velora.launcher.R
import app.velora.launcher.core.designsystem.motion.StaggeredEntrance
import app.velora.launcher.core.designsystem.theme.Shapes
import app.velora.launcher.core.designsystem.theme.Spacing
import app.velora.launcher.core.designsystem.theme.VeloraTheme

/**
 * Consejos de los primeros días, como notificaciones del inicio: cada uno lleva a hacer algo que
 * todavía no hiciste y se quita deslizándolo hacia un lado. Cuando ya lo hiciste, no vuelve.
 */
enum class HomeTip(val id: String, @StringRes val title: Int, @StringRes val body: Int, val icon: ImageVector) {
    SetDefault("default", R.string.tip_default_title, R.string.tip_default_body, Icons.Outlined.Home),
    Favorites("favorites", R.string.tip_favorites_title, R.string.tip_favorites_body, Icons.Outlined.Star),
    Theme("theme", R.string.tip_theme_title, R.string.tip_theme_body, Icons.Outlined.Edit),
    Alphabet("alphabet", R.string.tip_alphabet_title, R.string.tip_alphabet_body, Icons.Outlined.Menu),
    Notifications(
        "notifications",
        R.string.tip_notifications_title,
        R.string.tip_notifications_body,
        Icons.Outlined.Notifications,
    ),
    Widgets("widgets", R.string.tip_widgets_title, R.string.tip_widgets_body, Icons.Outlined.Add),
}

/** Hasta [MAX_TIPS] consejos a la vez, para que el inicio no se llene. */
@Composable
fun HomeTips(
    tips: List<HomeTip>,
    onOpen: (HomeTip) -> Unit,
    onDismiss: (HomeTip) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier) {
        tips.take(MAX_TIPS).forEachIndexed { index, tip ->
            key(tip.id) {
                StaggeredEntrance(index = index + 1) { TipCard(tip, onOpen, onDismiss) }
            }
        }
    }
}

@Composable
private fun TipCard(tip: HomeTip, onOpen: (HomeTip) -> Unit, onDismiss: (HomeTip) -> Unit) {
    val state = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value != SwipeToDismissBoxValue.Settled) onDismiss(tip)
            true
        },
    )
    val dismissLabel = stringResource(R.string.tip_dismiss)
    SwipeToDismissBox(
        state = state,
        backgroundContent = {},
        modifier = Modifier
            .padding(bottom = Spacing.s)
            .semantics {
                customActions = listOf(
                    CustomAccessibilityAction(dismissLabel) {
                        onDismiss(tip)
                        true
                    },
                )
            },
    ) {
        TipContent(tip, onClick = { onOpen(tip) })
    }
}

@Composable
private fun TipContent(tip: HomeTip, onClick: () -> Unit) {
    val colors = VeloraTheme.onWallpaper
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.surface, Shapes.menu)
            .clickable(onClick = onClick)
            .padding(horizontal = Spacing.m, vertical = Spacing.s),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(TipIconSize)
                .background(colors.accent, CircleShape),
        ) {
            Icon(tip.icon, contentDescription = null, tint = colors.onAccent, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(Spacing.m))
        Column(Modifier.weight(1f)) {
            Text(stringResource(tip.title), style = VeloraTheme.typography.appRow, color = colors.content)
            Text(stringResource(tip.body), style = VeloraTheme.typography.caption, color = colors.contentVariant)
        }
    }
}

private const val MAX_TIPS = 2
private val TipIconSize = 32.dp
