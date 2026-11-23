package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.R
import com.example.data.security.AppLockManager

@Composable
fun PinSetupDialog(
    onDismiss: () -> Unit,
    onPinConfirmed: (String) -> Unit
) {
    var firstPin by remember { mutableStateOf<String?>(null) }
    var currentInput by remember { mutableStateOf("") }
    var mismatchError by remember { mutableStateOf(false) }

    val isConfirmStep = firstPin != null
    val targetDotCount = firstPin?.length ?: AppLockManager.MAX_PIN_LENGTH

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .padding(horizontal = 20.dp, vertical = 24.dp)
                .widthIn(max = 420.dp)
                .fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            tonalElevation = 6.dp,
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(
                        if (isConfirmStep) R.string.pin_setup_confirm_title else R.string.pin_setup_create_title
                    ),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = if (isConfirmStep) {
                        stringResource(R.string.pin_setup_confirm_hint, firstPin!!.length)
                    } else {
                        stringResource(R.string.pin_setup_hint)
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(20.dp))

                PinDotsIndicator(
                    totalDots = targetDotCount,
                    filledDots = currentInput.length,
                    isError = mismatchError
                )

                Spacer(modifier = Modifier.height(10.dp))

                AnimatedVisibility(visible = mismatchError) {
                    Text(
                        text = stringResource(R.string.pin_setup_mismatch),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.testTag("pin_setup_error")
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                NumericPinPad(
                    enabled = true,
                    showBiometricButton = false,
                    onDigitClick = { digit ->
                        mismatchError = false
                        val maxLen = firstPin?.length ?: AppLockManager.MAX_PIN_LENGTH
                        if (currentInput.length < maxLen) {
                            val updated = currentInput + digit
                            currentInput = updated
                            if (isConfirmStep && updated.length == maxLen) {
                                if (updated == firstPin) {
                                    onPinConfirmed(updated)
                                } else {
                                    mismatchError = true
                                    currentInput = ""
                                }
                            }
                        }
                    },
                    onBackspaceClick = {
                        mismatchError = false
                        if (currentInput.isNotEmpty()) {
                            currentInput = currentInput.dropLast(1)
                        }
                    },
                    onBiometricClick = {}
                )

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("pin_setup_cancel")
                    ) {
                        Text(stringResource(R.string.action_cancel))
                    }
                    if (!isConfirmStep) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (AppLockManager.isValidPinFormat(currentInput)) {
                                    firstPin = currentInput
                                    currentInput = ""
                                    mismatchError = false
                                }
                            },
                            enabled = AppLockManager.isValidPinFormat(currentInput),
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("pin_setup_continue")
                        ) {
                            Text(stringResource(R.string.pin_setup_continue))
                        }
                    }
                }
            }
        }
    }
}
