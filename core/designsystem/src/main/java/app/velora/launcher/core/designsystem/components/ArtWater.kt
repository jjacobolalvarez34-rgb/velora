package app.velora.launcher.core.designsystem.components

import app.velora.launcher.core.designsystem.theme.ArtColors
import kotlin.math.exp
import kotlin.math.floor
import kotlin.math.sin

// Arte procedural de agua y cielo: Océano, Aurora, Galaxia y el cielo de Neón.

/**
 * Océano visto desde arriba: agua profunda arriba a la izquierda (tranquila, detrás del reloj)
 * que se aclara hacia una orilla imaginaria abajo a la derecha, con frentes de ola de espuma
 * deshilachada y un brillo aguamarina bajo cada cresta. La espuma se apaga a la izquierda, donde
 * van los favoritos.
 */
internal fun oceanArt(): PixelArt {
    val deep = ArtColors.oceanDeep.rgb()
    val mid = ArtColors.oceanMid.rgb()
    val shallow = ArtColors.oceanShallow.rgb()
    val glow = ArtColors.oceanGlow.rgb()
    val foam = ArtColors.oceanFoam.rgb()
    return PixelArt { x, y, v, out ->
        val warp = Noise.warped(x * 2.2f, y * 2.2f, SEED, 1.6f)
        val depth = v * 0.85f + x * 0.25f + (warp - 0.5f) * 0.2f
        out.set(deep)
        out.mix(mid, smooth(0.12f, 0.7f, depth))
        out.mix(shallow, smooth(0.78f, 1.12f, depth))
        // Luz que tiembla bajo el agua.
        out.scale(0.93f + Noise.fbm(x * 9f + warp * 2f, y * 9f, SEED + 3, 4) * 0.14f)
        // Frentes de ola paralelos a la orilla: borde nítido del lado de la costa y estela que se deshace.
        val phase = 1f - fract(depth * 6f + (warp - 0.5f) * 0.6f)
        val crest = maxOf(smooth(0.9f, 1f, phase), exp(-phase * 4f))
        val lace = Noise.warped(x * 14f, y * 14f, SEED + 7, 0.8f, 4)
        val zone = smooth(0.5f, 0.72f, depth) * (0.18f + 0.82f * smooth(0.1f, 0.75f, x))
        out.add(glow, crest * zone * 0.16f)
        out.mix(foam, smooth(0.42f, 0.75f, crest * (0.35f + lace)) * zone * 0.92f)
    }
}

/**
 * Aurora: noche azul con dos cortinas de luz onduladas en el centro (borde inferior verde y
 * nítido, que sube difusa hacia el violeta) y rayos verticales finos. Arriba queda oscuro.
 */
internal fun auroraArt(): PixelArt {
    val top = ArtColors.auroraTop.rgb()
    val bottom = ArtColors.auroraBottom.rgb()
    val green = ArtColors.auroraGreen.rgb()
    val teal = ArtColors.auroraTeal.rgb()
    val violet = ArtColors.auroraViolet.rgb()
    return PixelArt { x, _, v, out ->
        out.set(top)
        out.mix(bottom, v)
        val main = curtain(x, v, 0.5f, SEED + 11)
        val far = curtain(x * 1.3f + 0.4f, v, 0.64f, SEED + 17)
        out.add(green, main.edge * 0.55f + far.edge * 0.25f)
        out.add(teal, main.edge * 0.15f)
        out.add(violet, main.veil * 0.32f + far.veil * 0.18f)
    }
}

/** Una cortina de aurora: [edge] es el borde brillante de abajo y [veil] la luz que sube. */
private class Curtain(val edge: Float, val veil: Float)

private fun curtain(x: Float, v: Float, line: Float, seed: Int): Curtain {
    val center = line + 0.04f * sin(x * 4f + seed) + (Noise.fbm(x * 1.6f, seed * 0.37f, seed, 3) - 0.5f) * 0.12f
    val d = v - center
    val rays = 0.45f + 0.55f * Noise.fbm(x * 34f + d * 4f, d * 1.5f, seed + 5, 3)
    val edge = if (d > 0f) exp(-d * d / 0.0006f) else exp(-d * d / 0.006f)
    val veil = if (d > 0f) 0f else exp(-d * d / 0.03f) * smooth(0f, 0.04f, -d)
    return Curtain(edge * rays, veil * rays)
}

/**
 * Galaxia: negro azulado con una franja de nebulosa en diagonal, hecha de humo (fbm deformado)
 * que pasa del magenta al turquesa, con polvo oscuro que la recorta y un núcleo rosado. Las
 * estrellas y el planeta van encima, nítidos ([galaxyOverlay]).
 */
internal fun galaxyArt(): PixelArt {
    val base = ArtColors.galaxyBase.rgb()
    val deep = ArtColors.galaxyDeep.rgb()
    val magenta = ArtColors.galaxyMagenta.rgb()
    val teal = ArtColors.galaxyTeal.rgb()
    val core = ArtColors.galaxyCore.rgb()
    return PixelArt { x, y, v, out ->
        out.set(base)
        out.mix(deep, v)
        val smoke = Noise.warped(x * 1.8f, y * 1.8f, SEED + 30, 2.2f)
        val band = exp(-sq(v - (0.64f - 0.26f * x)) / 0.03f)
        val density = smooth(0.28f, 0.72f, smoke) * band
        val hue = Noise.fbm(x * 1.2f + 3f, y * 1.2f, SEED + 39, 3)
        out.add(magenta, density * 0.7f * (1f - smooth(0.35f, 0.65f, hue)))
        out.add(teal, density * 0.6f * smooth(0.35f, 0.65f, hue))
        val dust = smooth(0.52f, 0.7f, Noise.warped(x * 4f, y * 4f, SEED + 34, 1.2f))
        out.scale(1f - dust * band * 0.55f)
        out.add(core, density * density * density * 0.45f)
    }
}

/** Neón: cielo violeta con nubes muy tenues y bruma magenta en el horizonte; el sol y la rejilla van encima. */
internal fun neonArt(): PixelArt {
    val top = ArtColors.neonTop.rgb()
    val bottom = ArtColors.neonBottom.rgb()
    val haze = ArtColors.neonHaze.rgb()
    return PixelArt { x, y, v, out ->
        out.set(top)
        out.mix(bottom, smooth(0.1f, NEON_HORIZON, v))
        out.scale(0.9f + Noise.fbm(x * 2f, y * 6f, SEED + 41, 4) * 0.2f)
        out.add(haze, exp(-sq(v - NEON_HORIZON) / 0.004f) * 0.22f)
        if (v > NEON_HORIZON) out.mix(top, smooth(NEON_HORIZON, 1f, v) * 0.7f + 0.3f)
    }
}

internal fun fract(t: Float) = t - floor(t)

internal fun sq(t: Float) = t * t

/** Altura del horizonte de Neón: la comparten el cielo procedural y la rejilla dibujada. */
internal const val NEON_HORIZON = 0.68f
