package app.velora.launcher.feature.drawer

import androidx.compose.runtime.Immutable
import app.velora.launcher.core.model.AppEntry

/** Una fila de la lista completa: encabezado de letra o app. */
@Immutable
sealed interface DrawerItem {
    /** `key` estable para `LazyColumn` (RENDIMIENTO §3.3). */
    val key: String

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
 */
@Immutable
data class DrawerList(
    val items: List<DrawerItem> = emptyList(),
    val letters: List<String> = emptyList(),
    val letterIndex: List<Int> = emptyList(),
)

/** Inserta un encabezado antes de cada letra nueva. [apps] ya viene ordenada. */
fun buildDrawerList(apps: List<AppEntry>): DrawerList {
    val items = ArrayList<DrawerItem>(apps.size + MAX_SECTIONS)
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
    return DrawerList(items, letters, letterIndex)
}

private const val MAX_SECTIONS = 28
