package com.newsreader.app.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "feeds")
data class FeedEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val url: String,
    val description: String? = null,
    @ColumnInfo(name = "image_url")
    val imageUrl: String? = null,
    @ColumnInfo(name = "category_id")
    val categoryId: Long? = null,
    @ColumnInfo(name = "is_subscribed")
    val isSubscribed: Boolean = true,
    @ColumnInfo(name = "last_updated")
    val lastUpdated: Long? = null,
    @ColumnInfo(name = "error_message")
    val errorMessage: String? = null
)
