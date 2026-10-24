package app.velora.launcher.core.model

import androidx.compose.runtime.Immutable

/** Un atajo de app (`ShortcutInfo`), p. ej. "Nuevo mensaje". Solo identidad y texto. */
@Immutable
data class AppShortcut(val id: String, val packageName: String, val userSerial: Long, val label: String)
