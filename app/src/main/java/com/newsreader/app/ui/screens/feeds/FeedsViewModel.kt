package com.newsreader.app.ui.screens.feeds

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.newsreader.app.data.db.AppDatabase
import com.newsreader.app.data.rss.RssParser
import com.newsreader.app.data.rss.RssSyncManager
import com.newsreader.app.data.repository.ArticleRepository
import com.newsreader.app.data.repository.CategoryRepository
import com.newsreader.app.data.repository.FeedRepository
import com.newsreader.app.domain.model.Category
import com.newsreader.app.domain.model.Feed
import com.newsreader.app.util.ToastType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class ToastEvent(
    val message: String,
    val type: ToastType = ToastType.INFO
)

data class FeedsUiState(
    val feeds: List<Feed> = emptyList(),
    val categories: List<Category> = emptyList(),
    val isAdding: Boolean = false,
    val selectedCategoryId: Long? = null
)

class FeedsViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    private val feedRepo = FeedRepository(db.feedDao())
    private val articleRepo = ArticleRepository(db.articleDao(), db.feedDao())
    private val categoryRepo = CategoryRepository(db.categoryDao())
    private val syncManager = RssSyncManager(application)

    private val _toast = MutableSharedFlow<ToastEvent>()
    val toast = _toast.asSharedFlow()

    val uiState: StateFlow<FeedsUiState> = combine(
        feedRepo.getAllFeeds(),
        categoryRepo.getAllCategories()
    ) { feeds, categories ->
        FeedsUiState(feeds = feeds, categories = categories)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), FeedsUiState())

    fun fetchAndAddFeed(url: String, categoryId: Long? = null) {
        viewModelScope.launch {
            if (url.isBlank()) {
                _toast.emit(ToastEvent("Please enter a URL", ToastType.ERROR))
                return@launch
            }

            val validUrl = if (!url.startsWith("http")) "https://$url" else url

            if (!validUrl.matches(Regex("^https?://[\\w.-]+(:\\d+)?(/.*)?$"))) {
                _toast.emit(ToastEvent("Invalid URL format", ToastType.ERROR))
                return@launch
            }

            val existing = feedRepo.getFeedByUrl(validUrl)
            if (existing != null) {
                _toast.emit(ToastEvent("Feed already exists", ToastType.ERROR))
                return@launch
            }

            try {
                val result = withContext(Dispatchers.IO) {
                    val parser = RssParser()
                    parser.fetchAndParse(validUrl)
                }

                val feedId = feedRepo.addFeed(
                    Feed(
                        title = result.title,
                        url = validUrl,
                        description = result.description,
                        imageUrl = result.imageUrl,
                        categoryId = categoryId,
                        isSubscribed = true
                    )
                )

                val articles = result.articles.map { article ->
                    com.newsreader.app.data.db.entity.ArticleEntity(
                        feedId = feedId,
                        title = article.title,
                        link = article.link,
                        description = article.description,
                        content = article.content,
                        imageUrl = article.imageUrl,
                        pubDate = article.pubDate ?: System.currentTimeMillis(),
                        author = article.author,
                        guid = article.guid ?: article.link,
                        fetchedAt = System.currentTimeMillis()
                    )
                }
                articleRepo.insertArticles(articles)

                _toast.emit(ToastEvent("Feed added: ${result.title}", ToastType.SUCCESS))
            } catch (e: RssParser.RssException) {
                _toast.emit(ToastEvent("Failed to fetch feed: ${e.message}", ToastType.ERROR))
            } catch (e: Exception) {
                _toast.emit(ToastEvent("Error adding feed: ${e.message}", ToastType.ERROR))
            }
        }
    }

    fun toggleSubscription(feedId: Long, currentSubscribed: Boolean) {
        viewModelScope.launch {
            feedRepo.setSubscription(feedId, !currentSubscribed)
            _toast.emit(
                if (!currentSubscribed) ToastEvent("Subscribed", ToastType.SUCCESS)
                else ToastEvent("Unsubscribed", ToastType.INFO)
            )
        }
    }

    fun deleteFeed(feedId: Long) {
        viewModelScope.launch {
            articleRepo.deleteArticlesByFeed(feedId)
            feedRepo.deleteFeed(feedId)
            _toast.emit(ToastEvent("Feed removed", ToastType.INFO))
        }
    }

    fun refreshFeed(feedId: Long, feedUrl: String) {
        viewModelScope.launch {
            syncManager.syncFeed(feedId, feedUrl)
            _toast.emit(ToastEvent("Refresh queued", ToastType.INFO))
        }
    }

    fun updateFeedCategory(feedId: Long, categoryId: Long?) {
        viewModelScope.launch {
            val feed = feedRepo.getFeedById(feedId) ?: return@launch
            feedRepo.updateFeed(feed.copy(categoryId = categoryId))
        }
    }
}
