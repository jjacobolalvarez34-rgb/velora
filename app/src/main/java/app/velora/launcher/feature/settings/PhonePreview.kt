package app.velora.launcher.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.velora.launcher.R
import app.velora.launcher.core.designsystem.components.ThemeBackground
import app.velora.launcher.core.designsystem.theme.Spacing
import app.velora.launcher.core.designsystem.theme.VeloraTheme
import app.velora.launcher.core.model.AppEntry
import app.velora.launcher.core.model.ThemeSettings
import app.velora.launcher.feature.apps.AppRow
import app.velora.launcher.feature.apps.AppRowStyle
import app.velora.launcher.feature.apps.LocalIconOptions
import app.velora.launcher.feature.apps.LocalIconStyle
import app.velora.launcher.feature.apps.PreviewAppActions
import app.velora.launcher.feature.drawer.WaveAlphabetWidth
import app.velora.launcher.feature.home.components.ClockHeader

/** Tamaño real de un teléfono (19,5:9): la vista previa se dibuja así y se achica, nunca se re-maqueta. */
private val PhoneWidth = 360.dp
private val PhoneHeight = 780.dp

/**
 * El inicio tal como va a quedar, dentro de un marco de teléfono (ADR-028): el mismo fondo, reloj,
 * fuente, íconos y colores que el inicio de verdad, con tus favoritos. Ocupa lo más que puede del
 * espacio que le dan, sin deformarse ni dejar huecos; [compact] usa un bisel más fino (tarjetas).
 */
@Composable
fun PhonePreview(
    theme: ThemeSettings,
    preview: ClockPreview,
    photo: ImageBitmap?,
    favorites: List<AppEntry>,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
) {
    val description = stringResource(R.string.personalize_preview_description)
    val bezel = if (compact) CompactBezel else Bezel
    val radius = if (compact) CompactRadius else FrameRadius
    val outer = RoundedCornerShape(radius)
    Box(
        modifier = modifier
            .shadow(if (compact) CompactShadow else FrameShadow, outer, clip = false)
            .background(Color.Black, outer)
            .border(FrameRing, MaterialTheme.colorScheme.outlineVariant, outer)
            .padding(bezel)
            .clearAndSetSemantics { contentDescription = description },
    ) {
        Box(Modifier.clip(RoundedCornerShape(radius - bezel)).fitInside(PhoneWidth, PhoneHeight)) {
            PreviewHome(theme, preview, photo, favorites)
        }
    }
}

/** El color del tema elegido por el usuario (ARGB guardado como Long), o null para el del tema. */
internal fun ThemeSettings.userAccent(): Color? = custom.accentColor?.let { Color(it.toInt()) }

@Composable
private fun PreviewHome(theme: ThemeSettings, preview: ClockPreview, photo: ImageBitmap?, favorites: List<AppEntry>) {
    val density = LocalDensity.current
    VeloraTheme(font = theme.font, preset = theme.preset, reducedMotion = true, accent = theme.userAccent()) {
        CompositionLocalProvider(
            LocalDensity provides Density(density.density, density.fontScale * theme.textSize.scale),
            LocalIconStyle provides theme.icons,
            LocalIconOptions provides theme.custom.icons,
        ) {
            Box(Modifier.fillMaxSize()) {
                ThemeBackground(theme.preset, Modifier.fillMaxSize(), photo)
                if (theme.custom.visible.statusBar) StatusBar(preview)
                PreviewContent(theme, preview, favorites)
                AlphabetHint(Modifier.align(Alignment.BottomEnd).padding(end = Spacing.xs, bottom = AlphabetBottom))
                GestureBar(Modifier.align(Alignment.BottomCenter).padding(bottom = Spacing.s))
            }
        }
    }
}

/** Igual que el inicio: el reloj baja un décimo de la pantalla y los favoritos van abajo. */
@Composable
private fun PreviewContent(theme: ThemeSettings, preview: ClockPreview, favorites: List<AppEntry>) {
    val visible = theme.custom.visible
    Column(
        Modifier.fillMaxSize().padding(
            start = Spacing.l,
            end = Spacing.l + WaveAlphabetWidth,
            top = StatusBarHeight + PhoneHeight * HEADER_TOP_FRACTION,
            bottom = Spacing.xl,
        ),
    ) {
        if (visible.clock) {
            ClockHeader(clock = preview.clock, style = theme.clock, battery = preview.battery, showDate = visible.date)
        }
        Spacer(Modifier.weight(1f))
        val shown = favorites.take(MAX_PREVIEW_APPS)
        if (shown.isEmpty()) {
            PlaceholderRows()
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                shown.forEach { AppRow(app = it, style = AppRowStyle.Favorite, actions = PreviewAppActions) }
            }
        }
    }
}

