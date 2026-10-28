package app.velora.launcher.core.designsystem.components

import app.velora.launcher.core.designsystem.theme.ArtColors
import kotlin.math.floor
import kotlin.math.sqrt

// Arte procedural cálido y claro: degradados de malla, atardecer lofi, bosque con niebla, café.

/** Una mancha de color de un degradado de malla, en ([x], [v]) relativos a la pantalla. */
internal class MeshSpot(val x: Float, val v: Float, val r: Float, val color: Rgb)

/**
 * Degradado de malla (mesh gradient): manchas suaves de color sobre [base], con los bordes
 * deformados por ruido para que no se vean círculos perfectos.
 */
internal fun mesh(base: Rgb, spots: Array<MeshSpot>, seed: Int): PixelArt = PixelArt { x, _, v, out ->
    val wx = (Noise.fbm(x * 1.5f, v * 1.5f, seed, 3) - 0.5f) * 0.4f
    val wy = (Noise.fbm(x * 1.5f + 4f, v * 1.5f + 2f, seed + 1, 3) - 0.5f) * 0.4f
    out.set(base)
    for (i in spots.indices) {
        val s = spots[i]
        out.mix(s.color, blob(x + wx, v + wy, s.x, s.v, s.r))
    }
}

/** Lavanda: lila con manchas de violeta, rosa y azul pervinca. */
internal fun lavenderArt() = mesh(
    ArtColors.lavenderBase.rgb(),
    arrayOf(
        MeshSpot(0.1f, 0.1f, 0.5f, ArtColors.lavenderViolet.rgb()),
        MeshSpot(0.95f, 0.42f, 0.5f, ArtColors.lavenderPink.rgb()),
        MeshSpot(0.15f, 0.86f, 0.55f, ArtColors.lavenderBlue.rgb()),
        MeshSpot(0.85f, 0.98f, 0.4f, ArtColors.lavenderViolet.rgb()),
        MeshSpot(0.55f, 0.62f, 0.28f, ArtColors.lavenderMist.rgb()),
    ),
    SEED + 90,
)

/** Matcha: crema verdosa con manchas de té, pistacho y leche. */
internal fun matchaArt() = mesh(
    ArtColors.matchaBase.rgb(),
    arrayOf(
        MeshSpot(0.12f, 0.08f, 0.5f, ArtColors.matchaTea.rgb()),
        MeshSpot(0.92f, 0.36f, 0.45f, ArtColors.matchaLeaf.rgb()),
        MeshSpot(0.2f, 0.82f, 0.55f, ArtColors.matchaMilk.rgb()),
        MeshSpot(0.88f, 0.92f, 0.5f, ArtColors.matchaLeaf.rgb()),
        MeshSpot(0.5f, 0.56f, 0.3f, ArtColors.matchaMilk.rgb()),
    ),
    SEED + 95,
)

/** Sakura: cielo rosa con rubor durazno, rosa y lila; la luna y los pétalos van encima. */
internal fun sakuraArt() = mesh(
    ArtColors.sakuraSkyTop.rgb(),
    arrayOf(
        MeshSpot(0.1f, 0.12f, 0.45f, ArtColors.sakuraPeach.rgb()),
        MeshSpot(0.9f, 0.5f, 0.55f, ArtColors.sakuraRose.rgb()),
        MeshSpot(0.2f, 0.95f, 0.6f, ArtColors.sakuraLilac.rgb()),
        MeshSpot(0.7f, 1.05f, 0.5f, ArtColors.sakuraSkyBottom.rgb()),
    ),
    SEED + 99,
)

/**
 * Atardecer lofi: cielo de crema a durazno y rosa, un sol grande y difuso, nubes en hilos
 * horizontales y una ciudad lejana en dos capas, con algunas ventanas encendidas.
 */
internal fun sunsetArt(aspect: Float): PixelArt {
    val top = ArtColors.sunsetTop.rgb()
    val mid = ArtColors.sunsetMid.rgb()
    val low = ArtColors.sunsetLow.rgb()
    val sun = ArtColors.sunsetSun.rgb()
    val cloud = ArtColors.sunsetCloud.rgb()
    val city = City(ArtColors.sunsetCityFar.rgb(), ArtColors.sunsetCityNear.rgb(), ArtColors.sunsetWindow.rgb())
    val sunY = aspect * 0.8f
    return PixelArt { x, y, v, out ->
        out.set(top)
        out.mix(mid, smooth(0.15f, 0.55f, v))
        out.mix(low, smooth(0.55f, 0.95f, v))
        val d = sqrt(sq(x - 0.5f) + sq(y - sunY)) / SUN_RADIUS
        out.add(sun, (1f - smooth(1f, 3f, d)) * 0.12f)
        out.mix(sun, (1f - smooth(0.97f, 1.03f, d)) * 0.85f)
        val streak = smooth(0.55f, 0.78f, Noise.fbm(x * 2.5f, v * 24f, SEED + 110, 4))
        out.mix(cloud, streak * smooth(0.3f, 0.5f, v) * (1f - smooth(0.66f, 0.76f, v)) * 0.6f)
        skyline(x, v, out, city)
    }
}

