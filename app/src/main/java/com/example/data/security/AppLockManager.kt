package com.example.data.security

import android.content.Context
import android.util.Base64
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

private val Context.vaultLockDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "api_manager_lock_prefs"
)

data class StoredLockRecord(
    val mode: VaultLockMode = VaultLockMode.OFF,
    val pinHashBase64: String? = null,
    val pinSaltBase64: String? = null,
    val pinLength: Int = 4
)

interface LockPreferencesStore {
    val recordFlow: Flow<StoredLockRecord>
    suspend fun saveMode(mode: VaultLockMode)
    suspend fun savePinRecord(hashBase64: String, saltBase64: String, pinLength: Int)
    suspend fun clearPinRecord()
}

class DataStoreLockPreferencesStore(
    private val context: Context
) : LockPreferencesStore {

    override val recordFlow: Flow<StoredLockRecord> =
        context.vaultLockDataStore.data.map { prefs ->
            StoredLockRecord(
                mode = VaultLockMode.fromStored(prefs[KEY_LOCK_MODE]),
                pinHashBase64 = prefs[KEY_PIN_HASH],
                pinSaltBase64 = prefs[KEY_PIN_SALT],
                pinLength = (prefs[KEY_PIN_LENGTH] ?: 4).coerceIn(4, 6)
            )
        }

    override suspend fun saveMode(mode: VaultLockMode) {
        context.vaultLockDataStore.edit { prefs ->
            prefs[KEY_LOCK_MODE] = mode.name
        }
    }

    override suspend fun savePinRecord(hashBase64: String, saltBase64: String, pinLength: Int) {
        context.vaultLockDataStore.edit { prefs ->
            prefs[KEY_PIN_HASH] = hashBase64
            prefs[KEY_PIN_SALT] = saltBase64
            prefs[KEY_PIN_LENGTH] = pinLength
        }
    }

    override suspend fun clearPinRecord() {
        context.vaultLockDataStore.edit { prefs ->
            prefs.remove(KEY_PIN_HASH)
            prefs.remove(KEY_PIN_SALT)
            prefs.remove(KEY_PIN_LENGTH)
        }
    }

    private companion object {
        val KEY_LOCK_MODE = stringPreferencesKey("lock_mode")
        val KEY_PIN_HASH = stringPreferencesKey("pin_hash")
        val KEY_PIN_SALT = stringPreferencesKey("pin_salt")
        val KEY_PIN_LENGTH = intPreferencesKey("pin_length")
    }
}

class InMemoryLockPreferencesStore : LockPreferencesStore {
    private val state = MutableStateFlow(StoredLockRecord())
    override val recordFlow: Flow<StoredLockRecord> = state

    override suspend fun saveMode(mode: VaultLockMode) {
        state.value = state.value.copy(mode = mode)
    }

    override suspend fun savePinRecord(hashBase64: String, saltBase64: String, pinLength: Int) {
        state.value = state.value.copy(
            pinHashBase64 = hashBase64,
            pinSaltBase64 = saltBase64,
            pinLength = pinLength
        )
    }

    override suspend fun clearPinRecord() {
        state.value = state.value.copy(
            pinHashBase64 = null,
            pinSaltBase64 = null,
            pinLength = 4
        )
    }
}

class AppLockManager(
    private val store: LockPreferencesStore,
    private val cryptoDispatcher: CoroutineDispatcher = Dispatchers.Default
) {
    private val secureRandom = SecureRandom()

    val lockSettings: Flow<VaultLockSettings> = store.recordFlow.map { record ->
        VaultLockSettings(
            mode = record.mode,
            hasPin = !record.pinHashBase64.isNullOrBlank() && !record.pinSaltBase64.isNullOrBlank(),
            pinLength = record.pinLength.coerceIn(4, 6)
        )
    }

    suspend fun getCurrentSettings(): VaultLockSettings {
        return lockSettings.first()
    }

    suspend fun setPin(pin: String): Result<Unit> = withContext(cryptoDispatcher) {
        try {
            require(isValidPinFormat(pin)) { "PIN must be 4 to 6 digits" }
            val salt = ByteArray(SALT_BYTES).also { secureRandom.nextBytes(it) }
            val hash = pbkdf2(pin, salt)
            store.savePinRecord(
                hashBase64 = Base64.encodeToString(hash, Base64.NO_WRAP),
                saltBase64 = Base64.encodeToString(salt, Base64.NO_WRAP),
                pinLength = pin.length
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun verifyPin(pin: String): Boolean = withContext(cryptoDispatcher) {
        if (!isValidPinFormat(pin)) return@withContext false
        val record = store.recordFlow.first()
        val storedHashB64 = record.pinHashBase64 ?: return@withContext false
        val storedSaltB64 = record.pinSaltBase64 ?: return@withContext false
        val storedHash = Base64.decode(storedHashB64, Base64.NO_WRAP)
        val salt = Base64.decode(storedSaltB64, Base64.NO_WRAP)
        val candidateHash = pbkdf2(pin, salt)
        MessageDigest.isEqual(storedHash, candidateHash)
    }

    suspend fun setLockMode(mode: VaultLockMode) {
        store.saveMode(mode)
    }

    suspend fun disableLockAndClearPin() {
        store.saveMode(VaultLockMode.OFF)
        store.clearPinRecord()
    }

    private fun pbkdf2(pin: String, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(pin.toCharArray(), salt, PBKDF2_ITERATIONS, HASH_BITS)
        return try {
            val factory = SecretKeyFactory.getInstance(PBKDF2_ALGORITHM)
            factory.generateSecret(spec).encoded
        } finally {
            spec.clearPassword()
        }
    }

    companion object {
        const val MIN_PIN_LENGTH = 4
        const val MAX_PIN_LENGTH = 6
        private const val SALT_BYTES = 16
        private const val PBKDF2_ITERATIONS = 100_000
        private const val HASH_BITS = 256
        private const val PBKDF2_ALGORITHM = "PBKDF2WithHmacSHA256"

        fun isValidPinFormat(pin: String): Boolean {
            return pin.length in MIN_PIN_LENGTH..MAX_PIN_LENGTH && pin.all { it.isDigit() }
        }
    }
}
