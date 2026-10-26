package app.velora.launcher.core.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import app.velora.launcher.core.model.ClockStyle
import app.velora.launcher.core.model.FontChoice
import app.velora.launcher.core.model.IconStyle
import app.velora.launcher.core.model.TextSize
import app.velora.launcher.core.model.ThemePreset
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

    suspend fun setTextSize(size: TextSize)

    suspend fun setLeftHanded(enabled: Boolean)

    suspend fun setFullList(enabled: Boolean)

    /** Aplica un tema completo: su fondo y colores, y su fuente, reloj e íconos recomendados. */
    suspend fun applyPreset(preset: ThemePreset)

    /** true cuando el usuario ya vio la introducción. */
    val onboardingDone: Flow<Boolean>

    suspend fun setOnboardingDone()
}

private val Context.veloraPrefs: DataStore<Preferences> by preferencesDataStore(name = "velora_prefs")

class DataStorePrefsRepository(context: Context) : PrefsRepository {

    private val store = context.applicationContext.veloraPrefs

    override val theme: Flow<ThemeSettings> = store.data
        .map { prefs ->
            StoredTheme(
                font = prefs[FONT],
                clock = prefs[CLOCK],
                icons = prefs[ICONS],
                textSize = prefs[TEXT_SIZE],
                leftHanded = prefs[LEFT_HANDED],
                fullList = prefs[FULL_LIST],
                preset = prefs[PRESET],
            ).decode()
        }
        .distinctUntilChanged()

    override val onboardingDone: Flow<Boolean> = store.data.map { it[ONBOARDING_DONE] == true }.distinctUntilChanged()

    override suspend fun setFont(font: FontChoice) = put(FONT, font.name)

    override suspend fun setClockStyle(style: ClockStyle) = put(CLOCK, style.name)

    override suspend fun setIconStyle(style: IconStyle) = put(ICONS, style.name)

    override suspend fun setTextSize(size: TextSize) = put(TEXT_SIZE, size.name)

    override suspend fun setLeftHanded(enabled: Boolean) = put(LEFT_HANDED, enabled)

    override suspend fun setFullList(enabled: Boolean) = put(FULL_LIST, enabled)

    override suspend fun applyPreset(preset: ThemePreset) {
        val theme = preset.recommended
        store.edit {
            it[PRESET] = preset.name
            it[FONT] = theme.font.name
            it[CLOCK] = theme.clock.name
            it[ICONS] = theme.icons.name
        }
    }

    override suspend fun setOnboardingDone() = put(ONBOARDING_DONE, true)

    private suspend fun <T> put(key: Preferences.Key<T>, value: T) {
        store.edit { it[key] = value }
    }

    private companion object {
        val FONT = stringPreferencesKey("theme_font")
        val CLOCK = stringPreferencesKey("theme_clock")
        val ICONS = stringPreferencesKey("theme_icons")
        val TEXT_SIZE = stringPreferencesKey("theme_text_size")
        val LEFT_HANDED = booleanPreferencesKey("layout_left_handed")
        val FULL_LIST = booleanPreferencesKey("layout_full_list")
        val PRESET = stringPreferencesKey("theme_preset")
        val ONBOARDING_DONE = booleanPreferencesKey("onboarding_done")
    }
}

/**
 * Lo que hay guardado, tal cual. Se guardan los nombres de los enums: un valor desconocido
 * (de otra versión) vuelve al de fábrica en vez de romper el arranque.
 */
internal data class StoredTheme(
    val font: String? = null,
    val clock: String? = null,
    val icons: String? = null,
    val textSize: String? = null,
    val leftHanded: Boolean? = null,
    val fullList: Boolean? = null,
    val preset: String? = null,
) {
    fun decode(): ThemeSettings {
        val defaults = ThemeSettings()
        return ThemeSettings(
            font = FontChoice.entries.firstOrNull { it.name == font } ?: defaults.font,
            clock = ClockStyle.entries.firstOrNull { it.name == clock } ?: defaults.clock,
            icons = IconStyle.entries.firstOrNull { it.name == icons } ?: defaults.icons,
            textSize = TextSize.entries.firstOrNull { it.name == textSize } ?: defaults.textSize,
            leftHanded = leftHanded ?: defaults.leftHanded,
            fullList = fullList ?: defaults.fullList,
            preset = ThemePreset.entries.firstOrNull { it.name == preset } ?: defaults.preset,
        )
    }
}
