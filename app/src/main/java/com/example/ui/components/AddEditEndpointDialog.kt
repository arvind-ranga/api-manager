package com.example.ui.components

import android.content.Intent
import android.net.Uri
import android.util.Patterns
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.R
import com.example.data.model.ProviderType
import com.example.data.repository.ApiEndpointItem
import com.example.ui.DateFormats
import com.example.ui.theme.CodeTextStyle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditEndpointDialog(
    editingEndpoint: ApiEndpointItem?,
    initialPlainKey: String,
    onDismiss: () -> Unit,
    onSave: (name: String, baseUrl: String, apiKey: String, providerType: ProviderType, expiresAtMillis: Long?) -> Unit
) {
    val context = LocalContext.current
    val isEditing = editingEndpoint != null
    var name by remember(editingEndpoint) { mutableStateOf(editingEndpoint?.name ?: "") }
    var providerType by remember(editingEndpoint) {
        mutableStateOf(editingEndpoint?.providerType ?: ProviderType.OPENAI)
    }
    var baseUrl by remember(editingEndpoint) {
        mutableStateOf(editingEndpoint?.baseUrl ?: ProviderType.OPENAI.defaultBaseUrl)
    }
    var apiKey by remember(editingEndpoint, initialPlainKey) {
        mutableStateOf(if (isEditing) initialPlainKey else "")
    }
    var isProviderDropdownExpanded by remember { mutableStateOf(false) }
    var isApiKeyVisible by remember { mutableStateOf(false) }

    var expiryText by remember(editingEndpoint) {
        mutableStateOf(editingEndpoint?.expiresAtMillis?.let { DateFormats.formatExpiryDate(it) } ?: "")
    }
    var nameError by remember { mutableStateOf<Int?>(null) }
    var urlError by remember { mutableStateOf<Int?>(null) }
    var keyError by remember { mutableStateOf<Int?>(null) }
    var expiryError by remember { mutableStateOf<Int?>(null) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .padding(horizontal = 20.dp, vertical = 24.dp)
                .widthIn(max = 520.dp)
                .fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            tonalElevation = 6.dp,
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp)
            ) {
                Text(
                    text = stringResource(
                        if (isEditing) R.string.dialog_edit_title else R.string.dialog_add_title
                    ),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.Lock,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.encryption_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        nameError = null
                    },
                    label = { Text(stringResource(R.string.field_name_label)) },
                    placeholder = { Text(stringResource(R.string.field_name_placeholder)) },
                    supportingText = nameError?.let { res ->
                        {
                            Text(stringResource(res))
                        }
                    } ?: {
                        Text(stringResource(R.string.field_name_optional))
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_name")
                )

                Spacer(modifier = Modifier.height(12.dp))

                ExposedDropdownMenuBox(
                    expanded = isProviderDropdownExpanded,
                    onExpandedChange = { isProviderDropdownExpanded = !isProviderDropdownExpanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = providerType.displayName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(R.string.field_provider_label)) },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(
                                expanded = isProviderDropdownExpanded
                            )
                        },
                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                        modifier = Modifier
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                            .fillMaxWidth()
                            .testTag("dropdown_provider")
                    )

                    ExposedDropdownMenu(
                        expanded = isProviderDropdownExpanded,
                        onDismissRequest = { isProviderDropdownExpanded = false }
                    ) {
                        ProviderType.entries.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option.displayName) },
                                onClick = {
                                    val previousDefault = providerType.defaultBaseUrl
                                    providerType = option
                                    if (baseUrl.isBlank() || baseUrl == previousDefault) {
                                        baseUrl = option.defaultBaseUrl
                                        urlError = null
                                    }
                                    isProviderDropdownExpanded = false
                                },
                                modifier = Modifier.testTag("provider_option_${option.name.lowercase()}")
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = baseUrl,
                    onValueChange = {
                        baseUrl = it
                        urlError = null
                    },
                    label = { Text(stringResource(R.string.field_base_url_label)) },
                    placeholder = { Text(stringResource(R.string.field_base_url_placeholder)) },
                    textStyle = CodeTextStyle,
                    singleLine = true,
                    isError = urlError != null,
                    supportingText = urlError?.let { res -> { Text(stringResource(res)) } },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Uri,
                        imeAction = ImeAction.Next
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_base_url")
                )

                TextButton(
                    onClick = {
                        val query = listOf(name.trim(), baseUrl.trim(), "API documentation")
                            .filter { it.isNotBlank() }
                            .joinToString(" ")
                        context.startActivity(
                            Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/search?udm=50&q=" + Uri.encode(query)))
                        )
                    },
                    modifier = Modifier.testTag("search_api_docs_button")
                ) {
                    Text(stringResource(R.string.action_search_docs))
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = expiryText,
                    onValueChange = {
                        expiryText = it
                        expiryError = null
                    },
                    label = { Text(stringResource(R.string.field_expiry_label)) },
                    placeholder = { Text(stringResource(R.string.field_expiry_placeholder)) },
                    singleLine = true,
                    isError = expiryError != null,
                    supportingText = {
                        Text(expiryError?.let { stringResource(it) } ?: stringResource(R.string.field_expiry_hint))
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Next
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_expiry")
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = apiKey,
                    onValueChange = {
                        apiKey = it
                        keyError = null
                    },
                    label = { Text(stringResource(R.string.field_api_key_label)) },
                    placeholder = { Text(stringResource(R.string.field_api_key_placeholder)) },
                    textStyle = CodeTextStyle,
                    singleLine = true,
                    visualTransformation = if (isApiKeyVisible) {
                        VisualTransformation.None
                    } else {
                        PasswordVisualTransformation()
                    },
                    trailingIcon = {
                        IconButton(
                            onClick = { isApiKeyVisible = !isApiKeyVisible },
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("dialog_toggle_key_visibility")
                        ) {
                            Icon(
                                imageVector = if (isApiKeyVisible) {
                                    Icons.Outlined.VisibilityOff
                                } else {
                                    Icons.Outlined.Visibility
                                },
                                contentDescription = if (isApiKeyVisible) {
                                    stringResource(R.string.cd_hide_key)
                                } else {
                                    stringResource(R.string.cd_reveal_key)
                                }
                            )
                        }
                    },
                    isError = keyError != null,
                    supportingText = keyError?.let { res -> { Text(stringResource(res)) } },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_api_key")
                )

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("dialog_cancel_button")
                    ) {
                        Text(stringResource(R.string.action_cancel))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val trimmedName = name.trim()
                            val trimmedUrl = baseUrl.trim()
                            val trimmedKey = apiKey.trim()

                            var hasError = false
                            if (trimmedName.isEmpty()) {
                                nameError = R.string.error_name_required
                                hasError = true
                            }
                            if (!isValidHttpUrl(trimmedUrl)) {
                                urlError = R.string.error_url_invalid
                                hasError = true
                            }
                            if (trimmedKey.isEmpty()) {
                                keyError = R.string.error_key_required
                                hasError = true
                            }
                            val expiresAt = if (expiryText.isBlank()) {
                                null
                            } else {
                                DateFormats.parseExpiryDate(expiryText.trim()).also {
                                    if (it == null) {
                                        expiryError = R.string.error_expiry_invalid
                                        hasError = true
                                    }
                                }
                            }
                            if (!hasError) {
                                onSave(trimmedName, trimmedUrl, trimmedKey, providerType, expiresAt)
                            }
                        },
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("dialog_save_button")
                    ) {
                        Text(
                            stringResource(
                                if (isEditing) R.string.action_update else R.string.action_save
                            )
                        )
                    }
                }
            }
        }
    }
}

internal fun isValidHttpUrl(url: String): Boolean {
    if (url.isBlank()) return false
    val lower = url.lowercase()
    if (!lower.startsWith("https://") && !lower.startsWith("http://")) return false
    return Patterns.WEB_URL.matcher(url).matches()
}
