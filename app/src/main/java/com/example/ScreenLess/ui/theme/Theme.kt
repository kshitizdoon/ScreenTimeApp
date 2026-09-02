package com.example.ScreenLess.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = ScreenlessOrange,
    onPrimary = Color(0xFF2B1700),
    primaryContainer = ScreenlessOrangeContainer,
    onPrimaryContainer = Color(0xFFFFDDB8),
    secondary = Color(0xFFB7CCFF),
    onSecondary = Color(0xFF112E5C),
    background = ScreenlessBackground,
    onBackground = ScreenlessOnSurface,
    surface = ScreenlessSurface,
    onSurface = ScreenlessOnSurface,
    surfaceVariant = ScreenlessSurfaceVariant,
    onSurfaceVariant = ScreenlessOnSurfaceVariant,
    outline = ScreenlessOutline,
    outlineVariant = Color(0xFF4D4741),
    error = ScreenlessError,
    onError = Color(0xFF690005)
)

@Composable
fun MyApplicationTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}
