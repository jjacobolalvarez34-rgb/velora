package app.velora.launcher.feature.hidden

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp

/** Una tecla redonda grande (72 dp, cómoda con el pulgar). */
@Composable
internal fun DigitKey(digit: Char, enabled: Boolean, onClick: () -> Unit) {
    PadKey(enabled, onClick) {
        Text(digit.toString(), style = MaterialTheme.typography.headlineMedium)
    }
}

/** Borrar o aceptar: el ícono con su descripción para TalkBack. */
@Composable
internal fun IconKey(icon: ImageVector, @StringRes label: Int, enabled: Boolean, onClick: () -> Unit) {
    PadKey(enabled, onClick) {
        Icon(icon, contentDescription = stringResource(label))
    }
}

@Composable
private fun PadKey(enabled: Boolean, onClick: () -> Unit, content: @Composable () -> Unit) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        contentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = if (enabled) 1f else DISABLED_ALPHA),
        modifier = Modifier.size(KeySize),
    ) {
        Box(contentAlignment = Alignment.Center) { content() }
    }
}

private val KeySize = 72.dp
private const val DISABLED_ALPHA = 0.38f
