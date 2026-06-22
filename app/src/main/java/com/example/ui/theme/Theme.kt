package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Cream color helper helper
val CreamColor = Color(0xFFFEF9F5)

private val DarkColorScheme = darkColorScheme(
    primary = SleekPrimaryContainer,
    onPrimary = SleekPrimary,
    primaryContainer = SleekPrimary,
    onPrimaryContainer = SleekPrimaryContainer,
    secondary = SleekSecondaryContainer,
    onSecondary = SleekSecondary,
    secondaryContainer = SleekSecondary,
    onSecondaryContainer = SleekSecondaryContainer,
    tertiary = SleekTertiaryContainer,
    onTertiary = SleekTertiary,
    background = Color(0xFF1B1B1F),
    surface = Color(0xFF2C2D31),
    onBackground = SleekBg,
    onSurface = SleekBg,
    outline = SleekOutline
)

private val LightColorScheme = lightColorScheme(
    primary = SleekPrimary,
    onPrimary = SleekOnPrimary,
    primaryContainer = SleekPrimaryContainer,
    onPrimaryContainer = SleekOnPrimaryContainer,
    secondary = SleekSecondary,
    onSecondary = SleekOnSecondary,
    secondaryContainer = SleekSecondaryContainer,
    onSecondaryContainer = SleekOnSecondaryContainer,
    tertiary = SleekTertiary,
    onTertiary = SleekOnTertiary,
    tertiaryContainer = SleekTertiaryContainer,
    onTertiaryContainer = SleekOnTertiaryContainer,
    background = SleekBg,
    surface = SleekSurface,
    surfaceVariant = SleekSurfaceVariant,
    onBackground = SleekText,
    onSurface = SleekText,
    outline = SleekOutline
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) {
        DarkColorScheme
    } else {
        LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
