package com.example.ui

import android.net.Uri
import android.util.Log
import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.R
import com.example.data.backup.BackupCrypto
import com.example.data.backup.VaultBackupManager
import com.example.data.model.ProviderType
import com.example.data.repository.ApiEndpointItem
import com.example.data.repository.ApiEndpointRepository
import com.example.data.security.AppLockManager
import com.example.data.security.VaultLockMode
import com.example.data.security.VaultLockSettings
import com.example.data.settings.InMemoryThemePreferencesStore
import com.example.data.settings.ThemeMode
import com.example.data.settings.ThemePreferencesStore
import com.example.data.sync.LocalSyncClient
import com.example.data.sync.LocalSyncServer
import com.example.data.sync.QrBackupCodec
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class AppScreen {
    VAULT, SETTINGS, ABOUT, QR_SEND, QR_RECEIVE, LAN_SEND, LAN_RECEIVE
}

data class ApiManagerUiState(
    val allEndpoints: List<ApiEndpointItem> = emptyList(),
    val filteredEndpoints: List<ApiEndpointItem> = emptyList(),
    val revealedKeys: Map<Long, String> = emptyMap(),
    val selectedProviderFilter: ProviderType? = null,
    val searchQuery: String = "",
    val duplicateUrls: Map<String, Int> = emptyMap(),
    val isAddEditDialogOpen: Boolean = false,
    val editingEndpoint: ApiEndpointItem? = null,
    val editingPlainKey: String = "",
    val isPinSetupDialogOpen: Boolean = false,
    val deleteTarget: ApiEndpointItem? = null,
    @StringRes val snackbarMessageRes: Int? = null,
    val snackbarText: String? = null,
    val isLoading: Boolean = true,
    // Security lock
    val lockSettings: VaultLockSettings = VaultLockSettings(),
    val isLockInitialized: Boolean = false,
    val isLocked: Boolean = false,
    val lockSessionId: Int = 0,
    @StringRes val pinErrorRes: Int? = null,
    val cooldownSeconds: Int = 0,
    val biometricErrorMessage: String? = null,
    // Appearance & navigation
    val themeMode: ThemeMode = ThemeMode.AUTO,
    val currentScreen: AppScreen = AppScreen.VAULT,
    // Backup / sync
    val isSyncBusy: Boolean = false,
    val qrChunks: List<String> = emptyList(),
    val qrIndex: Int = 0,
    val syncUrl: String? = null,
    val syncMessage: String? = null,
    val transferCode: String? = null,
    val transferPassphraseInput: String = ""
)

private data class InteractionState(
    val revealedKeys: Map<Long, String> = emptyMap(),
    val selectedProviderFilter: ProviderType? = null,
    val searchQuery: String = "",
    val isAddEditDialogOpen: Boolean = false,
    val editingEndpoint: ApiEndpointItem? = null,
    val editingPlainKey: String = "",
    val isPinSetupDialogOpen: Boolean = false,
    val deleteTarget: ApiEndpointItem? = null,
    @StringRes val snackbarMessageRes: Int? = null,
    val snackbarText: String? = null,
    val isLockInitialized: Boolean = false,
    val isLocked: Boolean = false,
    val lockSessionId: Int = 0,
    @StringRes val pinErrorRes: Int? = null,
    val failedPinAttempts: Int = 0,
    val cooldownSeconds: Int = 0,
    val biometricErrorMessage: String? = null,
    val themeMode: ThemeMode = ThemeMode.AUTO,
    val currentScreen: AppScreen = AppScreen.VAULT,
    val isSyncBusy: Boolean = false,
    val qrChunks: List<String> = emptyList(),
    val qrIndex: Int = 0,
    val syncUrl: String? = null,
    val syncMessage: String? = null,
    val transferCode: String? = null,
    val transferPassphraseInput: String = "",
    val pendingModeAfterPinSetup: VaultLockMode? = null
)

