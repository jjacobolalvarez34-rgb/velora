package app.velora.launcher.baselineprofile

import android.content.Intent
import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Genera el Baseline Profile de los flujos críticos (RENDIMIENTO §4).
 * Fase 0: solo arranque. Agregar aquí: abrir lista, arrastrar alfabeto, buscar, menú de app.
 *
 * `gradlew :app:generateBaselineProfile` con un dispositivo API 33+ conectado.
 */
@RunWith(AndroidJUnit4::class)
class BaselineProfileGenerator {

    @get:Rule
    val rule = BaselineProfileRule()

    @Test
    fun generate() = rule.collect(packageName = TARGET_PACKAGE, includeInStartupProfile = true) {
        pressHome()
        startActivityAndWait(homeIntent())
    }
}

internal const val TARGET_PACKAGE = "app.velora.launcher"

/**
 * El launcher se abre por su Activity HOME explícita: el intent LAUNCHER del paquete
 * apunta a los ajustes, no al inicio.
 */
internal fun homeIntent(): Intent = Intent(Intent.ACTION_MAIN)
    .addCategory(Intent.CATEGORY_HOME)
    .setClassName(TARGET_PACKAGE, "$TARGET_PACKAGE.HomeActivity")
    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
