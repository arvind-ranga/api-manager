package com.example.data.security

enum class VaultLockMode {
    OFF,
    PIN_ONLY,
    BIOMETRIC_ONLY,
    PIN_AND_BIOMETRIC;

    val requiresPinOption: Boolean
        get() = this == PIN_ONLY || this == PIN_AND_BIOMETRIC

    val requiresBiometricOption: Boolean
        get() = this == BIOMETRIC_ONLY || this == PIN_AND_BIOMETRIC

    val isEnabled: Boolean
        get() = this != OFF

    companion object {
        fun fromStored(value: String?): VaultLockMode {
            return entries.firstOrNull { it.name == value } ?: OFF
        }
    }
}

data class VaultLockSettings(
    val mode: VaultLockMode = VaultLockMode.OFF,
    val hasPin: Boolean = false,
    val pinLength: Int = 4
)
