package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// ============================================================================
// Design tokens — vibrant Material 3 palette. Screen composables must consume
// these through MaterialTheme.colorScheme only; no hardcoded colors or dp
// literals belong in screens.
// ============================================================================

// Light palette — Electric Iris: vibrant indigo/violet on a cool gallery.
val VaultPrimaryLight = Color(0xFF4F46E5)        // Indigo 600
val VaultOnPrimaryLight = Color(0xFFFFFFFF)
val VaultPrimaryContainerLight = Color(0xFFE0E7FF) // Indigo 100
val VaultOnPrimaryContainerLight = Color(0xFF1E1B4B) // Indigo 950

val VaultSecondaryLight = Color(0xFF2563EB)      // Blue 600
val VaultOnSecondaryLight = Color(0xFFFFFFFF)
val VaultSecondaryContainerLight = Color(0xFFDBEAFE) // Blue 100
val VaultOnSecondaryContainerLight = Color(0xFF172554) // Blue 950

val VaultTertiaryLight = Color(0xFFC026D3)       // Fuchsia 600
val VaultOnTertiaryLight = Color(0xFFFFFFFF)
val VaultTertiaryContainerLight = Color(0xFFFAE8FF) // Fuchsia 100
val VaultOnTertiaryContainerLight = Color(0xFF4A044E) // Fuchsia 950

val VaultBackgroundLight = Color(0xFFF4F6FB)
val VaultSurfaceLight = Color(0xFFFFFFFF)
val VaultSurfaceVariantLight = Color(0xFFE8ECF7)
val VaultOnSurfaceLight = Color(0xFF101623)
val VaultOnSurfaceVariantLight = Color(0xFF4A5470)
val VaultOutlineLight = Color(0xFFC4CBDF)
val VaultOutlineVariantLight = Color(0xFFE0E4F0)
val VaultErrorLight = Color(0xFFDC2626)         // Red 600
val VaultOnErrorLight = Color(0xFFFFFFFF)
val VaultErrorContainerLight = Color(0xFFFEE2E2) // Red 100
val VaultOnErrorContainerLight = Color(0xFF7F1D1D) // Red 900

// Dark palette — Neon Nebula: luminous accents on deep midnight surfaces.
val VaultPrimaryDark = Color(0xFFA5B4FC)         // Indigo 300
val VaultOnPrimaryDark = Color(0xFF1E1B4B)
val VaultPrimaryContainerDark = Color(0xFF4338CA) // Indigo 700
val VaultOnPrimaryContainerDark = Color(0xFFE0E7FF)

val VaultSecondaryDark = Color(0xFF60A5FA)       // Blue 400
val VaultOnSecondaryDark = Color(0xFF172554)
val VaultSecondaryContainerDark = Color(0xFF1D4ED8) // Blue 700
val VaultOnSecondaryContainerDark = Color(0xFFDBEAFE)

val VaultTertiaryDark = Color(0xFFF0ABFC)        // Fuchsia 300
val VaultOnTertiaryDark = Color(0xFF4A044E)
val VaultTertiaryContainerDark = Color(0xFFA21CAF) // Fuchsia 700
val VaultOnTertiaryContainerDark = Color(0xFFFAE8FF)

val VaultBackgroundDark = Color(0xFF121826)
val VaultSurfaceDark = Color(0xFF1B2436)
val VaultSurfaceVariantDark = Color(0xFF26324A)
val VaultOnSurfaceDark = Color(0xFFEEF2FA)
val VaultOnSurfaceVariantDark = Color(0xFFAEB8D2)
val VaultOutlineDark = Color(0xFF46536F)
val VaultOutlineVariantDark = Color(0xFF2C3852)
val VaultErrorDark = Color(0xFFF87171)          // Red 400
val VaultOnErrorDark = Color(0xFF450A0A)
val VaultErrorContainerDark = Color(0xFF7F1D1D)
val VaultOnErrorContainerDark = Color(0xFFFECACA)

// Status tokens for expiry badges (used via theme extensions, never inline hex).
val VaultSuccessLight = Color(0xFF059669)        // Emerald 600
val VaultOnSuccessLight = Color(0xFFFFFFFF)
val VaultSuccessContainerLight = Color(0xFFD1FAE5)
val VaultOnSuccessContainerLight = Color(0xFF064E3B)
val VaultSuccessDark = Color(0xFF34D399)         // Emerald 400
val VaultOnSuccessDark = Color(0xFF052E16)
val VaultSuccessContainerDark = Color(0xFF065F46)
val VaultOnSuccessContainerDark = Color(0xFFD1FAE5)

// Warning tokens (about-to-expire state).
val VaultWarningLight = Color(0xFFD97706)        // Amber 600
val VaultWarningDark = Color(0xFFFBBF24)         // Amber 400

// Provider badge accent colors
val OpenAiAccentLight = Color(0xFF059669)
val OpenAiAccentDark = Color(0xFF34D399)
val AnthropicAccentLight = Color(0xFFD97706)
val AnthropicAccentDark = Color(0xFFFBBF24)
val GeminiAccentLight = Color(0xFF2563EB)
val GeminiAccentDark = Color(0xFF60A5FA)
val GroqAccentLight = Color(0xFFEA580C)
val GroqAccentDark = Color(0xFFFB923C)
val OpenRouterAccentLight = Color(0xFF0891B2)
val OpenRouterAccentDark = Color(0xFF22D3EE)
val CustomAccentLight = Color(0xFF7C3AED)
val CustomAccentDark = Color(0xFFA78BFA)
