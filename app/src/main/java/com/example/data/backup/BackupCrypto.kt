package com.example.data.backup

import android.util.Base64
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/**
 * Passphrase-based encryption used only for device-to-device transfers.
 *
 * A local ZIP backup stays bound to the Android Keystore of the device that
 * created it. QR/LAN transfers need to work on a second device, so the sender
 * generates a short one-time code, wraps each API key with it, and the receiver
 * unwraps and re-encrypts the keys with its own Keystore.
 */
object BackupCrypto {
    const val DEFAULT_ITERATIONS = 120_000
    private const val SALT_LENGTH_BYTES = 16
    private const val IV_LENGTH_BYTES = 12
    private const val TAG_LENGTH_BITS = 128
    private const val KEY_LENGTH_BITS = 256
    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
    private val secureRandom = SecureRandom()

    fun generateCode(length: Int = 12): String = buildString(length) {
        repeat(length) { append(ALPHABET[secureRandom.nextInt(ALPHABET.length)]) }
    }

    fun newSalt(): String {
        val salt = ByteArray(SALT_LENGTH_BYTES).also { secureRandom.nextBytes(it) }
        return encode(salt)
    }

    fun deriveKey(code: String, salt: String): SecretKeySpec {
        require(code.isNotBlank()) { "Transfer code cannot be empty" }
        val spec = PBEKeySpec(
            code.toCharArray(),
            decode(salt),
            DEFAULT_ITERATIONS,
            KEY_LENGTH_BITS
        )
        val key = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec)
        return SecretKeySpec(key.encoded, "AES")
    }

    fun encrypt(plaintext: String, key: SecretKeySpec): WrappedSecret {
        val iv = ByteArray(IV_LENGTH_BYTES).also { secureRandom.nextBytes(it) }
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(TAG_LENGTH_BITS, iv))
        return WrappedSecret(
            ciphertext = encode(cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))),
            iv = encode(iv)
        )
    }

    fun decrypt(ciphertext: String, iv: String, key: SecretKeySpec): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(TAG_LENGTH_BITS, decode(iv)))
        return String(cipher.doFinal(decode(ciphertext)), Charsets.UTF_8)
    }

    private fun encode(bytes: ByteArray): String =
        Base64.encodeToString(bytes, Base64.NO_WRAP)

    private fun decode(value: String): ByteArray =
        Base64.decode(value, Base64.NO_WRAP)

    data class WrappedSecret(val ciphertext: String, val iv: String)
}
