package app.velora.launcher.core.data.prefs

import app.velora.launcher.core.model.Customization
import app.velora.launcher.core.model.IconColor
import app.velora.launcher.core.model.IconOptions
import app.velora.launcher.core.model.IconPack
import app.velora.launcher.core.model.IconShape
import app.velora.launcher.core.model.VisibleElements

/**
 * [Customization] como texto, una clave por línea (ADR-028). Igual que los temas: lo que no se
 * reconoce vuelve al valor de fábrica, así una versión vieja nunca rompe el arranque.
 */
object CustomizationCodec {

    fun encode(custom: Customization): String = buildString {
        val icons = custom.icons
        appendLine("icons.pack=${icons.pack.name}")
        appendLine("icons.shape=${icons.shape.name}")
        appendLine("icons.color=${icons.color.name}")
        appendLine("icons.weight=${icons.weight}")
        appendLine("icons.softFill=${icons.softFill}")
        icons.thirdPartyPack?.let { appendLine("icons.thirdParty=$it") }
        custom.accentColor?.let { appendLine("accent=$it") }
        appendLine("visible.clock=${custom.visible.clock}")
        appendLine("visible.date=${custom.visible.date}")
        appendLine("visible.statusBar=${custom.visible.statusBar}")
        appendLine("visible.tips=${custom.visible.tips}")
        if (custom.dismissedTips.isNotEmpty()) {
            appendLine(
                "tips.dismissed=${custom.dismissedTips.sorted().joinToString(",")}",
            )
        }
        if (custom.usageLimits.isNotEmpty()) {
            appendLine(
                "limits=" + custom.usageLimits.entries.sortedBy {
                    it.key
                }.joinToString(",") { "${it.key}:${it.value}" },
            )
        }
    }

    fun decode(text: String?): Customization {
        if (text.isNullOrBlank()) return Customization()
        val values = text.lineSequence().mapNotNull { line ->
            line.indexOf('=').takeIf { it > 0 }?.let { line.substring(0, it).trim() to line.substring(it + 1).trim() }
        }.toMap()
        return fromValues(values)
    }

    /** Desde las claves ya separadas (también las de un tema compartido). */
    internal fun fromValues(values: Map<String, String>): Customization {
        val defaults = Customization()
        return Customization(
            icons = decodeIcons(values),
            accentColor = values["accent"]?.toLongOrNull(),
            visible = VisibleElements(
                clock = values["visible.clock"]?.toBooleanStrictOrNull() ?: defaults.visible.clock,
                date = values["visible.date"]?.toBooleanStrictOrNull() ?: defaults.visible.date,
                statusBar = values["visible.statusBar"]?.toBooleanStrictOrNull() ?: defaults.visible.statusBar,
                tips = values["visible.tips"]?.toBooleanStrictOrNull() ?: defaults.visible.tips,
            ),
            dismissedTips = values["tips.dismissed"]?.split(',')?.filter { it.isNotBlank() }?.toSet().orEmpty(),
            usageLimits = values["limits"]?.split(',')?.mapNotNull { entry ->
                val (pkg, minutes) = entry.split(':').takeIf { it.size == 2 } ?: return@mapNotNull null
                minutes.toIntOrNull()?.takeIf { it > 0 }?.let { pkg to it }
            }?.toMap().orEmpty(),
        )
    }

    /** Sirve también para los temas compartidos (`ThemeFile`). */
    internal fun decodeIcons(values: Map<String, String>): IconOptions {
        val defaults = IconOptions()
        return IconOptions(
            pack = values["icons.pack"]?.let { v -> IconPack.entries.firstOrNull { it.name == v } } ?: defaults.pack,
            shape = values["icons.shape"]?.let { v ->
                IconShape.entries.firstOrNull { it.name == v }
            } ?: defaults.shape,
            color = values["icons.color"]?.let { v ->
                IconColor.entries.firstOrNull { it.name == v }
            } ?: defaults.color,
            weight = values["icons.weight"]?.toFloatOrNull()
                ?.coerceIn(IconOptions.MIN_WEIGHT, IconOptions.MAX_WEIGHT) ?: defaults.weight,
            softFill = values["icons.softFill"]?.toBooleanStrictOrNull() ?: defaults.softFill,
            thirdPartyPack = values["icons.thirdParty"]?.takeIf { it.matches(PACKAGE_NAME) },
        )
    }

    /** Un nombre de paquete de Android válido; cualquier otra cosa se descarta. */
    private val PACKAGE_NAME = Regex("[A-Za-z][A-Za-z0-9_]*(\\.[A-Za-z][A-Za-z0-9_]*)+")
}
