package app.velora.launcher.core.data.apps

import org.xmlpull.v1.XmlPullParser

/** Una etiqueta del XML con sus atributos: formato intermedio para probar el parseo en la JVM. */
internal data class XmlElement(val name: String, val attributes: Map<String, String>)

/**
 * El `appfilter.xml` de un pack de íconos (formato común ADW/Nova/Lawnchair) ya interpretado.
 *
 * @property components `paquete/clase` → nombre del drawable.
 * @property byPackage primer drawable de cada paquete, por si la actividad cambió de clase.
 * @property backs fondos (`iconback`) para componer las apps que el pack no cubre.
 * @property scale tamaño del ícono original sobre el fondo compuesto (0–1).
 */
internal data class AppFilter(
    val components: Map<String, String> = emptyMap(),
    val byPackage: Map<String, String> = emptyMap(),
    val backs: List<String> = emptyList(),
    val mask: String? = null,
    val upon: String? = null,
    val scale: Float = DEFAULT_SCALE,
) {
    /** Si el pack trae piezas para componer un ícono de una app que no cubre. */
    val canCompose: Boolean get() = backs.isNotEmpty() || mask != null || upon != null

    fun drawableFor(packageName: String, className: String): String? =
        components["$packageName/$className"] ?: byPackage[packageName]

    /** El fondo de una app: siempre el mismo para la misma app si el pack trae varios. */
    fun backFor(packageName: String): String? =
        backs.takeIf { it.isNotEmpty() }?.let { it[Math.floorMod(packageName.hashCode(), it.size)] }

    companion object {
        const val DEFAULT_SCALE = 1f
        private const val MIN_SCALE = 0.1f

        /** Interpreta las etiquetas; ignora en silencio lo que no entiende (los packs son de terceros). */
        fun from(elements: Iterable<XmlElement>): AppFilter {
            val components = HashMap<String, String>()
            val byPackage = HashMap<String, String>()
            var filter = AppFilter()
            for (element in elements) {
                val attrs = element.attributes
                when (element.name) {
                    "item" -> {
                        val component = attrs["component"]?.let(::parseComponent)
                        val drawable = attrs["drawable"]?.trim()?.takeIf { it.isNotEmpty() }
                        if (component != null && drawable != null) {
                            components.putIfAbsent("${component.first}/${component.second}", drawable)
                            byPackage.putIfAbsent(component.first, drawable)
                        }
                    }

                    "iconback" -> filter = filter.copy(backs = filter.backs + images(attrs))

                    "iconmask" -> filter = filter.copy(mask = images(attrs).firstOrNull())

                    "iconupon" -> filter = filter.copy(upon = images(attrs).firstOrNull())

                    "scale" -> filter = filter.copy(scale = scaleOf(attrs["factor"]))
                }
            }
            return filter.copy(components = components, byPackage = byPackage)
        }

        /**
         * `ComponentInfo{paquete/clase}` → (paquete, clase). Acepta clases relativas (`.Main`) y
         * espacios; null si no tiene la forma esperada.
         */
        fun parseComponent(raw: String): Pair<String, String>? {
            val inner = raw.trim().removePrefix("ComponentInfo{").removeSuffix("}").trim()
            val packageName = inner.substringBefore('/', "").trim()
            val className = inner.substringAfter('/', "").trim()
            if (packageName.isEmpty() || className.isEmpty() || '/' in className) return null
            return packageName to if (className.startsWith('.')) packageName + className else className
        }

        private fun images(attrs: Map<String, String>): List<String> = attrs.entries
            .filter { it.key.startsWith("img") }
            .sortedBy { it.key }
            .map { it.value.trim() }
            .filter { it.isNotEmpty() }

        private fun scaleOf(raw: String?): Float =
            raw?.trim()?.toFloatOrNull()?.takeIf { it.isFinite() }?.coerceIn(MIN_SCALE, 1f) ?: DEFAULT_SCALE
    }
}

/** Lee las etiquetas de un `XmlPullParser` (recurso `res/xml/appfilter` o `assets/appfilter.xml`). */
internal object AppFilterParser {

    /** Tope de etiquetas: un pack enorme o malicioso no se come la memoria. */
    private const val MAX_ELEMENTS = 60_000

    fun elements(parser: XmlPullParser): List<XmlElement> {
        val out = ArrayList<XmlElement>()
        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT && out.size < MAX_ELEMENTS) {
            if (event == XmlPullParser.START_TAG) {
                val attributes = (0 until parser.attributeCount).associate {
                    parser.getAttributeName(it) to parser.getAttributeValue(it).orEmpty()
                }
                out += XmlElement(parser.name.orEmpty(), attributes)
            }
            event = parser.next()
        }
        return out
    }
}