/** Sin favoritos todavía: barras suaves donde irían las apps. */
@Composable
private fun PlaceholderRows() {
    val colors = VeloraTheme.onWallpaper
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.m)) {
        repeat(MAX_PREVIEW_APPS) { row ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(PlaceholderIcon).background(colors.iconCircle, CircleShape))
                Spacer(Modifier.width(Spacing.m))
                Box(
                    Modifier
                        .size(width = PlaceholderBar - PlaceholderStep * row, height = PlaceholderBarHeight)
                        .background(colors.contentVariant, CircleShape),
                )
            }
        }
    }
}

/** La barra de estado: hora y batería chiquitas, para ver si se oculta. */
@Composable
private fun StatusBar(preview: ClockPreview) {
    val colors = VeloraTheme.onWallpaper
    val style = VeloraTheme.typography.caption.copy(fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().height(StatusBarHeight).padding(horizontal = Spacing.l),
    ) {
        Text(preview.clock.time, style = style, color = colors.content, maxLines = 1)
        preview.battery?.let {
            Text(stringResource(R.string.clock_battery, it), style = style, color = colors.content, maxLines = 1)
        }
    }
}

/** El alfabeto del borde, en reposo: letras pequeñas y tenues, como en el inicio. */
@Composable
private fun AlphabetHint(modifier: Modifier = Modifier) {
    val colors = VeloraTheme.onWallpaper
    Text(
        text = remember { ('A'..'Z').joinToString("\n") },
        style = VeloraTheme.typography.letter.copy(lineHeight = 15.sp),
        color = colors.contentVariant,
        modifier = modifier.width(WaveAlphabetWidth),
        textAlign = TextAlign.Center,
    )
}

/** La rayita de los gestos del sistema, abajo al centro. */
@Composable
private fun GestureBar(modifier: Modifier = Modifier) {
    Box(
        modifier
            .size(width = GestureWidth, height = GestureHeight)
            .background(VeloraTheme.onWallpaper.content.copy(alpha = GESTURE_ALPHA), CircleShape),
    )
}

/**
 * La tarjeta grande de "Personalizar" en el menú de Ajustes: el inicio en miniatura a la izquierda
 * y, a la derecha, qué se puede cambiar y el tema de ahora.
 */
@Composable
fun PersonalizeHero(
    theme: ThemeSettings,
    preview: ClockPreview,
    photo: ImageBitmap?,
    favorites: List<AppEntry>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .padding(horizontal = Spacing.m)
            .fillMaxWidth()
            .clip(HeroShape)
            .background(scheme.surfaceContainerHigh)
            .clickable(onClick = onClick)
            .padding(Spacing.m),
    ) {
        PhonePreview(theme, preview, photo, favorites, Modifier.height(HeroPreviewHeight), compact = true)
        Spacer(Modifier.width(Spacing.l))
        HeroText(theme, Modifier.weight(1f))
    }
}

private const val MAX_PREVIEW_APPS = 4

/** El reloj del inicio baja un décimo de la pantalla (`HomeScreen`). */
private const val HEADER_TOP_FRACTION = 0.1f
private const val GESTURE_ALPHA = 0.55f
private val Bezel: Dp = 7.dp
private val CompactBezel: Dp = 4.dp
private val FrameRadius: Dp = 30.dp
private val CompactRadius: Dp = 18.dp
private val FrameRing: Dp = 1.dp
private val FrameShadow: Dp = 18.dp
private val CompactShadow: Dp = 6.dp
private val StatusBarHeight = 36.dp
private val AlphabetBottom = 96.dp
private val GestureWidth = 120.dp
private val GestureHeight = 5.dp
private val PlaceholderIcon = 34.dp
private val PlaceholderBar = 150.dp
private val PlaceholderStep = 18.dp
private val PlaceholderBarHeight = 14.dp
private val HeroShape = RoundedCornerShape(28.dp)
private val HeroPreviewHeight = 196.dp

/** Título, una línea de qué se cambia y la pastilla con el tema de ahora. */
@Composable
private fun HeroText(theme: ThemeSettings, modifier: Modifier) {
    val scheme = MaterialTheme.colorScheme
    Column(modifier) {
        Text(
            stringResource(R.string.personalize_title),
            style = MaterialTheme.typography.titleLarge,
            color = scheme.onSurface,
        )
        Spacer(Modifier.height(Spacing.xs))
        Text(
            stringResource(R.string.personalize_hero_short),
            style = MaterialTheme.typography.bodyMedium,
            color = scheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(Spacing.m))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(R.string.personalize_current, stringResource(presetName(theme.preset))),
                style = MaterialTheme.typography.labelLarge,
                color = scheme.onSecondaryContainer,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .weight(1f, fill = false)
                    .background(scheme.secondaryContainer, CircleShape)
                    .padding(horizontal = Spacing.m, vertical = Spacing.xs + 2.dp),
            )
            Spacer(Modifier.width(Spacing.s))
            Icon(
                Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                contentDescription = null,
                tint = scheme.onSurfaceVariant,
            )
        }
    }
}
