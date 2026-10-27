package app.velora.launcher.core.data.contacts

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.ContactsContract
import androidx.compose.runtime.Immutable
import androidx.core.content.ContextCompat
import app.velora.launcher.core.data.log.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Un contacto encontrado en el buscador. Solo el nombre y su dirección para abrirlo. */
@Immutable
data class ContactResult(val id: Long, val name: String, val uri: Uri)

/**
 * Contactos en el buscador (ADR-025). Opt-in con `READ_CONTACTS`; la consulta usa el filtro del
 * propio proveedor de contactos, solo el nombre, y nada se guarda ni sale del teléfono.
 */
class ContactsRepository(context: Context) {

    private val appContext = context.applicationContext

    fun hasPermission(): Boolean = ContextCompat.checkSelfPermission(appContext, Manifest.permission.READ_CONTACTS) ==
        PackageManager.PERMISSION_GRANTED

    suspend fun search(query: String): List<ContactResult> = withContext(Dispatchers.IO) {
        if (query.isBlank() || !hasPermission()) return@withContext emptyList()
        val uri = Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_FILTER_URI, Uri.encode(query.trim()))
        val projection = arrayOf(ContactsContract.Contacts._ID, ContactsContract.Contacts.DISPLAY_NAME_PRIMARY)
        runCatching {
            appContext.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
                buildList {
                    while (cursor.moveToNext() && size < MAX_RESULTS) {
                        val id = cursor.getLong(0)
                        val name = cursor.getString(1) ?: continue
                        add(
                            ContactResult(
                                id,
                                name,
                                ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, id),
                            ),
                        )
                    }
                }
            }.orEmpty()
        }.onFailure { Logger.w(TAG, it) { "Búsqueda de contactos" } }.getOrDefault(emptyList())
    }

    private companion object {
        const val TAG = "ContactsRepository"
        const val MAX_RESULTS = 4
    }
}
