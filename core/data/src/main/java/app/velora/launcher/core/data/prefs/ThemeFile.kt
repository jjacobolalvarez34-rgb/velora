package app.velora.launcher.core.data.prefs

import app.velora.launcher.core.model.ClockStyle
import app.velora.launcher.core.model.Customization
import app.velora.launcher.core.model.FontChoice
import app.velora.launcher.core.model.IconStyle
import app.velora.launcher.core.model.ThemePreset
import java.util.Base64

/** Un tema compartido: lo que se aplica al importarlo. Lo que no se reconoce queda en null. */
data class SharedTheme(
    val preset: ThemePreset,
    val font: FontChoice?,
    val clock: ClockStyle?,
    val icons: IconStyle?,
    /** JPEG del fondo propio, si el tema lo trae. */
    val photo: ByteArray?,
    /** Íconos, color y elementos visibles (sin consejos ni frenos de uso: esos son personales). */
    val custom: Customization? = null,
) {
    override fun equals(other: Any?): Boolean = other is SharedTheme &&
        preset == other.preset && font == other.font && clock == other.clock && icons == other.icons &&
        custom == other.custom && photo.contentEqualsOrNull(other.photo)

    override fun hashCode(): Int =
        listOf(preset, font, clock, icons, custom).hashCode() * HASH + (photo?.contentHashCode() ?: 0)

    private fun ByteArray?.contentEqualsOrNull(other: ByteArray?) =
        if (this == null) other == null else other != null && contentEquals(other)

    private companion object {
        const val HASH = 31
    }
}

/**
 * Archivo de tema de Velora (`.velora-theme`, ADR-027): texto plano, una clave por línea, para
 * compartir temas entre usuarios de Velora. Solo nombres de opciones de Velora y, opcional, la
 * foto en Base64; nada ejecutable. Al leer se valida todo: lo desconocido se ignora y un archivo
 * ajeno o demasiado grande se rechaza.
 */
object ThemeFile {

    const val HEADER = "velora-theme 1"
    const val MIME = "text/plain"
    const val EXTENSION = "velora-theme"

    /** Más que esto no es un tema de Velora (una foto reducida pesa unos cientos de KB). */
    const val MAX_BYTES = 8 * 1024 * 1024

    fun encode(theme: SharedTheme): String = buildString {
        appendLine(HEADER)
        appendLine("preset=${theme.preset.name}")
        theme.font?.let { appendLine("font=${it.name}") }
        theme.clock?.let { appendLine("clock=${it.name}") }
        theme.icons?.let { appendLine("icons=${it.name}") }
        theme.custom?.let { custom ->
            append(CustomizationCodec.encode(custom.copy(dismissedTips = emptySet(), usageLimits = emptyMap())))
        }
        theme.photo?.let { appendLine("photo=${Base64.getEncoder().encodeToString(it)}") }
    }

    /** null si no es un tema de Velora válido. */
    fun decode(text: String): SharedTheme? {
        val lines = text.takeIf { it.length <= MAX_BYTES * 2 }
            ?.lineSequence()?.map { it.trim() }?.filter { it.isNotEmpty() }?.toList()
            ?.takeIf { it.firstOrNull() == HEADER }
            ?: return null
        val values = lines.drop(1).mapNotNull { line ->
            line.indexOf('=').takeIf { it > 0 }?.let { line.substring(0, it) to line.substring(it + 1) }
        }.toMap()
        val preset = values["preset"]?.let { name -> ThemePreset.entries.firstOrNull { it.name == name } }
        return preset?.let {
            SharedTheme(
                preset = it,
                font = values["font"]?.let { name -> FontChoice.entries.firstOrNull { it.name == name } },
                clock = values["clock"]?.let { name -> ClockStyle.entries.firstOrNull { it.name == name } },
                icons = values["icons"]?.let { name -> IconStyle.entries.firstOrNull { it.name == name } },
                photo = values["photo"]?.let { runCatching { Base64.getDecoder().decode(it) }.getOrNull() },
                custom = if (values.keys.any { k -> k.startsWith("icons.") || k.startsWith("visible.") }) {
                    CustomizationCodec.fromValues(values.filterKeys { !it.startsWith("tips.") && it != "limits" })
                } else {
                    null
                },
            )
        }
    }
}
