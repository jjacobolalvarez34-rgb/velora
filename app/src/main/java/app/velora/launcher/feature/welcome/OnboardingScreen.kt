package app.velora.launcher.feature.welcome

import androidx.annotation.StringRes
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import app.velora.launcher.R
import app.velora.launcher.core.designsystem.theme.BrandColors
import app.velora.launcher.core.designsystem.theme.Shapes
import app.velora.launcher.core.designsystem.theme.Sizes
import app.velora.launcher.core.designsystem.theme.Spacing
import app.velora.launcher.core.designsystem.theme.VeloraTheme
import kotlinx.coroutines.launch

private val HeroLogoSize = 200.dp
private val IllustrationHeight = 220.dp
private val DotSize = 8.dp
private val DotActiveWidth = 24.dp

/** Una página del tutorial: su demo animada y su texto. */
private enum class IntroPage(@StringRes val title: Int, @StringRes val body: Int) {
    Hero(R.string.brand_name, R.string.welcome_tagline),
    Swipe(R.string.intro_swipe_title, R.string.intro_swipe_body),
    Wave(R.string.intro_wave_title, R.string.intro_wave_body),
    Menu(R.string.intro_menu_title, R.string.intro_menu_body),
    Theme(R.string.intro_theme_title, R.string.intro_theme_body),
}

/**
 * Introducción de la primera vez: qué hace Velora y cómo se usa, con demos animadas.
 * En la última página se ofrece hacerlo el inicio predeterminado.
 */
@Composable
fun OnboardingScreen(
    onSetDefault: () -> Unit,
    onNotNow: () -> Unit,
    modifier: Modifier = Modifier,
    alreadyDefault: Boolean = false,
) {
    val pages = IntroPage.entries
    val pager = rememberPagerState { pages.size }
    val scope = rememberCoroutineScope()
    val isLast = pager.currentPage == pages.lastIndex
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .fillMaxSize()
            .landingBackground()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = Spacing.l, vertical = Spacing.m),
    ) {
        Box(Modifier.fillMaxWidth().heightIn(min = Sizes.minTouchTarget), contentAlignment = Alignment.CenterEnd) {
            if (!isLast) {
                TextButton(onClick = { scope.launch { pager.animateScrollToPage(pages.lastIndex) } }) {
                    Text(stringResource(R.string.intro_skip), color = BrandColors.silverMuted)
                }
            }
        }
        HorizontalPager(state = pager, modifier = Modifier.weight(1f)) { index -> IntroPageContent(pages[index]) }
        PageDots(pager)
        Spacer(Modifier.height(Spacing.l))
        if (isLast) {
            WelcomeActions(onSetDefault, onNotNow, alreadyDefault)
        } else {
            NextButton { scope.launch { pager.animateScrollToPage(pager.currentPage + 1) } }
        }
    }
}

@Composable
private fun IntroPageContent(page: IntroPage) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxSize(),
    ) {
        Box(Modifier.height(IllustrationHeight).fillMaxWidth(), contentAlignment = Alignment.Center) {
            when (page) {
                IntroPage.Hero -> AnimatedLogo(logoSize = HeroLogoSize)
                IntroPage.Swipe -> SwipeUpDemo()
                IntroPage.Wave -> WaveDemo()
                IntroPage.Menu -> LongPressDemo()
                IntroPage.Theme -> ThemeDemo()
            }
        }
        Spacer(Modifier.height(Spacing.xl))
        val type = VeloraTheme.typography
        Text(
            text = stringResource(page.title),
            style = if (page == IntroPage.Hero) type.brandTitle else type.favorite,
            color = BrandColors.silver,
            textAlign = TextAlign.Center,
            modifier = Modifier.semantics { heading() },
        )
        Spacer(Modifier.height(Spacing.s))
        Text(
            text = stringResource(page.body),
            style = type.date,
            color = BrandColors.silverMuted,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = Spacing.m),
        )
    }
}

/** Puntos de página: el actual se alarga con un resorte. */
@Composable
private fun PageDots(pager: PagerState) {
    val description = stringResource(R.string.intro_page, pager.currentPage + 1, pager.pageCount)
    Row(
        horizontalArrangement = Arrangement.spacedBy(Spacing.s),
        modifier = Modifier.semantics { contentDescription = description },
    ) {
        repeat(pager.pageCount) { index ->
            val active = index == pager.currentPage
            val motion = VeloraTheme.motion
            val width by animateDpAsState(
                if (active) DotActiveWidth else DotSize,
                motion.spatialDefault(),
                label = "dot",
            )
            val color by animateColorAsState(
                if (active) BrandColors.silver else BrandColors.silverMuted,
                motion.effectDefault(),
                label = "dot-color",
            )
            Box(Modifier.size(width = width, height = DotSize).background(color, CircleShape))
        }
    }
}

@Composable
private fun NextButton(onClick: () -> Unit) {
    Button(
        onClick = onClick,
        shape = Shapes.sheet,
        colors = ButtonDefaults.buttonColors(
            containerColor = BrandColors.silver,
            contentColor = BrandColors.background,
        ),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = Sizes.appRowHeight),
    ) {
        Text(stringResource(R.string.intro_next), style = VeloraTheme.typography.appRow)
    }
    // Mismo alto que la pareja de botones de la última página: el contenido no salta.
    Spacer(Modifier.height(Sizes.minTouchTarget).width(1.dp))
}

@Preview(name = "Introducción", heightDp = 760)
@Composable
private fun OnboardingPreview() {
    VeloraTheme(reducedMotion = true) {
        OnboardingScreen(onSetDefault = {}, onNotNow = {})
    }
}
