package com.newsreader.app.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "articles",
    foreignKeys = [
        ForeignKey(
            entity = FeedEntity::class,
            parentColumns = ["id"],
            childColumns = ["feed_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("feed_id")]
)
data class ArticleEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "feed_id")
    val feedId: Long,
    val title: String,
    val link: String,
    val description: String? = null,
    val content: String? = null,
    @ColumnInfo(name = "image_url")
    val imageUrl: String? = null,
    @ColumnInfo(name = "pub_date")
    val pubDate: Long? = null,
    val author: String? = null,
    @ColumnInfo(name = "is_read")
    val isRead: Boolean = false,
    @ColumnInfo(name = "is_favorite")
    val isFavorite: Boolean = false,
    @ColumnInfo(name = "fetched_at")
    val fetchedAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "guid")
    val guid: String? = null
)
