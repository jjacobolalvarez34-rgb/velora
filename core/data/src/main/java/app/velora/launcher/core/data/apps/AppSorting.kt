package app.velora.launcher.core.data.apps

import app.velora.launcher.core.model.AppEntry
import java.text.Collator
import java.text.Normalizer
import java.util.Locale

/**
 * Orden alfabético, secciones y claves de búsqueda (B-009).
 * Kotlin puro: se prueba en la JVM sin Android.
 */
object AppSorting {

    const val SYMBOLS_SECTION = "#"

    private val combiningMarks = Regex("\\p{Mn}+")

    /**
     * Letra de sección: la inicial sin tildes y en mayúscula. La Ñ es letra propia en español;
     * números, símbolos y emojis van a [SYMBOLS_SECTION].
     */
    fun sectionOf(label: String, locale: Locale): String {
        val first = label.trimStart().firstOrNull() ?: return SYMBOLS_SECTION
        if (locale.language == "es" && (first == 'ñ' || first == 'Ñ')) return "Ñ"
        if (!first.isLetter()) return SYMBOLS_SECTION
        val base = Normalizer.normalize(first.toString(), Normalizer.Form.NFD).first()
        return base.uppercase(locale)
    }

    /** Minúsculas y sin tildes: "Cámara" → "camara", "Ñequi" → "nequi". */
    fun searchKeyOf(label: String, locale: Locale): String =
        Normalizer.normalize(label.lowercase(locale), Normalizer.Form.NFD).replace(combiningMarks, "")

    /**
     * Ordena con el `Collator` del idioma (nunca `sortedBy { it.label }`, REGLAS §5).
     * La sección de símbolos va primero; dentro de cada grupo manda el `Collator`.
     */
    fun sorted(apps: List<AppEntry>, locale: Locale): List<AppEntry> {
        val collator = Collator.getInstance(locale).apply { strength = Collator.PRIMARY }
        val byLabel = Comparator<AppEntry> { a, b -> collator.compare(a.label, b.label) }
        return apps.sortedWith(
            compareBy<AppEntry> { it.section != SYMBOLS_SECTION }
                .then(byLabel)
                .thenBy { it.key.userSerial },
        )
    }
}
