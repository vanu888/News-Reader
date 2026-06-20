package com.newsreader.app.ui.screens.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.newsreader.app.ui.theme.ThemeMode

data class SettingsUiState(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val appVersion: String = "1.0"
)

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences("newsreader_prefs", 0)

    fun loadThemeMode(): ThemeMode {
        val ordinal = prefs.getInt("theme_mode", ThemeMode.SYSTEM.ordinal)
        return ThemeMode.entries[ordinal]
    }

    fun saveThemeMode(mode: ThemeMode) {
        prefs.edit().putInt("theme_mode", mode.ordinal).apply()
    }
}
