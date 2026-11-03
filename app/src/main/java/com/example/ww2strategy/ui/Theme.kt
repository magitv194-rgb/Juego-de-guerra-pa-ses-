package com.example.ww2strategy.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Dark = darkColorScheme(
    primary = Color(0xFF8AAFF5),
    secondary = Color(0xFFD77A7A),
    background = Color(0xFF121417),
    surface = Color(0xFF1E2227)
)
private val Light = lightColorScheme(
    primary = Color(0xFF2E4A8F),
    secondary = Color(0xFFB02525)
)

@Composable
fun WW2Theme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) Dark else Light,
        content = content
    )
}
