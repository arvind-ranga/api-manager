package com.example

import android.content.Context
import android.util.Base64
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.crypto.CryptoManager
import com.example.data.crypto.InMemorySecretKeyProvider
import com.example.data.local.AppDatabase
import com.example.data.model.ProviderType
import com.example.data.repository.ApiEndpointRepository
import com.example.data.security.AppLockManager
import com.example.data.security.InMemoryLockPreferencesStore
import com.example.data.security.VaultLockMode
import com.example.ui.ApiManagerScreen
import com.example.ui.ApiManagerViewModel
import com.example.ui.theme.ApiManagerTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import javax.crypto.AEADBadTagException

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w412dp-h915dp")
class ApiVaultRobolectricTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var database: AppDatabase
    private lateinit var cryptoManager: CryptoManager
    private lateinit var repository: ApiEndpointRepository
    private lateinit var lockManager: AppLockManager

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .setQueryExecutor { it.run() }
            .setTransactionExecutor { it.run() }
            .build()
        cryptoManager = CryptoManager(InMemorySecretKeyProvider())
        repository = ApiEndpointRepository(
            dao = database.apiEndpointDao(),
            cryptoManager = cryptoManager,
            ioDispatcher = Dispatchers.Unconfined
        )
        lockManager = AppLockManager(
            store = InMemoryLockPreferencesStore(),
            cryptoDispatcher = Dispatchers.Unconfined
        )
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `aes gcm encryption uses 12 byte random iv and decrypts accurately`() {
        val secret = "sk-test-1234567890abcdef"
        val first = cryptoManager.encrypt(secret)
        val second = cryptoManager.encrypt(secret)

        val firstIv = Base64.decode(first.ivBase64, Base64.NO_WRAP)
        val secondIv = Base64.decode(second.ivBase64, Base64.NO_WRAP)

        assertEquals(12, firstIv.size)
        assertEquals(12, secondIv.size)
        assertNotEquals(first.ivBase64, second.ivBase64)
        assertNotEquals(first.ciphertextBase64, second.ciphertextBase64)
        assertFalse(first.ciphertextBase64.contains(secret))

        assertEquals(secret, cryptoManager.decrypt(first))
        assertEquals(secret, cryptoManager.decrypt(second))
    }

    @Test(expected = AEADBadTagException::class)
    fun `aes gcm decryption rejects tampered ciphertext`() {
        val encrypted = cryptoManager.encrypt("sk-ant-api03-sensitive")
        val cipherBytes = Base64.decode(encrypted.ciphertextBase64, Base64.NO_WRAP)
        cipherBytes[0] = (cipherBytes[0].toInt() xor 0xFF).toByte()
        val tampered = encrypted.copy(
            ciphertextBase64 = Base64.encodeToString(cipherBytes, Base64.NO_WRAP)
        )
        cryptoManager.decrypt(tampered)
    }

    @Test
    fun `repository persists encrypted key and decrypts on demand`() = runTest {
        val plainKey = "sk-live-secret-key-999"
        val addResult = repository.addEndpoint(
            name = "OpenAI Prod",
            baseUrl = "https://api.openai.com/v1",
            plainApiKey = plainKey,
            providerType = ProviderType.OPENAI
        )
        assertTrue(addResult.isSuccess)

        val storedEntities = database.apiEndpointDao().observeAll().first()
        assertEquals(1, storedEntities.size)
        assertFalse(storedEntities[0].encryptedApiKey.contains(plainKey))
        assertEquals(12, Base64.decode(storedEntities[0].iv, Base64.NO_WRAP).size)

        val items = repository.observeEndpoints().first()
        val decrypted = repository.decryptApiKey(items.first()).getOrThrow()
        assertEquals(plainKey, decrypted)
    }

    @Test
    fun `app lock supports 4 to 6 digit pins and rejects invalid formats`() = runTest {
        assertTrue(lockManager.setPin("123").isFailure)
        assertTrue(lockManager.setPin("1234567").isFailure)
        assertTrue(lockManager.setPin("12a4").isFailure)

        assertTrue(lockManager.setPin("2580").isSuccess)
        assertTrue(lockManager.verifyPin("2580"))
        assertFalse(lockManager.verifyPin("0000"))

        assertTrue(lockManager.setPin("135790").isSuccess)
        assertEquals(6, lockManager.getCurrentSettings().pinLength)
        assertTrue(lockManager.verifyPin("135790"))
        assertFalse(lockManager.verifyPin("13579"))
    }

    @Test
    fun `ui lock screen gates vault behind 5 digit pin and unlocks cleanly`() = runTest {
        lockManager.setPin("48291")
        lockManager.setLockMode(VaultLockMode.PIN_ONLY)
        repository.addEndpoint(
            name = "Locked Key",
            baseUrl = "https://api.openai.com/v1",
            plainApiKey = "sk-hidden-behind-pin",
            providerType = ProviderType.OPENAI
        )

        val viewModel = ApiManagerViewModel(repository, lockManager)
        composeTestRule.setContent {
            ApiManagerTheme(dynamicColor = false) {
                ApiManagerScreen(viewModel = viewModel)
            }
        }

        composeTestRule.waitUntil(timeoutMillis = 3_000L) {
            composeTestRule.onAllNodesWithText("API Manager Locked").fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithText("API Manager Locked").assertIsDisplayed()

        // Wrong PIN shows error
        listOf("1", "1", "1", "1", "1").forEach { digit ->
            composeTestRule.onNodeWithTag("pin_key_$digit").performClick()
        }
        composeTestRule.waitUntil(timeoutMillis = 3_000L) {
            composeTestRule.onAllNodesWithText("Incorrect PIN. Try again.").fetchSemanticsNodes().isNotEmpty()
        }

        // Correct 5-digit PIN unlocks vault
        listOf("4", "8", "2", "9", "1").forEach { digit ->
            composeTestRule.onNodeWithTag("pin_key_$digit").performClick()
        }
        composeTestRule.waitUntil(timeoutMillis = 3_000L) {
            composeTestRule.onAllNodesWithText("Locked Key").fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithText("Locked Key").assertIsDisplayed()
    }

    @Test
    fun `ui masks api key by default and toggles reveal`() = runTest {
        repository.addEndpoint(
            name = "Claude Key",
            baseUrl = "https://api.anthropic.com/v1",
            plainApiKey = "sk-ant-revealed-secret",
            providerType = ProviderType.ANTHROPIC
        )
        val items = repository.observeEndpoints().first()
        val id = items.first().id

        val viewModel = ApiManagerViewModel(repository, lockManager)
        composeTestRule.setContent {
            ApiManagerTheme(dynamicColor = false) {
                ApiManagerScreen(viewModel = viewModel)
            }
        }

        composeTestRule.waitUntil(timeoutMillis = 3_000L) {
            composeTestRule.onAllNodesWithText("Claude Key").fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithText("Claude Key").assertIsDisplayed()
        composeTestRule.onNodeWithText("••••••••••••").assertIsDisplayed()

        composeTestRule.onNodeWithTag("toggle_key_$id").performClick()
        composeTestRule.waitUntil(timeoutMillis = 3_000L) {
            composeTestRule.onAllNodesWithText("sk-ant-revealed-secret").fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithText("sk-ant-revealed-secret").assertIsDisplayed()

        composeTestRule.onNodeWithTag("toggle_key_$id").performClick()
        composeTestRule.waitUntil(timeoutMillis = 3_000L) {
            composeTestRule.onAllNodesWithText("••••••••••••").fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithText("••••••••••••").assertIsDisplayed()
    }

    @Test
    fun `ui add endpoint dialog saves and displays new card`() {
        val viewModel = ApiManagerViewModel(repository, lockManager)
        composeTestRule.setContent {
            ApiManagerTheme(dynamicColor = false) {
                ApiManagerScreen(viewModel = viewModel)
            }
        }
        composeTestRule.waitUntil(timeoutMillis = 3_000L) {
            composeTestRule.onAllNodesWithText("Your API Vault is Empty").fetchSemanticsNodes().isNotEmpty()
        }

        composeTestRule.onNodeWithTag("add_endpoint_fab").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("input_name").performTextInput("Custom Gateway")
        composeTestRule.onNodeWithTag("dropdown_provider").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("provider_option_custom").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("input_base_url").performTextInput("https://gateway.internal.dev")
        composeTestRule.onNodeWithTag("input_api_key").performTextInput("gw-secret-key-42")
        composeTestRule.onNodeWithTag("dialog_save_button").performClick()

        composeTestRule.waitUntil(timeoutMillis = 3_000L) {
            composeTestRule.onAllNodesWithText("Custom Gateway").fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithText("Custom Gateway").assertIsDisplayed()
        composeTestRule.onNodeWithText("https://gateway.internal.dev").assertIsDisplayed()
        composeTestRule.onNodeWithText("••••••••••••").assertIsDisplayed()
    }
}
