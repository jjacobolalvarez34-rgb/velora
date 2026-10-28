package app.velora.launcher.core.data.apps

import java.util.Random
import kotlin.math.cos
import kotlin.math.sin

/** Una estrellita del estilo Stellar: posición y radio en fracción del lado; [alpha] 0–1. */
internal data class Star(val x: Float, val y: Float, val radius: Float, val alpha: Float)

/** Una veta del mármol: curva cúbica de [points] (8 valores x/y, fracción del lado). */
internal class Vein(val points: FloatArray, val width: Float, val alpha: Float)

/**
 * Adornos deterministas por app (semilla = hash del paquete): cada ícono tiene siempre el mismo
 * cielo o las mismas vetas, en cualquier tamaño y tras reiniciar. Matemática pura.
 */
internal object IconOrnaments {

    const val MIN_STARS = 6
    const val MAX_STARS = 10

    /** Las estrellas van en un anillo alrededor del glifo, sin taparlo ni salirse de la forma. */
    const val STAR_INNER = 0.27f
    const val STAR_OUTER = 0.43f

    private const val STAR_MIN_RADIUS = 0.009f
    private const val STAR_RADIUS_RANGE = 0.014f
    private const val STAR_MIN_ALPHA = 0.45f
    private const val STAR_ALPHA_RANGE = 0.55f

    private const val MIN_VEINS = 2
    private const val VEIN_CHOICES = 2
    private const val VEIN_EDGE_MARGIN = 0.1f
    private const val VEIN_SPAN = 0.8f
    private const val VEIN_DRIFT = 0.35f
    private const val VEIN_BEND = 0.25f
    private const val VEIN_MIN_WIDTH = 0.012f
    private const val VEIN_WIDTH_RANGE = 0.016f
    private const val VEIN_MIN_ALPHA = 0.16f
    private const val VEIN_ALPHA_RANGE = 0.14f
    private const val THIRD = 1f / 3f
    private const val TWO_THIRDS = 2f / 3f
    private const val HALF = 0.5f

    fun stars(seed: Int): List<Star> {
        val random = Random(seed.toLong())
        val count = MIN_STARS + random.nextInt(MAX_STARS - MIN_STARS + 1)
        return List(count) {
            val angle = random.nextDouble() * 2 * Math.PI
            val distance = STAR_INNER + random.nextFloat() * (STAR_OUTER - STAR_INNER)
            Star(
                x = HALF + (cos(angle) * distance).toFloat(),
                y = HALF + (sin(angle) * distance).toFloat(),
                radius = STAR_MIN_RADIUS + random.nextFloat() * STAR_RADIUS_RANGE,
                alpha = STAR_MIN_ALPHA + random.nextFloat() * STAR_ALPHA_RANGE,
            )
        }
    }

    /** 2–3 vetas de lado a lado, en diagonal suave, cada una con su curva. */
    fun veins(seed: Int): List<Vein> {
        val random = Random(seed.toLong())
        val count = MIN_VEINS + random.nextInt(VEIN_CHOICES)
        return List(count) {
            val startY = VEIN_EDGE_MARGIN + random.nextFloat() * VEIN_SPAN
            val endY = (startY + (random.nextFloat() * 2 - 1) * VEIN_DRIFT).coerceIn(0f, 1f)
            val bend1 = (random.nextFloat() * 2 - 1) * VEIN_BEND
            val bend2 = (random.nextFloat() * 2 - 1) * VEIN_BEND
            val points = floatArrayOf(
                0f,
                startY,
                THIRD,
                startY + (endY - startY) * THIRD + bend1,
                TWO_THIRDS,
                startY + (endY - startY) * TWO_THIRDS + bend2,
                1f,
                endY,
            )
            Vein(
                points = points,
                width = VEIN_MIN_WIDTH + random.nextFloat() * VEIN_WIDTH_RANGE,
                alpha = VEIN_MIN_ALPHA + random.nextFloat() * VEIN_ALPHA_RANGE,
            )
        }
    }
}
