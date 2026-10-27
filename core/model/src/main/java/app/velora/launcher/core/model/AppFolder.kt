package app.velora.launcher.core.model

import androidx.compose.runtime.Immutable

/** Una carpeta del inicio con sus apps (las que siguen instaladas y visibles), por nombre. */
@Immutable
data class AppFolder(val id: Long, val name: String, val apps: List<AppEntry>)
