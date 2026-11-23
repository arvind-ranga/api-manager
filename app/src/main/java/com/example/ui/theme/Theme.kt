package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
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
    error = VaultErrorDark,
    onError = VaultOnErrorDark
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
    error = VaultErrorLight,
    onError = VaultOnErrorLight
)

@Composable
fun ApiManagerTheme(
    themeMode: ThemeMode = ThemeMode.AUTO,
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

    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        typography = activeTypography,
        content = content
    )
}
