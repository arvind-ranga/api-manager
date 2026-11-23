package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Backspace
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.security.VaultLockMode

@Composable
fun LockScreen(
    lockMode: VaultLockMode,
    expectedPinLength: Int,
    lockSessionId: Int,
    pinErrorRes: Int?,
    cooldownSeconds: Int,
    biometricErrorMessage: String?,
    onPinDigitEntered: (String) -> Unit,
    onRequestBiometric: () -> Unit,
    modifier: Modifier = Modifier
) {
    var enteredPin by remember(lockSessionId) { mutableStateOf("") }

    LaunchedEffect(lockSessionId, lockMode) {
        if (lockMode.requiresBiometricOption) {
            onRequestBiometric()
        }
    }

    LaunchedEffect(pinErrorRes) {
        if (pinErrorRes != null) {
            enteredPin = ""
        }
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(horizontal = 24.dp, vertical = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 380.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(64.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (lockMode == VaultLockMode.BIOMETRIC_ONLY) {
                                Icons.Outlined.Fingerprint
                            } else {
                                Icons.Outlined.Lock
                            },
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = stringResource(R.string.lock_screen_title),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                val subtitle = when (lockMode) {
                    VaultLockMode.PIN_ONLY ->
                        stringResource(R.string.lock_screen_subtitle_pin, expectedPinLength)
                    VaultLockMode.BIOMETRIC_ONLY ->
                        stringResource(R.string.lock_screen_subtitle_biometric)
                    VaultLockMode.PIN_AND_BIOMETRIC ->
                        stringResource(R.string.lock_screen_subtitle_both, expectedPinLength)
                    VaultLockMode.OFF -> ""
                }
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                if (lockMode.requiresPinOption) {
                    Spacer(modifier = Modifier.height(28.dp))

                    PinDotsIndicator(
                        totalDots = expectedPinLength,
                        filledDots = enteredPin.length,
                        isError = pinErrorRes != null
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    val errorMessage = when {
                        cooldownSeconds > 0 ->
                            stringResource(R.string.lock_cooldown_message, cooldownSeconds)
                        pinErrorRes != null ->
                            stringResource(pinErrorRes)
                        biometricErrorMessage != null ->
                            biometricErrorMessage
                        else -> null
                    }
                    AnimatedVisibility(visible = errorMessage != null) {
                        Text(
                            text = errorMessage.orEmpty(),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.testTag("lock_error_text")
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    NumericPinPad(
                        enabled = cooldownSeconds == 0,
                        showBiometricButton = lockMode.requiresBiometricOption,
                        onDigitClick = { digit ->
                            if (cooldownSeconds == 0 && enteredPin.length < expectedPinLength) {
                                val updated = enteredPin + digit
                                enteredPin = updated
                                if (updated.length == expectedPinLength) {
                                    onPinDigitEntered(updated)
                                }
                            }
                        },
                        onBackspaceClick = {
                            if (enteredPin.isNotEmpty()) {
                                enteredPin = enteredPin.dropLast(1)
                            }
                        },
                        onBiometricClick = onRequestBiometric
                    )
                } else if (lockMode == VaultLockMode.BIOMETRIC_ONLY) {
                    Spacer(modifier = Modifier.height(28.dp))

                    if (biometricErrorMessage != null) {
                        Text(
                            text = biometricErrorMessage,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                    }

                    Button(
                        onClick = onRequestBiometric,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("lock_fingerprint_button")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Fingerprint,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.lock_use_fingerprint))
                    }
                }

                if (lockMode == VaultLockMode.PIN_AND_BIOMETRIC) {
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedButton(
                        onClick = onRequestBiometric,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("lock_fingerprint_button")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Fingerprint,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.lock_use_fingerprint))
                    }
                }
            }
        }
    }
}

@Composable
internal fun PinDotsIndicator(
    totalDots: Int,
    filledDots: Int,
    isError: Boolean = false,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(totalDots) { index ->
            val filled = index < filledDots
            val dotColor = when {
                isError -> MaterialTheme.colorScheme.error
                filled -> MaterialTheme.colorScheme.primary
                else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
            }
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .clip(CircleShape)
                    .then(
                        if (filled) {
                            Modifier.background(dotColor)
                        } else {
                            Modifier.border(2.dp, dotColor, CircleShape)
                        }
                    )
            )
        }
    }
}

@Composable
internal fun NumericPinPad(
    enabled: Boolean,
    showBiometricButton: Boolean,
    onDigitClick: (String) -> Unit,
    onBackspaceClick: () -> Unit,
    onBiometricClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val rows = listOf(
        listOf("1", "2", "3"),
        listOf("4", "5", "6"),
        listOf("7", "8", "9")
    )
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        rows.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                row.forEach { digit ->
                    PinDigitButton(
                        digit = digit,
                        enabled = enabled,
                        onClick = { onDigitClick(digit) }
                    )
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            if (showBiometricButton) {
                Surface(
                    onClick = onBiometricClick,
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f),
                    modifier = Modifier
                        .size(64.dp)
                        .testTag("pin_key_biometric")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Outlined.Fingerprint,
                            contentDescription = stringResource(R.string.cd_pin_fingerprint),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            } else {
                Spacer(modifier = Modifier.size(64.dp))
            }

            PinDigitButton(
                digit = "0",
                enabled = enabled,
                onClick = { onDigitClick("0") }
            )

            Surface(
                onClick = onBackspaceClick,
                enabled = enabled,
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier
                    .size(64.dp)
                    .testTag("pin_key_backspace")
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.Backspace,
                        contentDescription = stringResource(R.string.cd_pin_backspace),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun PinDigitButton(
    digit: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
        tonalElevation = 2.dp,
        modifier = Modifier
            .size(64.dp)
            .testTag("pin_key_$digit")
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = digit,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
