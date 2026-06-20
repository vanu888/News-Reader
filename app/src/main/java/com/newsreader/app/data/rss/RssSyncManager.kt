package com.newsreader.app.data.rss

import android.content.Context
import android.util.Log
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import java.util.concurrent.TimeUnit

class RssSyncManager(private val context: Context) {

    companion object {
        private const val PERIODIC_SYNC_TAG = "periodic_rss_sync"
        private const val PERIODIC_SYNC_NAME = "periodic_rss_sync_work"

        fun schedulePeriodicSync(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val request = PeriodicWorkRequestBuilder<RssPeriodicWorker>(
                30, TimeUnit.MINUTES
            )
                .setConstraints(constraints)
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    1, TimeUnit.MINUTES
                )
                .addTag(PERIODIC_SYNC_TAG)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                PERIODIC_SYNC_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }

        fun cancelPeriodicSync(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(PERIODIC_SYNC_NAME)
        }
    }

    fun syncFeed(feedId: Long, feedUrl: String) {
        val request = OneTimeWorkRequestBuilder<RssFetchWorker>()
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                30, TimeUnit.SECONDS
            )
            .setInputData(
                workDataOf(
                    RssFetchWorker.KEY_FEED_ID to feedId,
                    RssFetchWorker.KEY_FEED_URL to feedUrl
                )
            )
            .addTag("sync_feed_$feedId")
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            "sync_feed_$feedId",
            ExistingWorkPolicy.REPLACE,
            request
        )
        Log.d("RssSyncManager", "Queued sync for feed $feedId")
    }

    fun syncAllFeeds(feeds: List<Pair<Long, String>>) {
        for ((id, url) in feeds) {
            syncFeed(id, url)
        }
    }
}
