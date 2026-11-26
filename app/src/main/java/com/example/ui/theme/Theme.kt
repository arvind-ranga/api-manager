package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalConfiguration
import com.example.data.settings.ThemeMode

private val DarkColorScheme = darkColorScheme(
    primary = VaultPrimaryDark,
    onPrimary = VaultOnPrimaryDark,
    primaryContainer = VaultPrimaryContainerDark,
    onPrimaryContainer = VaultOnPrimaryContainerDark,
    secondary = VaultSecondaryDark,
    onSecondary = VaultOnSecondaryDark,
    secondaryContainer = VaultSecondaryContainerDark,
    onSecondaryContainer = VaultOnSecondaryContainerDark,
    tertiary = VaultTertiaryDark,
    onTertiary = VaultOnTertiaryDark,
    tertiaryContainer = VaultTertiaryContainerDark,
    onTertiaryContainer = VaultOnTertiaryContainerDark,
    background = VaultBackgroundDark,
    onBackground = VaultOnSurfaceDark,
    surface = VaultSurfaceDark,
    onSurface = VaultOnSurfaceDark,
    surfaceVariant = VaultSurfaceVariantDark,
    onSurfaceVariant = VaultOnSurfaceVariantDark,
    outline = VaultOutlineDark,
    outlineVariant = VaultOutlineVariantDark,
    error = VaultErrorDark,
    onError = VaultOnErrorDark,
    errorContainer = VaultErrorContainerDark,
    onErrorContainer = VaultOnErrorContainerDark
)

private val LightColorScheme = lightColorScheme(
    primary = VaultPrimaryLight,
    onPrimary = VaultOnPrimaryLight,
    primaryContainer = VaultPrimaryContainerLight,
    onPrimaryContainer = VaultOnPrimaryContainerLight,
    secondary = VaultSecondaryLight,
    onSecondary = VaultOnSecondaryLight,
    secondaryContainer = VaultSecondaryContainerLight,
    onSecondaryContainer = VaultOnSecondaryContainerLight,
    tertiary = VaultTertiaryLight,
    onTertiary = VaultOnTertiaryLight,
    tertiaryContainer = VaultTertiaryContainerLight,
    onTertiaryContainer = VaultOnTertiaryContainerLight,
    background = VaultBackgroundLight,
    onBackground = VaultOnSurfaceLight,
    surface = VaultSurfaceLight,
    onSurface = VaultOnSurfaceLight,
    surfaceVariant = VaultSurfaceVariantLight,
    onSurfaceVariant = VaultOnSurfaceVariantLight,
    outline = VaultOutlineLight,
    outlineVariant = VaultOutlineVariantLight,
    error = VaultErrorLight,
    onError = VaultOnErrorLight,
    errorContainer = VaultErrorContainerLight,
    onErrorContainer = VaultOnErrorContainerLight
)

/** Success colours for expiry status badges; dark-theme aware. */
data class VaultStatusColors(
    val success: Color,
    val onSuccess: Color,
    val successContainer: Color,
    val onSuccessContainer: Color,
    val warning: Color
)

private val LightStatusColors = VaultStatusColors(
    success = VaultSuccessLight,
    onSuccess = VaultOnSuccessLight,
    successContainer = VaultSuccessContainerLight,
    onSuccessContainer = VaultOnSuccessContainerLight,
    warning = VaultWarningLight
)

private val DarkStatusColors = VaultStatusColors(
    success = VaultSuccessDark,
    onSuccess = VaultOnSuccessDark,
    successContainer = VaultSuccessContainerDark,
    onSuccessContainer = VaultOnSuccessContainerDark,
    warning = VaultWarningDark
)

@Composable
fun vaultStatusColors(): VaultStatusColors =
    if (MaterialTheme.colorScheme.surface.luminance() < 0.5f) {
        DarkStatusColors
    } else {
        LightStatusColors
    }

@Composable
fun ApiManagerTheme(
    themeMode: ThemeMode = ThemeMode.AUTO,
    accentColor: Int? = null,
    // Kept for source compatibility with existing tests; colours are now fully local.
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.AUTO -> isSystemInDarkTheme()
    }

    val isCompactWidth = LocalConfiguration.current.screenWidthDp < 600
    val activeTypography = remember(isCompactWidth) {
        if (isCompactWidth) Typography.scaledForPhone() else Typography
    }

    val baseScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val colorScheme = remember(baseScheme, accentColor, darkTheme) {
        if (accentColor == null) {
            baseScheme
        } else {
            val accent = Color(accentColor)
            val onAccent = if (accent.luminance() > 0.5f) Color(0xFF101623) else Color(0xFFFFFFFF)
            baseScheme.copy(
                primary = accent,
                onPrimary = onAccent,
                primaryContainer = accent.copy(alpha = 0.16f),
                onPrimaryContainer = accent,
                secondary = accent,
                onSecondary = onAccent,
                secondaryContainer = accent.copy(alpha = 0.12f),
                onSecondaryContainer = accent,
                tertiary = accent,
                onTertiary = onAccent,
                tertiaryContainer = accent.copy(alpha = 0.16f),
                onTertiaryContainer = accent
            )
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = activeTypography,
        content = content
    )
}
