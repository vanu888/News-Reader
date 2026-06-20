package com.newsreader.app.data.rss

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.newsreader.app.data.db.AppDatabase
import com.newsreader.app.data.db.entity.FeedEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

class RssPeriodicWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val db = AppDatabase.getInstance(applicationContext)
            val feedDao = db.feedDao()
            val syncManager = RssSyncManager(applicationContext)

            val feeds = feedDao.getSubscribedFeeds().first()

            for (feed in feeds) {
                syncManager.syncFeed(feed.id, feed.url)
            }

            Result.success()
        } catch (e: Exception) {
            if (runAttemptCount < 3) Result.retry() else Result.failure()
        }
    }
}
