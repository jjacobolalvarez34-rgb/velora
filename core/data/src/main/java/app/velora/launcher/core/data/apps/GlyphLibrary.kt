package app.velora.launcher.core.data.apps

import android.content.Context
import android.graphics.Path
import androidx.core.graphics.PathParser
import app.velora.launcher.core.data.log.Logger
import java.util.concurrent.ConcurrentHashMap

/**
 * Trazos en línea de Tabler Icons (MIT) empaquetados en `assets/glyphs/tabler.tsv` (ADR-021).
 * Se leen la primera vez que se dibuja un ícono minimalista, siempre fuera del hilo principal.
 */
internal class GlyphLibrary(private val context: Context) {

    private val raw: Map<String, List<String>> by lazy {
        runCatching {
            context.assets.open(ASSET).bufferedReader().useLines { lines ->
                lines.filter { '\t' in it }
                    .associate { it.substringBefore('\t') to it.substringAfter('\t').split('|') }
            }
        }.onFailure { Logger.w(TAG, it) { "No se pudieron leer los íconos en línea" } }
            .getOrDefault(emptyMap())
    }

    private val parsed = ConcurrentHashMap<String, List<Path>>()

    /** Trazos del ícono en un lienzo de 24×24, o null si no existe. */
    fun paths(name: String): List<Path>? = parsed[name] ?: raw[name]?.let { data ->
        runCatching { data.map(PathParser::createPathFromPathData) }.getOrNull()?.also { parsed[name] = it }
    }

    private companion object {
        const val TAG = "GlyphLibrary"
        const val ASSET = "glyphs/tabler.tsv"
    }
}
