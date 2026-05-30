package com.grcarmenaty.lifegame.domain.notify

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.content.getSystemService

object NotificationChannels {
    const val DAEMON_NUDGES = "daemon_nudges"

    /** Idempotent — safe to call on every app start. */
    fun ensureCreated(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val mgr = context.getSystemService<NotificationManager>() ?: return
        val channel = NotificationChannel(
            DAEMON_NUDGES,
            "Daemon nudges",
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = "When a daemon thinks you should pay attention to its quests."
        }
        mgr.createNotificationChannel(channel)
    }
}
