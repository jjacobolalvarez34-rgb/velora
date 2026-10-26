package app.velora.launcher.feature.home

import android.content.Intent
import app.cash.turbine.test
import app.velora.launcher.core.data.apps.AppsRepository
import app.velora.launcher.core.data.battery.BatteryRepository
import app.velora.launcher.core.data.clock.ClockRepository
import app.velora.launcher.core.data.favorites.FavoriteEntity
import app.velora.launcher.core.data.favorites.FavoritesDao
import app.velora.launcher.core.data.favorites.FavoritesRepository
import app.velora.launcher.core.data.favorites.HiddenAppEntity
import app.velora.launcher.core.data.favorites.HiddenAppsDao
import app.velora.launcher.core.data.favorites.HiddenAppsRepository
import app.velora.launcher.core.data.home.DefaultHomeRepository
import app.velora.launcher.core.data.prefs.PrefsRepository
import app.velora.launcher.core.model.AppEntry
import app.velora.launcher.core.model.AppKey
import app.velora.launcher.core.model.ClockState
import app.velora.launcher.core.model.ClockStyle
import app.velora.launcher.core.model.FontChoice
import app.velora.launcher.core.model.IconStyle
import app.velora.launcher.core.model.TextSize
import app.velora.launcher.core.model.ThemePreset
import app.velora.launcher.core.model.ThemeSettings
import java.time.LocalDateTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    private class FakeClock(var now: ClockState) : ClockRepository {
        val ticks = MutableSharedFlow<ClockState>(replay = 1).apply { tryEmit(now) }
        override val clock = ticks
        override fun current() = now
    }

    private class FakeApps(apps: List<AppEntry>) : AppsRepository {
        override val apps = MutableStateFlow(apps)
        var refreshes = 0
        override fun refresh() {
            refreshes++
        }
    }

    private class FakeFavoritesDao : FavoritesDao {
        val rows = MutableStateFlow<List<FavoriteEntity>>(emptyList())
        override fun observe() = rows.map { list -> list.sortedBy { it.position } }
        override suspend fun count() = rows.value.size
        override suspend fun maxPosition() = rows.value.maxOfOrNull { it.position } ?: -1
        override suspend fun insert(favorite: FavoriteEntity) {
            if (rows.value.none { it.stableId == favorite.stableId }) rows.value += favorite
        }
        override suspend fun delete(stableId: String) {
            rows.value = rows.value.filterNot { it.stableId == stableId }
        }
        override suspend fun setPosition(stableId: String, position: Int) {
            rows.value = rows.value.map { if (it.stableId == stableId) it.copy(position = position) else it }
        }
    }

    private class FakeDefaultHome(isDefault: Boolean?) : DefaultHomeRepository {
        override val isDefault = MutableStateFlow(isDefault)
        override val welcomeDismissed = MutableStateFlow(false)
        override fun dismissWelcome() {
            welcomeDismissed.value = true
        }
        override suspend fun refresh() = Unit
        override fun requestIntent() = Intent()
    }

    private class FakeHiddenDao : HiddenAppsDao {
        val rows = MutableStateFlow<List<HiddenAppEntity>>(emptyList())
        override fun observe() = rows
        override suspend fun insert(app: HiddenAppEntity) {
            rows.value += app
        }
        override suspend fun delete(stableId: String) {
            rows.value = rows.value.filterNot { it.stableId == stableId }
        }
    }

    private class FakePrefs : PrefsRepository {
        override val theme = MutableStateFlow(ThemeSettings())
        override suspend fun setFont(font: FontChoice) {
            theme.value = theme.value.copy(font = font)
        }
        override suspend fun setClockStyle(style: ClockStyle) {
            theme.value = theme.value.copy(clock = style)
        }
        override suspend fun setIconStyle(style: IconStyle) {
            theme.value = theme.value.copy(icons = style)
        }
        override suspend fun setTextSize(size: TextSize) {
            theme.value = theme.value.copy(textSize = size)
        }
        override suspend fun setLeftHanded(enabled: Boolean) {
            theme.value = theme.value.copy(leftHanded = enabled)
        }
        override suspend fun setFullList(enabled: Boolean) {
            theme.value = theme.value.copy(fullList = enabled)
        }
        override suspend fun applyPreset(preset: ThemePreset) {
            theme.value = preset.recommended
        }
        override suspend fun setPhotoBackground(path: String?) {
            theme.value = theme.value.copy(photoBackground = path)
        }
        override suspend fun setNotifications(enabled: Boolean) {
            theme.value = theme.value.copy(notifications = enabled)
        }
        override suspend fun setDoubleTapLock(enabled: Boolean) {
            theme.value = theme.value.copy(doubleTapLock = enabled)
        }
        override val onboardingDone = MutableStateFlow(true)
        override suspend fun setOnboardingDone() {
            onboardingDone.value = true
        }
    }

    private class FakeBattery : BatteryRepository {
        override val level = MutableStateFlow<Int?>(82)
    }

    /** Formateador trivial: la prueba es del ViewModel, no del formato del idioma. */
    private val formatter = ClockFormatter { state ->
        ClockText(time = "${state.now.hour}:${state.now.minute}", date = if (state.is24Hour) "24" else "12")
    }

    private val start = ClockState(LocalDateTime.of(2026, 10, 23, 7, 5), is24Hour = false)

    private fun app(label: String) = AppEntry(
        key = AppKey("pkg.$label", "Main", 0),
        label = label,
        section = label.take(1),
        searchKey = label.lowercase(),
        isWorkProfile = false,
        canUninstall = true,
    )

    private val camera = app("Cámara")
    private val clock = app("Reloj")

    private fun viewModel(
        clockRepo: FakeClock = FakeClock(start),
        apps: FakeApps = FakeApps(listOf(camera, clock)),
        dao: FakeFavoritesDao = FakeFavoritesDao(),
        home: FakeDefaultHome = FakeDefaultHome(isDefault = true),
        prefs: FakePrefs = FakePrefs(),
    ) = HomeViewModel(
        clockRepository = clockRepo,
        clockFormatter = formatter,
        appsRepository = apps,
        favoritesRepository = FavoritesRepository(dao, apps.apps),
        defaultHomeRepository = home,
        hiddenAppsRepository = HiddenAppsRepository(FakeHiddenDao(), apps.apps),
        prefsRepository = prefs,
        batteryRepository = FakeBattery(),
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `el primer estado ya trae la hora sin esperar al primer tick`() {
        assertEquals(ClockText(time = "7:5", date = "12"), viewModel().uiState.value.clock)
    }

    @Test
    fun `cada tick del reloj actualiza la hora mostrada`() = runTest {
        val clockRepo = FakeClock(start)
        val viewModel = viewModel(clockRepo = clockRepo)

        viewModel.uiState.test {
            assertEquals("7:5", awaitItem().clock.time)
            clockRepo.ticks.emit(ClockState(LocalDateTime.of(2026, 10, 23, 7, 6), is24Hour = false))
            assertEquals("7:6", awaitItem().clock.time)
        }
    }

    @Test
    fun `agregar un favorito lo muestra en el inicio en orden`() = runTest {
        val viewModel = viewModel()

        viewModel.uiState.test {
            skipItems(1)
            viewModel.onAddFavorite(clock.key)
            viewModel.onAddFavorite(camera.key)
            assertEquals(listOf("Reloj", "Cámara"), expectMostRecentItem().favorites.map { it.label })
        }
    }

    @Test
    fun `un favorito desinstalado no aparece`() = runTest {
        val apps = FakeApps(listOf(camera, clock))
        val viewModel = viewModel(apps = apps)

        viewModel.uiState.test {
            viewModel.onAddFavorite(camera.key)
            apps.apps.value = listOf(clock)
            assertTrue(expectMostRecentItem().favorites.isEmpty())
        }
    }

    @Test
    fun `con 12 favoritos avisa en vez de agregar otro`() = runTest {
        val dao = FakeFavoritesDao()
        repeat(12) { dao.insert(FavoriteEntity("id$it", "p$it", "c", 0, it)) }
        val viewModel = viewModel(dao = dao)

        viewModel.onAddFavorite(camera.key)

        assertEquals(HomeEvent.FavoritesFull, viewModel.events.first())
        assertEquals(12, dao.count())
    }

    @Test
    fun `la bienvenida se muestra si Velora no es el inicio y se oculta con Ahora no`() = runTest {
        val viewModel = viewModel(home = FakeDefaultHome(isDefault = false))

        viewModel.uiState.test {
            assertTrue(expectMostRecentItem().showWelcome)
            viewModel.onWelcomeDismissed()
            assertFalse(awaitItem().showWelcome)
        }
    }

    @Test
    fun `mientras no se sabe si es el inicio no se muestra la bienvenida`() = runTest {
        val viewModel = viewModel(home = FakeDefaultHome(isDefault = null))

        viewModel.uiState.test {
            assertFalse(expectMostRecentItem().showWelcome)
        }
    }

    @Test
    fun `el estilo de reloj y la batería llegan al inicio`() = runTest {
        val prefs = FakePrefs()
        val viewModel = viewModel(prefs = prefs)

        viewModel.uiState.test {
            assertEquals(82, expectMostRecentItem().battery)
            prefs.setClockStyle(ClockStyle.Day)
            assertEquals(ClockStyle.Day, awaitItem().clockStyle)
        }
    }

    @Test
    fun `ocultar una app avisa dónde recuperarla`() = runTest {
        val viewModel = viewModel()

        viewModel.onHide(camera)

        assertEquals(HomeEvent.AppHidden("Cámara"), viewModel.events.first())
    }

    @Test
    fun `el tema se conoce antes de dibujar el inicio`() {
        assertEquals(ThemeSettings(), viewModel().theme.value)
    }

    @Test
    fun `al volver al frente se refresca la lista de apps`() {
        val apps = FakeApps(emptyList())

        viewModel(apps = apps).onResume()

        assertEquals(1, apps.refreshes)
    }
}
