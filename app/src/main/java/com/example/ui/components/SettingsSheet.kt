package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Backup
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.LockOpen
import androidx.compose.material.icons.outlined.Pin
import androidx.compose.material.icons.outlined.QrCode
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.security.BiometricAvailability
import com.example.data.security.VaultLockMode
import com.example.data.security.VaultLockSettings
import com.example.data.settings.ThemeMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsSheet(
    lockSettings: VaultLockSettings,
    biometricAvailability: BiometricAvailability,
    themeMode: ThemeMode,
    onSelectLockMode: (VaultLockMode) -> Unit,
    onRequestChangePin: () -> Unit,
    onThemeModeSelected: (ThemeMode) -> Unit,
    onExportBackup: (Uri) -> Unit,
    onImportBackup: (Uri) -> Unit,
    onQrSend: () -> Unit,
    onQrReceive: () -> Unit,
    onLanSend: () -> Unit,
    onLanReceive: () -> Unit,
    onOpenAbout: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val biometricReady = biometricAvailability == BiometricAvailability.AVAILABLE
    var searchQuery by remember { mutableStateOf("") }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/zip")
    ) { uri -> if (uri != null) onExportBackup(uri) }
    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri -> if (uri != null) onImportBackup(uri) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = stringResource(R.string.sheet_title),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(20.dp))
            SectionTitle(stringResource(R.string.section_app_lock))
            Spacer(modifier = Modifier.height(8.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                LockOptionCard(
                    title = stringResource(R.string.lock_mode_off_title),
                    description = stringResource(R.string.lock_mode_off_desc),
                    icon = Icons.Outlined.LockOpen,
                    selected = lockSettings.mode == VaultLockMode.OFF,
                    enabled = true,
                    testTag = "lock_option_off",
                    onClick = { onSelectLockMode(VaultLockMode.OFF) }
                )
                LockOptionCard(
                    title = stringResource(R.string.lock_mode_pin_title),
                    description = stringResource(R.string.lock_mode_pin_desc),
                    icon = Icons.Outlined.Pin,
                    selected = lockSettings.mode == VaultLockMode.PIN_ONLY,
                    enabled = true,
                    testTag = "lock_option_pin_only",
                    onClick = { onSelectLockMode(VaultLockMode.PIN_ONLY) }
                )
                LockOptionCard(
                    title = stringResource(R.string.lock_mode_bio_title),
                    description = if (biometricReady) {
                        stringResource(R.string.lock_mode_bio_desc)
                    } else {
                        stringResource(R.string.biometric_unavailable_note)
                    },
                    icon = Icons.Outlined.Fingerprint,
                    selected = lockSettings.mode == VaultLockMode.BIOMETRIC_ONLY,
                    enabled = biometricReady,
                    testTag = "lock_option_biometric_only",
                    onClick = { onSelectLockMode(VaultLockMode.BIOMETRIC_ONLY) }
                )
                LockOptionCard(
                    title = stringResource(R.string.lock_mode_both_title),
                    description = if (biometricReady) {
                        stringResource(R.string.lock_mode_both_desc)
                    } else {
                        stringResource(R.string.biometric_unavailable_note)
                    },
                    icon = Icons.Outlined.VerifiedUser,
                    selected = lockSettings.mode == VaultLockMode.PIN_AND_BIOMETRIC,
                    enabled = biometricReady,
                    testTag = "lock_option_pin_and_biometric",
                    onClick = { onSelectLockMode(VaultLockMode.PIN_AND_BIOMETRIC) }
                )
                if (lockSettings.hasPin) {
                    TextButton(
                        onClick = onRequestChangePin,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("change_pin_button")
                    ) {
                        Text(stringResource(R.string.action_change_pin))
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(20.dp))

            SectionTitle(stringResource(R.string.section_appearance))
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                ThemeMode.entries.forEach { mode ->
                    val label = when (mode) {
                        ThemeMode.LIGHT -> stringResource(R.string.theme_light)
                        ThemeMode.DARK -> stringResource(R.string.theme_dark)
                        ThemeMode.AUTO -> stringResource(R.string.theme_auto)
                    }
                    Surface(
                        onClick = { onThemeModeSelected(mode) },
                        shape = RoundedCornerShape(12.dp),
                        color = if (themeMode == mode) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.surface
                        },
                        border = BorderStroke(
                            1.dp,
                            if (themeMode == mode) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
                            }
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("theme_option_${mode.name.lowercase()}")
                    ) {
                        Row(
                            modifier = Modifier.padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = themeMode == mode, onClick = null)
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(label, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(20.dp))

            SectionTitle(stringResource(R.string.section_backup))
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = stringResource(R.string.backup_desc),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = { exportLauncher.launch("api-manager-backup.zip") },
                    modifier = Modifier
                        .weight(1f)
                        .minimumInteractiveComponentSize()
                        .testTag("export_backup_button")
                ) {
                    Icon(Icons.Outlined.Backup, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(stringResource(R.string.action_export_backup))
                }
                OutlinedButton(
                    onClick = { importLauncher.launch(arrayOf("application/zip", "application/octet-stream")) },
                    modifier = Modifier
                        .weight(1f)
                        .minimumInteractiveComponentSize()
                        .testTag("import_backup_button")
                ) {
                    Text(stringResource(R.string.action_import_backup))
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.sync_desc),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ElevatedButton(
                    onClick = onQrSend,
                    modifier = Modifier
                        .weight(1f)
                        .minimumInteractiveComponentSize()
                        .testTag("qr_send_button")
                ) {
                    Icon(Icons.Outlined.QrCode, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(stringResource(R.string.action_qr_send))
                }
                ElevatedButton(
                    onClick = onQrReceive,
                    modifier = Modifier
                        .weight(1f)
                        .minimumInteractiveComponentSize()
                        .testTag("qr_receive_button")
                ) {
                    Text(stringResource(R.string.action_qr_receive))
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = onLanSend,
                    modifier = Modifier
                        .weight(1f)
                        .minimumInteractiveComponentSize()
                        .testTag("lan_send_button")
                ) {
                    Text(stringResource(R.string.action_lan_send))
                }
                OutlinedButton(
                    onClick = onLanReceive,
                    modifier = Modifier
                        .weight(1f)
                        .minimumInteractiveComponentSize()
                        .testTag("lan_receive_button")
                ) {
                    Text(stringResource(R.string.action_lan_receive))
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(20.dp))

            SectionTitle(stringResource(R.string.section_discover))
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = stringResource(R.string.discover_desc),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(10.dp))
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text(stringResource(R.string.search_hint)) },
                leadingIcon = {
                    Icon(Icons.Outlined.Search, contentDescription = null)
                },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("web_search_input")
            )
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = {
                    val query = searchQuery.ifBlank { "LLM API documentation" }
                    context.openUrl("https://duckduckgo.com/?q=" + Uri.encode(query))
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .minimumInteractiveComponentSize()
                    .testTag("web_search_button")
            ) {
                Text(stringResource(R.string.action_search_web))
            }

            Spacer(modifier = Modifier.height(20.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(12.dp))
            TextButton(
                onClick = onOpenAbout,
                modifier = Modifier
                    .fillMaxWidth()
                    .minimumInteractiveComponentSize()
                    .testTag("about_button")
            ) {
                Icon(Icons.Outlined.Info, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.action_about))
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary
    )
}

@Composable
private fun LockOptionCard(
    title: String,
    description: String,
    icon: ImageVector,
    selected: Boolean,
    enabled: Boolean,
    testTag: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(14.dp),
        color = if (selected) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
        } else {
            MaterialTheme.colorScheme.surface
        },
        border = BorderStroke(
            width = if (selected) 1.5.dp else 1.dp,
            color = if (selected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
            }
        ),
        modifier = Modifier
            .fillMaxWidth()
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (enabled) {
                    if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
                }
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall)
                Text(
                    description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            RadioButton(selected = selected, onClick = null, enabled = enabled)
        }
    }
}

internal fun Context.openUrl(url: String) {
    runCatching {
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}
