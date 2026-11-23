package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Http
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.LockOpen
import androidx.compose.material.icons.outlined.Psychology
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.ProviderType
import com.example.data.repository.ApiEndpointItem
import com.example.ui.DateFormats
import com.example.ui.theme.AnthropicAccentDark
import com.example.ui.theme.AnthropicAccentLight
import com.example.ui.theme.CodeTextStyle
import com.example.ui.theme.CustomAccentDark
import com.example.ui.theme.CustomAccentLight
import com.example.ui.theme.GenericAccentDark
import com.example.ui.theme.GenericAccentLight
import com.example.ui.theme.OpenAiAccentDark
import com.example.ui.theme.OpenAiAccentLight

@Composable
fun EndpointCard(
    item: ApiEndpointItem,
    revealedKey: String?,
    onToggleReveal: () -> Unit,
    onCopyKey: () -> Unit,
    onCopyUrl: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    isDuplicate: Boolean = false,
    modifier: Modifier = Modifier
) {
    val isRevealed = revealedKey != null
    val providerColor = providerAccentColor(item.providerType)
    val providerIcon = providerIcon(item.providerType)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("endpoint_card_${item.id}"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.elevatedCardColors(),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ProviderBadge(
                        providerType = item.providerType,
                        accentColor = providerColor,
                        icon = providerIcon
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = item.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.testTag("endpoint_name_${item.id}")
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("edit_endpoint_${item.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Edit,
                            contentDescription = stringResource(R.string.cd_edit_endpoint),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("delete_endpoint_${item.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.DeleteOutline,
                            contentDescription = stringResource(R.string.cd_delete_endpoint),
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Outlined.Link,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = item.baseUrl,
                    style = CodeTextStyle,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("endpoint_url_${item.id}")
                )
                IconButton(
                    onClick = onCopyUrl,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("copy_url_${item.id}")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.ContentCopy,
                        contentDescription = stringResource(R.string.cd_copy_url),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isExpired(item.expiresAtMillis)) {
                        MaterialTheme.colorScheme.errorContainer
                    } else {
                        MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f)
                    }
                ) {
                    Text(
                        text = expiryLabel(item.expiresAtMillis),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isExpired(item.expiresAtMillis)) {
                            MaterialTheme.colorScheme.onErrorContainer
                        } else {
                            MaterialTheme.colorScheme.onSecondaryContainer
                        },
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
                if (isDuplicate) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.7f)
                    ) {
                        Text(
                            text = stringResource(R.string.badge_duplicate),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                border = BorderStroke(
                    width = 1.dp,
                    color = if (isRevealed) {
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
                    } else {
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                    }
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 12.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (isRevealed) Icons.Outlined.LockOpen else Icons.Outlined.Key,
                        contentDescription = null,
                        tint = if (isRevealed) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    AnimatedContent(
                        targetState = revealedKey ?: stringResource(R.string.masked_api_key),
                        transitionSpec = { fadeIn() togetherWith fadeOut() },
                        label = "api_key_mask_transition",
                        modifier = Modifier.weight(1f)
                    ) { displayKey ->
                        Text(
                            text = displayKey,
                            style = CodeTextStyle.copy(
                                fontWeight = if (isRevealed) FontWeight.Medium else FontWeight.Normal
                            ),
                            color = if (isRevealed) {
                                MaterialTheme.colorScheme.onSurface
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.testTag("endpoint_key_text_${item.id}")
                        )
                    }
                    IconButton(
                        onClick = onToggleReveal,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("toggle_key_${item.id}")
                    ) {
                        Icon(
                            imageVector = if (isRevealed) {
                                Icons.Outlined.VisibilityOff
                            } else {
                                Icons.Outlined.Visibility
                            },
                            contentDescription = if (isRevealed) {
                                stringResource(R.string.cd_hide_key)
                            } else {
                                stringResource(R.string.cd_reveal_key)
                            },
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(
                        onClick = onCopyKey,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("copy_key_${item.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.ContentCopy,
                            contentDescription = stringResource(R.string.cd_copy_key),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.Lock,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = stringResource(R.string.badge_aes_gcm),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Text(
                    text = formatTimestamp(item.updatedAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ProviderBadge(
    providerType: ProviderType,
    accentColor: Color,
    icon: ImageVector
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = accentColor.copy(alpha = 0.14f),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.35f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = stringResource(R.string.cd_provider_icon),
                tint = accentColor,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = providerType.displayName,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                color = accentColor
            )
        }
    }
}

@Composable
private fun providerAccentColor(providerType: ProviderType): Color {
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    return when (providerType) {
        ProviderType.OPENAI -> if (isDark) OpenAiAccentDark else OpenAiAccentLight
        ProviderType.ANTHROPIC -> if (isDark) AnthropicAccentDark else AnthropicAccentLight
        ProviderType.GENERIC -> if (isDark) GenericAccentDark else GenericAccentLight
        ProviderType.CUSTOM -> if (isDark) CustomAccentDark else CustomAccentLight
    }
}

private fun providerIcon(providerType: ProviderType): ImageVector {
    return when (providerType) {
        ProviderType.OPENAI -> Icons.Outlined.AutoAwesome
        ProviderType.ANTHROPIC -> Icons.Outlined.Psychology
        ProviderType.GENERIC -> Icons.Outlined.Http
        ProviderType.CUSTOM -> Icons.Outlined.Tune
    }
}

private fun formatTimestamp(epochMillis: Long): String = DateFormats.formatDateTime(epochMillis)

@Composable
private fun expiryLabel(expiresAtMillis: Long?): String {
    if (expiresAtMillis == null) return stringResource(R.string.expiry_never)
    return stringResource(R.string.expiry_on, DateFormats.formatExpiryDate(expiresAtMillis))
}

private fun isExpired(expiresAtMillis: Long?): Boolean =
    expiresAtMillis != null && expiresAtMillis < System.currentTimeMillis()
