package com.newsreader.app.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.RssFeed
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(
    val route: String,
    val title: String,
    val icon: ImageVector
) {
    data object Home : Screen("home", "Home", Icons.Default.Home)
    data object Feeds : Screen("feeds", "Feeds", Icons.Default.RssFeed)
    data object Categories : Screen("categories", "Categories", Icons.Default.Category)
    data object Favorites : Screen("favorites", "Favorites", Icons.Default.Bookmark)
    data object Settings : Screen("settings", "Settings", Icons.Default.Settings)

    companion object {
        val bottomNavItems = listOf(Home, Feeds, Categories, Favorites, Settings)
    }
}
