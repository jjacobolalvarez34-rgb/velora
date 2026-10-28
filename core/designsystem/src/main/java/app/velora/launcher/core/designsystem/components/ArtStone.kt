package app.velora.launcher.core.designsystem.components

import app.velora.launcher.core.designsystem.theme.ArtColors
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.sin

// Arte procedural sobrio: mármoles, grafito, noche y papel. Mucho aire y textura fina.

/**
 * Cuánto hay que calmar el dibujo detrás del texto: 1 en la franja del reloj (10–30 % de la
 * altura) y en la zona de favoritos (abajo a la izquierda), 0 en el resto.
 */
internal fun quiet(x: Float, v: Float): Float {
    val clock = smooth(0.04f, 0.12f, v) * (1f - smooth(0.3f, 0.4f, v))
    val list = smooth(0.5f, 0.62f, v) * (1f - smooth(0.55f, 0.85f, x))
    return max(clock, list)
}

/** Una veta: 1 justo donde `sin(t)` cruza el cero, con grosor [width]. */
private fun vein(t: Float, width: Float) = 1f - smooth(0f, width, abs(sin(t)))

/**
 * Mármol blanco: nubes grises muy suaves, una veta principal que serpentea en diagonal (con un
 * halo apenas más oscuro) y vetas finas que la cruzan; todo más tenue detrás del texto.
 */
internal fun marbleArt(): PixelArt {
    val base = ArtColors.marbleBase.rgb()
    val cloud = ArtColors.marbleCloud.rgb()
    val main = ArtColors.marbleVein.rgb()
    val fine = ArtColors.marbleFine.rgb()
    return PixelArt { x, y, v, out ->
        val warp = Noise.warped(x * 0.9f, y * 0.9f, SEED + 20, 2f)
        val calm = 1f - quiet(x, v) * 0.55f
        out.set(base)
        out.mix(cloud, smooth(0.42f, 0.78f, warp) * 0.75f)
        val t = (x * 0.9f + y * 0.55f) * 3.2f + warp * 2.2f
        out.mix(cloud, (1f - smooth(0f, 0.3f, abs(sin(t)))) * 0.35f * calm)
        out.mix(main, vein(t, 0.02f + 0.05f * warp) * 0.75f * calm)
        val fineWarp = Noise.warped(x * 3f, y * 3f, SEED + 21, 1.8f)
        out.mix(fine, vein((x * 0.4f - y * 0.9f) * 6f + fineWarp * 4f, 0.03f) * 0.3f * calm)
    }
}

/**
 * Mármol negro: humo gris carbón sobre casi negro, pocas vetas doradas finas (aparecen y se
 * cortan como en la piedra real, con destellos) y finísimas líneas claras.
 */
internal fun blackMarbleArt(): PixelArt {
    val base = ArtColors.blackMarbleBase.rgb()
    val cloud = ArtColors.blackMarbleCloud.rgb()
    val smoke = ArtColors.blackMarbleSmoke.rgb()
    val gold = ArtColors.blackMarbleGold.rgb()
    val glint = ArtColors.blackMarbleGlint.rgb()
    val hair = ArtColors.blackMarbleHair.rgb()
    return PixelArt { x, y, v, out ->
        val warp = Noise.warped(x * 0.9f, y * 0.9f, SEED + 50, 2f)
        val calm = 1f - quiet(x, v) * 0.6f
        out.set(base)
        out.mix(cloud, smooth(0.4f, 0.75f, warp) * 0.8f)
        out.mix(smoke, smooth(0.62f, 0.85f, Noise.warped(x * 3f, y * 3f, SEED + 51, 1.5f)) * 0.5f)
        val shown = smooth(0.3f, 0.55f, Noise.fbm(x * 1.3f, y * 1.3f, SEED + 52, 3))
        val gv = vein((x * 0.7f + y * 0.6f) * 3f + warp * 2.2f, 0.02f + 0.03f * warp) * shown * calm
        out.mix(gold, gv * 0.85f)
        out.add(glint, gv * smooth(0.6f, 0.8f, Noise.fbm(x * 30f, y * 30f, SEED + 53, 3)) * 0.35f)
        out.mix(hair, vein((x * 0.5f - y * 0.8f) * 6f + warp * 4f, 0.015f) * 0.3f * calm)
    }
}

/** Grafito: gris carbón que oscurece hacia abajo, una luz suave arriba a la derecha y viñeta. */
internal fun graphiteArt(aspect: Float): PixelArt {
    val top = ArtColors.graphiteTop.rgb()
    val bottom = ArtColors.graphiteBottom.rgb()
    val light = ArtColors.graphiteLight.rgb()
    return PixelArt { x, y, v, out ->
        out.set(top)
        out.mix(bottom, smooth(0f, 1f, v))
        out.add(light, blob(x, y, 0.95f, aspect * 0.02f, 0.75f) * 0.35f)
        out.scale(0.97f + Noise.fbm(x * 1.5f, y * 1.5f, SEED + 60, 4) * 0.06f)
        out.scale(1f - smooth(0.35f, 0.75f, abs(x - 0.5f) + abs(v - 0.5f) * 0.6f) * 0.25f)
    }
}

/** Noche: casi negro, un brillo frío muy tenue arriba a la izquierda y bruma apenas visible. */
internal fun nightArt(aspect: Float): PixelArt {
    val base = ArtColors.nightBase.rgb()
    val haze = ArtColors.nightHaze.rgb()
    val glow = ArtColors.nightGlow.rgb()
    return PixelArt { x, y, _, out ->
        out.set(base)
        out.mix(haze, Noise.warped(x * 1.3f, y * 1.3f, SEED + 70, 1.8f) * 0.6f)
        out.add(glow, blob(x, y, 0.15f, aspect * 0.05f, 0.9f) * 0.4f)
    }
}

/**
 * Papel de algodón: crema con manchas cálidas casi invisibles, fibras finas y curvas (ruido
 * "crestado") claras y oscuras, diente fino y bordes un poco más tostados.
 */
internal fun paperArt(): PixelArt {
    val base = ArtColors.paperBase.rgb()
    val warm = ArtColors.paperWarm.rgb()
    val fiber = ArtColors.paperFiber.rgb()
    val bright = ArtColors.paperFiberLight.rgb()
    val edge = ArtColors.paperEdge.rgb()
    return PixelArt { x, y, v, out ->
        out.set(base)
        out.mix(warm, smooth(0.45f, 0.8f, Noise.fbm(x * 3f, y * 3f, SEED + 80, 4)) * 0.6f)
        val wx = Noise.fbm(x * 6f, y * 6f, SEED + 81, 3) * 6f
        val wy = Noise.fbm(x * 6f + 3f, y * 6f, SEED + 82, 3) * 6f
        out.mix(fiber, ridge(Noise.value(x * 22f + wx, y * 22f + wy, SEED + 83)) * 0.2f)
        out.mix(bright, ridge(Noise.value(x * 30f - wy, y * 30f + wx, SEED + 84)) * 0.3f)
        out.scale(0.985f + Noise.value(x * 160f, y * 160f, SEED + 85) * 0.03f)
        out.mix(edge, smooth(0.38f, 0.5f, max(abs(x - 0.5f), abs(v - 0.5f))) * 0.5f)
    }
}

/** Ruido crestado: líneas finas donde el ruido pasa por la mitad (fibras, hilos). */
private fun ridge(n: Float): Float {
    val r = 1f - abs(2f * n - 1f)
    val r2 = r * r
    val r4 = r2 * r2
    return r4 * r4 * r4
}
