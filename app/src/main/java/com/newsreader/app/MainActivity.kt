package com.newsreader.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.newsreader.app.ui.navigation.NavGraph
import com.newsreader.app.ui.screens.categories.CategoriesViewModel
import com.newsreader.app.ui.screens.favorites.FavoritesViewModel
import com.newsreader.app.ui.screens.feeds.FeedsViewModel
import com.newsreader.app.ui.screens.home.HomeViewModel
import com.newsreader.app.ui.screens.settings.SettingsViewModel
import com.newsreader.app.ui.theme.NewsReaderTheme
import com.newsreader.app.ui.theme.ThemeMode
import com.newsreader.app.util.ToastContainer
import com.newsreader.app.util.ToastMessage
import com.newsreader.app.util.ToastType
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val homeViewModel = HomeViewModel(application)
        val feedsViewModel = FeedsViewModel(application)
        val categoriesViewModel = CategoriesViewModel(application)
        val favoritesViewModel = FavoritesViewModel(application)
        val settingsViewModel = SettingsViewModel(application)

        setContent {
            val prefs = remember { applicationContext.getSharedPreferences("newsreader_prefs", 0) }
            var themeMode by remember {
                mutableStateOf(
                    ThemeMode.entries[prefs.getInt("theme_mode", ThemeMode.SYSTEM.ordinal)]
                )
            }

            NewsReaderTheme(themeMode = themeMode) {
                val scope = rememberCoroutineScope()

                // Toast state
                var toastMessage by remember { mutableStateOf<ToastMessage?>(null) }

                // Collect toasts from ViewModels
                LaunchedEffect(Unit) {
                    feedsViewModel.toast.collect { event ->
                        toastMessage = ToastMessage(
                            message = event.message,
                            type = event.type
                        )
                    }
                }
                LaunchedEffect(Unit) {
                    categoriesViewModel.toast.collect { event ->
                        toastMessage = ToastMessage(
                            message = event.message,
                            type = event.type
                        )
                    }
                }

                Box(modifier = Modifier.fillMaxSize()) {
                    NavGraph(
                        homeViewModel = homeViewModel,
                        feedsViewModel = feedsViewModel,
                        categoriesViewModel = categoriesViewModel,
                        favoritesViewModel = favoritesViewModel,
                        settingsViewModel = settingsViewModel,
                        currentTheme = themeMode,
                        onThemeChange = { mode ->
                            themeMode = mode
                            prefs.edit().putInt("theme_mode", mode.ordinal).apply()
                        }
                    )

                    // Toast overlay
                    ToastContainer(
                        toast = toastMessage,
                        onDismiss = { toastMessage = null },
                        modifier = Modifier.padding(top = 48.dp)
                    )
                }
            }
        }
    }
}
