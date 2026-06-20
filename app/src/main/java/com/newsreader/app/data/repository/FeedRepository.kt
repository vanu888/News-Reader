package com.newsreader.app.data.repository

import com.newsreader.app.data.db.dao.FeedDao
import com.newsreader.app.data.db.entity.FeedEntity
import com.newsreader.app.domain.model.Feed
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class FeedRepository(private val feedDao: FeedDao) {

    fun getAllFeeds(): Flow<List<Feed>> =
        feedDao.getAllFeeds().map { entities -> entities.map { it.toDomain() } }

    fun getSubscribedFeeds(): Flow<List<Feed>> =
        feedDao.getSubscribedFeeds().map { entities -> entities.map { it.toDomain() } }

    fun getFeedsByCategory(categoryId: Long): Flow<List<Feed>> =
        feedDao.getFeedsByCategory(categoryId).map { entities -> entities.map { it.toDomain() } }

    suspend fun getFeedById(id: Long): Feed? = feedDao.getFeedById(id)?.toDomain()

    suspend fun getFeedByUrl(url: String): Feed? = feedDao.getFeedByUrl(url)?.toDomain()

    suspend fun addFeed(feed: Feed): Long {
        return feedDao.insertFeed(feed.toEntity())
    }

    suspend fun updateFeed(feed: Feed) {
        feedDao.updateFeed(feed.toEntity())
    }

    suspend fun deleteFeed(id: Long) {
        feedDao.deleteFeedById(id)
    }

    suspend fun setSubscription(feedId: Long, subscribed: Boolean) {
        feedDao.setSubscription(feedId, subscribed)
    }

    suspend fun updateLastUpdated(feedId: Long, timestamp: Long) {
        feedDao.updateLastUpdated(feedId, timestamp)
    }

    suspend fun updateError(feedId: Long, error: String?) {
        feedDao.updateError(feedId, error)
    }

    private fun FeedEntity.toDomain() = Feed(
        id = id,
        title = title,
        url = url,
        description = description,
        imageUrl = imageUrl,
        categoryId = categoryId,
        isSubscribed = isSubscribed,
        lastUpdated = lastUpdated,
        errorMessage = errorMessage
    )

    private fun Feed.toEntity() = FeedEntity(
        id = id,
        title = title,
        url = url,
        description = description,
        imageUrl = imageUrl,
        categoryId = categoryId,
        isSubscribed = isSubscribed,
        lastUpdated = lastUpdated,
        errorMessage = errorMessage
    )
}
