package com.example.data.repository

import com.example.data.crypto.CryptoManager
import com.example.data.crypto.EncryptedPayload
import com.example.data.local.ApiEndpointDao
import com.example.data.local.ApiEndpointEntity
import com.example.data.model.ProviderType
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.util.UUID

data class ApiEndpointItem(
    val id: Long,
    val remoteId: String,
    val name: String,
    val baseUrl: String,
    val encryptedApiKey: String,
    val iv: String,
    val providerType: ProviderType,
    val createdAt: Long,
    val updatedAt: Long,
    val expiresAtMillis: Long? = null,
    val deletedAtMillis: Long? = null
) {
    val isInTrash: Boolean get() = deletedAtMillis != null
}

class ApiEndpointRepository(
    private val dao: ApiEndpointDao,
    private val cryptoManager: CryptoManager,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    fun observeEndpoints(): Flow<List<ApiEndpointItem>> {
        return dao.observeAll().map { entities -> entities.map { it.toDomain() } }
    }

    fun observeTrash(): Flow<List<ApiEndpointItem>> {
        return dao.observeTrash().map { entities -> entities.map { it.toDomain() } }
    }

    suspend fun addEndpoint(
        name: String,
        baseUrl: String,
        plainApiKey: String,
        providerType: ProviderType,
        expiresAtMillis: Long? = null
    ): Result<Long> = withContext(ioDispatcher) {
        try {
            val trimmedUrl = normalizeUrl(baseUrl.trim())
            val trimmedKey = plainApiKey.trim()
            require(name.isNotBlank()) { "Endpoint name cannot be empty" }
            require(trimmedUrl.isNotEmpty()) { "Base URL cannot be empty" }
            require(trimmedKey.isNotEmpty()) { "API key cannot be empty" }

            val finalName = suggestNameForUrl(name.trim(), trimmedUrl)
            val encrypted = cryptoManager.encrypt(trimmedKey)
            val now = System.currentTimeMillis()
            val id = dao.insert(
                ApiEndpointEntity(
                    remoteId = UUID.randomUUID().toString(),
                    name = finalName,
                    baseUrl = trimmedUrl,
                    encryptedApiKey = encrypted.ciphertextBase64,
                    iv = encrypted.ivBase64,
                    providerType = providerType.name,
                    createdAt = now,
                    updatedAt = now,
                    expiresAtMillis = expiresAtMillis
                )
            )
            Result.success(id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateEndpoint(
        id: Long,
        name: String,
        baseUrl: String,
        plainApiKey: String,
        providerType: ProviderType,
        expiresAtMillis: Long? = null
    ): Result<Unit> = withContext(ioDispatcher) {
        try {
            val existing = dao.getById(id)
                ?: return@withContext Result.failure(IllegalArgumentException("Endpoint not found"))
            val trimmedUrl = normalizeUrl(baseUrl.trim())
            val trimmedKey = plainApiKey.trim()
            require(name.isNotBlank()) { "Endpoint name cannot be empty" }
            require(trimmedUrl.isNotEmpty()) { "Base URL cannot be empty" }
            require(trimmedKey.isNotEmpty()) { "API key cannot be empty" }

            val encrypted = cryptoManager.encrypt(trimmedKey)
            dao.update(
                existing.copy(
                    remoteId = existing.remoteId.ifBlank { UUID.randomUUID().toString() },
                    name = name.trim(),
                    baseUrl = trimmedUrl,
                    encryptedApiKey = encrypted.ciphertextBase64,
                    iv = encrypted.ivBase64,
                    providerType = providerType.name,
                    updatedAt = System.currentTimeMillis(),
                    expiresAtMillis = expiresAtMillis
                )
            )
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun decryptApiKey(item: ApiEndpointItem): Result<String> = withContext(ioDispatcher) {
        try {
            Result.success(
                cryptoManager.decrypt(
                    EncryptedPayload(
                        ciphertextBase64 = item.encryptedApiKey,
                        ivBase64 = item.iv
                    )
                )
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteEndpoint(id: Long): Result<Unit> = withContext(ioDispatcher) {
        try {
            dao.deleteById(id)
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** Moves an endpoint to Trash. It can be restored for [TRASH_RETENTION_DAYS] days. */
    suspend fun moveToTrash(id: Long): Result<Unit> = withContext(ioDispatcher) {
        try {
            dao.moveToTrash(id, System.currentTimeMillis())
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun restoreFromTrash(id: Long): Result<Unit> = withContext(ioDispatcher) {
        try {
            dao.restoreFromTrash(id, System.currentTimeMillis())
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteFromTrash(id: Long): Result<Unit> = withContext(ioDispatcher) {
        try {
            dao.deleteById(id)
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun emptyTrash(): Result<Int> = withContext(ioDispatcher) {
        try {
            Result.success(dao.emptyTrash())
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** Deletes Trash items older than 14 days. Safe to call on every app start. */
    suspend fun purgeExpiredTrash(): Int = withContext(ioDispatcher) {
        val cutoff = System.currentTimeMillis() - TRASH_RETENTION_MILLIS
        dao.purgeTrashOlderThan(cutoff)
    }

    suspend fun getEntitiesForBackup(): List<ApiEndpointEntity> = withContext(ioDispatcher) {
        dao.getAllOnce()
    }

    suspend fun importEntities(entities: List<ApiEndpointEntity>): Result<Int> =
        withContext(ioDispatcher) {
            try {
                val existingIds = dao.getAllOnce().mapTo(HashSet()) { it.remoteId }
                val fresh = entities.map { entity ->
                    if (entity.remoteId.isBlank() || entity.remoteId in existingIds) {
                        entity.copy(remoteId = UUID.randomUUID().toString(), id = 0L)
                    } else {
                        entity.copy(id = 0L)
                    }
                }
                dao.upsertAll(fresh)
                Result.success(fresh.size)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    suspend fun duplicateCountsFor(items: List<ApiEndpointItem>): Map<String, Int> =
        withContext(ioDispatcher) {
            items.groupingBy { normalizeUrl(it.baseUrl) }.eachCount()
        }

    /**
     * If another key for the same website already exists, append 1, 2, 3, ...
     * so keys for one provider are easy to tell apart.
     */
    suspend fun suggestNameForUrl(name: String, baseUrl: String): String =
        withContext(ioDispatcher) {
            val key = normalizeUrl(baseUrl)
            val taken = dao.getAllOnce()
                .filter { normalizeUrl(it.baseUrl) == key }
                .mapTo(HashSet()) { it.name.lowercase() }
            if (key.isEmpty() || name.lowercase() !in taken) return@withContext name
            var index = 1
            while ("$name $index".lowercase() in taken) index++
            "$name $index"
        }

    /** Removes trailing slash and query so the same site is recognised reliably. */
    fun normalizeUrl(url: String): String {
        return url.trim().trimEnd('/').lowercase()
    }

    private fun ApiEndpointEntity.toDomain(): ApiEndpointItem = ApiEndpointItem(
        id = id,
        remoteId = remoteId,
        name = name,
        baseUrl = baseUrl,
        encryptedApiKey = encryptedApiKey,
        iv = iv,
        providerType = ProviderType.fromStored(providerType),
        createdAt = createdAt,
        updatedAt = updatedAt,
        expiresAtMillis = expiresAtMillis,
        deletedAtMillis = deletedAtMillis
    )

    companion object {
        const val TRASH_RETENTION_DAYS = 14L
        const val TRASH_RETENTION_MILLIS = TRASH_RETENTION_DAYS * 24L * 60L * 60L * 1000L
    }
}
