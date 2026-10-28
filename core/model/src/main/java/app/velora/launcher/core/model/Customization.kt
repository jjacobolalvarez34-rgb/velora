package app.velora.launcher.core.model

import androidx.compose.runtime.Immutable

/**
 * Estilo de los íconos de Velora (ADR-028): el mismo set de glifos propios dibujado de muchas
 * maneras. Funciona para todas las apps, tengan o no ícono temático.
 */
enum class IconPack {
    /** Glifo de línea sobre la forma (por defecto): el minimalista de siempre. */
    Line,

    /** Forma llena del color de la app (o del tema) y glifo blanco encima. */
    Material,

    /** Solo un punto del color de la app: lo más mínimo. */
    Dots,

    /** Forma con degradado, borde blanco y sombra: estilo pegatina. */
    Sticker,

    /** Colores vivos y glifo oscuro: alegre. */
    Bright,

    /** Vidrio: forma translúcida con borde fino, para fondos con foto. */
    Glass,

    /** Cielo estrellado dentro de la forma, glifo claro: para fondos oscuros. */
    Stellar,

    /** Mármol blanco con vetas y glifo oscuro. */
    Marble,
}

/** Forma que contiene el ícono (de Velora o el original de la app). */
enum class IconShape { Circle, Squircle, Rounded, Teardrop }

/** De dónde sale el color de cada ícono. */
enum class IconColor {
    /** El acento del tema: todos iguales, muy ordenado. */
    Theme,

    /** El color principal del ícono original de cada app. */
    App,

    /** Blanco y negro según el fondo. */
    Mono,
}

/**
 * Cómo se dibujan los íconos. [weight] es el grosor del trazo en unidades del glifo (24 de
 * ancho); [softFill] pinta un relleno suave dentro del glifo de línea. [thirdPartyPack] es el
 * paquete de un pack de íconos instalado (solo con `IconStyle.Pack`).
 */
@Immutable
data class IconOptions(
    val pack: IconPack = IconPack.Line,
    val shape: IconShape = IconShape.Circle,
    val color: IconColor = IconColor.Theme,
    val weight: Float = DEFAULT_WEIGHT,
    val softFill: Boolean = false,
    val thirdPartyPack: String? = null,
) {
    companion object {
        const val DEFAULT_WEIGHT = 2f
        const val MIN_WEIGHT = 1.25f
        const val MAX_WEIGHT = 3f
    }
}

/** Qué se ve en el inicio ("elementos visibles"). */
@Immutable
data class VisibleElements(
    val clock: Boolean = true,
    val date: Boolean = true,
    val statusBar: Boolean = true,
    /** Consejos de los primeros días (tarjetas que se deslizan para quitar). */
    val tips: Boolean = true,
)

/**
 * Personalización fina (ADR-028), guardada aparte del tema para poder crecer sin migraciones.
 *
 * @property accentColor color del tema elegido por el usuario (ARGB), o null para el del tema.
 * @property dismissedTips consejos del inicio que el usuario ya hizo o quitó.
 * @property usageLimits freno de uso: minutos por día para cada paquete (ADR-024).
 */
@Immutable
data class Customization(
    val icons: IconOptions = IconOptions(),
    val accentColor: Long? = null,
    val visible: VisibleElements = VisibleElements(),
    val dismissedTips: Set<String> = emptySet(),
    val usageLimits: Map<String, Int> = emptyMap(),
)
