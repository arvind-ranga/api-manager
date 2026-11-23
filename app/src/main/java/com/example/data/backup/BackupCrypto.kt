package com.example.data.backup

import android.util.Base64
import java.io.ByteArrayOutputStream
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
    private const val BACKUP_MAGIC = "APIMGRBAK"
    private const val BACKUP_VERSION = 1
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

    /** Password-only backup encryption. Safe to move between phones and installs. */
    fun encryptWithPassword(plaintext: ByteArray, password: String): ByteArray {
        require(password.isNotBlank()) { "Backup password cannot be empty" }
        val salt = ByteArray(SALT_LENGTH_BYTES).also { secureRandom.nextBytes(it) }
        val iv = ByteArray(IV_LENGTH_BYTES).also { secureRandom.nextBytes(it) }
        val key = derivePasswordKey(password, salt)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(TAG_LENGTH_BITS, iv))
        val encrypted = cipher.doFinal(plaintext)
        return ByteArrayOutputStream().apply {
            write(BACKUP_MAGIC.toByteArray(Charsets.US_ASCII))
            write(byteArrayOf(BACKUP_VERSION.toByte()))
            write(salt)
            write(iv)
            write(encrypted)
        }.toByteArray()
    }

    fun decryptWithPassword(data: ByteArray, password: String): ByteArray {
        val headerLength = BACKUP_MAGIC.length + 1 + SALT_LENGTH_BYTES + IV_LENGTH_BYTES
        require(data.size > headerLength) { "This is not a valid API Manager backup" }
        val magic = data.copyOfRange(0, BACKUP_MAGIC.length).toString(Charsets.US_ASCII)
        require(magic == BACKUP_MAGIC) { "This is not a valid API Manager backup" }
        require(data[BACKUP_MAGIC.length].toInt() == BACKUP_VERSION) { "Unsupported backup version" }
        val offset = BACKUP_MAGIC.length + 1
        val salt = data.copyOfRange(offset, offset + SALT_LENGTH_BYTES)
        val iv = data.copyOfRange(offset + SALT_LENGTH_BYTES, offset + SALT_LENGTH_BYTES + IV_LENGTH_BYTES)
        val encrypted = data.copyOfRange(headerLength, data.size)
        val key = derivePasswordKey(password, salt)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(TAG_LENGTH_BITS, iv))
        return cipher.doFinal(encrypted)
    }

    private fun derivePasswordKey(password: String, salt: ByteArray): SecretKeySpec {
        val spec = PBEKeySpec(password.toCharArray(), salt, DEFAULT_ITERATIONS, KEY_LENGTH_BITS)
        return try {
            val key = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec)
            SecretKeySpec(key.encoded, "AES")
        } finally {
            spec.clearPassword()
        }
    }

    private fun encode(bytes: ByteArray): String =
        Base64.encodeToString(bytes, Base64.NO_WRAP)

    private fun decode(value: String): ByteArray =
        Base64.decode(value, Base64.NO_WRAP)

    data class WrappedSecret(val ciphertext: String, val iv: String)
}
