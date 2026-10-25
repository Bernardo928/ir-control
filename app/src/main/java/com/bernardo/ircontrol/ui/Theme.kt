package com.bernardo.ircontrol.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

object Palette {
    val Background = Color(0xFF2A2B38)
    val BackgroundDeep = Color(0xFF1F2029)
    val Button = Color(0xFF3B3C4B)
    val ButtonPressed = Color(0xFF4A4B5C)
    val Outline = Color(0xFF5C5D6E)
    val Text = Color(0xFFD3D4E0)
    val TextDim = Color(0xFF9A9BAC)
    val Accent = Color(0xFF4F7BF7)
    val Red = Color(0xFFEF4136)
    val Orange = Color(0xFFF7941D)
    val Pink = Color(0xFFD9418A)
    val Blue = Color(0xFF4A6CF7)
    val Purple = Color(0xFF9B4DDB)

    val okGradient = Brush.linearGradient(listOf(Orange, Red, Pink, Purple, Blue))
    val ringGradient = Brush.sweepGradient(listOf(Purple, Blue, Purple, Pink, Red, Orange, Red, Pink, Purple))
}

@Composable
fun RemoteTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Palette.Accent,
            background = Palette.Background,
            surface = Palette.Background,
            surfaceVariant = Palette.Button,
            onBackground = Palette.Text,
            onSurface = Palette.Text,
            onSurfaceVariant = Palette.TextDim,
        ),
        content = content,
    )
}
