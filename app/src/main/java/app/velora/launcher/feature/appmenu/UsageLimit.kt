package app.velora.launcher.feature.appmenu

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import app.velora.launcher.R
import app.velora.launcher.core.designsystem.theme.Spacing

/** Opciones del freno de uso, en minutos por día; null = sin límite. */
private val LimitChoices = listOf(null, 15, 30, 60, 120, 180)

/**
 * Freno de uso en el panel de una app (ADR-024): cuánto tiempo al día. Al pasarlo, abrir la app
 * pregunta antes (no la bloquea: siempre se puede "Abrir igual").
 */
@Composable
internal fun LimitChooser(current: Int?, onChoose: (Int?) -> Unit) {
    Text(
        stringResource(R.string.usage_limit_title),
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = Spacing.l, vertical = Spacing.xs),
    )
    LazyRow(
        contentPadding = PaddingValues(horizontal = Spacing.l),
        horizontalArrangement = Arrangement.spacedBy(Spacing.s),
    ) {
        items(LimitChoices) { minutes ->
            FilterChip(
                selected = current == minutes,
                onClick = { onChoose(minutes) },
                label = { Text(limitLabel(minutes)) },
            )
        }
    }
}

@Composable
private fun limitLabel(minutes: Int?): String = when {
    minutes == null -> stringResource(R.string.usage_limit_none)
    minutes < MINUTES_PER_HOUR -> stringResource(R.string.usage_minutes, minutes)
    else -> stringResource(R.string.usage_limit_hours, minutes / MINUTES_PER_HOUR)
}

/** Se pasó el límite: cuánto lleva hoy y la opción de abrirla igual o dejarla. */
@Composable
fun LimitDialog(appLabel: String, usedMinutes: Long, limitMinutes: Int, onOpen: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.usage_limit_reached_title, appLabel)) },
        text = { Text(stringResource(R.string.usage_limit_reached_body, usedMinutes, limitLabel(limitMinutes))) },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.usage_limit_leave)) }
        },
        dismissButton = {
            TextButton(onClick = {
                onDismiss()
                onOpen()
            }) { Text(stringResource(R.string.usage_limit_open_anyway)) }
        },
    )
}

private const val MINUTES_PER_HOUR = 60
