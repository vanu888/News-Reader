package com.newsreader.app.domain.model

data class Article(
    val id: Long = 0,
    val feedId: Long,
    val feedTitle: String = "",
    val feedImageUrl: String? = null,
    val title: String,
    val link: String,
    val description: String? = null,
    val content: String? = null,
    val imageUrl: String? = null,
    val pubDate: Long? = null,
    val author: String? = null,
    val isRead: Boolean = false,
    val isFavorite: Boolean = false,
    val fetchedAt: Long = System.currentTimeMillis()
)
