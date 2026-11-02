package app.velora.launcher.feature.settings

import android.os.Build
import androidx.annotation.StringRes
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import app.velora.launcher.R

/**
 * Familias de fabricantes con ajustes propios que apagan servicios en segundo plano (B-002):
 * cada una tiene sus pasos para que no se corten las notificaciones ni el bloqueo con doble toque.
 */
enum class DeviceFamily(@StringRes val label: Int, @StringRes val steps: Int) {
    Xiaomi(R.string.help_family_xiaomi, R.string.help_steps_xiaomi),
    Samsung(R.string.help_family_samsung, R.string.help_steps_samsung),
    Huawei(R.string.help_family_huawei, R.string.help_steps_huawei),
    Oppo(R.string.help_family_oppo, R.string.help_steps_oppo),
    Vivo(R.string.help_family_vivo, R.string.help_steps_vivo),
    Other(R.string.help_family_other, R.string.help_steps_other),
    ;

    companion object {
        /** Por la marca del teléfono (`Build.MANUFACTURER`), sin permisos. */
        fun of(manufacturer: String): DeviceFamily = when (manufacturer.lowercase()) {
            "xiaomi", "redmi", "poco" -> Xiaomi
            "samsung" -> Samsung
            "huawei", "honor" -> Huawei
            "oppo", "realme", "oneplus" -> Oppo
            "vivo", "iqoo" -> Vivo
            else -> Other
        }

        val current: DeviceFamily get() = of(Build.MANUFACTURER.orEmpty())
    }
}

/**
 * Ayuda para tu teléfono: los pasos de tu marca primero y un botón a la pantalla de batería del
 * sistema. Las demás marcas quedan debajo, por si el teléfono es de otra familia.
 */
internal fun LazyListScope.deviceHelpSection(onOpenBattery: () -> Unit) {
    val mine = DeviceFamily.current
    item(key = "help-title") { SectionTitle(R.string.help_title) }
    item(key = "help-mine") { HelpCard(mine.label, mine.steps, highlighted = true) }
    item(key = "help-battery") {
        Option(
            title = stringResource(R.string.help_open_battery),
            subtitle = stringResource(R.string.help_open_battery_summary),
            onClick = onOpenBattery,
            icon = Icons.Outlined.Settings,
        )
    }
    item(key = "help-others") { SectionTitle(R.string.help_other_brands) }
    DeviceFamily.entries.filter { it != mine }.forEach { family ->
        item(key = "help-${family.name}") { HelpCard(family.label, family.steps, highlighted = false) }
    }
}

@Composable
private fun HelpCard(@StringRes label: Int, @StringRes steps: Int, highlighted: Boolean) {
    ListItem(
        headlineContent = { Text(stringResource(label)) },
        supportingContent = { Text(stringResource(steps)) },
        overlineContent = if (highlighted) ({ Text(stringResource(R.string.help_your_phone)) }) else null,
        colors = cardColors(),
        modifier = Modifier.settingsCard(),
    )
}
