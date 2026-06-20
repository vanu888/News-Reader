package com.newsreader.app.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.newsreader.app.data.db.entity.FeedEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FeedDao {

    @Query("SELECT * FROM feeds ORDER BY title ASC")
    fun getAllFeeds(): Flow<List<FeedEntity>>

    @Query("SELECT * FROM feeds WHERE is_subscribed = 1 ORDER BY title ASC")
    fun getSubscribedFeeds(): Flow<List<FeedEntity>>

    @Query("SELECT * FROM feeds WHERE id = :id")
    suspend fun getFeedById(id: Long): FeedEntity?

    @Query("SELECT * FROM feeds WHERE url = :url LIMIT 1")
    suspend fun getFeedByUrl(url: String): FeedEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFeed(feed: FeedEntity): Long

    @Update
    suspend fun updateFeed(feed: FeedEntity)

    @Delete
    suspend fun deleteFeed(feed: FeedEntity)

    @Query("DELETE FROM feeds WHERE id = :id")
    suspend fun deleteFeedById(id: Long)

    @Query("SELECT * FROM feeds WHERE category_id = :categoryId ORDER BY title ASC")
    fun getFeedsByCategory(categoryId: Long): Flow<List<FeedEntity>>

    @Query("UPDATE feeds SET is_subscribed = :isSubscribed WHERE id = :feedId")
    suspend fun setSubscription(feedId: Long, isSubscribed: Boolean)

    @Query("UPDATE feeds SET last_updated = :timestamp WHERE id = :feedId")
    suspend fun updateLastUpdated(feedId: Long, timestamp: Long)

    @Query("UPDATE feeds SET error_message = :error WHERE id = :feedId")
    suspend fun updateError(feedId: Long, error: String?)
}