/** Colores de la ciudad del atardecer. */
private class City(val far: Rgb, val near: Rgb, val window: Rgb)

/** La ciudad del atardecer: edificios lejanos claros y cercanos un poco más oscuros, con ventanas. */
private fun skyline(x: Float, v: Float, out: Pixel, city: City) {
    val far = floor(x * 24f).toInt()
    if (1f - v < 0.08f + 0.1f * sq(Noise.hash(far, 0, SEED + 120))) out.mix(city.far, 0.8f)
    val nx = x * 13f + 0.37f
    val near = floor(nx).toInt()
    if (1f - v < 0.035f + 0.07f * Noise.hash(near, 1, SEED + 121)) {
        out.mix(city.near, 0.9f)
        val wx = floor(fract(nx) * 6f).toInt()
        val wy = floor(v * 90f).toInt()
        val inside = fract(fract(nx) * 6f) in 0.3f..0.75f && fract(v * 90f) in 0.3f..0.75f
        if (inside && Noise.hash(near * 7 + wx, wy, SEED + 122) > 0.8f) out.mix(city.window, 0.7f)
    }
}

/**
 * Bosque con niebla: cuatro filas de colinas con copas de pino (borde dentado), de la más lejana
 * y clara a la más cercana y oscura, con niebla que se junta en el fondo de cada valle.
 */
internal fun forestArt(): PixelArt {
    val top = ArtColors.forestTop.rgb()
    val mist = ArtColors.forestMist.rgb()
    val layers = arrayOf(
        ArtColors.forestRidge1.rgb(),
        ArtColors.forestRidge2.rgb(),
        ArtColors.forestRidge3.rgb(),
        ArtColors.forestRidge4.rgb(),
    )
    return PixelArt { x, y, v, out ->
        out.set(top)
        out.mix(mist, smooth(0.2f, 0.62f, v) * 0.7f)
        out.scale(0.95f + Noise.fbm(x * 2f, y * 4f, SEED + 130, 4) * 0.1f)
        for (i in layers.indices) {
            val ridgeTop = 0.54f + i * 0.105f - 0.13f * Noise.fbm(x * (1.6f + i * 0.6f), i * 7f, SEED + 131, 4) -
                0.012f * Noise.value(x * (70f + i * 30f), i * 3f, SEED + 132)
            if (v > ridgeTop) {
                out.set(layers[i])
                out.mix(mist, smooth(ridgeTop, ridgeTop + 0.14f, v) * (0.4f - i * 0.08f))
            }
        }
    }
}

/** Café: marrón tostado, una luz cálida de ventana arriba a la derecha y crema que gira muy tenue. */
internal fun coffeeArt(aspect: Float): PixelArt {
    val top = ArtColors.coffeeTop.rgb()
    val bottom = ArtColors.coffeeBottom.rgb()
    val glow = ArtColors.coffeeGlow.rgb()
    val crema = ArtColors.coffeeCrema.rgb()
    return PixelArt { x, y, v, out ->
        out.set(top)
        out.mix(bottom, v)
        out.mix(crema, smooth(0.5f, 0.8f, Noise.warped(x * 1.8f, y * 1.8f, SEED + 140, 3f)) * 0.25f)
        out.add(glow, blob(x, y, 0.85f, aspect * 0.04f, 0.7f) * 0.16f)
        out.scale(1f - smooth(0.3f, 0.7f, sq(x - 0.5f) + sq(v - 0.45f)) * 0.3f)
    }
}

/** Nieve: blanco con un cielo frío arriba y tres lomas de nieve muy suaves abajo. */
internal fun snowArt(): PixelArt {
    val base = ArtColors.snowBase.rgb()
    val sky = ArtColors.snowSky.rgb()
    val shade = ArtColors.snowShade.rgb()
    return PixelArt { x, _, v, out ->
        out.set(base)
        out.mix(sky, (1f - smooth(0f, 0.45f, v)) * 0.7f)
        for (i in 0 until 3) {
            val crest = 0.76f + i * 0.08f - 0.04f * Noise.fbm(x * (1.8f + i), i * 5f, SEED + 150, 4)
            if (v > crest) {
                out.set(base)
                out.mix(shade, (1f - smooth(crest, crest + 0.05f, v)) * 0.8f + 0.1f)
            }
        }
    }
}

private const val SUN_RADIUS = 0.2f
