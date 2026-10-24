package app.velora.launcher.feature.home

import app.cash.turbine.test
import app.velora.launcher.core.data.clock.ClockRepository
import app.velora.launcher.core.model.ClockState
import java.time.LocalDateTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    private class FakeClockRepository(var now: ClockState) : ClockRepository {
        val ticks = MutableSharedFlow<ClockState>()
        override val clock = ticks
        override fun current() = now
    }

    /** Formateador trivial: la prueba es del ViewModel, no del formato del idioma. */
    private val formatter = ClockFormatter { state ->
        ClockText(time = "${state.now.hour}:${state.now.minute}", date = if (state.is24Hour) "24" else "12")
    }

    private val start = ClockState(LocalDateTime.of(2026, 10, 23, 7, 5), is24Hour = false)

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
        val viewModel = HomeViewModel(FakeClockRepository(start), formatter)

        assertEquals(ClockText(time = "7:5", date = "12"), viewModel.uiState.value.clock)
    }

    @Test
    fun `cada tick del reloj actualiza la hora mostrada`() = runTest {
        val repository = FakeClockRepository(start)
        val viewModel = HomeViewModel(repository, formatter)

        viewModel.uiState.test {
            assertEquals("7:5", awaitItem().clock.time)
            repository.ticks.emit(ClockState(LocalDateTime.of(2026, 10, 23, 7, 6), is24Hour = false))
            assertEquals("7:6", awaitItem().clock.time)
        }
    }

    @Test
    fun `cambiar a formato 24 h se refleja sin reiniciar`() = runTest {
        val repository = FakeClockRepository(start)
        val viewModel = HomeViewModel(repository, formatter)

        viewModel.uiState.test {
            skipItems(1)
            repository.ticks.emit(start.copy(is24Hour = true))
            assertEquals("24", awaitItem().clock.date)
        }
    }
}
