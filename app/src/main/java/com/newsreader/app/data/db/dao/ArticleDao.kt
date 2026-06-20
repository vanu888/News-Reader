package com.newsreader.app.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.newsreader.app.data.db.entity.ArticleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ArticleDao {

    @Query("""
        SELECT a.* FROM articles a 
        INNER JOIN feeds f ON a.feed_id = f.id 
        WHERE f.is_subscribed = 1 
        ORDER BY a.pub_date DESC
    """)
    fun getSubscribedArticles(): Flow<List<ArticleEntity>>

    @Query("SELECT * FROM articles ORDER BY pub_date DESC")
    fun getAllArticles(): Flow<List<ArticleEntity>>

    @Query("SELECT * FROM articles WHERE feed_id = :feedId ORDER BY pub_date DESC")
    fun getArticlesByFeed(feedId: Long): Flow<List<ArticleEntity>>

    @Query("SELECT * FROM articles WHERE is_favorite = 1 ORDER BY pub_date DESC")
    fun getFavoriteArticles(): Flow<List<ArticleEntity>>

    @Query("""
        SELECT a.* FROM articles a 
        INNER JOIN feeds f ON a.feed_id = f.id 
        WHERE f.category_id = :categoryId AND f.is_subscribed = 1 
        ORDER BY a.pub_date DESC
    """)
    fun getArticlesByCategory(categoryId: Long): Flow<List<ArticleEntity>>

    @Query("SELECT * FROM articles WHERE id = :id")
    suspend fun getArticleById(id: Long): ArticleEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertArticle(article: ArticleEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertArticles(articles: List<ArticleEntity>)

    @Update
    suspend fun updateArticle(article: ArticleEntity)

    @Delete
    suspend fun deleteArticle(article: ArticleEntity)

    @Query("UPDATE articles SET is_read = 1 WHERE id = :articleId")
    suspend fun markAsRead(articleId: Long)

    @Query("UPDATE articles SET is_read = 1 WHERE feed_id = :feedId")
    suspend fun markAllAsRead(feedId: Long)

    @Query("UPDATE articles SET is_favorite = NOT is_favorite WHERE id = :articleId")
    suspend fun toggleFavorite(articleId: Long)

    @Query("SELECT COUNT(*) FROM articles WHERE is_read = 0 AND feed_id IN (SELECT id FROM feeds WHERE is_subscribed = 1)")
    fun getUnreadCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM articles WHERE is_read = 0 AND feed_id = :feedId")
    fun getUnreadCountByFeed(feedId: Long): Flow<Int>

    @Query("SELECT * FROM articles WHERE guid = :guid AND feed_id = :feedId LIMIT 1")
    suspend fun getArticleByGuid(feedId: Long, guid: String): ArticleEntity?

    @Query("DELETE FROM articles WHERE feed_id = :feedId")
    suspend fun deleteArticlesByFeed(feedId: Long)

    @Query("DELETE FROM articles WHERE guid IS NULL AND feed_id = :feedId AND link = :link")
    suspend fun deleteDuplicate(feedId: Long, link: String)
}
