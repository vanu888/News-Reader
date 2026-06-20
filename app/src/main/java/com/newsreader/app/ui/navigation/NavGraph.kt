package com.newsreader.app.ui.navigation

import android.app.Application
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.newsreader.app.data.db.AppDatabase
import com.newsreader.app.data.repository.ArticleRepository
import com.newsreader.app.domain.model.Article
import com.newsreader.app.ui.screens.categories.CategoriesScreen
import com.newsreader.app.ui.screens.categories.CategoriesViewModel
import com.newsreader.app.ui.screens.favorites.FavoritesScreen
import com.newsreader.app.ui.screens.favorites.FavoritesViewModel
import com.newsreader.app.ui.screens.feeds.FeedsScreen
import com.newsreader.app.ui.screens.feeds.FeedsViewModel
import com.newsreader.app.ui.screens.home.HomeScreen
import com.newsreader.app.ui.screens.home.HomeViewModel
import com.newsreader.app.ui.screens.settings.SettingsScreen
import com.newsreader.app.ui.screens.settings.SettingsViewModel
import com.newsreader.app.ui.theme.ThemeMode
import com.newsreader.app.util.DateUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NavGraph(
    homeViewModel: HomeViewModel,
    feedsViewModel: FeedsViewModel,
    categoriesViewModel: CategoriesViewModel,
    favoritesViewModel: FavoritesViewModel,
    settingsViewModel: SettingsViewModel,
    currentTheme: ThemeMode,
    onThemeChange: (ThemeMode) -> Unit
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val showBottomBar = Screen.bottomNavItems.any { it.route == currentRoute }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 2.dp
                ) {
                    Screen.bottomNavItems.forEach { screen ->
                        val selected = currentRoute == screen.route
                        NavigationBarItem(
                            icon = { Icon(screen.icon, contentDescription = screen.title) },
                            label = { Text(screen.title) },
                            selected = selected,
                            onClick = {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                            )
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Home.route) {
                HomeScreen(
                    viewModel = homeViewModel,
                    onArticleClick = { article ->
                        navController.navigate("article/${article.id}")
                    }
                )
            }

            composable(Screen.Feeds.route) {
                FeedsScreen(viewModel = feedsViewModel)
            }

            composable(Screen.Categories.route) {
                CategoriesScreen(viewModel = categoriesViewModel)
            }

            composable(Screen.Favorites.route) {
                FavoritesScreen(
                    viewModel = favoritesViewModel,
                    onArticleClick = { article ->
                        navController.navigate("article/${article.id}")
                    }
                )
            }

            composable(Screen.Settings.route) {
                SettingsScreen(
                    viewModel = settingsViewModel,
                    currentTheme = currentTheme,
                    onThemeChange = onThemeChange
                )
            }

            composable(
                route = "article/{articleId}",
                arguments = listOf(navArgument("articleId") { type = NavType.LongType })
            ) { backStackEntry ->
                val articleId = backStackEntry.arguments?.getLong("articleId") ?: return@composable
                ArticleDetailScreen(
                    articleId = articleId,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ArticleDetailScreen(
    articleId: Long,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var article by remember { mutableStateOf<Article?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(articleId) {
        try {
            val db = AppDatabase.getInstance(context)
            val repo = ArticleRepository(db.articleDao(), db.feedDao())
            val loaded = repo.getArticleById(articleId)
            article = loaded
            if (loaded != null && !loaded.isRead) {
                repo.markAsRead(articleId)
            }
        } catch (_: Exception) {}
        isLoading = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = article?.feedTitle ?: "Article",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (article != null) {
                        IconButton(onClick = {
                            val intent = android.content.Intent(
                                android.content.Intent.ACTION_VIEW,
                                android.net.Uri.parse(article!!.link)
                            )
                            context.startActivity(intent)
                        }) {
                            Icon(Icons.Default.OpenInBrowser, contentDescription = "Open in browser")
                        }
                        IconButton(onClick = {
                            val intent = android.content.Intent(
                                android.content.Intent.ACTION_SEND
                            ).apply {
                                type = "text/plain"
                                putExtra(android.content.Intent.EXTRA_TEXT, "${article!!.title}\n${article!!.link}")
                            }
                            context.startActivity(android.content.Intent.createChooser(intent, "Share"))
                        }) {
                            Icon(Icons.Default.Share, contentDescription = "Share")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                article == null -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Article not found")
                    }
                }
                else -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(20.dp)
                    ) {
                        Text(
                            text = article!!.title,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(Modifier.height(8.dp))

                        article!!.author?.let { author ->
                            Text(
                                text = "By $author",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        article!!.pubDate?.let { date ->
                            Text(
                                text = DateUtils.formatRelativeTime(date),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        article!!.description?.let { description ->
                            Spacer(Modifier.height(16.dp))
                            Text(
                                text = description,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f)
                            )
                        }

                        Spacer(Modifier.height(24.dp))

                        Button(
                            onClick = {
                                val intent = android.content.Intent(
                                    android.content.Intent.ACTION_VIEW,
                                    android.net.Uri.parse(article!!.link)
                                )
                                context.startActivity(intent)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(Icons.Default.OpenInBrowser, contentDescription = null)
                            Spacer(Modifier.width(4.dp))
                            Text("Open Original Article")
                        }
                    }
                }
            }
        }
    }
}
