package app.velora.launcher.core.data.apps

import app.velora.launcher.core.model.AppEntry
import java.util.Locale

/** Filtro de búsqueda (RENDIMIENTO §2: ≤ 16 ms para 300 apps). Kotlin puro. */
object AppSearch {

    /**
     * Devuelve las apps que contienen [query], sin importar tildes ni mayúsculas.
     * Primero las que empiezan por la búsqueda, luego las que tienen una palabra que empieza
     * por ella y al final el resto; dentro de cada grupo se conserva el orden de [apps].
     */
    fun filter(apps: List<AppEntry>, query: String, locale: Locale): List<AppEntry> {
        val needle = AppSorting.searchKeyOf(query.trim(), locale)
        if (needle.isEmpty()) return emptyList()
        return apps
            .mapNotNull { app -> rank(app.searchKey, needle)?.let { app to it } }
            .sortedBy { it.second }
            .map { it.first }
    }

    private fun rank(haystack: String, needle: String): Int? = when {
        haystack.startsWith(needle) -> 0
        haystack.split(' ', '-', '_', '.').any { it.startsWith(needle) } -> 1
        haystack.contains(needle) -> 2
        else -> null
    }
}
