package app.velora.launcher.core.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import app.velora.launcher.core.model.ClockStyle
import app.velora.launcher.core.model.FontChoice
import app.velora.launcher.core.model.IconStyle
import app.velora.launcher.core.model.ThemeSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/** Preferencias del usuario (ARQUITECTURA §4.3, ADR-017). */
interface PrefsRepository {
    val theme: Flow<ThemeSettings>

    suspend fun setFont(font: FontChoice)

    suspend fun setClockStyle(style: ClockStyle)

    suspend fun setIconStyle(style: IconStyle)
}

private val Context.veloraPrefs: DataStore<Preferences> by preferencesDataStore(name = "velora_prefs")

class DataStorePrefsRepository(context: Context) : PrefsRepository {

    private val store = context.applicationContext.veloraPrefs

    override val theme: Flow<ThemeSettings> = store.data
        .map { prefs -> decodeTheme(prefs[FONT], prefs[CLOCK], prefs[ICONS]) }
        .distinctUntilChanged()

    override suspend fun setFont(font: FontChoice) = put(FONT, font.name)

    override suspend fun setClockStyle(style: ClockStyle) = put(CLOCK, style.name)

    override suspend fun setIconStyle(style: IconStyle) = put(ICONS, style.name)

    private suspend fun put(key: Preferences.Key<String>, value: String) {
        store.edit { it[key] = value }
    }

    private companion object {
        val FONT = stringPreferencesKey("theme_font")
        val CLOCK = stringPreferencesKey("theme_clock")
        val ICONS = stringPreferencesKey("theme_icons")
    }
}

/**
 * Se guardan los nombres de los enums. Un valor desconocido (de otra versión) vuelve al
 * de fábrica en vez de romper el arranque.
 */
internal fun decodeTheme(font: String?, clock: String?, icons: String?): ThemeSettings {
    val defaults = ThemeSettings()
    return ThemeSettings(
        font = FontChoice.entries.firstOrNull { it.name == font } ?: defaults.font,
        clock = ClockStyle.entries.firstOrNull { it.name == clock } ?: defaults.clock,
        icons = IconStyle.entries.firstOrNull { it.name == icons } ?: defaults.icons,
    )
}
