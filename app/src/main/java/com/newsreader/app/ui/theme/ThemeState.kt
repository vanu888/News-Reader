package com.newsreader.app.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf

enum class ThemeMode {
    LIGHT, DARK, SYSTEM
}

data class ThemeState(
    val mode: ThemeMode = ThemeMode.SYSTEM,
    val isDark: Boolean = false
)

val LocalThemeState = staticCompositionLocalOf { ThemeState() }
