package com.newsreader.app.ui.screens.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.newsreader.app.data.db.AppDatabase
import com.newsreader.app.data.repository.ArticleRepository
import com.newsreader.app.data.repository.CategoryRepository
import com.newsreader.app.data.repository.FeedRepository
import com.newsreader.app.domain.model.Article
import com.newsreader.app.domain.model.Category
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HomeUiState(
    val articles: List<Article> = emptyList(),
    val categories: List<Category> = emptyList(),
    val selectedCategoryId: Long? = null,
    val unreadCount: Int = 0
)

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    private val articleRepo = ArticleRepository(db.articleDao(), db.feedDao())
    private val categoryRepo = CategoryRepository(db.categoryDao())

    private val selectedCategoryId = MutableStateFlow<Long?>(null)

    private val articlesFlow: Flow<List<Article>> = selectedCategoryId.flatMapLatest { catId ->
        if (catId == null) articleRepo.getSubscribedArticles()
        else articleRepo.getArticlesByCategory(catId)
    }

    val uiState: StateFlow<HomeUiState> = combine(
        articlesFlow,
        categoryRepo.getAllCategories(),
        selectedCategoryId,
        articleRepo.getUnreadCount()
    ) { articles, categories, catId, unread ->
        HomeUiState(
            articles = articles,
            categories = categories,
            selectedCategoryId = catId,
            unreadCount = unread
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HomeUiState())

    fun selectCategory(categoryId: Long?) {
        selectedCategoryId.value = categoryId
    }

    fun markAsRead(articleId: Long) {
        viewModelScope.launch { articleRepo.markAsRead(articleId) }
    }

    fun toggleFavorite(articleId: Long) {
        viewModelScope.launch { articleRepo.toggleFavorite(articleId) }
    }
}
