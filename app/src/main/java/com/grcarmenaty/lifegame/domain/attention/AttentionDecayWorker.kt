package com.grcarmenaty.lifegame.domain.attention

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.grcarmenaty.lifegame.LifegameApplication

/**
 * Periodic attention decay. Runs every ~12 hours as a floor — the
 * `NudgeWorker` also calls `repository.runDecay()` inline as its first
 * step (Architect rule: decay before nudge so notification text reads
 * current attention).
 */
class AttentionDecayWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as LifegameApplication
        runCatching { app.repository.runDecay() }
            .getOrElse { return Result.retry() }
        return Result.success()
    }
}
