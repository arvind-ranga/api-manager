package com.example.data.backup

import android.content.Context
import android.net.Uri
import com.example.data.crypto.CryptoManager
import com.example.data.crypto.EncryptedPayload
import com.example.data.local.ApiEndpointEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

data class BackupEndpoint(
    val remoteId: String,
    val name: String,
    val baseUrl: String,
    val encryptedApiKey: String,
    val iv: String,
    val providerType: String,
    val createdAt: Long,
    val updatedAt: Long,
    val expiresAtMillis: Long?,
    val transferIv: String? = null
)

data class BackupEnvelope(
    val schemaVersion: Int = BACKUP_SCHEMA_VERSION,
    val appVersion: String,
    val exportedAtMillis: Long,
    val endpoints: List<BackupEndpoint>,
    val transferSalt: String? = null,
    val transferIterations: Int? = null
)

const val BACKUP_SCHEMA_VERSION = 1
private const val ENTRY_MANIFEST = "manifest.json"
private const val ENTRY_ENDPOINTS = "endpoints.json"
private const val MAX_ENDPOINTS = 10_000
private const val MAX_NAME = 200
private const val MAX_URL = 2_000
private const val MAX_KEY = 20_000
private const val MAX_IV = 200

/**
 * Creates and reads user-controlled backup files. Keys stay encrypted with the
 * Android Keystore key, so a backup can only be restored on the same install/device.
 */
