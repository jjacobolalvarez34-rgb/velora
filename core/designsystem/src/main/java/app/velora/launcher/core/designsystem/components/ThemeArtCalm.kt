package app.velora.launcher.core.designsystem.components

import androidx.compose.ui.draw.CacheDrawScope
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.dp
import app.velora.launcher.core.designsystem.theme.ArtColors
import kotlin.random.Random

// Detalles vectoriales nítidos que van encima del arte procedural de los temas tranquilos.

/** Sakura: una luna grande con halo y pocos pétalos, sobre el cielo de malla. */
internal fun CacheDrawScope.sakuraOverlay(): DrawScope.() -> Unit {
    val w = size.width
    val h = size.height
    val random = Random(SEED)
    val petals = List(PETALS) {
        Offset(random.nextFloat() * w, h * (0.15f + random.nextFloat() * 0.6f)) to random.nextFloat() * 180f
    }
    val petal = Size(10.dp.toPx(), 6.dp.toPx())
    val moon = Offset(w * 0.78f, h * 0.2f)
    val radius = w * 0.3f
    val halo = Brush.radialGradient(listOf(ArtColors.sakuraMoonHalo, Color.Transparent), moon, radius * 1.6f)
    return {
        drawCircle(halo, radius * 1.6f, moon)
        drawCircle(ArtColors.sakuraMoon, radius = radius, center = moon)
        petals.forEach { (at, angle) -> rotate(angle, pivot = at) { drawOval(ArtColors.sakuraPetal, at, petal) } }
    }
}

/** Aurora: pocas estrellas pequeñas en la mitad de arriba, más tenues cerca de la luz. */
internal fun CacheDrawScope.auroraStars(): DrawScope.() -> Unit {
    val w = size.width
    val h = size.height
    val random = Random(SEED + 3)
    val stars = List(STARS) {
        Triple(
            Offset(random.nextFloat() * w, random.nextFloat() * h * 0.55f),
            (0.4f + random.nextFloat() * 0.8f).dp.toPx(),
            0.2f + random.nextFloat() * 0.6f,
        )
    }
    return {
        stars.forEach { (at, radius, alpha) -> drawCircle(ArtColors.auroraStar.copy(alpha = alpha), radius, at) }
    }
}
