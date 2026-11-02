package app.velora.launcher.ui

import app.velora.launcher.core.model.AppEntry
import app.velora.launcher.core.model.AppKey
import app.velora.launcher.core.model.Customization
import app.velora.launcher.core.model.ThemePreset
import app.velora.launcher.core.model.ThemeSettings
import app.velora.launcher.core.model.VisibleElements
import app.velora.launcher.feature.home.HomeUiState
import app.velora.launcher.feature.home.components.HomeTip
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeTipsTest {

    private val app = AppEntry(AppKey("pkg", "Main", 0), "App", "A", "app", isWorkProfile = false, canUninstall = true)

    @Test
    fun `al empezar aparecen los consejos de lo que falta, en orden`() {
        val tips = pendingTips(ThemeSettings(), HomeUiState(), isDefault = false)

        assertEquals(HomeTip.SetDefault, tips.first())
        assertTrue(HomeTip.Favorites in tips)
        assertTrue(HomeTip.Theme in tips)
    }

    @Test
    fun `lo que ya está hecho no se sugiere`() {
        val theme = ThemeSettings(preset = ThemePreset.Galaxy, notifications = true)
        val tips = pendingTips(theme, HomeUiState(favorites = listOf(app)), isDefault = true)

        assertFalse(HomeTip.SetDefault in tips)
        assertFalse(HomeTip.Favorites in tips)
        assertFalse(HomeTip.Theme in tips)
        assertFalse(HomeTip.Notifications in tips)
    }

    @Test
    fun `los quitados no vuelven y apagar los consejos los oculta todos`() {
        val dismissed = ThemeSettings(custom = Customization(dismissedTips = setOf("alphabet", "widgets")))
        val off = ThemeSettings(custom = Customization(visible = VisibleElements(tips = false)))

        assertFalse(HomeTip.Alphabet in pendingTips(dismissed, HomeUiState(), isDefault = true))
        assertTrue(pendingTips(off, HomeUiState(), isDefault = false).isEmpty())
    }
}