class VaultBackupManager(
    private val context: Context,
    private val appVersion: String,
    private val cryptoManager: CryptoManager = CryptoManager()
) {
    fun buildEnvelope(entities: List<ApiEndpointEntity>): BackupEnvelope = BackupEnvelope(
        appVersion = appVersion,
        exportedAtMillis = System.currentTimeMillis(),
        endpoints = entities.map { entity ->
            BackupEndpoint(
                remoteId = entity.remoteId,
                name = entity.name,
                baseUrl = entity.baseUrl,
                encryptedApiKey = entity.encryptedApiKey,
                iv = entity.iv,
                providerType = entity.providerType,
                createdAt = entity.createdAt,
                updatedAt = entity.updatedAt,
                expiresAtMillis = entity.expiresAtMillis
            )
        }
    )

    /**
     * Re-encrypts every API key with a one-time transfer code so the payload can
     * be decrypted on a different device with a different Android Keystore.
     */
    fun buildPortableEnvelope(
        entities: List<ApiEndpointEntity>,
        code: String
    ): Result<BackupEnvelope> = runCatching {
        val base = buildEnvelope(entities)
        val salt = BackupCrypto.newSalt()
        val key = BackupCrypto.deriveKey(code, salt)
        base.copy(
            endpoints = base.endpoints.map { endpoint ->
                val plaintext = cryptoManager.decrypt(
                    EncryptedPayload(
                        ciphertextBase64 = endpoint.encryptedApiKey,
                        ivBase64 = endpoint.iv
                    )
                )
                val wrapped = BackupCrypto.encrypt(plaintext, key)
                endpoint.copy(
                    encryptedApiKey = wrapped.ciphertext,
                    transferIv = wrapped.iv
                )
            },
            transferSalt = salt,
            transferIterations = BackupCrypto.DEFAULT_ITERATIONS
        )
    }

    /** Unwraps a transfer envelope and re-encrypts each key for this device. */
    fun fromPortableEnvelope(
        envelope: BackupEnvelope,
        code: String
    ): Result<List<ApiEndpointEntity>> = runCatching {
        val salt = checkNotNull(envelope.transferSalt) { "Backup is not a portable transfer" }
        val iterations = envelope.transferIterations ?: BackupCrypto.DEFAULT_ITERATIONS
        require(iterations == BackupCrypto.DEFAULT_ITERATIONS) {
            "Unsupported transfer encryption settings"
        }
        val key = BackupCrypto.deriveKey(code, salt)
        envelope.endpoints.map { endpoint ->
            val transferIv = checkNotNull(endpoint.transferIv) {
                "Backup is missing transfer encryption data"
            }
            val plaintext = BackupCrypto.decrypt(endpoint.encryptedApiKey, transferIv, key)
            val local = cryptoManager.encrypt(plaintext)
            ApiEndpointEntity(
                remoteId = endpoint.remoteId,
                name = endpoint.name,
                baseUrl = endpoint.baseUrl,
                encryptedApiKey = local.ciphertextBase64,
                iv = local.ivBase64,
                providerType = endpoint.providerType,
                createdAt = endpoint.createdAt,
                updatedAt = endpoint.updatedAt,
                expiresAtMillis = endpoint.expiresAtMillis
            )
        }
    }

    fun toJson(envelope: BackupEnvelope): String {
        val root = JSONObject().apply {
            put("schemaVersion", envelope.schemaVersion)
            put("appVersion", envelope.appVersion)
            put("exportedAtMillis", envelope.exportedAtMillis)
            if (envelope.transferSalt != null) {
                put("transferSalt", envelope.transferSalt)
                put("transferIterations", envelope.transferIterations ?: BackupCrypto.DEFAULT_ITERATIONS)
            }
            put(
                "endpoints",
                JSONArray().also { array ->
                    envelope.endpoints.forEach { endpoint ->
                        array.put(
                            JSONObject().apply {
                                put("remoteId", endpoint.remoteId)
                                put("name", endpoint.name)
                                put("baseUrl", endpoint.baseUrl)
                                put("encryptedApiKey", endpoint.encryptedApiKey)
                                put("iv", endpoint.iv)
                                if (endpoint.transferIv != null) {
                                    put("transferIv", endpoint.transferIv)
                                }
                                put("providerType", endpoint.providerType)
                                put("createdAt", endpoint.createdAt)
                                put("updatedAt", endpoint.updatedAt)
                                if (endpoint.expiresAtMillis != null) {
                                    put("expiresAtMillis", endpoint.expiresAtMillis)
                                }
                            }
                        )
                    }
                }
            )
        }
        return root.toString()
    }

    fun fromJson(json: String): Result<BackupEnvelope> = runCatching {
        val root = JSONObject(json)
        val version = root.optInt("schemaVersion", -1)
        require(version == BACKUP_SCHEMA_VERSION) { "Unsupported backup version: $version" }
        val array = root.optJSONArray("endpoints") ?: JSONArray()
        require(array.length() <= MAX_ENDPOINTS) { "Backup contains too many endpoints" }
        val transferSalt = if (root.has("transferSalt")) root.optString("transferSalt") else null
        val transferIterations = if (root.has("transferIterations")) {
            root.optInt("transferIterations", BackupCrypto.DEFAULT_ITERATIONS)
        } else {
            null
        }
        val endpoints = (0 until array.length()).map { index ->
            val item = array.getJSONObject(index)
            val endpoint = BackupEndpoint(
                remoteId = item.optString("remoteId"),
                name = item.optString("name"),
                baseUrl = item.optString("baseUrl"),
                encryptedApiKey = item.optString("encryptedApiKey"),
                iv = item.optString("iv"),
                transferIv = if (item.has("transferIv")) item.optString("transferIv") else null,
                providerType = item.optString("providerType", "CUSTOM"),
                createdAt = item.optLong("createdAt", System.currentTimeMillis()),
                updatedAt = item.optLong("updatedAt", System.currentTimeMillis()),
                expiresAtMillis = if (item.isNull("expiresAtMillis")) {
                    null
                } else {
                    item.optLong("expiresAtMillis")
                }
            )
            validate(endpoint)
            require((endpoint.transferIv == null) == (transferSalt == null)) {
                "Backup has inconsistent transfer encryption metadata"
            }
            endpoint
        }
        BackupEnvelope(
            schemaVersion = version,
            appVersion = root.optString("appVersion", "unknown"),
            exportedAtMillis = root.optLong("exportedAtMillis", System.currentTimeMillis()),
            endpoints = endpoints,
            transferSalt = transferSalt,
            transferIterations = transferIterations
        )
    }

    fun toZipBytes(envelope: BackupEnvelope): ByteArray {
        val json = toJson(envelope)
        val output = ByteArrayOutputStream()
        ZipOutputStream(output).use { zip ->
            zip.putNextEntry(ZipEntry(ENTRY_MANIFEST))
            JSONObject().apply {
                put("format", "api-manager-backup")
                put("schemaVersion", envelope.schemaVersion)
                put("appVersion", envelope.appVersion)
                put("exportedAtMillis", envelope.exportedAtMillis)
                put("endpointCount", envelope.endpoints.size)
                put(
                    "encryption",
                    if (envelope.transferSalt == null) {
                        "AES-256-GCM (Android Keystore bound)"
                    } else {
                        "AES-256-GCM (PBKDF2 transfer code, iterations=${envelope.transferIterations})"
                    }
                )
            }.toString().also { zip.write(it.toByteArray(Charsets.UTF_8)) }
            zip.closeEntry()

            zip.putNextEntry(ZipEntry(ENTRY_ENDPOINTS))
            zip.write(json.toByteArray(Charsets.UTF_8))
            zip.closeEntry()
        }
        return output.toByteArray()
    }

    fun fromZipBytes(bytes: ByteArray): Result<BackupEnvelope> = runCatching {
        var endpointsJson: String? = null
        ZipInputStream(ByteArrayInputStream(bytes)).use { zip ->
            var entry: ZipEntry? = zip.nextEntry
            while (entry != null) {
                if (entry.name == ENTRY_ENDPOINTS) {
                    endpointsJson = zip.readBytes().toString(Charsets.UTF_8)
                    zip.closeEntry()
                    break
                }
                zip.closeEntry()
                entry = zip.nextEntry
            }
        }
        val json = endpointsJson ?: throw IllegalArgumentException("Backup is missing $ENTRY_ENDPOINTS")
        fromJson(json).getOrThrow()
    }

    suspend fun exportTo(uri: Uri, envelope: BackupEnvelope): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val bytes = toZipBytes(envelope)
            val output = checkNotNull(context.contentResolver.openOutputStream(uri)) {
                "Cannot write to the selected location"
            }
            output.use { it.write(bytes) }
        }
    }

    suspend fun importFrom(uri: Uri): Result<BackupEnvelope> = withContext(Dispatchers.IO) {
        runCatching {
            val bytes = checkNotNull(context.contentResolver.openInputStream(uri)) {
                "Cannot read the selected file"
            }.use { it.readBytes() }
            fromZipBytes(bytes).getOrThrow()
        }
    }

    fun toEntities(envelope: BackupEnvelope): List<ApiEndpointEntity> = envelope.endpoints.map {
        ApiEndpointEntity(
            remoteId = it.remoteId,
            name = it.name,
            baseUrl = it.baseUrl,
            encryptedApiKey = it.encryptedApiKey,
            iv = it.iv,
            providerType = it.providerType,
            createdAt = it.createdAt,
            updatedAt = it.updatedAt,
            expiresAtMillis = it.expiresAtMillis
        )
    }

    private fun validate(endpoint: BackupEndpoint) {
        require(endpoint.name.length in 1..MAX_NAME) { "Invalid endpoint name in backup" }
        require(endpoint.baseUrl.length in 1..MAX_URL) { "Invalid URL in backup" }
        require(endpoint.encryptedApiKey.length in 1..MAX_KEY) { "Invalid encrypted key in backup" }
        require(endpoint.iv.length in 1..MAX_IV) { "Invalid IV in backup" }
    }
}
