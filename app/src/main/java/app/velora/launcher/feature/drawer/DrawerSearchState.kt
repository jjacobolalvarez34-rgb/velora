package app.velora.launcher.feature.drawer

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.clearText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

/**
 * Estado del buscador de la lista. "Buscando" = el campo tiene el foco o hay texto: en ese modo
 * la lista completa desaparece y quedan las recientes o los resultados.
 */
@Stable
class DrawerSearchState {
    val text = TextFieldState()

    var focused by mutableStateOf(false)

    /** Letra elegida en el alfabeto; null = aún no se ha tocado (se ven recientes y buscador). */
    var letter by mutableStateOf<String?>(null)

    val active: Boolean get() = focused || text.text.isNotEmpty()

    fun clear() = text.clearText()
}

@Composable
fun rememberDrawerSearchState(): DrawerSearchState = remember { DrawerSearchState() }
