package com.example.data.crypto

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

data class EncryptedPayload(
    val ciphertextBase64: String,
    val ivBase64: String
)

interface SecretKeyProvider {
    val requiresExplicitIv: Boolean
    fun getOrCreateKey(): SecretKey
}

class AndroidKeystoreSecretKeyProvider(
    private val keyAlias: String = DEFAULT_KEY_ALIAS
) : SecretKeyProvider {
    override val requiresExplicitIv: Boolean = false

    override fun getOrCreateKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        val existingEntry = keyStore.getEntry(keyAlias, null) as? KeyStore.SecretKeyEntry
        if (existingEntry != null) {
            return existingEntry.secretKey
        }

        val keyGenerator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            ANDROID_KEYSTORE
        )
        val spec = KeyGenParameterSpec.Builder(
            keyAlias,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(AES_KEY_SIZE_BITS)
            .setRandomizedEncryptionRequired(true)
            .build()
        keyGenerator.init(spec)
        return keyGenerator.generateKey()
    }

    private companion object {
        const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val DEFAULT_KEY_ALIAS = "api_manager_aes_gcm_master_key"
        const val AES_KEY_SIZE_BITS = 256
    }
}

class InMemorySecretKeyProvider : SecretKeyProvider {
    override val requiresExplicitIv: Boolean = true

    private val key: SecretKey by lazy {
        val keyGenerator = KeyGenerator.getInstance("AES")
        keyGenerator.init(256)
        keyGenerator.generateKey()
    }

    override fun getOrCreateKey(): SecretKey = key
}

class CryptoManager(
    private val keyProvider: SecretKeyProvider = AndroidKeystoreSecretKeyProvider()
) {
    private val secureRandom = SecureRandom()

    fun encrypt(plaintext: String): EncryptedPayload {
        require(plaintext.isNotEmpty()) { "API key must not be empty" }
        val secretKey = keyProvider.getOrCreateKey()
        val cipher = Cipher.getInstance(TRANSFORMATION)
        if (keyProvider.requiresExplicitIv) {
            val iv = ByteArray(GCM_IV_LENGTH_BYTES).also { secureRandom.nextBytes(it) }
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv))
        } else {
            cipher.init(Cipher.ENCRYPT_MODE, secretKey)
        }

        val ivBytes = cipher.iv
        require(ivBytes != null && ivBytes.size == GCM_IV_LENGTH_BYTES) {
            "AES-GCM encryption requires a $GCM_IV_LENGTH_BYTES-byte IV, got ${ivBytes?.size ?: 0}"
        }

        val ciphertextBytes = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))
        return EncryptedPayload(
            ciphertextBase64 = Base64.encodeToString(ciphertextBytes, Base64.NO_WRAP),
            ivBase64 = Base64.encodeToString(ivBytes, Base64.NO_WRAP)
        )
    }

    fun decrypt(payload: EncryptedPayload): String {
        val ivBytes = Base64.decode(payload.ivBase64, Base64.NO_WRAP)
        require(ivBytes.size == GCM_IV_LENGTH_BYTES) {
            "Invalid IV length: expected $GCM_IV_LENGTH_BYTES bytes, got ${ivBytes.size}"
        }
        val ciphertextBytes = Base64.decode(payload.ciphertextBase64, Base64.NO_WRAP)
        val secretKey = keyProvider.getOrCreateKey()
        val cipher = Cipher.getInstance(TRANSFORMATION)
        val spec = GCMParameterSpec(GCM_TAG_LENGTH_BITS, ivBytes)
        cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)
        val plaintextBytes = cipher.doFinal(ciphertextBytes)
        return String(plaintextBytes, Charsets.UTF_8)
    }

    companion object {
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val GCM_IV_LENGTH_BYTES = 12
        const val GCM_TAG_LENGTH_BITS = 128
    }
}