class ApiManagerViewModel(
    private val repository: ApiEndpointRepository,
    private val lockManager: AppLockManager,
    private val themeStore: ThemePreferencesStore = InMemoryThemePreferencesStore(),
    private val backupManager: VaultBackupManager? = null
) : ViewModel() {

    private val interactionState = MutableStateFlow(InteractionState())
    private var cooldownJob: Job? = null
    private var suppressAutoLockDepth = 0
    private var localSyncServer: LocalSyncServer? = null

    init {
        viewModelScope.launch {
            lockManager.lockSettings.collect { settings ->
                interactionState.update { state ->
                    val shouldInitialLock = !state.isLockInitialized && settings.mode.isEnabled
                    state.copy(
                        isLockInitialized = true,
                        isLocked = if (!state.isLockInitialized) shouldInitialLock else state.isLocked,
                        lockSessionId = if (shouldInitialLock) state.lockSessionId + 1 else state.lockSessionId
                    )
                }
            }
        }
        viewModelScope.launch {
            themeStore.themeMode.collect { mode ->
                interactionState.update { it.copy(themeMode = mode) }
            }
        }
    }

    private val themeFlow: Flow<ThemeMode> = themeStore.themeMode

    val uiState: StateFlow<ApiManagerUiState> = combine(
        repository.observeEndpoints(),
        lockManager.lockSettings,
        themeFlow,
        interactionState
    ) { endpoints, lockSettings, themeMode, ui ->
        val validIds = endpoints.mapTo(HashSet()) { it.id }
        val activeRevealed = ui.revealedKeys.filterKeys { it in validIds }
        val query = ui.searchQuery.trim()
        val filtered = endpoints.filter { item ->
            val matchesProvider = ui.selectedProviderFilter == null ||
                item.providerType == ui.selectedProviderFilter
            val matchesQuery = query.isEmpty() ||
                item.name.contains(query, ignoreCase = true) ||
                item.baseUrl.contains(query, ignoreCase = true) ||
                item.providerType.displayName.contains(query, ignoreCase = true)
            matchesProvider && matchesQuery
        }
        val duplicates = endpoints.groupingBy { repository.normalizeUrl(it.baseUrl) }.eachCount()
            .filterValues { it > 1 }

        ApiManagerUiState(
            allEndpoints = endpoints,
            filteredEndpoints = filtered,
            revealedKeys = activeRevealed,
            selectedProviderFilter = ui.selectedProviderFilter,
            searchQuery = ui.searchQuery,
            duplicateUrls = duplicates,
            isAddEditDialogOpen = ui.isAddEditDialogOpen,
            editingEndpoint = ui.editingEndpoint,
            editingPlainKey = ui.editingPlainKey,
            isPinSetupDialogOpen = ui.isPinSetupDialogOpen,
            deleteTarget = ui.deleteTarget,
            snackbarMessageRes = ui.snackbarMessageRes,
            snackbarText = ui.snackbarText,
            isLoading = false,
            lockSettings = lockSettings,
            isLockInitialized = ui.isLockInitialized,
            isLocked = ui.isLocked,
            lockSessionId = ui.lockSessionId,
            pinErrorRes = ui.pinErrorRes,
            cooldownSeconds = ui.cooldownSeconds,
            biometricErrorMessage = ui.biometricErrorMessage,
            themeMode = themeMode,
            currentScreen = ui.currentScreen,
            isSyncBusy = ui.isSyncBusy,
            qrChunks = ui.qrChunks,
            qrIndex = ui.qrIndex,
            syncUrl = ui.syncUrl,
            syncMessage = ui.syncMessage,
            transferCode = ui.transferCode,
            transferPassphraseInput = ui.transferPassphraseInput
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = ApiManagerUiState(isLoading = true)
    )

    // --- Appearance & navigation ---

    fun onThemeModeSelected(mode: ThemeMode) {
        viewModelScope.launch { themeStore.setThemeMode(mode) }
    }

    fun onOpenSettings() {
        interactionState.update { it.copy(currentScreen = AppScreen.SETTINGS) }
    }

    fun onOpenAbout() {
        interactionState.update { it.copy(currentScreen = AppScreen.ABOUT) }
    }

    fun onOpenVault() {
        interactionState.update { it.copy(currentScreen = AppScreen.VAULT) }
    }

    fun onBack() {
        if (interactionState.value.currentScreen == AppScreen.LAN_SEND) {
            localSyncServer?.stop()
            localSyncServer = null
        }
        interactionState.update {
            if (it.currentScreen == AppScreen.VAULT) {
                it
            } else {
                it.copy(
                    currentScreen = AppScreen.VAULT,
                    syncUrl = null,
                    syncMessage = null,
                    transferCode = null
                )
            }
        }
    }

    // --- App Lock & Biometrics ---

    fun setExternalAuthInProgress(inProgress: Boolean) {
        if (inProgress) {
            suppressAutoLockDepth++
        } else if (suppressAutoLockDepth > 0) {
            suppressAutoLockDepth--
        }
    }

    fun onAppBackgrounded() {
        if (suppressAutoLockDepth > 0) return
        if (uiState.value.lockSettings.mode.isEnabled) lockVaultNow()
    }

    fun lockVaultNow() {
        interactionState.update {
            it.copy(
                isLocked = true,
                lockSessionId = it.lockSessionId + 1,
                revealedKeys = emptyMap(),
                pinErrorRes = null,
                biometricErrorMessage = null,
                currentScreen = AppScreen.VAULT
            )
        }
    }

    fun onPinAttempt(pin: String) {
        if (interactionState.value.cooldownSeconds > 0) return
        viewModelScope.launch {
            val valid = lockManager.verifyPin(pin)
            if (valid) {
                cooldownJob?.cancel()
                interactionState.update {
                    it.copy(
                        isLocked = false,
                        pinErrorRes = null,
                        failedPinAttempts = 0,
                        cooldownSeconds = 0,
                        biometricErrorMessage = null
                    )
                }
            } else {
                val failures = interactionState.value.failedPinAttempts + 1
                if (failures >= MAX_PIN_ATTEMPTS) {
                    startCooldown()
                } else {
                    interactionState.update {
                        it.copy(
                            failedPinAttempts = failures,
                            pinErrorRes = R.string.lock_error_wrong_pin
                        )
                    }
                }
            }
        }
    }

    private fun startCooldown() {
        cooldownJob?.cancel()
        cooldownJob = viewModelScope.launch {
            for (remaining in COOLDOWN_SECONDS downTo 1) {
                interactionState.update {
                    it.copy(
                        failedPinAttempts = MAX_PIN_ATTEMPTS,
                        cooldownSeconds = remaining,
                        pinErrorRes = R.string.lock_error_wrong_pin
                    )
                }
                delay(1000L)
            }
            interactionState.update {
                it.copy(
                    failedPinAttempts = 0,
                    cooldownSeconds = 0,
                    pinErrorRes = null
                )
            }
        }
    }

    fun onBiometricSuccess() {
        cooldownJob?.cancel()
        interactionState.update {
            it.copy(
                isLocked = false,
                pinErrorRes = null,
                failedPinAttempts = 0,
                cooldownSeconds = 0,
                biometricErrorMessage = null
            )
        }
    }

    fun onBiometricError(message: String?) {
        interactionState.update { it.copy(biometricErrorMessage = message) }
    }

    fun onSelectLockMode(targetMode: VaultLockMode) {
        viewModelScope.launch {
            val currentSettings = lockManager.getCurrentSettings()
            if (targetMode.requiresPinOption && !currentSettings.hasPin) {
                interactionState.update {
                    it.copy(
                        isPinSetupDialogOpen = true,
                        pendingModeAfterPinSetup = targetMode
                    )
                }
            } else {
                lockManager.setLockMode(targetMode)
                interactionState.update {
                    it.copy(snackbarMessageRes = R.string.snackbar_lock_updated)
                }
            }
        }
    }

    fun onRequestChangePin() {
        interactionState.update {
            it.copy(isPinSetupDialogOpen = true, pendingModeAfterPinSetup = null)
        }
    }

    fun onDismissPinSetupDialog() {
        interactionState.update {
            it.copy(isPinSetupDialogOpen = false, pendingModeAfterPinSetup = null)
        }
    }

    fun onPinSetupConfirmed(pin: String) {
        val pendingMode = interactionState.value.pendingModeAfterPinSetup
        viewModelScope.launch {
            lockManager.setPin(pin)
                .onSuccess {
                    if (pendingMode != null) lockManager.setLockMode(pendingMode)
                    interactionState.update {
                        it.copy(
                            isPinSetupDialogOpen = false,
                            pendingModeAfterPinSetup = null,
                            snackbarMessageRes = R.string.snackbar_lock_updated
                        )
                    }
                }
                .onFailure {
                    interactionState.update { it.copy(snackbarMessageRes = R.string.snackbar_crypto_error) }
                }
        }
    }

    // --- Endpoints & filtering ---

    fun onFilterSelected(providerType: ProviderType?) {
        interactionState.update { it.copy(selectedProviderFilter = providerType) }
    }

    fun onSearchQueryChanged(query: String) {
        interactionState.update { it.copy(searchQuery = query) }
    }

    fun onToggleKeyVisibility(item: ApiEndpointItem) {
        val currentlyRevealed = interactionState.value.revealedKeys.containsKey(item.id)
        if (currentlyRevealed) {
            interactionState.update { state -> state.copy(revealedKeys = state.revealedKeys - item.id) }
        } else {
            viewModelScope.launch {
                repository.decryptApiKey(item)
                    .onSuccess { decrypted ->
                        interactionState.update { state ->
                            state.copy(revealedKeys = state.revealedKeys + (item.id to decrypted))
                        }
                    }
                    .onFailure {
                        interactionState.update { it.copy(snackbarMessageRes = R.string.snackbar_crypto_error) }
                    }
            }
        }
    }

    fun onCopyApiKey(item: ApiEndpointItem, onReadyToCopy: (String) -> Unit) {
        val cached = interactionState.value.revealedKeys[item.id]
        if (cached != null) {
            onReadyToCopy(cached)
            interactionState.update { it.copy(snackbarMessageRes = R.string.snackbar_copied) }
            return
        }
        viewModelScope.launch {
            repository.decryptApiKey(item)
                .onSuccess { decrypted ->
                    onReadyToCopy(decrypted)
                    interactionState.update { it.copy(snackbarMessageRes = R.string.snackbar_copied) }
                }
                .onFailure {
                    interactionState.update { it.copy(snackbarMessageRes = R.string.snackbar_crypto_error) }
                }
        }
    }

    fun onOpenAddDialog() {
        interactionState.update {
            it.copy(isAddEditDialogOpen = true, editingEndpoint = null, editingPlainKey = "")
        }
    }

    fun onOpenEditDialog(item: ApiEndpointItem) {
        viewModelScope.launch {
            val cached = interactionState.value.revealedKeys[item.id]
            val result = if (cached != null) Result.success(cached) else repository.decryptApiKey(item)
            result.onSuccess { decrypted ->
                interactionState.update {
                    it.copy(
                        isAddEditDialogOpen = true,
                        editingEndpoint = item,
                        editingPlainKey = decrypted
                    )
                }
            }.onFailure {
                interactionState.update { it.copy(snackbarMessageRes = R.string.snackbar_crypto_error) }
            }
        }
    }

    fun onDismissAddEditDialog() {
        interactionState.update {
            it.copy(isAddEditDialogOpen = false, editingEndpoint = null, editingPlainKey = "")
        }
    }

    fun onSaveEndpoint(
        name: String,
        baseUrl: String,
        plainApiKey: String,
        providerType: ProviderType,
        expiresAtMillis: Long?
    ) {
        val editing = interactionState.value.editingEndpoint
        viewModelScope.launch {
            val result = if (editing == null) {
                repository.addEndpoint(name, baseUrl, plainApiKey, providerType, expiresAtMillis)
            } else {
                repository.updateEndpoint(editing.id, name, baseUrl, plainApiKey, providerType, expiresAtMillis)
            }
            result.onSuccess {
                interactionState.update {
                    it.copy(
                        isAddEditDialogOpen = false,
                        editingEndpoint = null,
                        editingPlainKey = "",
                        revealedKeys = if (editing != null) it.revealedKeys - editing.id else it.revealedKeys,
                        snackbarMessageRes = if (editing == null) {
                            R.string.snackbar_saved
                        } else {
                            R.string.snackbar_updated
                        }
                    )
                }
            }.onFailure {
                interactionState.update { it.copy(snackbarMessageRes = R.string.snackbar_crypto_error) }
            }
        }
    }

    fun onRequestDelete(item: ApiEndpointItem) {
        interactionState.update { it.copy(deleteTarget = item) }
    }

    fun onDismissDelete() {
        interactionState.update { it.copy(deleteTarget = null) }
    }

    fun onConfirmDelete() {
        val target = interactionState.value.deleteTarget ?: return
        viewModelScope.launch {
            repository.deleteEndpoint(target.id).onSuccess {
                interactionState.update {
                    it.copy(
                        deleteTarget = null,
                        revealedKeys = it.revealedKeys - target.id,
                        snackbarMessageRes = R.string.snackbar_deleted
                    )
                }
            }
        }
    }

    fun onDismissSnackbar() {
        interactionState.update { it.copy(snackbarMessageRes = null, snackbarText = null) }
    }

    // --- Local backup ---

    fun onExportBackup(uri: Uri) {
        val manager = backupManager
        if (manager == null) {
            interactionState.update { it.copy(snackbarText = "Backup is unavailable") }
            return
        }
        viewModelScope.launch {
            interactionState.update { it.copy(isSyncBusy = true) }
            runCatching {
                val entities = repository.getEntitiesForBackup()
                manager.exportTo(uri, manager.buildEnvelope(entities)).getOrThrow()
            }.onSuccess {
                interactionState.update {
                    it.copy(isSyncBusy = false, snackbarMessageRes = R.string.snackbar_backup_exported)
                }
            }.onFailure { error ->
                interactionState.update {
                    it.copy(isSyncBusy = false, snackbarText = error.message ?: "Export failed")
                }
            }
        }
    }

    fun onImportBackup(uri: Uri) {
        val manager = backupManager
        if (manager == null) {
            interactionState.update { it.copy(snackbarText = "Backup is unavailable") }
            return
        }
        viewModelScope.launch {
            interactionState.update { it.copy(isSyncBusy = true) }
            runCatching {
                val envelope = manager.importFrom(uri).getOrThrow()
                repository.importEntities(manager.toEntities(envelope)).getOrThrow()
            }.onSuccess { count ->
                interactionState.update {
                    it.copy(
                        isSyncBusy = false,
                        snackbarMessageRes = R.string.snackbar_backup_imported,
                        snackbarText = null,
                        syncMessage = "Restored $count endpoint(s)"
                    )
                }
            }.onFailure { error ->
                interactionState.update {
                    it.copy(isSyncBusy = false, snackbarText = error.message ?: "Import failed")
                }
            }
        }
    }

    // --- QR / LAN device-to-device sync ---

    private suspend fun currentQrPayload(code: String): Result<String> = runCatching {
        val manager = checkNotNull(backupManager) { "Backup is unavailable" }
        val envelope = manager.buildPortableEnvelope(repository.getEntitiesForBackup(), code)
            .getOrThrow()
        manager.toJson(envelope)
    }

    fun onStartQrSend() {
        viewModelScope.launch {
            interactionState.update { it.copy(isSyncBusy = true, currentScreen = AppScreen.QR_SEND) }
            val code = BackupCrypto.generateCode()
            currentQrPayload(code)
                .onSuccess { json ->
                    val chunks = QrBackupCodec.encodeChunks(json)
                    interactionState.update {
                        it.copy(
                            isSyncBusy = false,
                            qrChunks = chunks,
                            qrIndex = 0,
                            syncMessage = null,
                            transferCode = code
                        )
                    }
                }
                .onFailure { error ->
                    interactionState.update {
                        it.copy(
                            isSyncBusy = false,
                            snackbarText = error.message ?: "Backup is unavailable"
                        )
                    }
                }
        }
    }

    fun onQrPageChanged(index: Int) {
        val count = interactionState.value.qrChunks.size
        if (count == 0) return
        interactionState.update {
            it.copy(qrIndex = index.coerceIn(0, count - 1))
        }
    }

    fun onStartQrReceive() {
        receivedQrParts.clear()
        interactionState.update {
            it.copy(
                currentScreen = AppScreen.QR_RECEIVE,
                syncMessage = "Scan all QR codes, then tap Assemble",
                transferPassphraseInput = "",
                transferCode = null
            )
        }
    }

    fun onTransferPassphraseChanged(value: String) {
        interactionState.update { it.copy(transferPassphraseInput = value) }
    }

    private val receivedQrParts = LinkedHashMap<Int, QrBackupCodec.Part>()

    fun onQrScanned(value: String) {
        val part = QrBackupCodec.decodePart(value) ?: run {
            interactionState.update { it.copy(syncMessage = "That is not an API Manager sync code") }
            return
        }
        receivedQrParts[part.index] = part
        interactionState.update {
            it.copy(syncMessage = "Scanned ${receivedQrParts.size}/${part.total} QR codes")
        }
    }

    fun onAssembleQrBackup() {
        val manager = backupManager
        if (manager == null) {
            interactionState.update { it.copy(syncMessage = "Backup is unavailable") }
            return
        }
        val code = interactionState.value.transferPassphraseInput.trim()
        if (code.isBlank()) {
            interactionState.update { it.copy(syncMessage = "Enter the transfer code shown by the sender") }
            return
        }
        viewModelScope.launch {
            interactionState.update { it.copy(isSyncBusy = true) }
            runCatching {
                val json = QrBackupCodec.assemble(receivedQrParts.values.toList()).getOrThrow()
                val envelope = manager.fromJson(json).getOrThrow()
                val entities = manager.fromPortableEnvelope(envelope, code).getOrThrow()
                repository.importEntities(entities).getOrThrow()
            }.onSuccess { count ->
                receivedQrParts.clear()
                interactionState.update {
                    it.copy(
                        isSyncBusy = false,
                        currentScreen = AppScreen.VAULT,
                        snackbarText = "Imported $count endpoint(s) from QR codes",
                        transferPassphraseInput = ""
                    )
                }
            }.onFailure { error ->
                interactionState.update {
                    it.copy(isSyncBusy = false, syncMessage = error.message ?: "Invalid QR backup")
                }
            }
        }
    }

    fun onStartLanSend() {
        viewModelScope.launch {
            interactionState.update { it.copy(isSyncBusy = true, currentScreen = AppScreen.LAN_SEND) }
            val manager = backupManager
            if (manager == null) {
                interactionState.update {
                    it.copy(isSyncBusy = false, syncMessage = "Backup is unavailable")
                }
                return@launch
            }
            val code = BackupCrypto.generateCode()
            runCatching {
                val json = currentQrPayload(code).getOrThrow()
                val envelope = manager.fromJson(json).getOrThrow()
                val bytes = manager.toZipBytes(envelope)
                localSyncServer?.stop()
                val server = LocalSyncServer(bytes, viewModelScope)
                localSyncServer = server
                server.start()
            }.onSuccess { url ->
                interactionState.update {
                    it.copy(
                        isSyncBusy = false,
                        syncUrl = url,
                        syncMessage = null,
                        transferCode = code
                    )
                }
            }.onFailure { error ->
                localSyncServer?.stop()
                localSyncServer = null
                interactionState.update {
                    it.copy(isSyncBusy = false, syncMessage = error.message ?: "Could not start local transfer")
                }
            }
        }
    }

    fun onStartLanReceive() {
        interactionState.update {
            it.copy(
                currentScreen = AppScreen.LAN_RECEIVE,
                syncMessage = "Scan the sender's QR code and enter the transfer code",
                transferPassphraseInput = "",
                transferCode = null
            )
        }
    }

    fun onLanAddressEntered(value: String) {
        val manager = backupManager
        if (manager == null) {
            interactionState.update { it.copy(syncMessage = "Backup is unavailable") }
            return
        }
        val code = interactionState.value.transferPassphraseInput.trim()
        if (code.isBlank()) {
            interactionState.update { it.copy(syncMessage = "Enter the transfer code shown by the sender") }
            return
        }
        viewModelScope.launch {
            interactionState.update { it.copy(isSyncBusy = true, syncMessage = "Downloading…") }
            runCatching {
                val bytes = LocalSyncClient.download(value.trim()).getOrThrow()
                val envelope = manager.fromZipBytes(bytes).getOrThrow()
                val entities = manager.fromPortableEnvelope(envelope, code).getOrThrow()
                repository.importEntities(entities).getOrThrow()
            }.onSuccess { count ->
                interactionState.update {
                    it.copy(
                        isSyncBusy = false,
                        currentScreen = AppScreen.VAULT,
                        snackbarText = "Imported $count endpoint(s) over LAN",
                        transferPassphraseInput = ""
                    )
                }
            }.onFailure { error ->
                interactionState.update {
                    it.copy(isSyncBusy = false, syncMessage = error.message ?: "Connection failed")
                }
            }
        }
    }

    fun onStopLocalSync() {
        localSyncServer?.stop()
        localSyncServer = null
        interactionState.update {
            it.copy(
                syncUrl = null,
                currentScreen = AppScreen.SETTINGS,
                syncMessage = null,
                transferCode = null
            )
        }
    }

    fun showSyncMessage(message: String) {
        interactionState.update { it.copy(syncMessage = message) }
    }

    override fun onCleared() {
        localSyncServer?.stop()
        super.onCleared()
    }

    private companion object {
        const val TAG = "ApiManagerVM"
        const val MAX_PIN_ATTEMPTS = 5
        const val COOLDOWN_SECONDS = 30
    }
}
