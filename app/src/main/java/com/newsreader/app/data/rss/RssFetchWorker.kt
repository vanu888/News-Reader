package com.newsreader.app.data.rss

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.newsreader.app.data.db.AppDatabase
import com.newsreader.app.data.db.entity.ArticleEntity

class RssFetchWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    companion object {
        const val TAG = "RssFetchWorker"
        const val KEY_FEED_ID = "feed_id"
        const val KEY_FEED_URL = "feed_url"
    }

    override suspend fun doWork(): Result {
        val feedId = inputData.getLong(KEY_FEED_ID, -1L)
        val feedUrl = inputData.getString(KEY_FEED_URL) ?: return Result.failure()

        val db = AppDatabase.getInstance(applicationContext)
        val feedRepo = com.newsreader.app.data.repository.FeedRepository(db.feedDao())
        val articleRepo = com.newsreader.app.data.repository.ArticleRepository(
            db.articleDao(), db.feedDao()
        )

        return try {
            val parser = RssParser()
            val result = parser.fetchAndParse(feedUrl)

            // Update feed info
            val feed = feedRepo.getFeedById(feedId)
            if (feed != null) {
                feedRepo.updateFeed(
                    feed.copy(
                        title = result.title.ifEmpty { feed.title },
                        description = result.description ?: feed.description,
                        imageUrl = result.imageUrl ?: feed.imageUrl,
                        lastUpdated = System.currentTimeMillis(),
                        errorMessage = null
                    )
                )
            }

            // Insert new articles (deduplicate by guid or link)
            var newCount = 0
            for (article in result.articles) {
                if (article.title == "Untitled" && article.link.isEmpty()) continue

                val existing = if (article.guid != null) {
                    articleRepo.getArticleByGuid(feedId, article.guid)
                } else {
                    null
                }

                if (existing == null) {
                    val entity = ArticleEntity(
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
                    articleRepo.insertArticles(listOf(entity))
                    newCount++
                }
            }

            Log.d(TAG, "Fetched feed $feedUrl: ${result.articles.size} articles, $newCount new")
            Result.success()

        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch feed $feedUrl: ${e.message}")
            feedRepo.updateError(feedId, e.message ?: "Unknown error")
            if (runAttemptCount < 3) Result.retry() else Result.failure()
        }
    }
}
