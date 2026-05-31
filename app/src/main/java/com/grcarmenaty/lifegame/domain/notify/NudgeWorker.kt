package com.grcarmenaty.lifegame.domain.notify

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.grcarmenaty.lifegame.LifegameApplication
import com.grcarmenaty.lifegame.MainActivity
import com.grcarmenaty.lifegame.R
import com.grcarmenaty.lifegame.domain.dialogue.LineCategory
import java.util.Calendar

/**
 * Periodic nudge sweep. Runs roughly every 2 hours via WorkManager.
 * For each daemon with notifications enabled, asks the dialogue engine
 * for a `NUDGE`-category line. Posts at most ONE notification per run
 * (human-friendly cap) so a user with 10 daemons doesn't get carpet-bombed.
 *
 * Quiet hours short-circuit early: the worker still runs, but if the
 * current hour is inside the user's quiet window, no notifications post.
 *
 * The line picked is `markPlayed` via the engine's normal path so the
 * same nudge doesn't repeat next run.
 */
class NudgeWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as LifegameApplication
        val prefs = NotificationPrefs(applicationContext)

        if (!prefs.isMasterEnabled()) return Result.success()
        if (!hasPostNotificationsPermission()) return Result.success()

        val (qStart, qEnd) = prefs.getQuietHours()
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        if (NotificationPrefs.isQuietHour(hour, qStart, qEnd)) return Result.success()

        val repo = app.repository
        // Architect's "decay-before-nudge" rule: nudge text references
        // current attention, so settle decay first.
        runCatching { repo.runDecay() }
        val daemons = repo.allDaemonsForNudge()
        if (daemons.isEmpty()) return Result.success()

        // Find at most ONE daemon to nudge this run, preferring the one
        // whose last nudge is oldest (or who has never been nudged).
        val candidates = daemons
            .filter { it.notificationsEnabled }
            .sortedBy { it.lastNudgeAt ?: 0L }

        for (entry in candidates) {
            val text = repo.pickNudgeLine(entry.daemonId) ?: continue
            postNudge(entry.daemonId, entry.daemonName, text)
            repo.recordNudgeShown(entry.daemonId)
            return Result.success()
        }
        return Result.success()
    }

    private fun hasPostNotificationsPermission(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return ContextCompat.checkSelfPermission(
            applicationContext, Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun postNudge(daemonId: Long, daemonName: String, text: String) {
        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_DAEMON_ID, daemonId)
        }
        val pendingFlags = PendingIntent.FLAG_UPDATE_CURRENT or
            (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
        val pi = PendingIntent.getActivity(
            applicationContext, daemonId.toInt(), intent, pendingFlags,
        )
        val notification = NotificationCompat.Builder(applicationContext, NotificationChannels.DAEMON_NUDGES)
            .setContentTitle(daemonName)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setSmallIcon(R.mipmap.ic_launcher)
            .setAutoCancel(true)
            .setContentIntent(pi)
            .build()
        try {
            NotificationManagerCompat.from(applicationContext)
                .notify(daemonId.toInt(), notification)
        } catch (_: SecurityException) {
            // POST_NOTIFICATIONS revoked between our check and post. Skip.
        }
    }

    companion object {
        const val EXTRA_DAEMON_ID = "com.grcarmenaty.lifegame.NUDGE_DAEMON_ID"
    }
}
