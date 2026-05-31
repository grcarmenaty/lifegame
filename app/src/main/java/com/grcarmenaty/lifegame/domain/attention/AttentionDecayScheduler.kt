package com.grcarmenaty.lifegame.domain.attention

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

object AttentionDecayScheduler {
    private const val UNIQUE_WORK_NAME = "attention_decay"

    fun schedule(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
            .build()
        // 12-hour cadence with 1-hour flex window. The actual decay
        // arithmetic is whole-day granularity so the worker just has
        // to fire often enough that lapses are caught promptly.
        val request = PeriodicWorkRequestBuilder<AttentionDecayWorker>(
            repeatInterval = 12, repeatIntervalTimeUnit = TimeUnit.HOURS,
            flexTimeInterval = 1, flexTimeIntervalUnit = TimeUnit.HOURS,
        )
            .setConstraints(constraints)
            .setInitialDelay(1, TimeUnit.HOURS)
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            UNIQUE_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request,
        )
    }

    fun cancel(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(UNIQUE_WORK_NAME)
    }
}
