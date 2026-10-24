package app.velora.launcher.core.model

import androidx.compose.runtime.Immutable

/**
 * Identidad de una actividad abrible: componente + perfil de usuario (personal, trabajo…).
 * [stableId] es la `key` en listas lazy y la clave primaria en Room.
 */
@Immutable
data class AppKey(val packageName: String, val className: String, val userSerial: Long) {
    val stableId: String = "$packageName/$className#$userSerial"
}

/**
 * Una app de la lista, ya preparada para mostrar, ordenar y buscar.
 *
 * @property section letra de sección ("A", "Ñ", "#"…).
 * @property searchKey nombre en minúsculas y sin tildes, para buscar.
 */
@Immutable
data class AppEntry(
    val key: AppKey,
    val label: String,
    val section: String,
    val searchKey: String,
    val isWorkProfile: Boolean,
    val canUninstall: Boolean,
)
