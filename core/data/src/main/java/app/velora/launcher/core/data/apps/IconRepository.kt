package app.velora.launcher.core.data.apps

import android.app.ActivityManager
import android.content.Context
import android.content.pm.LauncherActivityInfo
import android.util.LruCache
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import app.velora.launcher.core.data.log.Logger
import app.velora.launcher.core.model.AppKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Íconos ya renderizados al tamaño final (RENDIMIENTO §3.9), en memoria (ARQUITECTURA §4.2).
 * Nunca decodifica en el hilo principal.
 */
class IconRepository(context: Context, private val infoFor: (AppKey) -> LauncherActivityInfo?) {
    private val densityDpi = context.resources.displayMetrics.densityDpi

    private val cache: LruCache<String, ImageBitmap> = run {
        val memoryClassMb = context.getSystemService(ActivityManager::class.java).memoryClass
        val maxBytes = memoryClassMb * BYTES_PER_MB / CACHE_FRACTION
        object : LruCache<String, ImageBitmap>(maxBytes) {
            override fun sizeOf(key: String, value: ImageBitmap) = value.asAndroidBitmap().byteCount
        }
    }

    /** Lectura instantánea para no parpadear al recomponer o al volver a una fila. */
    fun cached(key: AppKey, sizePx: Int): ImageBitmap? = cache.get(cacheKey(key, sizePx))

    suspend fun load(key: AppKey, sizePx: Int): ImageBitmap? =
        cached(key, sizePx) ?: infoFor(key)?.let { info -> render(key, info, sizePx) }

    private suspend fun render(key: AppKey, info: LauncherActivityInfo, sizePx: Int): ImageBitmap? =
        withContext(Dispatchers.Default) {
            runCatching {
                info.getBadgedIcon(densityDpi).toBitmap(sizePx, sizePx).asImageBitmap()
            }.onFailure {
                Logger.w(TAG, it) { "No se pudo renderizar el ícono de ${key.packageName}" }
            }.getOrNull()
        }?.also { cache.put(cacheKey(key, sizePx), it) }

    /** Un paquete se actualizó: su ícono puede haber cambiado. */
    fun evictPackage(packageName: String) {
        cache.snapshot().keys.filter { it.startsWith("$packageName/") }.forEach(cache::remove)
    }

    /** `onTrimMemory`: suelta todo salvo el tamaño de los favoritos (RENDIMIENTO §5). */
    fun trimExcept(keepSizePx: Int) {
        val suffix = "@$keepSizePx"
        cache.snapshot().keys.filterNot { it.endsWith(suffix) }.forEach(cache::remove)
    }

    private fun cacheKey(key: AppKey, sizePx: Int) = "${key.stableId}@$sizePx"

    private companion object {
        const val TAG = "IconRepository"
        const val BYTES_PER_MB = 1024 * 1024
        const val CACHE_FRACTION = 8
    }
}
