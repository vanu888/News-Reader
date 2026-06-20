package com.newsreader.app.ui.screens.categories

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.newsreader.app.data.db.AppDatabase
import com.newsreader.app.data.repository.CategoryRepository
import com.newsreader.app.data.repository.FeedRepository
import com.newsreader.app.domain.model.Category
import com.newsreader.app.ui.screens.feeds.ToastEvent
import com.newsreader.app.ui.theme.CategoryColors
import com.newsreader.app.util.ToastType
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class CategoriesUiState(
    val categories: List<Category> = emptyList()
)

class CategoriesViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    private val categoryRepo = CategoryRepository(db.categoryDao())
    private val feedRepo = FeedRepository(db.feedDao())

    private val _toast = MutableSharedFlow<ToastEvent>()
    val toast = _toast.asSharedFlow()

    val uiState: StateFlow<CategoriesUiState> = categoryRepo.getAllCategories()
        .map { CategoriesUiState(categories = it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), CategoriesUiState())

    fun createCategory(name: String) {
        viewModelScope.launch {
            if (name.isBlank()) {
                _toast.emit(ToastEvent("Category name cannot be empty", ToastType.ERROR))
                return@launch
            }

            val existing = categoryRepo.getCategoryByName(name)
            if (existing != null) {
                _toast.emit(ToastEvent("Category already exists", ToastType.ERROR))
                return@launch
            }

            val colorIndex = uiState.value.categories.size % CategoryColors.size
            val color = CategoryColors[colorIndex]
            categoryRepo.createCategory(name, color)
            _toast.emit(ToastEvent("Category created: $name", ToastType.SUCCESS))
        }
    }

    fun deleteCategory(id: Long) {
        viewModelScope.launch {
            val feeds = db.feedDao().getFeedsByCategory(id).first()
            for (feed in feeds) {
                db.feedDao().updateFeed(feed.copy(categoryId = null))
            }
            categoryRepo.deleteCategory(id)
            _toast.emit(ToastEvent("Category deleted", ToastType.INFO))
        }
    }
}
