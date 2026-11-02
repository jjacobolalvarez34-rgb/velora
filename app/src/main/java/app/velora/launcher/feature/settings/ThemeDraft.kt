package app.velora.launcher.feature.settings

import androidx.compose.runtime.Immutable
import app.velora.launcher.core.data.prefs.PrefsRepository
import app.velora.launcher.core.model.ThemePreset
import app.velora.launcher.core.model.ThemeSettings

/**
 * Borrador de Personalizar: los cambios se ven en la vista previa, pero el inicio no cambia hasta
 * "Guardar" ([saveTo]). [original] es lo guardado cuando empezó la edición; [edited], lo que se ve.
 * Solo cuenta lo que se elige en Personalizar ([sameLook]): modo zurdo, notificaciones, consejos
 * y frenos de uso no son parte del borrador.
 */
@Immutable
data class ThemeDraft(val original: ThemeSettings, val edited: ThemeSettings = original) {

    /** true si lo que se ve es distinto de lo guardado. */
    val hasChanges: Boolean get() = !edited.sameLook(original)

    fun edit(change: (ThemeSettings) -> ThemeSettings): ThemeDraft = copy(edited = change(edited))

    /** Vuelve a lo guardado. */
    fun discard(): ThemeDraft = copy(edited = original)

    /**
     * Escribe en [prefs] solo lo que cambió, de una vez. Si cambió el tema, primero se aplica (trae
     * su fuente, reloj, íconos y color) y luego se escribe encima lo que se eligió después.
     */
    suspend fun saveTo(prefs: PrefsRepository) {
        val old = original
        val new = edited
        val presetChanged = new.preset != old.preset
        if (presetChanged) prefs.applyPreset(new.preset)
        if (presetChanged || new.font != old.font) prefs.setFont(new.font)
        if (presetChanged || new.clock != old.clock) prefs.setClockStyle(new.clock)
        if (presetChanged || new.icons != old.icons) prefs.setIconStyle(new.icons)
        if (new.textSize != old.textSize) prefs.setTextSize(new.textSize)
        if (new.photoBackground != old.photoBackground) prefs.setPhotoBackground(new.photoBackground)
        if (presetChanged || new.customLook() != old.customLook()) {
            val look = new.custom
            prefs.updateCustom {
                it.copy(icons = look.icons, accentColor = look.accentColor, visible = look.visible)
            }
        }
    }
}

/**
 * Lo mismo que hace `PrefsRepository.applyPreset`, pero sobre el borrador: el fondo y los colores
 * del tema, con su fuente, reloj, íconos y diseño de íconos recomendados. La forma, el color de
 * los íconos y el grosor del usuario se quedan; el color del tema vuelve al del tema.
 */
fun ThemeSettings.withPreset(preset: ThemePreset): ThemeSettings {
    val recommended = preset.recommended
    return copy(
        preset = preset,
        font = recommended.font,
        clock = recommended.clock,
        icons = recommended.icons,
        custom = custom.copy(
            icons = custom.icons.copy(pack = recommended.custom.icons.pack),
            accentColor = null,
        ),
    )
}

/** true si [other] se ve igual en el inicio en todo lo que se elige en Personalizar. */
fun ThemeSettings.sameLook(other: ThemeSettings): Boolean = font == other.font &&
    clock == other.clock &&
    icons == other.icons &&
    textSize == other.textSize &&
    preset == other.preset &&
    photoBackground == other.photoBackground &&
    customLook() == other.customLook()

/** La parte de la personalización fina que se edita en Personalizar. */
private fun ThemeSettings.customLook() = Triple(custom.icons, custom.accentColor, custom.visible)
