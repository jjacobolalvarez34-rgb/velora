package app.velora.launcher.core.designsystem.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.CacheDrawScope
import androidx.compose.ui.draw.DrawResult
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ImageShader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntSize
import app.velora.launcher.core.designsystem.theme.ArtColors
import app.velora.launcher.core.designsystem.theme.VeloraTheme
import app.velora.launcher.core.model.ThemePreset
import kotlin.math.roundToInt

/**
 * Fondo de cada tema (ADR-018), dibujado por Velora: no cambia el wallpaper del sistema ni pide
 * permisos. El arte es procedural ([ArtCache]): se genera una vez fuera del hilo principal y,
 * mientras tanto, se ve el degradado del tema; luego entra con un fundido corto. Encima van los
 * detalles vectoriales y un grano fino. No hay animación continua, así que no gasta batería.
 * "Tu fondo" deja ver el wallpaper con un velo suave.
 */
@Composable
fun ThemeBackground(preset: ThemePreset, modifier: Modifier = Modifier, photo: ImageBitmap? = null) {
    if (photo != null) {
        PhotoBackground(photo, modifier)
        return
    }
    val look = ThemeLooks[preset]
    if (look == null) {
        WallpaperScrim(modifier)
        return
    }
    BoxWithConstraints(modifier.fillMaxSize()) {
        val bounded = constraints.hasBoundedWidth && constraints.hasBoundedHeight
        val layer = if (bounded) rememberArtLayer(preset, look, constraints.maxWidth, constraints.maxHeight) else null
        val grain = rememberGrain(look.grain > 0f)
        Box(Modifier.fillMaxSize().drawWithCache { drawLook(look, layer, grain) })
    }
}

/** El arte ya generado (o null mientras tanto) y su opacidad para el fundido de entrada. */
@Stable
private class ArtLayer(val image: MutableState<ImageBitmap?>, val alpha: Animatable<Float, AnimationVector1D>)

/**
 * Busca el arte en memoria al componer (sin parpadeo si ya existe) y, si no está, lo carga de
 * disco o lo genera en segundo plano y lo funde con `effectDefault`.
 */
@Composable
private fun rememberArtLayer(preset: ThemePreset, look: ThemeLook, width: Int, height: Int): ArtLayer? {
    val art = look.art ?: return null
    if (width <= 0 || height <= 0) return null
    val context = LocalContext.current.applicationContext
    val motion = VeloraTheme.motion
    val key = ArtCache.key(preset, width, height)
    val layer = remember(key) {
        val cached = ArtCache.cached(key)
        ArtLayer(mutableStateOf(cached), Animatable(if (cached != null) 1f else 0f))
    }
    LaunchedEffect(key) {
        if (layer.image.value == null) {
            layer.image.value = ArtCache.load(context, key, { art(height / width.toFloat()) }, width, height)
            layer.alpha.animateTo(1f, motion.effectDefault())
        }
    }
    return layer
}

/** El grano compartido por todos los temas; se crea la primera vez en segundo plano. */
@Composable
private fun rememberGrain(enabled: Boolean): ImageBitmap? {
    if (!enabled) return null
    return produceState(ArtCache.grainIfReady) { value = ArtCache.grainTile() }.value
}

private fun CacheDrawScope.drawLook(look: ThemeLook, layer: ArtLayer?, grain: ImageBitmap?): DrawResult {
    val fallback = Brush.verticalGradient(listOf(look.top, look.bottom))
    val overlay = look.overlay?.invoke(this)
    val grainBrush = grain?.let { grainBrush(it, density) }
    // Leído aquí: cuando llega el arte se rehace la caché de dibujo, sin recomponer.
    val image = layer?.image?.value
    val target = IntSize(size.width.roundToInt(), size.height.roundToInt())
    return onDrawBehind {
        val alpha = layer?.alpha?.value ?: 0f
        if (look.art != null && (image == null || alpha < 1f)) drawRect(fallback)
        if (image != null) drawImage(image, dstSize = target, alpha = alpha)
        overlay?.invoke(this)
        if (grainBrush != null) drawRect(grainBrush, alpha = look.grain)
    }
}

/** El mosaico de grano repetido; en pantallas densas se agranda un poco para que se note fino, no ausente. */
private fun grainBrush(tile: ImageBitmap, density: Float): Brush {
    val shader = ImageShader(tile, TileMode.Repeated, TileMode.Repeated)
    val scale = (density / 2f).coerceAtLeast(1f)
    shader.setLocalMatrix(android.graphics.Matrix().apply { setScale(scale, scale) })
    return ShaderBrush(shader)
}

/**
 * Foto propia del usuario (ADR-022) llenando la pantalla, con un velo para que el texto se lea:
 * oscuro en los temas de texto claro, claro en los de texto oscuro.
 */
@Composable
private fun PhotoBackground(photo: ImageBitmap, modifier: Modifier) {
    Box(modifier.fillMaxSize()) {
        Image(photo, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        if (VeloraTheme.onWallpaper.isLight) {
            Box(Modifier.fillMaxSize().background(ArtColors.lightVeil))
        } else {
            WallpaperScrim()
        }
    }
}
