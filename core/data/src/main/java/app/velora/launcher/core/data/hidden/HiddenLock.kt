package app.velora.launcher.core.data.hidden

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * Archivo propio, aparte de las preferencias: se excluye del respaldo de Android y del archivo
 * `.velora-backup` (`backup_rules.xml`, `data_extraction_rules.xml`). Solo hash, sal y contador.
 */
private val Context.veloraLock: DataStore<Preferences> by preferencesDataStore(name = "velora_lock")

/**
 * PIN opcional de las apps ocultas (ADR-034). Se guarda hasheado con PBKDF2 ([PinHasher]) y
 * cuenta los intentos fallidos con espera creciente ([PinPolicy]). El hash se calcula en
 * `Dispatchers.Default`: nunca en el hilo principal.
 */
class HiddenLock(context: Context, private val now: () -> Long = System::currentTimeMillis) {

    private val store = context.applicationContext.veloraLock
    private val mutex = Mutex()

    /** true si el usuario puso un PIN. */
    val hasPin: Flow<Boolean> = store.data.map { it[PIN] != null }.distinctUntilChanged()

    /** Pone o cambia el PIN; false si no tiene de 4 a 8 dígitos. Reinicia los intentos. */
    suspend fun setPin(pin: String): Boolean {
        if (!PinHasher.isValid(pin)) return false
        val stored = withContext(Dispatchers.Default) { PinHasher.create(pin) }
        mutex.withLock {
            store.edit {
                it[PIN] = PinHasher.encode(stored)
                it.remove(FAILURES)
                it.remove(LOCKED_UNTIL)
            }
        }
        return true
    }

    /** Quita el PIN: las ocultas vuelven a verse sin pedir nada. */
    suspend fun clearPin() {
        mutex.withLock { store.edit { it.clear() } }
    }

    /** Prueba un PIN. Sin PIN guardado siempre es correcto. */
    suspend fun check(pin: String): PinCheck = mutex.withLock {
        val prefs = store.data.first()
        val stored = PinHasher.decode(prefs[PIN]) ?: return@withLock PinCheck.Correct
        val attempts = PinAttempts(prefs[FAILURES] ?: 0, prefs[LOCKED_UNTIL] ?: 0L)
        val (result, next) = withContext(Dispatchers.Default) { PinPolicy.check(stored, attempts, pin, now()) }
        if (next != attempts) {
            store.edit {
                it[FAILURES] = next.failures
                it[LOCKED_UNTIL] = next.lockedUntilMs
            }
        }
        result
    }

    /** Cuánto falta para poder volver a probar (0 si se puede ya). */
    suspend fun remainingWait(): Long {
        val prefs = store.data.first()
        return PinPolicy.remaining(PinAttempts(prefs[FAILURES] ?: 0, prefs[LOCKED_UNTIL] ?: 0L), now())
    }

    private companion object {
        val PIN = stringPreferencesKey("hidden_pin")
        val FAILURES = intPreferencesKey("hidden_pin_failures")
        val LOCKED_UNTIL = longPreferencesKey("hidden_pin_locked_until")
    }
}
