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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.velora.launcher.R
import app.velora.launcher.core.designsystem.components.ThemeBackground
import app.velora.launcher.core.designsystem.theme.Spacing
import app.velora.launcher.core.designsystem.theme.VeloraTheme
import app.velora.launcher.core.model.AppEntry
import app.velora.launcher.core.model.ThemeSettings
import app.velora.launcher.feature.apps.AppRow
import app.velora.launcher.feature.apps.AppRowStyle
import app.velora.launcher.feature.apps.LocalIconStyle
import app.velora.launcher.feature.apps.PreviewAppActions
import app.velora.launcher.feature.home.components.ClockHeader

/** Tamaño real de un teléfono: la vista previa se dibuja así y se achica, nunca se re-maqueta. */
private val PhoneWidth = 360.dp
private val PhoneHeight = 760.dp
private val PhoneShape = RoundedCornerShape(36.dp)

/**
 * El inicio tal como va a quedar, en miniatura (ADR-028): el mismo fondo, reloj, fuente, íconos y
 * colores que el inicio de verdad, con tus favoritos. Cambia al instante con cada opción.
 *
 * @param scale qué tanto se achica (0.4 ≈ tarjeta, 0.5 ≈ vista principal de Personalizar).
 */
@Composable
fun PhonePreview(
    theme: ThemeSettings,
    preview: ClockPreview,
    photo: ImageBitmap?,
    favorites: List<AppEntry>,
    scale: Float,
    modifier: Modifier = Modifier,
) {
    val description = stringResource(R.string.personalize_preview_description)
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(PhoneWidth * scale, PhoneHeight * scale)
            .clearAndSetSemantics { contentDescription = description },
    ) {
        Box(
            Modifier
                .requiredSize(PhoneWidth, PhoneHeight)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    clip = true
                    shape = PhoneShape
                }
                .border(BorderWidth / scale, MaterialTheme.colorScheme.outlineVariant, PhoneShape),
        ) {
            PreviewHome(theme, preview, photo, favorites)
        }
    }
}

private val BorderWidth: Dp = 1.5.dp

/** El color del tema elegido por el usuario (ARGB guardado como Long), o null para el del tema. */
internal fun ThemeSettings.userAccent(): Color? = custom.accentColor?.let { Color(it.toInt()) }

@Composable
private fun PreviewHome(theme: ThemeSettings, preview: ClockPreview, photo: ImageBitmap?, favorites: List<AppEntry>) {
    val density = LocalDensity.current
    VeloraTheme(font = theme.font, preset = theme.preset, reducedMotion = true, accent = theme.userAccent()) {
        CompositionLocalProvider(
            LocalDensity provides Density(density.density, density.fontScale * theme.textSize.scale),
            LocalIconStyle provides theme.icons,
        ) {
            Box(Modifier.fillMaxSize()) {
                ThemeBackground(theme.preset, Modifier.fillMaxSize(), photo)
                PreviewContent(theme, preview, favorites)
            }
        }
    }
}

@Composable
private fun PreviewContent(theme: ThemeSettings, preview: ClockPreview, favorites: List<AppEntry>) {
    val visible = theme.custom.visible
    Column(Modifier.fillMaxSize().padding(start = Spacing.l, end = Spacing.l, top = 96.dp, bottom = Spacing.xl)) {
        if (visible.statusBar) StatusBarDots()
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
                Box(Modifier.size(34.dp).clip(RoundedCornerShape(50)).background(colors.iconCircle))
                Spacer(Modifier.width(Spacing.m))
                Box(
                    Modifier.size(
                        width = (150 - row * 18).dp,
                        height = 14.dp,
                    ).clip(RoundedCornerShape(7.dp)).background(colors.contentVariant),
                )
            }
        }
    }
}

/** La barra de estado de mentira (hora y batería chiquitas), para ver si se oculta. */
@Composable
private fun StatusBarDots() {
    val colors = VeloraTheme.onWallpaper
    Row(
        Modifier
            .fillMaxWidth()
            .padding(bottom = Spacing.xl),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Box(
            Modifier.size(
                width = 34.dp,
                height = 8.dp,
            ).clip(RoundedCornerShape(4.dp)).background(colors.contentVariant),
        )
        Box(
            Modifier.size(
                width = 46.dp,
                height = 8.dp,
            ).clip(RoundedCornerShape(4.dp)).background(colors.contentVariant),
        )
    }
}

/**
 * La tarjeta grande de "Personalizar" en el menú de Ajustes: la vista previa a la izquierda y qué
 * se puede cambiar a la derecha.
 */
@Composable
fun PersonalizeHero(
    theme: ThemeSettings,
    preview: ClockPreview,
    photo: ImageBitmap?,
    favorites: List<AppEntry>,
    onClick: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .padding(horizontal = Spacing.m, vertical = Spacing.s)
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .clickable(onClick = onClick)
            .padding(Spacing.m),
    ) {
        PhonePreview(theme, preview, photo, favorites, scale = HERO_SCALE)
        Spacer(Modifier.width(Spacing.l))
        Column(Modifier.weight(1f)) {
            Text(stringResource(R.string.personalize_title), style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.size(Spacing.xs))
            Text(
                stringResource(R.string.personalize_hero_summary),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.size(Spacing.m))
            Text(
                stringResource(R.string.personalize_current, stringResource(presetName(theme.preset))),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

private const val MAX_PREVIEW_APPS = 4
private const val HERO_SCALE = 0.3f
