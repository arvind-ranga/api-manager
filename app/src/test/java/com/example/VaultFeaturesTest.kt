package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.backup.VaultBackupManager
import com.example.data.crypto.CryptoManager
import com.example.data.crypto.EncryptedPayload
import com.example.data.crypto.InMemorySecretKeyProvider
import com.example.data.local.ApiEndpointEntity
import com.example.data.local.AppDatabase
import com.example.data.model.ProviderType
import com.example.data.repository.ApiEndpointRepository
import com.example.data.settings.ThemeMode
import com.example.data.sync.LocalSyncClient
import com.example.data.sync.QrBackupCodec
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class VaultFeaturesTest {

    private lateinit var database: AppDatabase
    private lateinit var repository: ApiEndpointRepository

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .setQueryExecutor { it.run() }
            .setTransactionExecutor { it.run() }
            .build()
        repository = ApiEndpointRepository(
            dao = database.apiEndpointDao(),
            cryptoManager = CryptoManager(InMemorySecretKeyProvider()),
            ioDispatcher = Dispatchers.Unconfined
        )
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `expiry date is stored with endpoint`() = runTest {
        val expiresAt = 1_900_000_000_000L
        repository.addEndpoint(
            name = "Expiring",
            baseUrl = "https://api.example.com/v1",
            plainApiKey = "secret",
            providerType = ProviderType.CUSTOM,
            expiresAtMillis = expiresAt
        ).getOrThrow()

        val item = repository.observeEndpoints().first().single()
        assertEquals(expiresAt, item.expiresAtMillis)
    }

    @Test
    fun `duplicate sites are counted and auto named`() = runTest {
        repository.addEndpoint("Prod", "https://api.example.com/v1", "key-1", ProviderType.CUSTOM)
            .getOrThrow()
        repository.addEndpoint("Prod", "https://api.example.com/v1/", "key-2", ProviderType.CUSTOM)
            .getOrThrow()
        repository.addEndpoint("Staging", "https://staging.example.com/v1", "key-3", ProviderType.CUSTOM)
            .getOrThrow()

        val items = repository.observeEndpoints().first()
        assertEquals(setOf("Prod", "Prod 1", "Staging"), items.map { it.name }.toSet())

        val duplicates = repository.duplicateCountsFor(items)
        assertEquals(2, duplicates["https://api.example.com/v1"])
        assertEquals(1, duplicates["https://staging.example.com/v1"])
    }

    @Test
    fun `backup zip round trip preserves encrypted keys`() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val manager = VaultBackupManager(context, "2.0")

        repository.addEndpoint("Backup me", "https://api.example.com", "encrypted-secret", ProviderType.CUSTOM)
            .getOrThrow()
        val original = repository.getEntitiesForBackup()
        val envelope = manager.buildEnvelope(original)

        val zipBytes = manager.toZipBytes(envelope)
        val restored = manager.fromZipBytes(zipBytes).getOrThrow()
        val entities = manager.toEntities(restored)

        assertEquals(1, entities.size)
        assertEquals(original.first().encryptedApiKey, entities.first().encryptedApiKey)
        assertEquals(original.first().iv, entities.first().iv)
        assertEquals(original.first().remoteId, entities.first().remoteId)
    }

    @Test
    fun `qr chunks assemble back into original json`() {
        val json = "{\"schemaVersion\":1,\"endpoints\":[{\"name\":\"A\"}]}"
        val chunks = QrBackupCodec.encodeChunks(json)

        assertEquals(1, chunks.size)
        assertTrue(chunks.single().startsWith("APIMGR1|1|1|"))
        val parts = chunks.mapNotNull(QrBackupCodec::decodePart)
        assertEquals(json, QrBackupCodec.assemble(parts).getOrThrow())
    }

    @Test
    fun `portable transfer works between different keystores`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val sourceCrypto = CryptoManager(InMemorySecretKeyProvider())
        val targetCrypto = CryptoManager(InMemorySecretKeyProvider())
        val sourceManager = VaultBackupManager(context, "2.0", sourceCrypto)
        val targetManager = VaultBackupManager(context, "2.0", targetCrypto)

        val encrypted = sourceCrypto.encrypt("cross-device-secret")
        val entity = ApiEndpointEntity(
            name = "Portable",
            baseUrl = "https://api.example.com",
            encryptedApiKey = encrypted.ciphertextBase64,
            iv = encrypted.ivBase64,
            providerType = "CUSTOM"
        )
        val envelope = sourceManager.buildPortableEnvelope(listOf(entity), "TRANSFER123")
            .getOrThrow()
        val parsed = sourceManager.fromJson(sourceManager.toJson(envelope)).getOrThrow()
        val restored = targetManager.fromPortableEnvelope(parsed, "TRANSFER123").getOrThrow()
            .single()

        assertEquals(
            "cross-device-secret",
            targetCrypto.decrypt(
                EncryptedPayload(restored.encryptedApiKey, restored.iv)
            )
        )
        assertTrue(
            targetManager.fromPortableEnvelope(parsed, "WRONGCODE123").isFailure
        )
    }

    @Test
    fun `local sync client rejects public addresses`() = runTest {
        val result = LocalSyncClient.download("http://8.8.8.8:8976/sync?token=abc")
        assertTrue(result.isFailure)
    }

    @Test
    fun `theme mode falls back to auto for unknown stored value`() {
        assertEquals(ThemeMode.AUTO, ThemeMode.fromStored("unknown"))
        assertEquals(ThemeMode.DARK, ThemeMode.fromStored("dark"))
    }
}
