package com.example.ui.components

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Patterns
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.outlined.OpenInNew
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
import com.example.ui.ExpiryStatus
import com.example.ui.expiryStatusOf
import com.example.ui.theme.AnthropicAccentDark
import com.example.ui.theme.AnthropicAccentLight
import com.example.ui.theme.CodeTextStyle
import com.example.ui.theme.CustomAccentDark
import com.example.ui.theme.CustomAccentLight
import com.example.ui.theme.GeminiAccentDark
import com.example.ui.theme.GeminiAccentLight
import com.example.ui.theme.GroqAccentDark
import com.example.ui.theme.GroqAccentLight
import com.example.ui.theme.OpenAiAccentDark
import com.example.ui.theme.OpenAiAccentLight
import com.example.ui.theme.OpenRouterAccentDark
import com.example.ui.theme.OpenRouterAccentLight
import com.example.ui.theme.vaultStatusColors

@Composable
fun EndpointCard(
    item: ApiEndpointItem,
    index: Int,
    revealedKey: String?,
    onToggleReveal: () -> Unit,
    onCopyKey: () -> Unit,
    onCopyUrl: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onOpenOfficialLink: (String) -> Unit = {},
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
                    IndexBadge(
                        index = index,
                        modifier = Modifier.testTag("endpoint_index_${item.id}")
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = item.name,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.testTag("endpoint_name_${item.id}")
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = providerIcon,
                                contentDescription = null,
                                tint = providerColor,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = item.providerType.displayName,
                                style = MaterialTheme.typography.labelSmall,
                                color = providerColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
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

            ExpiryStatusBadge(
                expiresAtMillis = item.expiresAtMillis,
                modifier = Modifier.testTag("expiry_badge_${item.id}")
            )

            if (isDuplicate) {
                Spacer(modifier = Modifier.height(6.dp))
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
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp)
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

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp),
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

            val officialUrl = officialSiteFor(item.providerType, item.baseUrl)
            if (isValidHttpUrl(officialUrl)) {
                Surface(
                    onClick = { onOpenOfficialLink(officialUrl) },
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                    border = BorderStroke(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp)
                        .testTag("open_official_link_${item.id}")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.OpenInNew,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.action_open_official_site),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        Icon(
                            imageVector = Icons.Outlined.Link,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }
    }
}

/** Numbered position badge (1, 2, 3…) shown in front of every vault entry. */
@Composable
private fun IndexBadge(
    index: Int,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        modifier = modifier.size(34.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = index.toString(),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

/** Vibrant expiry status badge: emerald while active, red once expired. */
@Composable
private fun ExpiryStatusBadge(
    expiresAtMillis: Long?,
    modifier: Modifier = Modifier
) {
    val status = expiryStatusOf(expiresAtMillis)
    val statusColors = vaultStatusColors()
    val container = if (status == ExpiryStatus.EXPIRED) {
        MaterialTheme.colorScheme.errorContainer
    } else {
        statusColors.successContainer
    }
    val content = if (status == ExpiryStatus.EXPIRED) {
        MaterialTheme.colorScheme.onErrorContainer
    } else {
        statusColors.onSuccessContainer
    }
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = container,
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .androidBackground(
                        if (status == ExpiryStatus.EXPIRED) {
                            MaterialTheme.colorScheme.error
                        } else {
                            statusColors.success
                        }
                    )
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = when (status) {
                    ExpiryStatus.EXPIRED -> stringResource(
                        R.string.expiry_status_expired,
                        DateFormats.formatExpiryDate(requireNotNull(expiresAtMillis))
                    )
                    ExpiryStatus.ACTIVE -> expiresAtMillis?.let {
                        stringResource(R.string.expiry_on, DateFormats.formatExpiryDate(it))
                    } ?: stringResource(R.string.expiry_never)
                },
                style = MaterialTheme.typography.labelSmall,
                color = content
            )
        }
    }
}

private fun Modifier.androidBackground(color: Color): Modifier =
    this.then(Modifier.background(color))

/**
 * Resolves the official website / dashboard for a provider. Known providers open
 * their real site (not the API base URL, which is often not a browsable page);
 * custom entries fall back to the stored base URL.
 */
internal fun officialSiteFor(providerType: ProviderType, baseUrl: String): String = when (providerType) {
    ProviderType.OPENAI -> "https://platform.openai.com/api-keys"
    ProviderType.ANTHROPIC -> "https://console.anthropic.com/settings/keys"
    ProviderType.GEMINI -> "https://aistudio.google.com/app/apikey"
    ProviderType.GROQ -> "https://console.groq.com/keys"
    ProviderType.OPENROUTER -> "https://openrouter.ai/keys"
    ProviderType.CUSTOM -> mainSiteOf(baseUrl)
}

/**
 * Reduces an API base URL to its main site by dropping version/endpoint path
 * segments such as /v1, /v2, /v1beta, /openai/v1. Returns the original value
 * when it cannot be parsed, so the caller's URL validation still applies.
 */
internal fun mainSiteOf(rawUrl: String): String {
    val trimmed = rawUrl.trim()
    val withScheme = if (trimmed.startsWith("http://", true) || trimmed.startsWith("https://", true)) {
        trimmed
    } else {
        "https://$trimmed"
    }
    return runCatching {
        val uri = java.net.URI(withScheme)
        val host = uri.host ?: return trimmed
        val scheme = uri.scheme ?: "https"
        "$scheme://$host"
    }.getOrDefault(trimmed)
}

/**
 * Opens an official website/dashboard link in the device browser with URL
 * validation. Returns false (and shows nothing) when no browser handles it.
 */
internal fun openOfficialLink(context: Context, rawUrl: String): Boolean {
    val trimmed = rawUrl.trim()
    val hasScheme = trimmed.startsWith("https://", ignoreCase = true) ||
        trimmed.startsWith("http://", ignoreCase = true)
    val safeUrl = if (hasScheme) trimmed else "https://$trimmed"
    if (Patterns.WEB_URL.matcher(safeUrl).matches()) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(safeUrl))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return try {
            context.startActivity(intent)
            true
        } catch (_: ActivityNotFoundException) {
            false
        }
    }
    return false
}

@Composable
private fun providerAccentColor(providerType: ProviderType): Color {
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    return when (providerType) {
        ProviderType.OPENAI -> if (isDark) OpenAiAccentDark else OpenAiAccentLight
        ProviderType.ANTHROPIC -> if (isDark) AnthropicAccentDark else AnthropicAccentLight
        ProviderType.GEMINI -> if (isDark) GeminiAccentDark else GeminiAccentLight
        ProviderType.GROQ -> if (isDark) GroqAccentDark else GroqAccentLight
        ProviderType.OPENROUTER -> if (isDark) OpenRouterAccentDark else OpenRouterAccentLight
        ProviderType.CUSTOM -> if (isDark) CustomAccentDark else CustomAccentLight
    }
}

private fun providerIcon(providerType: ProviderType): ImageVector {
    return when (providerType) {
        ProviderType.OPENAI -> Icons.Outlined.AutoAwesome
        ProviderType.ANTHROPIC -> Icons.Outlined.Psychology
        ProviderType.GEMINI -> Icons.Outlined.AutoAwesome
        ProviderType.GROQ -> Icons.Outlined.Http
        ProviderType.OPENROUTER -> Icons.Outlined.Link
        ProviderType.CUSTOM -> Icons.Outlined.Tune
    }
}

private fun formatTimestamp(epochMillis: Long): String = DateFormats.formatDateTime(epochMillis)
