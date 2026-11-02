package app.velora.launcher.feature.settings

import app.velora.launcher.core.data.prefs.PrefsRepository
import app.velora.launcher.core.model.ClockStyle
import app.velora.launcher.core.model.Customization
import app.velora.launcher.core.model.FontChoice
import app.velora.launcher.core.model.IconShape
import app.velora.launcher.core.model.IconStyle
import app.velora.launcher.core.model.TextSize
import app.velora.launcher.core.model.ThemePreset
import app.velora.launcher.core.model.ThemeSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ThemeDraftTest {

    /** Preferencias en memoria; anota cada escritura para ver que se guarda solo lo necesario. */
    private class FakePrefs(initial: ThemeSettings) : PrefsRepository {
        val writes = mutableListOf<String>()
        override val theme = MutableStateFlow(initial)

        private fun write(name: String, change: (ThemeSettings) -> ThemeSettings) {
            writes += name
            theme.value = change(theme.value)
        }

        override suspend fun setFont(font: FontChoice) = write("font") { it.copy(font = font) }
        override suspend fun setClockStyle(style: ClockStyle) = write("clock") { it.copy(clock = style) }
        override suspend fun setIconStyle(style: IconStyle) = write("icons") { it.copy(icons = style) }
        override suspend fun setTextSize(size: TextSize) = write("textSize") { it.copy(textSize = size) }
        override suspend fun setLeftHanded(enabled: Boolean) = write("leftHanded") { it.copy(leftHanded = enabled) }
        override suspend fun setFullList(enabled: Boolean) = write("fullList") { it.copy(fullList = enabled) }
        override suspend fun setPhotoBackground(path: String?) = write("photo") { it.copy(photoBackground = path) }
        override suspend fun setNotifications(enabled: Boolean) =
            write("notifications") { it.copy(notifications = enabled) }
        override suspend fun setDoubleTapLock(enabled: Boolean) = write("lock") { it.copy(doubleTapLock = enabled) }
        override suspend fun setUsageTime(enabled: Boolean) = write("usage") { it.copy(usageTime = enabled) }
        override suspend fun setContactsSearch(enabled: Boolean) =
            write("contacts") { it.copy(contactsSearch = enabled) }

        /** Como `DataStorePrefsRepository.applyPreset`: el resto de las preferencias se queda. */
        override suspend fun applyPreset(preset: ThemePreset) = write("preset") {
            val recommended = preset.recommended
            it.copy(
                preset = preset,
                font = recommended.font,
                clock = recommended.clock,
                icons = recommended.icons,
                custom = it.custom.copy(
                    icons = it.custom.icons.copy(pack = recommended.custom.icons.pack),
                    accentColor = null,
                ),
            )
        }

        override suspend fun updateCustom(change: (Customization) -> Customization) =
            write("custom") { it.copy(custom = change(it.custom)) }

        override val onboardingDone = MutableStateFlow(true)
        override suspend fun setOnboardingDone() {
            onboardingDone.value = true
        }
    }

    private val saved = ThemeSettings(
        leftHanded = true,
        notifications = true,
        photoBackground = "/files/backgrounds/vieja.jpg",
        custom = Customization(dismissedTips = setOf("alphabet"), usageLimits = mapOf("pkg.video" to 30)),
    )

    @Test
    fun `un borrador recien creado no tiene cambios`() {
        assertFalse(ThemeDraft(saved).hasChanges)
    }

    @Test
    fun `cambiar algo del borrador marca cambios`() {
        val draft = ThemeDraft(saved).edit { it.copy(clock = ClockStyle.Analog) }

        assertTrue(draft.hasChanges)
        assertEquals(ClockStyle.Analog, draft.edited.clock)
        assertEquals(ClockStyle.Classic, draft.original.clock)
    }

    @Test
    fun `volver a elegir lo de antes deja el borrador sin cambios`() {
        val draft = ThemeDraft(saved)
            .edit { it.copy(font = FontChoice.Fraunces) }
            .edit { it.copy(font = saved.font) }

        assertFalse(draft.hasChanges)
    }

    @Test
    fun `descartar vuelve a lo guardado`() {
        val draft = ThemeDraft(saved)
            .edit { it.copy(font = FontChoice.Cinzel, textSize = TextSize.Large) }
            .discard()

        assertFalse(draft.hasChanges)
        assertEquals(saved, draft.edited)
    }

    @Test
    fun `lo que no se elige en Personalizar no cuenta como cambio`() {
        val draft = ThemeDraft(saved).edit { it.copy(leftHanded = false, notifications = false) }

        assertFalse(draft.hasChanges)
    }

    @Test
    fun `guardar escribe todo lo elegido en las preferencias`() = runTest {
        val prefs = FakePrefs(saved)
        val draft = ThemeDraft(saved).edit {
            it.copy(
                font = FontChoice.Fraunces,
                clock = ClockStyle.Words,
                icons = IconStyle.Minimal,
                textSize = TextSize.Small,
                photoBackground = "/files/backgrounds/nueva.jpg",
                custom = it.custom.copy(
                    accentColor = 0xFFFF7A7A,
                    icons = it.custom.icons.copy(shape = IconShape.Squircle),
                    visible = it.custom.visible.copy(statusBar = false),
                ),
            )
        }

        draft.saveTo(prefs)

        val result = prefs.theme.value
        assertTrue(result.sameLook(draft.edited))
        assertEquals("/files/backgrounds/nueva.jpg", result.photoBackground)
        assertEquals(0xFFFF7A7A, result.custom.accentColor)
        assertFalse(result.custom.visible.statusBar)
        // Lo personal no se toca al guardar el aspecto.
        assertTrue(result.leftHanded)
        assertTrue(result.notifications)
        assertEquals(setOf("alphabet"), result.custom.dismissedTips)
        assertEquals(mapOf("pkg.video" to 30), result.custom.usageLimits)
    }

    @Test
    fun `antes de guardar el inicio no cambia`() {
        val prefs = FakePrefs(saved)
        ThemeDraft(saved).edit { it.copy(photoBackground = null, clock = ClockStyle.Outline) }

        assertEquals(saved, prefs.theme.value)
        assertTrue(prefs.writes.isEmpty())
    }

    @Test
    fun `guardar sin cambios no escribe nada`() = runTest {
        val prefs = FakePrefs(saved)

        ThemeDraft(saved).saveTo(prefs)

        assertTrue(prefs.writes.isEmpty())
    }

    @Test
    fun `guardar escribe solo lo que cambio`() = runTest {
        val prefs = FakePrefs(saved)

        ThemeDraft(saved).edit { it.copy(clock = ClockStyle.Minimal) }.saveTo(prefs)

        assertEquals(listOf("clock"), prefs.writes)
    }

    @Test
    fun `con otro tema se aplica el tema y encima lo que se eligio despues`() = runTest {
        val prefs = FakePrefs(saved)
        val draft = ThemeDraft(saved)
            .edit { it.withPreset(ThemePreset.Paper) }
            .edit { it.copy(font = FontChoice.Nunito) }

        draft.saveTo(prefs)

        val result = prefs.theme.value
        assertEquals(ThemePreset.Paper, result.preset)
        assertEquals(FontChoice.Nunito, result.font)
        assertEquals(ThemePreset.Paper.recommended.clock, result.clock)
        assertEquals("preset", prefs.writes.first())
        assertTrue(result.sameLook(draft.edited))
    }

    @Test
    fun `elegir un tema en el borrador quita el color propio y deja la forma de los iconos`() {
        val mine = saved.copy(
            custom = saved.custom.copy(
                accentColor = 0xFF7FB8FF,
                icons = saved.custom.icons.copy(shape = IconShape.Teardrop),
            ),
        )

        val themed = mine.withPreset(ThemePreset.Galaxy)

        assertNull(themed.custom.accentColor)
        assertEquals(IconShape.Teardrop, themed.custom.icons.shape)
        assertEquals(ThemePreset.Galaxy.recommended.custom.icons.pack, themed.custom.icons.pack)
        assertEquals(ThemePreset.Galaxy.recommended.font, themed.font)
    }
}
