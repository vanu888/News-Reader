package com.newsreader.app.domain.model

data class Feed(
    val id: Long = 0,
    val title: String,
    val url: String,
    val description: String? = null,
    val imageUrl: String? = null,
    val categoryId: Long? = null,
    val isSubscribed: Boolean = true,
    val lastUpdated: Long? = null,
    val errorMessage: String? = null
)
