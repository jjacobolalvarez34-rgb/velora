package app.velora.launcher.core.data.hidden

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * PIN guardado (ADR-034): solo el hash PBKDF2 con su sal y sus iteraciones, nunca el PIN.
 * Las iteraciones se guardan para poder subirlas en el futuro sin romper los PIN viejos.
 */
data class StoredPin(val hash: ByteArray, val salt: ByteArray, val iterations: Int) {
    override fun equals(other: Any?): Boolean = other is StoredPin &&
        hash.contentEquals(other.hash) && salt.contentEquals(other.salt) && iterations == other.iterations

    override fun hashCode(): Int = (hash.contentHashCode() * HASH + salt.contentHashCode()) * HASH + iterations

    private companion object {
        const val HASH = 31
    }
}

/** Hash y verificación del PIN con `javax.crypto` (sin librerías). Es lento a propósito: fuera del main thread. */
object PinHasher {

    const val MIN_LENGTH = 4
    const val MAX_LENGTH = 8

    /** ~0,1–0,3 s en un teléfono de gama media: caro para probar los 10^8 PIN posibles. */
    const val DEFAULT_ITERATIONS = 120_000

    private const val ALGORITHM = "PBKDF2WithHmacSHA256"
    private const val SALT_BYTES = 16
    private const val KEY_BITS = 256

    private val random = SecureRandom()

    /** Solo dígitos, de 4 a 8. */
    fun isValid(pin: String): Boolean = pin.length in MIN_LENGTH..MAX_LENGTH && pin.all { it in '0'..'9' }

    fun create(pin: String, iterations: Int = DEFAULT_ITERATIONS): StoredPin {
        require(isValid(pin)) { "El PIN debe tener de $MIN_LENGTH a $MAX_LENGTH dígitos" }
        val salt = ByteArray(SALT_BYTES).also(random::nextBytes)
        return StoredPin(derive(pin, salt, iterations), salt, iterations)
    }

    /** Comparación en tiempo constante, para no filtrar cuántos bytes coinciden. */
    fun matches(pin: String, stored: StoredPin): Boolean =
        isValid(pin) && MessageDigest.isEqual(derive(pin, stored.salt, stored.iterations), stored.hash)

    private fun derive(pin: String, salt: ByteArray, iterations: Int): ByteArray {
        val chars = pin.toCharArray()
        val spec = PBEKeySpec(chars, salt, iterations, KEY_BITS)
        return try {
            SecretKeyFactory.getInstance(ALGORITHM).generateSecret(spec).encoded
        } finally {
            spec.clearPassword()
            chars.fill('\u0000')
        }
    }

    /** `iteraciones:sal:hash` en Base64, para guardarlo como un solo texto. */
    fun encode(stored: StoredPin): String {
        val b64 = Base64.getEncoder()
        return "${stored.iterations}:${b64.encodeToString(stored.salt)}:${b64.encodeToString(stored.hash)}"
    }

    /** null si el texto está dañado: se trata como "sin PIN" solo si no había nada guardado. */
    fun decode(text: String?): StoredPin? {
        val parts = text?.split(':')?.takeIf { it.size == PARTS } ?: return null
        val iterations = parts[0].toIntOrNull()?.takeIf { it > 0 } ?: return null
        return runCatching {
            StoredPin(Base64.getDecoder().decode(parts[2]), Base64.getDecoder().decode(parts[1]), iterations)
        }.getOrNull()
    }

    private const val PARTS = 3
}

/** Resultado de probar un PIN. */
sealed interface PinCheck {
    data object Correct : PinCheck

    /** @property attemptsLeft intentos antes de la primera espera (0 si ya se está esperando). */
    data class Wrong(val attemptsLeft: Int, val waitMs: Long) : PinCheck

    /** Demasiados intentos: no se prueba nada hasta que pase la espera. */
    data class Wait(val remainingMs: Long) : PinCheck
}

/** Intentos fallidos y hasta cuándo hay que esperar (epoch ms). */
data class PinAttempts(val failures: Int = 0, val lockedUntilMs: Long = 0L)

/**
 * Reglas de los intentos (ADR-034), sin Android para poder probarlas: 5 intentos libres; luego
 * cada fallo obliga a esperar 30 s, 1 min, 2 min… hasta un máximo de 30 min.
 */
object PinPolicy {

    const val FREE_ATTEMPTS = 5
    const val BASE_WAIT_MS = 30_000L
    const val MAX_WAIT_MS = 30 * 60_000L

    /** Espera tras [failures] fallos seguidos. */
    fun waitAfter(failures: Int): Long {
        if (failures < FREE_ATTEMPTS) return 0L
        val doublings = (failures - FREE_ATTEMPTS).coerceAtMost(MAX_DOUBLINGS)
        return (BASE_WAIT_MS shl doublings).coerceAtMost(MAX_WAIT_MS)
    }

    /**
     * Cuánto falta para poder probar. Si el reloj del teléfono se atrasó, la espera nunca pasa
     * del máximo (no se puede quedar bloqueado para siempre).
     */
    fun remaining(attempts: PinAttempts, nowMs: Long): Long = (attempts.lockedUntilMs - nowMs).coerceIn(0L, MAX_WAIT_MS)

    /** Prueba [pin] y devuelve el resultado con el nuevo contador. */
    fun check(stored: StoredPin, attempts: PinAttempts, pin: String, nowMs: Long): Pair<PinCheck, PinAttempts> {
        val remaining = remaining(attempts, nowMs)
        if (remaining > 0) return PinCheck.Wait(remaining) to attempts
        return if (PinHasher.matches(pin, stored)) PinCheck.Correct to PinAttempts() else failed(attempts, nowMs)
    }

    private fun failed(attempts: PinAttempts, nowMs: Long): Pair<PinCheck, PinAttempts> {
        val failures = attempts.failures + 1
        val wait = waitAfter(failures)
        val next = PinAttempts(failures, if (wait > 0) nowMs + wait else 0L)
        return PinCheck.Wrong((FREE_ATTEMPTS - failures).coerceAtLeast(0), wait) to next
    }

    /** 30 s · 2^6 = 32 min: con 6 duplicaciones ya se llega al máximo. */
    private const val MAX_DOUBLINGS = 6
}
