package app.velora.launcher.macrobenchmark

import android.content.Intent
import androidx.benchmark.macro.BaselineProfileMode
import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.StartupTimingMetric
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Arranque del inicio (RENDIMIENTO §2): TTID objetivo ≤ 400 ms en frío y ≤ 100 ms en caliente.
 * Comparar sin compilar vs. con Baseline Profile muestra cuánto aporta el perfil.
 */
@RunWith(AndroidJUnit4::class)
class StartupBenchmark {

    @get:Rule
    val rule = MacrobenchmarkRule()

    @Test
    fun coldStartupNoCompilation() = startup(StartupMode.COLD, CompilationMode.None())

    @Test
    fun coldStartupBaselineProfile() = startup(StartupMode.COLD, CompilationMode.Partial(BaselineProfileMode.Require))

    @Test
    fun warmStartupBaselineProfile() = startup(StartupMode.WARM, CompilationMode.Partial(BaselineProfileMode.Require))

    private fun startup(startupMode: StartupMode, compilationMode: CompilationMode) = rule.measureRepeated(
        packageName = TARGET_PACKAGE,
        metrics = listOf(StartupTimingMetric()),
        compilationMode = compilationMode,
        startupMode = startupMode,
        iterations = ITERATIONS,
        setupBlock = { pressHome() },
    ) {
        startActivityAndWait(homeIntent())
    }

    private companion object {
        const val ITERATIONS = 10
        const val TARGET_PACKAGE = "app.velora.launcher"

        /** El intent LAUNCHER del paquete abre los ajustes; el inicio se abre explícito. */
        fun homeIntent(): Intent = Intent(Intent.ACTION_MAIN)
            .addCategory(Intent.CATEGORY_HOME)
            .setClassName(TARGET_PACKAGE, "$TARGET_PACKAGE.HomeActivity")
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
}
