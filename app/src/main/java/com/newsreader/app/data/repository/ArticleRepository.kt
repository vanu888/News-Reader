package com.newsreader.app.data.repository

import com.newsreader.app.data.db.dao.ArticleDao
import com.newsreader.app.data.db.dao.FeedDao
import com.newsreader.app.data.db.entity.ArticleEntity
import com.newsreader.app.domain.model.Article
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ArticleRepository(
    private val articleDao: ArticleDao,
    private val feedDao: FeedDao
) {

    fun getSubscribedArticles(): Flow<List<Article>> =
        articleDao.getSubscribedArticles().map { entities ->
            entities.map { it.toDomain() }
        }

    fun getAllArticles(): Flow<List<Article>> =
        articleDao.getAllArticles().map { entities -> entities.map { it.toDomain() } }

    fun getArticlesByFeed(feedId: Long): Flow<List<Article>> =
        articleDao.getArticlesByFeed(feedId).map { entities ->
            entities.map { it.toDomain() }
        }

    fun getFavoriteArticles(): Flow<List<Article>> =
        articleDao.getFavoriteArticles().map { entities ->
            entities.map { it.toDomain() }
        }

    fun getArticlesByCategory(categoryId: Long): Flow<List<Article>> =
        articleDao.getArticlesByCategory(categoryId).map { entities ->
            entities.map { it.toDomain() }
        }

    fun getUnreadCount(): Flow<Int> = articleDao.getUnreadCount()

    fun getUnreadCountByFeed(feedId: Long): Flow<Int> =
        articleDao.getUnreadCountByFeed(feedId)

    suspend fun getArticleById(id: Long): Article? =
        articleDao.getArticleById(id)?.toDomain()

    suspend fun insertArticles(articles: List<ArticleEntity>) {
        articleDao.insertArticles(articles)
    }

    suspend fun markAsRead(articleId: Long) {
        articleDao.markAsRead(articleId)
    }

    suspend fun markAllAsRead(feedId: Long) {
        articleDao.markAllAsRead(feedId)
    }

    suspend fun toggleFavorite(articleId: Long) {
        articleDao.toggleFavorite(articleId)
    }

    suspend fun getArticleByGuid(feedId: Long, guid: String): ArticleEntity? {
        return articleDao.getArticleByGuid(feedId, guid)
    }

    suspend fun deleteArticlesByFeed(feedId: Long) {
        articleDao.deleteArticlesByFeed(feedId)
    }

    private suspend fun ArticleEntity.toDomain(): Article {
        val feed = feedDao.getFeedById(feedId)
        return Article(
            id = id,
            feedId = feedId,
            feedTitle = feed?.title ?: "Unknown",
            feedImageUrl = feed?.imageUrl,
            title = title,
            link = link,
            description = description,
            content = content,
            imageUrl = imageUrl,
            pubDate = pubDate,
            author = author,
            isRead = isRead,
            isFavorite = isFavorite,
            fetchedAt = fetchedAt
        )
    }
}
