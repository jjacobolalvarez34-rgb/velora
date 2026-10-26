package app.velora.launcher.feature.home

import android.text.format.DateFormat
import app.velora.launcher.core.model.ClockState
import java.time.format.DateTimeFormatter
import java.util.Locale

fun interface ClockFormatter {
    fun format(state: ClockState): ClockText
}

/**
 * Formatea con los patrones del idioma actual (REGLAS §5). El reloj grande no lleva
 * "a. m./p. m.": en 12 h se muestra solo "7:05".
 */
class LocalizedClockFormatter : ClockFormatter {

    private class Formatters(val locale: Locale, val is24Hour: Boolean) {
        val time: DateTimeFormatter = DateTimeFormatter.ofPattern(timePattern(locale, is24Hour), locale)
        val date: DateTimeFormatter =
            DateTimeFormatter.ofPattern(DateFormat.getBestDateTimePattern(locale, "EEEEdMMMM"), locale)
        val weekdayShort: DateTimeFormatter = DateTimeFormatter.ofPattern("EEE", locale)
        val weekday: DateTimeFormatter = DateTimeFormatter.ofPattern("EEEE", locale)
        val dayMonth: DateTimeFormatter =
            DateTimeFormatter.ofPattern(DateFormat.getBestDateTimePattern(locale, "dMMMM"), locale)
    }

    private var cached: Formatters? = null

    override fun format(state: ClockState): ClockText {
        val locale = Locale.getDefault()
        val formatters = cached?.takeIf { it.locale == locale && it.is24Hour == state.is24Hour }
            ?: Formatters(locale, state.is24Hour).also { cached = it }
        return ClockText(
            time = formatters.time.format(state.now),
            date = formatters.date.format(state.now).replaceFirstChar { it.titlecase(locale) },
            // "jue." → "JUE" para el estilo de reloj Día.
            weekdayShort = formatters.weekdayShort.format(state.now).removeSuffix(".").uppercase(locale),
            weekday = formatters.weekday.format(state.now).uppercase(locale),
            dayMonth = formatters.dayMonth.format(state.now),
        )
    }

    private companion object {
        private val amPmMarker = Regex("\\s*a\\s*")

        fun timePattern(locale: Locale, is24Hour: Boolean): String = if (is24Hour) {
            DateFormat.getBestDateTimePattern(locale, "Hm")
        } else {
            DateFormat.getBestDateTimePattern(locale, "hm").replace(amPmMarker, "").trim()
        }
    }
}
