package com.example.ui

import android.app.Activity
import android.content.ClipData
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.os.PersistableBundle
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.DeleteForever
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.model.ProviderType
import com.example.data.repository.ApiEndpointItem
import com.example.data.repository.ApiEndpointRepository
import com.example.data.security.BiometricHelper
import com.example.ui.components.AddEditEndpointDialog
import com.example.ui.components.EndpointCard
import com.example.ui.components.LockScreen
import com.example.ui.components.PinSetupDialog
import com.example.ui.components.SettingsSheet
import com.example.ui.components.openOfficialLink

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApiManagerScreen(
    viewModel: ApiManagerViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    val biometricAvailability = remember(uiState.currentScreen) {
        BiometricHelper.checkAvailability(context)
    }

    val snackbarMessage = uiState.snackbarText
        ?: uiState.snackbarMessageRes?.let { stringResource(it) }
    LaunchedEffect(uiState.snackbarMessageRes, uiState.snackbarText) {
        if (snackbarMessage != null) {
            snackbarHostState.showSnackbar(snackbarMessage)
            viewModel.onDismissSnackbar()
        }
    }

    val triggerBiometricPrompt: () -> Unit = {
        val activity = context as? FragmentActivity
        if (activity != null) {
            val negativeLabel = if (uiState.lockSettings.mode.requiresPinOption) {
                context.getString(R.string.biometric_prompt_use_pin)
            } else {
                context.getString(R.string.biometric_prompt_cancel)
            }
            viewModel.setExternalAuthInProgress(true)
            BiometricHelper.showBiometricPrompt(
                activity = activity,
                negativeButtonText = negativeLabel,
                onSuccess = {
                    viewModel.setExternalAuthInProgress(false)
                    viewModel.onBiometricSuccess()
                },
                onErrorOrCancelled = { err ->
                    viewModel.setExternalAuthInProgress(false)
                    viewModel.onBiometricError(err)
                }
            )
        }
    }

    BackHandler(enabled = !uiState.isLocked && uiState.currentScreen != AppScreen.VAULT) {
        viewModel.onBack()
    }

    if (!uiState.isLockInitialized) {
        Surface(
            modifier = modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Box(contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        return
    }

    if (uiState.isLocked) {
        LockScreen(
            lockMode = uiState.lockSettings.mode,
            expectedPinLength = uiState.lockSettings.pinLength,
            lockSessionId = uiState.lockSessionId,
            pinErrorRes = uiState.pinErrorRes,
            cooldownSeconds = uiState.cooldownSeconds,
            biometricErrorMessage = uiState.biometricErrorMessage,
            onPinDigitEntered = viewModel::onPinAttempt,
            onRequestBiometric = triggerBiometricPrompt,
            modifier = modifier
        )
        return
    }

    val vault: @Composable () -> Unit = {
        VaultScreen(
            uiState = uiState,
            viewModel = viewModel,
            snackbarHostState = snackbarHostState,
            modifier = modifier
        )
    }

    when (uiState.currentScreen) {
        AppScreen.VAULT -> vault()

        AppScreen.SETTINGS -> {
            vault()
            SettingsSheet(
                lockSettings = uiState.lockSettings,
                biometricAvailability = biometricAvailability,
                themeMode = uiState.themeMode,
                searchProvider = uiState.searchProvider,
                trashCount = uiState.trashEndpoints.size,
                onSelectLockMode = viewModel::onSelectLockMode,
                onRequestChangePin = viewModel::onRequestChangePin,
                onThemeModeSelected = viewModel::onThemeModeSelected,
                onSearchProviderSelected = viewModel::onSearchProviderSelected,
                onExportBackup = viewModel::onExportBackup,
                onImportBackup = viewModel::onImportBackup,
                onQrSend = viewModel::onStartQrSend,
                onQrReceive = viewModel::onStartQrReceive,
                onLanSend = viewModel::onStartLanSend,
                onLanReceive = viewModel::onStartLanReceive,
                onOpenTrash = viewModel::onOpenTrash,
                onOpenAbout = viewModel::onOpenAbout,
                accentColor = uiState.accentColor,
                onAccentColorSelected = viewModel::onAccentColorSelected,
                isBackupPasswordDialogOpen = uiState.isBackupPasswordDialogOpen,
                backupDialogMode = uiState.backupDialogMode,
                backupPasswordInput = uiState.backupPasswordInput,
                backupPasswordError = uiState.backupPasswordError,
                onBackupPasswordChanged = viewModel::onBackupPasswordChanged,
                onDismissBackupPasswordDialog = viewModel::onDismissBackupPasswordDialog,
                onConfirmBackupPassword = viewModel::onConfirmBackupPassword,
                onDismiss = viewModel::onOpenVault
            )
        }

        AppScreen.ABOUT -> AboutScreen(onBack = viewModel::onBack)

        AppScreen.TRASH -> TrashScreen(
            items = uiState.trashEndpoints,
            onRestore = viewModel::onRestoreFromTrash,
            onDeleteForever = viewModel::onRequestDelete,
            onEmptyTrash = viewModel::onEmptyTrash,
            onBack = viewModel::onBack
        )

        AppScreen.QR_SEND -> QrSendScreen(
            chunks = uiState.qrChunks,
            index = uiState.qrIndex,
            isBusy = uiState.isSyncBusy,
            onPageChanged = viewModel::onQrPageChanged,
            onBack = viewModel::onBack
        )

        AppScreen.QR_RECEIVE -> QrReceiveScreen(
            message = uiState.syncMessage,
            isBusy = uiState.isSyncBusy,
            onScanned = viewModel::onQrScanned,
            onAssemble = viewModel::onAssembleQrBackup,
            onBack = viewModel::onBack
        )

        AppScreen.LAN_SEND -> LanSendScreen(
            url = uiState.syncUrl,
            message = uiState.syncMessage,
            isBusy = uiState.isSyncBusy,
            onStop = viewModel::onStopLocalSync
        )

        AppScreen.LAN_RECEIVE -> LanReceiveScreen(
            message = uiState.syncMessage,
            isBusy = uiState.isSyncBusy,
            onScanned = { viewModel.showSyncMessage("Address captured from QR. Tap Connect.") },
            onConnect = viewModel::onLanAddressEntered,
            onBack = viewModel::onBack
        )
    }

    if (uiState.isAddEditDialogOpen) {
        AddEditEndpointDialog(
            editingEndpoint = uiState.editingEndpoint,
            initialPlainKey = uiState.editingPlainKey,
            onDismiss = viewModel::onDismissAddEditDialog,
            onSave = viewModel::onSaveEndpoint
        )
    }

    if (uiState.isPinSetupDialogOpen) {
        PinSetupDialog(
            onDismiss = viewModel::onDismissPinSetupDialog,
            onPinConfirmed = viewModel::onPinSetupConfirmed
        )
    }

    uiState.deleteTarget?.let { target ->
        val permanently = target.isInTrash
        AlertDialog(
            onDismissRequest = viewModel::onDismissDelete,
            title = {
                Text(
                    stringResource(
                        if (permanently) R.string.delete_forever_dialog_title else R.string.delete_dialog_title
                    )
                )
            },
            text = {
                Text(
                    stringResource(
                        if (permanently) R.string.delete_forever_dialog_message else R.string.delete_dialog_message,
                        target.name
                    )
                )
            },
            confirmButton = {
                Button(
                    onClick = viewModel::onConfirmDelete,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    ),
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("confirm_delete_button")
                ) {
                    Text(
                        stringResource(
                            if (permanently) R.string.action_delete_forever else R.string.action_delete
                        )
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = viewModel::onDismissDelete,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("cancel_delete_button")
                ) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }

    if (uiState.isConfirmEmptyTrashDialogOpen) {
        AlertDialog(
            onDismissRequest = viewModel::onDismissEmptyTrashDialog,
            title = { Text(stringResource(R.string.empty_trash_dialog_title)) },
            text = {
                Text(
                    stringResource(
                        R.string.empty_trash_dialog_message,
                        uiState.trashEndpoints.size
                    )
                )
            },
            confirmButton = {
                Button(
                    onClick = viewModel::onConfirmEmptyTrash,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    ),
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("confirm_empty_trash_button")
                ) {
                    Text(stringResource(R.string.action_empty_trash))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = viewModel::onDismissEmptyTrashDialog,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("cancel_empty_trash_button")
                ) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VaultScreen(
    uiState: ApiManagerUiState,
    viewModel: ApiManagerViewModel,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val anyKeyVisible = uiState.revealedKeys.isNotEmpty()

    // Protect the vault from task-switcher screenshots while a secret is on screen.
    DisposableEffect(anyKeyVisible) {
        val window = (context as? Activity)?.window
        if (window != null) {
            if (anyKeyVisible) {
                window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
            } else {
                window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
            }
        }
        onDispose { window?.clearFlags(WindowManager.LayoutParams.FLAG_SECURE) }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets.safeDrawing,
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Outlined.Shield,
                                    contentDescription = stringResource(R.string.cd_vault_badge_icon),
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = stringResource(R.string.app_name),
                                style = MaterialTheme.typography.titleLarge
                            )
                            Text(
                                text = stringResource(R.string.vault_subtitle),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                },
                actions = {
                    AssistChip(
                        onClick = viewModel::onOpenSettings,
                        label = {
                            Text(
                                text = stringResource(R.string.badge_offline),
                                style = MaterialTheme.typography.labelSmall
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Outlined.Security,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                        ),
                        modifier = Modifier.testTag("topbar_offline_chip")
                    )

                    if (uiState.trashEndpoints.isNotEmpty()) {
                        IconButton(
                            onClick = viewModel::onOpenTrash,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("open_trash_topbar")
                        ) {
                            BadgedBox(
                                badge = {
                                    Badge {
                                        Text(uiState.trashEndpoints.size.toString())
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.DeleteOutline,
                                    contentDescription = stringResource(R.string.cd_open_trash),
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }

                    if (uiState.lockSettings.mode.isEnabled) {
                        IconButton(
                            onClick = viewModel::lockVaultNow,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("lock_now_button")
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Lock,
                                contentDescription = stringResource(R.string.cd_lock_vault_now),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    IconButton(
                        onClick = viewModel::onOpenSettings,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("security_settings_button")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Security,
                            contentDescription = stringResource(R.string.cd_security_sync_settings),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = viewModel::onOpenAddDialog,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier
                    .size(52.dp)
                    .testTag("add_endpoint_fab")
            ) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = stringResource(R.string.cd_add_endpoint)
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 680.dp)
                    .fillMaxSize()
            ) {
                AnimatedVisibility(visible = uiState.allEndpoints.isNotEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    ) {
                        OutlinedTextField(
                            value = uiState.searchQuery,
                            onValueChange = viewModel::onSearchQueryChanged,
                            placeholder = { Text(stringResource(R.string.search_placeholder)) },
                            textStyle = MaterialTheme.typography.bodyMedium,
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Outlined.Search,
                                    contentDescription = null
                                )
                            },
                            trailingIcon = {
                                if (uiState.searchQuery.isNotEmpty()) {
                                    IconButton(
                                        onClick = { viewModel.onSearchQueryChanged("") },
                                        modifier = Modifier
                                            .minimumInteractiveComponentSize()
                                            .testTag("clear_search_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.Close,
                                            contentDescription = stringResource(R.string.cd_clear_search)
                                        )
                                    }
                                }
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 50.dp)
                                .testTag("search_endpoints_input")
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            ExpiryFilter.entries.forEach { filter ->
                                val label = when (filter) {
                                    ExpiryFilter.ALL -> stringResource(R.string.filter_any_expiry)
                                    ExpiryFilter.ACTIVE -> stringResource(R.string.filter_active)
                                    ExpiryFilter.EXPIRED -> stringResource(R.string.filter_expired)
                                }
                                FilterChip(
                                    selected = uiState.selectedExpiryFilter == filter,
                                    onClick = { viewModel.onExpiryFilterSelected(filter) },
                                    label = {
                                        Text(label, style = MaterialTheme.typography.labelMedium)
                                    },
                                    modifier = Modifier.testTag("expiry_filter_${filter.name.lowercase()}")
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            FilterChip(
                                selected = uiState.selectedProviderFilter == null,
                                onClick = { viewModel.onFilterSelected(null) },
                                label = {
                                    Text(
                                        stringResource(R.string.filter_all_providers),
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                },
                                modifier = Modifier.testTag("filter_chip_all")
                            )
                            ProviderType.entries.forEach { provider ->
                                FilterChip(
                                    selected = uiState.selectedProviderFilter == provider,
                                    onClick = {
                                        val next = if (uiState.selectedProviderFilter == provider) {
                                            null
                                        } else {
                                            provider
                                        }
                                        viewModel.onFilterSelected(next)
                                    },
                                    label = {
                                        Text(
                                            provider.displayName,
                                            style = MaterialTheme.typography.labelMedium
                                        )
                                    },
                                    modifier = Modifier.testTag("filter_chip_${provider.name.lowercase()}")
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                    }
                }

                when {
                    uiState.isLoading -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }

                    uiState.allEndpoints.isEmpty() -> {
                        EmptyVaultState(
                            onAddClick = viewModel::onOpenAddDialog,
                            onOpenSettings = viewModel::onOpenSettings,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp)
                        )
                    }

                    uiState.filteredEndpoints.isEmpty() -> {
                        FilteredEmptyState(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp)
                        )
                    }

                    else -> {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .testTag("endpoints_list"),
                            contentPadding = PaddingValues(
                                start = 16.dp,
                                end = 16.dp,
                                top = 8.dp,
                                bottom = 96.dp
                            ),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            itemsIndexed(
                                items = uiState.filteredEndpoints,
                                key = { _, item -> item.id }
                            ) { position, item ->
                                EndpointCard(
                                    item = item,
                                    index = position + 1,
                                    revealedKey = uiState.revealedKeys[item.id],
                                    onToggleReveal = { viewModel.onToggleKeyVisibility(item) },
                                    onCopyKey = {
                                        viewModel.onCopyApiKey(item) { plainKey ->
                                            copySensitiveToClipboard(context, item.name, plainKey)
                                        }
                                    },
                                    onEdit = { viewModel.onOpenEditDialog(item) },
                                    onDelete = { viewModel.onRequestDelete(item) },
                                    onCopyUrl = {
                                        copyPlainTextToClipboard(context, item.baseUrl)
                                        viewModel.onUrlCopied()
                                    },
                                    onOpenOfficialLink = { url ->
                                        val opened = openOfficialLink(context, url)
                                        viewModel.onOfficialLinkResult(opened)
                                    },
                                    isDuplicate = (uiState.duplicateUrls[normalizeUrl(item.baseUrl)] ?: 0) > 1
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun normalizeUrl(url: String): String = url.trim().trimEnd('/').lowercase()

@Composable
private fun EmptyVaultState(
    onAddClick: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.img_vault_empty_1795270997836),
            contentDescription = stringResource(R.string.cd_empty_vault_illustration),
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .widthIn(max = 260.dp)
                .fillMaxWidth(0.72f)
                .clip(RoundedCornerShape(24.dp))
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = stringResource(R.string.empty_state_title),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.empty_state_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(max = 360.dp)
        )
        Spacer(modifier = Modifier.height(20.dp))
        Button(
            onClick = onAddClick,
            modifier = Modifier
                .minimumInteractiveComponentSize()
                .testTag("empty_add_button")
        ) {
            Icon(
                imageVector = Icons.Filled.Add,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(R.string.empty_state_cta))
        }
        Spacer(modifier = Modifier.height(8.dp))
        TextButton(
            onClick = onOpenSettings,
            modifier = Modifier
                .minimumInteractiveComponentSize()
                .testTag("empty_security_sync_button")
        ) {
            Icon(
                imageVector = Icons.Outlined.Security,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(stringResource(R.string.sheet_title))
        }
    }
}

@Composable
private fun FilteredEmptyState(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Outlined.Search,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(48.dp)
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = stringResource(R.string.filtered_empty_title),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = stringResource(R.string.filtered_empty_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TrashScreen(
    items: List<ApiEndpointItem>,
    onRestore: (ApiEndpointItem) -> Unit,
    onDeleteForever: (ApiEndpointItem) -> Unit,
    onEmptyTrash: () -> Unit,
    onBack: () -> Unit
) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets.safeDrawing,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.trash_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_back)
                        )
                    }
                },
                actions = {
                    if (items.isNotEmpty()) {
                        TextButton(
                            onClick = onEmptyTrash,
                            modifier = Modifier.testTag("empty_trash_button")
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.DeleteSweep,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("🗑️ " + stringResource(R.string.action_empty_trash))
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        if (items.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(text = "🗑️", style = MaterialTheme.typography.displayMedium)
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = stringResource(R.string.trash_empty_title),
                    style = MaterialTheme.typography.titleLarge,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.trash_empty_body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .testTag("trash_list"),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Text(
                        text = stringResource(R.string.trash_retention_note),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                items(items = items, key = { it.id }) { item ->
                    TrashItemCard(
                        item = item,
                        onRestore = { onRestore(item) },
                        onDeleteForever = { onDeleteForever(item) }
                    )
                }
            }
        }
    }
}

@Composable
private fun TrashItemCard(
    item: ApiEndpointItem,
    onRestore: () -> Unit,
    onDeleteForever: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("trash_item_${item.id}")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = "🗑️ ${item.name}",
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = item.baseUrl,
                style = com.example.ui.theme.CodeTextStyle,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
            val deletedAt = item.deletedAtMillis ?: item.updatedAt
            val purgeAt = deletedAt + ApiEndpointRepository.TRASH_RETENTION_MILLIS
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = stringResource(
                    R.string.trash_item_deleted_on,
                    DateFormats.formatDate(deletedAt),
                    DateFormats.formatDate(purgeAt)
                ),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(
                    onClick = onRestore,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("restore_item_${item.id}")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Restore,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(stringResource(R.string.action_restore))
                }
                Spacer(modifier = Modifier.width(8.dp))
                TextButton(
                    onClick = onDeleteForever,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("delete_forever_${item.id}")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.DeleteForever,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = stringResource(R.string.action_delete_forever),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

private fun copyPlainTextToClipboard(
    context: Context,
    text: String
) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return
    clipboard.setPrimaryClip(ClipData.newPlainText("Endpoint URL", text))
}

private fun copySensitiveToClipboard(
    context: Context,
    label: String,
    secretValue: String
) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return
    val clip = ClipData.newPlainText(label, secretValue).apply {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            description.extras = PersistableBundle().apply {
                putBoolean(ClipDescription.EXTRA_IS_SENSITIVE, true)
            }
        }
    }
    clipboard.setPrimaryClip(clip)
}
