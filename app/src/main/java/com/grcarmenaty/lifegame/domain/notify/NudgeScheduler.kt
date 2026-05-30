package com.grcarmenaty.lifegame.domain.notify

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

object NudgeScheduler {
    private const val UNIQUE_WORK_NAME = "daemon_nudges"

    fun schedule(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
            .build()
        // Min period for PeriodicWorkRequest is 15 minutes. 2-hour interval
        // is the rate-limit; the engine's cooldown groups handle finer
        // per-line throttling.
        val request = PeriodicWorkRequestBuilder<NudgeWorker>(
            repeatInterval = 2, repeatIntervalTimeUnit = TimeUnit.HOURS,
            flexTimeInterval = 30, flexTimeIntervalUnit = TimeUnit.MINUTES,
        )
            .setConstraints(constraints)
            .setInitialDelay(30, TimeUnit.MINUTES)
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
