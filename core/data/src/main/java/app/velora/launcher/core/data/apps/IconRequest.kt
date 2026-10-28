package app.velora.launcher.core.data.apps

import app.velora.launcher.core.model.AppKey
import app.velora.launcher.core.model.IconOptions
import app.velora.launcher.core.model.IconStyle

/**
 * Qué ícono pedir: la app, el tamaño final en píxeles, el estilo y sus opciones, y los colores
 * del tema en ARGB: [tint] (glifo de línea), [backdrop] (forma de fondo) y [accent].
 */
data class IconRequest(
    val key: AppKey,
    val sizePx: Int,
    val style: IconStyle,
    val tint: Int,
    val backdrop: Int,
    val options: IconOptions = IconOptions(),
    val accent: Int = 0,
) {
    /**
     * Nombre en disco y clave en memoria: empieza por el paquete para poder borrarlo junto, y
     * lleva todo lo que cambia el dibujo para que cambiar una opción lo regenere.
     */
    val fileName: String = IconDiskCache.packagePrefix(key.packageName) +
        "v${CACHE_VERSION}_${key.className.hashCode().toHex()}_${key.userSerial}_${style.name}_" +
        "${variantKey(style, options, tint, backdrop, accent).hashCode().toHex()}@$sizePx.png"

    private companion object {
        /** Se sube cuando cambia cómo se dibuja un estilo: los íconos viejos no se reutilizan. */
        const val CACHE_VERSION = 2
        const val HEX = 16

        fun Int.toHex() = toUInt().toString(HEX)
    }
}

/**
 * Lo que de verdad cambia el dibujo de cada estilo, como texto estable (`hashCode` de un enum
 * cambia entre procesos; su `name` no). Solo lo que importa: cambiar el estilo de los íconos de
 * Velora no invalida los recortes del ícono original.
 */
internal fun variantKey(style: IconStyle, options: IconOptions, tint: Int, backdrop: Int, accent: Int): String =
    when (style) {
        IconStyle.System, IconStyle.None -> ""

        IconStyle.Circle -> options.shape.name

        IconStyle.Pack -> "${options.shape.name}|${options.thirdPartyPack.orEmpty()}"

        IconStyle.Minimal -> listOf(
            options.pack.name,
            options.shape.name,
            options.color.name,
            options.weight.toString(),
            options.softFill.toString(),
            tint.toUInt().toString(),
            backdrop.toUInt().toString(),
            accent.toUInt().toString(),
        ).joinToString("|")
    }
