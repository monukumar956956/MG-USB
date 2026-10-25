package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val MgDarkColorScheme = darkColorScheme(
    primary = MgNeonGreen,
    onPrimary = MgOnNeonGreen,
    primaryContainer = MgNeonGreenContainer,
    onPrimaryContainer = MgNeonGreen,
    secondary = MgCyberCyan,
    onSecondary = Color(0xFF001F24),
    secondaryContainer = MgCyberCyanContainer,
    onSecondaryContainer = MgCyberCyan,
    tertiary = MgAmberWarning,
    onTertiary = Color(0xFF261900),
    background = MgDarkBackground,
    onBackground = MgTextPrimaryDark,
    surface = MgDarkSurface,
    onSurface = MgTextPrimaryDark,
    surfaceVariant = MgDarkSurfaceVariant,
    onSurfaceVariant = MgTextSecondaryDark,
    outline = MgDarkCardBorder,
    error = MgCrimsonError,
    onError = Color.White
)

private val MgLightColorScheme = lightColorScheme(
    primary = MgLightPrimary,
    onPrimary = Color.White,
    primaryContainer = MgLightPrimaryContainer,
    onPrimaryContainer = Color(0xFF00210B),
    secondary = MgLightSecondary,
    onSecondary = Color.White,
    secondaryContainer = MgLightSecondaryContainer,
    onSecondaryContainer = Color(0xFF001F24),
    tertiary = Color(0xFFB26A00),
    onTertiary = Color.White,
    background = MgLightBackground,
    onBackground = MgTextPrimaryLight,
    surface = MgLightSurface,
    onSurface = MgTextPrimaryLight,
    surfaceVariant = MgLightSurfaceVariant,
    onSurfaceVariant = MgTextSecondaryLight,
    outline = Color(0xFFB8CFC1),
    error = Color(0xFFD32F2F),
    onError = Color.White
)

@Composable
fun MGUsbTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) MgDarkColorScheme else MgLightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    MGUsbTheme(darkTheme = darkTheme, content = content)
}
