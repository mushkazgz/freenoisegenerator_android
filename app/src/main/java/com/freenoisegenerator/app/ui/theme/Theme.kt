package com.freenoisegenerator.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColors = darkColorScheme(
    primary = Color(0xFF9ED6C5),
    onPrimary = Color(0xFF00382D),
    primaryContainer = Color(0xFF005143),
    onPrimaryContainer = Color(0xFFCDEADF),
    secondary = Color(0xFFD6C692),
    onSecondary = Color(0xFF3A3000),
    tertiary = Color(0xFFDEB9D0),
    background = Color(0xFF111413),
    onBackground = Color(0xFFE1E4E1),
    surface = Color(0xFF111413),
    onSurface = Color(0xFFE1E4E1),
    surfaceVariant = Color(0xFF414943),
    onSurfaceVariant = Color(0xFFC0C8C2)
)

@Composable
fun FreeNoiseGeneratorTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColors,
        content = content
    )
}
