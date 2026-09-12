package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DigiLabsColorScheme = darkColorScheme(
    primary = DigiCyan,
    onPrimary = Color(0xFF00363F),
    primaryContainer = Color(0xFF004F5D),
    onPrimaryContainer = DigiCyanLight,
    secondary = DigiGold,
    onSecondary = Color(0xFF432B00),
    secondaryContainer = Color(0xFF614000),
    onSecondaryContainer = Color(0xFFFFDDB3),
    tertiary = DigiPurple,
    onTertiary = Color.White,
    background = CyberNavyBg,
    onBackground = TextPrimary,
    surface = CyberCardSurface,
    onSurface = TextPrimary,
    surfaceVariant = CyberCardElevated,
    onSurfaceVariant = TextSecondary,
    outline = CyberCardBorder,
    error = DigiRed
)

@Composable
fun DigiSwissTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DigiLabsColorScheme,
        typography = Typography,
        content = content
    )
}

