package app.velora.launcher.core.data.backup

import app.velora.launcher.core.data.favorites.FolderSnapshot
import app.velora.launcher.core.data.prefs.CustomizationCodec
import app.velora.launcher.core.model.AppKey
import java.util.Base64

/**
 * Archivo de respaldo de Velora (`.velora-backup`, ADR-035): texto plano, una clave por línea,
 * como `.velora-theme`. Al leer se valida todo: encabezado y versión, tamaño máximo, solo claves
 * conocidas (lo demás se ignora), nombres de paquete y de clase con expresión regular, y topes
 * por lista. Nada de lo que trae se ejecuta: son nombres de apps y opciones de Velora.
 */
object BackupFile {

    const val HEADER = "velora-backup 1"
    const val MIME = "text/plain"
    const val EXTENSION = "velora-backup"

    /** Con la foto reducida no llega ni a 1 MB; más que esto no es un respaldo de Velora. */
    const val MAX_BYTES = 8 * 1024 * 1024

    /** Ninguna lista real llega a esto: protege la memoria ante un archivo armado a mano. */
    const val MAX_ITEMS = 1_000

    internal const val FAVORITE = "favorite"
    internal const val HIDDEN = "hidden"
    internal const val FOLDER = "folder"
    internal const val FOLDER_APP = "folder.app"
    internal const val WIDGET = "widget"
    internal const val PHOTO = "photo"
    internal const val PREF = "pref."

    fun encode(backup: VeloraBackup): String = buildString {
        appendLine(HEADER)
        backup.favorites.forEach { appendLine("$FAVORITE=${BackupText.key(it)}") }
        backup.hidden.forEach { appendLine("$HIDDEN=${BackupText.key(it)}") }
        backup.folders.forEachIndexed { index, folder ->
            appendLine("$FOLDER=$index:${BackupText.name(folder.name)}")
            folder.apps.forEach { appendLine("$FOLDER_APP=$index:${BackupText.key(it)}") }
        }
        backup.widgets.forEach { appendLine("$WIDGET=${it.provider}|${it.heightDp}") }
        append(BackupText.prefs(backup.prefs))
        backup.photo?.let { appendLine("$PHOTO=${Base64.getEncoder().encodeToString(it)}") }
    }

    /** null si no es un respaldo de Velora (encabezado distinto, otra versión o demasiado grande). */
    fun decode(text: String): VeloraBackup? {
        if (text.length > MAX_BYTES * 2) return null
        val lines = text.lineSequence().map { it.trim() }.filter { it.isNotEmpty() }.iterator()
        if (!lines.hasNext() || lines.next() != HEADER) return null
        val reader = BackupReader()
        lines.forEach { line ->
            val split = line.indexOf('=')
            if (split > 0) reader.read(line.substring(0, split), line.substring(split + 1))
        }
        return reader.build()
    }
}

/** Lee línea a línea; cada valor inválido se descarta sin tirar el resto del archivo. */
private class BackupReader {
    private val favorites = mutableListOf<AppKey>()
    private val hidden = mutableListOf<AppKey>()
    private val folderNames = sortedMapOf<Int, String>()
    private val folderApps = mutableMapOf<Int, MutableList<AppKey>>()
    private val widgets = mutableListOf<BackupWidget>()
    private val prefs = mutableMapOf<String, String>()
    private val custom = mutableMapOf<String, String>()
    private var photo: ByteArray? = null

    fun read(key: String, value: String) {
        when {
            key == BackupFile.FAVORITE -> BackupText.parseKey(value)?.let { favorites.addCapped(it) }
            key == BackupFile.HIDDEN -> BackupText.parseKey(value)?.let { hidden.addCapped(it) }
            key == BackupFile.FOLDER -> readFolder(value)
            key == BackupFile.FOLDER_APP -> readFolderApp(value)
            key == BackupFile.WIDGET -> BackupText.parseWidget(value)?.let { widgets.addCapped(it) }
            key == BackupFile.PHOTO -> photo = runCatching { Base64.getDecoder().decode(value) }.getOrNull()
            key.startsWith(BackupFile.PREF) -> prefs[key.removePrefix(BackupFile.PREF)] = value
            BackupText.isCustomizationKey(key) -> custom[key] = value
            // Cualquier otra clave (de una versión futura o inventada) se ignora.
        }
    }

    private fun readFolder(value: String) {
        val (index, rest) = BackupText.indexed(value) ?: return
        if (folderNames.size >= BackupFile.MAX_ITEMS) return
        BackupText.parseName(rest)?.let { folderNames[index] = it }
    }

    private fun readFolderApp(value: String) {
        val (index, rest) = BackupText.indexed(value) ?: return
        val key = BackupText.parseKey(rest) ?: return
        folderApps.getOrPut(index) { mutableListOf() }.addCapped(key)
    }

    private fun <T> MutableList<T>.addCapped(item: T) {
        if (size < BackupFile.MAX_ITEMS) add(item)
    }

    fun build() = VeloraBackup(
        favorites = favorites.distinctBy { it.stableId },
        folders = folderNames.map { (index, name) ->
            FolderSnapshot(name, folderApps[index].orEmpty().distinctBy { it.stableId })
        },
        hidden = hidden.distinctBy { it.stableId },
        widgets = widgets,
        prefs = BackupText.parsePrefs(prefs, custom.takeIf { it.isNotEmpty() }?.let(CustomizationCodec::fromValues)),
        photo = photo,
    )
}
