package app.velora.launcher.core.data.apps

import android.app.ActivityManager
import android.content.Context
import android.content.pm.LauncherActivityInfo
import android.util.LruCache
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import app.velora.launcher.core.data.log.Logger
import app.velora.launcher.core.model.AppKey
import app.velora.launcher.core.model.IconStyle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Íconos ya renderizados al tamaño y estilo finales (RENDIMIENTO §3.9), en memoria
 * (ARQUITECTURA §4.2). Nunca decodifica en el hilo principal.
 */
class IconRepository(context: Context, private val infoFor: suspend (AppKey) -> LauncherActivityInfo?) {

    private val renderer = IconRenderer(context, context.resources.displayMetrics.densityDpi)

    private val cache: LruCache<String, ImageBitmap> = run {
        val memoryClassMb = context.getSystemService(ActivityManager::class.java).memoryClass
        val maxBytes = memoryClassMb * BYTES_PER_MB / CACHE_FRACTION
        object : LruCache<String, ImageBitmap>(maxBytes) {
            override fun sizeOf(key: String, value: ImageBitmap) = value.asAndroidBitmap().byteCount
        }
    }

    /** Lectura instantánea para no parpadear al recomponer o al volver a una fila. */
    fun cached(request: IconRequest): ImageBitmap? = cache.get(request.cacheKey)

    suspend fun load(request: IconRequest): ImageBitmap? =
        cached(request) ?: infoFor(request.key)?.let { info -> render(request, info) }

    private suspend fun render(request: IconRequest, info: LauncherActivityInfo): ImageBitmap? =
        withContext(Dispatchers.Default) {
            runCatching {
                renderer.render(info, request.sizePx, request.style, request.tint).asImageBitmap()
            }.onFailure {
                Logger.w(TAG, it) { "No se pudo renderizar el ícono de ${request.key.packageName}" }
            }.getOrNull()
        }?.also { cache.put(request.cacheKey, it) }

    /** Un paquete se actualizó: su ícono puede haber cambiado. */
    fun evictPackage(packageName: String) {
        cache.snapshot().keys.filter { it.startsWith("$packageName/") }.forEach(cache::remove)
    }

    /** `onTrimMemory`: suelta todo salvo el tamaño de los favoritos (RENDIMIENTO §5). */
    fun trimExcept(keepSizePx: Int) {
        val marker = "@$keepSizePx:"
        cache.snapshot().keys.filterNot { marker in it }.forEach(cache::remove)
    }

    private companion object {
        const val TAG = "IconRepository"
        const val BYTES_PER_MB = 1024 * 1024
        const val CACHE_FRACTION = 8
    }
}

/** Qué ícono pedir: la app, el tamaño final en píxeles, el estilo y el color de acento (ARGB). */
data class IconRequest(val key: AppKey, val sizePx: Int, val style: IconStyle, val tint: Int) {
    internal val cacheKey: String = "${key.stableId}@$sizePx:${style.name}:$tint"
}
