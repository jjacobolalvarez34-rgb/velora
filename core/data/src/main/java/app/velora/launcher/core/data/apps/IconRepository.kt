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
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Íconos ya dibujados al tamaño y estilo finales (RENDIMIENTO §3.9) en tres niveles: memoria
 * (`LruCache`), disco ([IconDiskCache]) y, si no están, se dibujan (ARQUITECTURA §4.2).
 * Nunca decodifica ni dibuja en el hilo principal.
 */
class IconRepository(
    context: Context,
    private val scope: CoroutineScope,
    private val infoFor: suspend (AppKey) -> LauncherActivityInfo?,
) {
    private val densityDpi = context.resources.displayMetrics.densityDpi
    private val renderer = IconRenderer(context, densityDpi)

    /** Packs de íconos de terceros (`IconStyle.Pack`); el selector de Ajustes usa el mismo. */
    val packs = IconPackRepository(context)
    private val disk = IconDiskCache(File(context.cacheDir, "icons"))

    private val memory: LruCache<String, ImageBitmap> = run {
        val memoryClassMb = context.getSystemService(ActivityManager::class.java).memoryClass
        val maxBytes = memoryClassMb * BYTES_PER_MB / CACHE_FRACTION
        object : LruCache<String, ImageBitmap>(maxBytes) {
            override fun sizeOf(key: String, value: ImageBitmap) = value.asAndroidBitmap().byteCount
        }
    }

    /** Lectura instantánea para no parpadear al recomponer o al volver a una fila. */
    fun cached(request: IconRequest): ImageBitmap? = memory.get(request.fileName)

    suspend fun load(request: IconRequest): ImageBitmap? =
        cached(request) ?: fromDisk(request) ?: infoFor(request.key)?.let { info -> render(request, info) }

    /**
     * Prepara íconos antes de que se vean (favoritos primero, luego la lista), uno tras otro y
     * fuera del hilo principal, para que al abrir la lista ya estén.
     */
    suspend fun prefetch(requests: List<IconRequest>) {
        for (request in requests) {
            if (cached(request) == null) load(request)
        }
    }

    private suspend fun fromDisk(request: IconRequest): ImageBitmap? = withContext(Dispatchers.IO) {
        runCatching { disk.read(request.fileName)?.asImageBitmap() }.getOrNull()
    }?.also { memory.put(request.fileName, it) }

    /** Los packs de terceros se leen en IO; el dibujo, en Default. */
    private suspend fun render(request: IconRequest, info: LauncherActivityInfo): ImageBitmap? {
        val pack = request.options.thirdPartyPack?.takeIf { request.style == IconStyle.Pack }
        val packIcon = pack?.let { packs.iconFor(it, info.componentName, densityDpi) }
        val bitmap = withContext(Dispatchers.Default) {
            runCatching { renderer.render(info, request, packIcon) }
                .onFailure { Logger.w(TAG, it) { "No se pudo dibujar el ícono de ${request.key.packageName}" } }
                .getOrNull()
        } ?: return null
        withContext(Dispatchers.IO) {
            runCatching { disk.write(request.fileName, bitmap) }
                .onFailure { Logger.w(TAG, it) { "No se guardó el ícono en disco" } }
        }
        return bitmap.asImageBitmap().also { memory.put(request.fileName, it) }
    }

    /** Un paquete se actualizó: su ícono puede haber cambiado. Se puede llamar desde el main thread. */
    fun evictPackage(packageName: String) {
        packs.invalidate(packageName)
        evictFromMemory(packageName)
        scope.launch(Dispatchers.IO) { disk.deletePackage(packageName) }
    }

    /** Tras leer las apps del sistema: descarta los íconos de las que se actualizaron con Velora cerrado. */
    suspend fun syncVersions(versions: Map<String, String>) {
        val changed = withContext(Dispatchers.IO) {
            runCatching { disk.syncVersions(versions) }.getOrDefault(emptySet())
        }
        changed.forEach(::evictFromMemory)
    }

    /** `onTrimMemory`: suelta todo salvo el tamaño de los favoritos (RENDIMIENTO §5). En disco siguen. */
    fun trimExcept(keepSizePx: Int) {
        val marker = "@$keepSizePx."
        memory.snapshot().keys.filterNot { marker in it }.forEach(memory::remove)
    }

    private fun evictFromMemory(packageName: String) {
        val prefix = IconDiskCache.packagePrefix(packageName)
        memory.snapshot().keys.filter { it.startsWith(prefix) }.forEach(memory::remove)
    }

    private companion object {
        const val TAG = "IconRepository"
        const val BYTES_PER_MB = 1024 * 1024
        const val CACHE_FRACTION = 8
    }
}
