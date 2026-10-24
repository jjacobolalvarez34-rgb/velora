package app.velora.launcher.feature.drawer

import androidx.compose.runtime.Immutable
import app.velora.launcher.core.model.AppEntry

/** Una fila de la lista completa. */
@Immutable
sealed interface DrawerItem {
    /** `key` estable para `LazyColumn` (RENDIMIENTO §3.3). */
    val key: String

    /** "Recién instaladas", arriba del todo. */
    @Immutable
    data object RecentHeader : DrawerItem {
        override val key: String = "recent-header"
    }

    /** Una app reciente. Lleva otra key: la misma app también está en su letra. */
    @Immutable
    data class Recent(val entry: AppEntry) : DrawerItem {
        override val key: String = "recent:${entry.key.stableId}"
    }

    @Immutable
    data class Header(val letter: String) : DrawerItem {
        override val key: String = "header:$letter"
    }

    @Immutable
    data class App(val entry: AppEntry) : DrawerItem {
        override val key: String = entry.key.stableId
    }
}

/**
 * Lista completa ya armada.
 *
 * @property letters letras presentes, en el orden de la lista (alimenta el alfabeto ola).
 * @property letterIndex posición del encabezado de cada letra, para saltar sin buscar.
 * @property byLetter apps de cada letra, para la vista "solo la letra elegida".
 */
@Immutable
data class DrawerList(
    val items: List<DrawerItem> = emptyList(),
    val letters: List<String> = emptyList(),
    val letterIndex: List<Int> = emptyList(),
    val byLetter: Map<String, List<AppEntry>> = emptyMap(),
)

/**
 * Inserta las recientes arriba y un encabezado antes de cada letra nueva.
 * [apps] ya viene ordenada; [recents] de la más nueva a la más vieja.
 */
fun buildDrawerList(apps: List<AppEntry>, recents: List<AppEntry> = emptyList()): DrawerList {
    val items = ArrayList<DrawerItem>(apps.size + recents.size + MAX_SECTIONS)
    if (recents.isNotEmpty()) {
        items += DrawerItem.RecentHeader
        recents.mapTo(items) { DrawerItem.Recent(it) }
    }
    val letters = ArrayList<String>()
    val letterIndex = ArrayList<Int>()
    for (app in apps) {
        if (app.section != letters.lastOrNull()) {
            letters += app.section
            letterIndex += items.size
            items += DrawerItem.Header(app.section)
        }
        items += DrawerItem.App(app)
    }
    return DrawerList(items, letters, letterIndex, byLetter = apps.groupBy { it.section })
}

private const val MAX_SECTIONS = 28
