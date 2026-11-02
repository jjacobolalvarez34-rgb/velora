package app.velora.launcher.core.data.backup

import app.velora.launcher.core.data.favorites.FoldersRepository
import app.velora.launcher.core.data.favorites.WidgetsRepository
import app.velora.launcher.core.data.prefs.CustomizationCodec
import app.velora.launcher.core.model.AppKey
import app.velora.launcher.core.model.ClockStyle
import app.velora.launcher.core.model.Customization
import app.velora.launcher.core.model.FontChoice
import app.velora.launcher.core.model.IconStyle
import app.velora.launcher.core.model.TextSize
import app.velora.launcher.core.model.ThemePreset
import java.util.Base64

/** Cómo se escribe y se valida cada valor del respaldo ([BackupFile]). */
internal object BackupText {

    /** Nombre de paquete de Android: `com.ejemplo.app`. */
    private val PACKAGE = Regex("[A-Za-z][A-Za-z0-9_]*(\\.[A-Za-z][A-Za-z0-9_]*)+")

    /** Nombre de clase Java, completo o corto (`.Main`), con clases internas (`$`). */
    private val CLASS = Regex("\\.?[A-Za-z_$][A-Za-z0-9_$]*(\\.[A-Za-z_$][A-Za-z0-9_$]*)*")

    private const val MAX_NAME_CHARS = 200
    private const val MAX_TIPS = 100

    /** Claves de [CustomizationCodec] que se aceptan; cualquier otra se ignora. */
    private val CUSTOM_KEYS = setOf("accent", "tips.dismissed", "limits")
    private val CUSTOM_PREFIXES = listOf("icons.", "visible.")

    fun key(key: AppKey) = "${key.packageName}/${key.className}#${key.userSerial}"

    /** Base64 URL: el nombre puede tener `=`, `:` o saltos de línea sin romper el formato. */
    fun name(name: String): String =
        Base64.getUrlEncoder().withoutPadding().encodeToString(name.toByteArray(Charsets.UTF_8))

    fun prefs(prefs: BackupPrefs): String = buildString {
        prefs.preset?.let { appendLine("${BackupFile.PREF}preset=${it.name}") }
        prefs.font?.let { appendLine("${BackupFile.PREF}font=${it.name}") }
        prefs.clock?.let { appendLine("${BackupFile.PREF}clock=${it.name}") }
        prefs.icons?.let { appendLine("${BackupFile.PREF}icons=${it.name}") }
        prefs.textSize?.let { appendLine("${BackupFile.PREF}textSize=${it.name}") }
        prefs.leftHanded?.let { appendLine("${BackupFile.PREF}leftHanded=$it") }
        prefs.fullList?.let { appendLine("${BackupFile.PREF}fullList=$it") }
        prefs.custom?.let { append(CustomizationCodec.encode(it)) }
    }

    fun parseKey(text: String): AppKey? {
        val slash = text.indexOf('/')
        val hash = text.lastIndexOf('#')
        if (slash <= 0 || hash <= slash + 1) return null
        val pkg = text.substring(0, slash).takeIf { it.matches(PACKAGE) }
        val cls = text.substring(slash + 1, hash).takeIf { it.length <= MAX_NAME_CHARS && it.matches(CLASS) }
        val serial = text.substring(hash + 1).toLongOrNull()?.takeIf { it >= 0 }
        return if (pkg != null && cls != null && serial != null) AppKey(pkg, cls, serial) else null
    }

    fun parseName(text: String): String? {
        val raw = runCatching { String(Base64.getUrlDecoder().decode(text), Charsets.UTF_8) }.getOrNull() ?: return null
        return raw.filterNot { it.isISOControl() }.trim().take(FoldersRepository.MAX_NAME).takeIf { it.isNotBlank() }
    }

    /** `3:resto` → (3, "resto"). */
    fun indexed(text: String): Pair<Int, String>? {
        val colon = text.indexOf(':')
        val index = text.substring(0, colon.coerceAtLeast(0)).toIntOrNull()?.takeIf {
            it in 0 until BackupFile.MAX_ITEMS
        }
        return if (colon > 0 && index != null) index to text.substring(colon + 1) else null
    }

    /** `paquete/clase|alto`, con el alto dentro de los límites de los widgets. */
    fun parseWidget(text: String): BackupWidget? {
        val (provider, height) = text.split('|').takeIf { it.size == 2 } ?: return null
        val pkg = provider.substringBefore('/', missingDelimiterValue = "")
        val cls = provider.substringAfter('/', missingDelimiterValue = "")
        if (!pkg.matches(PACKAGE) || cls.length > MAX_NAME_CHARS || !cls.matches(CLASS)) return null
        val heightDp = height.toIntOrNull()?.coerceIn(WidgetsRepository.MIN_HEIGHT_DP, WidgetsRepository.MAX_HEIGHT_DP)
        return heightDp?.let { BackupWidget(provider, it) }
    }

    fun isCustomizationKey(key: String) = key in CUSTOM_KEYS || CUSTOM_PREFIXES.any { key.startsWith(it) }

    fun parsePrefs(values: Map<String, String>, custom: Customization?) = BackupPrefs(
        preset = values["preset"]?.let { v -> ThemePreset.entries.firstOrNull { it.name == v } },
        font = values["font"]?.let { v -> FontChoice.entries.firstOrNull { it.name == v } },
        clock = values["clock"]?.let { v -> ClockStyle.entries.firstOrNull { it.name == v } },
        icons = values["icons"]?.let { v -> IconStyle.entries.firstOrNull { it.name == v } },
        textSize = values["textSize"]?.let { v -> TextSize.entries.firstOrNull { it.name == v } },
        leftHanded = values["leftHanded"]?.toBooleanStrictOrNull(),
        fullList = values["fullList"]?.toBooleanStrictOrNull(),
        // Los frenos de uso llevan nombres de paquete: también se validan.
        custom = custom?.copy(
            usageLimits = custom.usageLimits.filterKeys { it.matches(PACKAGE) },
            dismissedTips = custom.dismissedTips.take(MAX_TIPS).toSet(),
        ),
    )
}
