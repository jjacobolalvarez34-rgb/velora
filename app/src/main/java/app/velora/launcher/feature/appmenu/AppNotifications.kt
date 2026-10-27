package app.velora.launcher.feature.appmenu

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import app.velora.launcher.R
import app.velora.launcher.core.data.notifications.NotificationActions
import app.velora.launcher.core.designsystem.theme.Spacing
import app.velora.launcher.core.model.AppNotification

/**
 * Las notificaciones de una app dentro de su panel (Fase 3, pop-ups): tocar abre, la X descarta
 * y "Responder" usa la respuesta rápida de la propia notificación (SEGURIDAD §3.2).
 */
@Composable
internal fun AppNotificationsList(
    notifications: List<AppNotification>,
    actions: NotificationActions,
    onDismiss: () -> Unit,
) {
    if (notifications.isEmpty()) return
    Text(
        text = stringResource(R.string.app_menu_notifications),
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier
            .padding(horizontal = Spacing.l, vertical = Spacing.xs)
            .semantics { heading() },
    )
    notifications.take(MAX_SHOWN).forEach { notification ->
        NotificationRow(notification, actions, onDismiss)
    }
}

@Composable
private fun NotificationRow(notification: AppNotification, actions: NotificationActions, onDismiss: () -> Unit) {
    var replying by remember(notification.key) { mutableStateOf(false) }
    Column {
        ListItem(
            headlineContent = {
                Text(
                    notification.title ?: stringResource(R.string.notification_private),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            },
            supportingContent = notification.text?.let { { Text(it, maxLines = 2, overflow = TextOverflow.Ellipsis) } },
            trailingContent = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (notification.canReply && !replying) {
                        TextButton(onClick = { replying = true }) { Text(stringResource(R.string.notification_reply)) }
                    }
                    IconButton(onClick = { actions.dismiss(notification.key) }) {
                        Icon(Icons.Outlined.Clear, contentDescription = stringResource(R.string.notification_dismiss))
                    }
                }
            },
            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
            modifier = Modifier.clickable {
                if (actions.open(notification.key)) onDismiss()
            },
        )
        AnimatedVisibility(visible = replying) {
            ReplyField(notification, actions) {
                replying = false
                onDismiss()
            }
        }
    }
}

@Composable
private fun ReplyField(notification: AppNotification, actions: NotificationActions, onSent: () -> Unit) {
    val context = LocalContext.current
    var text by remember { mutableStateOf("") }
    val send = {
        if (text.isNotBlank()) {
            val ok = actions.reply(notification.key, text.trim())
            Toast.makeText(
                context,
                if (ok) R.string.notification_reply_sent else R.string.notification_reply_failed,
                Toast.LENGTH_SHORT,
            ).show()
            if (ok) onSent()
        }
    }
    OutlinedTextField(
        value = text,
        onValueChange = { text = it },
        placeholder = { Text(stringResource(R.string.notification_reply_hint)) },
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
        keyboardActions = KeyboardActions(onSend = { send() }),
        trailingIcon = {
            IconButton(onClick = send) {
                Icon(
                    Icons.AutoMirrored.Outlined.Send,
                    contentDescription = stringResource(R.string.notification_reply_send),
                )
            }
        },
        singleLine = true,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.l, vertical = Spacing.xs),
    )
}

private const val MAX_SHOWN = 5
