package com.newsreader.app.ui.screens.favorites

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.newsreader.app.data.db.AppDatabase
import com.newsreader.app.data.repository.ArticleRepository
import com.newsreader.app.domain.model.Article
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class FavoritesUiState(
    val articles: List<Article> = emptyList()
)

class FavoritesViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    private val articleRepo = ArticleRepository(db.articleDao(), db.feedDao())

    val uiState: StateFlow<FavoritesUiState> = articleRepo.getFavoriteArticles()
        .map { FavoritesUiState(articles = it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), FavoritesUiState())

    fun toggleFavorite(articleId: Long) {
        viewModelScope.launch { articleRepo.toggleFavorite(articleId) }
    }

    fun markAsRead(articleId: Long) {
        viewModelScope.launch { articleRepo.markAsRead(articleId) }
    }
}
