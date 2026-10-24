package app.velora.launcher.feature.drawer

import androidx.compose.animation.ContentTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import app.velora.launcher.core.designsystem.motion.VeloraMotion
import app.velora.launcher.core.designsystem.theme.Spacing
import app.velora.launcher.core.designsystem.theme.VeloraTheme
import app.velora.launcher.feature.apps.AppActions

/** Al cambiar de letra, las apps se deslizan un cuarto de pantalla. */
private const val SLIDE_DIVISOR = 4

/** Qué muestra la lista en cada momento. */
internal sealed interface DrawerMode {
    /** Buscador con foco o con texto: recientes o resultados. */
    data object Search : DrawerMode

    /** Se abrió deslizando, sin letra: recientes y la pista para usar el alfabeto. */
    data object Explore : DrawerMode

    /** Opción de Ajustes: todas las apps en una lista con scroll. */
    data object Full : DrawerMode

    /** Solo las apps de la letra elegida en el alfabeto (por defecto). */
    data class Letter(val letter: String) : DrawerMode
}

internal fun drawerMode(search: DrawerSearchState, fullList: Boolean): DrawerMode {
    val letter = search.letter
    return when {
        search.active -> DrawerMode.Search
        fullList -> DrawerMode.Full
        letter == null -> DrawerMode.Explore
        else -> DrawerMode.Letter(letter)
    }
}

/** Entre letras: se desliza en la dirección del dedo. Entre modos: un fundido. */
internal fun modeTransition(
    from: DrawerMode,
    to: DrawerMode,
    letters: List<String>,
    motion: VeloraMotion,
): ContentTransform {
    if (from !is DrawerMode.Letter || to !is DrawerMode.Letter) {
        return fadeIn(motion.effectDefault()) togetherWith fadeOut(motion.effectFast())
    }
    val sign = if (letters.indexOf(to.letter) > letters.indexOf(from.letter)) 1 else -1
    val enter = slideInVertically(motion.spatialFast()) { height -> sign * height / SLIDE_DIVISOR }
    val exit = slideOutVertically(motion.spatialFast()) { height -> -sign * height / SLIDE_DIVISOR }
    return (enter + fadeIn(motion.effectFast())) togetherWith (exit + fadeOut(motion.effectFast()))
}

/** Solo las apps de una letra, con la letra grande arriba. */
@Composable
internal fun LetterPage(
    letter: String,
    uiState: DrawerUiState,
    padding: PaddingValues,
    actions: AppActions,
    modifier: Modifier,
) {
    val apps = uiState.list.byLetter[letter].orEmpty()
    LazyColumn(state = rememberLazyListState(), contentPadding = padding, modifier = modifier) {
        item(key = "letter") {
            Text(
                text = letter,
                style = VeloraTheme.typography.clock,
                color = VeloraTheme.onWallpaper.content,
                modifier = Modifier
                    .padding(bottom = Spacing.s)
                    .semantics { heading() },
            )
        }
        items(apps, key = { it.key.stableId }, contentType = { "app" }) { app ->
            ListApp(app, uiState, actions, animatedItem())
        }
    }
}
