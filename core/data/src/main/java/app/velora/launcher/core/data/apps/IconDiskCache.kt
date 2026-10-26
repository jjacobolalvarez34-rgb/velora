package app.velora.launcher.core.data.apps

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.File

/**
 * Íconos ya dibujados en disco (`cacheDir/icons`, no se respalda): tras reiniciar el teléfono
 * aparecen al instante, sin esperar a `PackageManager` ni volver a dibujarlos (B-020).
 * Solo se llama desde `Dispatchers.IO`.
 */
internal class IconDiskCache(private val dir: File) {

    fun read(name: String): Bitmap? = File(dir, name).takeIf { it.exists() }?.let { BitmapFactory.decodeFile(it.path) }

    /** Escribe en un temporal y lo renombra: un corte a mitad nunca deja un ícono roto. */
    fun write(name: String, bitmap: Bitmap) {
        dir.mkdirs()
        val temp = File(dir, "$name.tmp")
        temp.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, PNG_QUALITY, it) }
        temp.renameTo(File(dir, name))
    }

    fun deletePackage(packageName: String) {
        dir.listFiles { file -> file.name.startsWith(packagePrefix(packageName)) }?.forEach { it.delete() }
    }

    /**
     * Compara la versión de cada app (su `sourceDir`, que cambia en cada actualización) con la
     * de la última vez. Borra los íconos de las apps que cambiaron o ya no están y las devuelve.
     */
    fun syncVersions(current: Map<String, String>): Set<String> {
        val file = File(dir, VERSIONS_FILE)
        val previous = if (file.exists()) {
            file.readLines().filter { '\t' in it }.associate { it.substringBefore('\t') to it.substringAfter('\t') }
        } else {
            emptyMap()
        }
        val changed = previous.filter { (pkg, version) -> current[pkg] != version }.keys
        changed.forEach(::deletePackage)
        if (previous != current) {
            dir.mkdirs()
            file.writeText(current.entries.joinToString("\n") { "${it.key}\t${it.value}" })
        }
        return changed
    }

    companion object {
        private const val VERSIONS_FILE = "versions.tsv"
        private const val PNG_QUALITY = 100

        fun packagePrefix(packageName: String) = "${packageName}__"
    }
}
