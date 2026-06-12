package com.grcarmenaty.lifegame.domain.attention

import com.grcarmenaty.lifegame.data.dao.DaemonDao
import com.grcarmenaty.lifegame.data.dao.DialogueDao
import com.grcarmenaty.lifegame.domain.VoicePreset
import com.grcarmenaty.lifegame.domain.notify.NotificationPrefs

/**
 * Applies attention decay for one daemon. Skeptic-guard + user-toggle
 * gates from the v0.0.10 council outcomes:
 *
 *   - if [DaemonState.decayDisabled] → no decay (user kill switch)
 *   - if notifications are globally off (master toggle) AND notifications
 *     are off for this daemon → no decay (user opted out of attention)
 *   - never punish before [ResolvedAttentionConfig.decayGraceDays] of inactivity
 *
 * "No foreground sessions in the window" guard isn't wired in v0.0.10
 * (would need foreground-session tracking we don't have); the
 * notification-off proxy covers the same "user can't see" intent for now.
 *
 * Updates `lastAttentionUpdateAt` even when decay is skipped, so the
 * grace clock advances correctly relative to "the user is back" events.
 * Returns the attention drop applied (0 if none).
 */
class AttentionDecay(
    private val daemonDao: DaemonDao,
    private val dialogueDao: DialogueDao,
    private val notificationPrefs: NotificationPrefs,
) {
    suspend fun applyForAll(now: Long = System.currentTimeMillis()): Int {
        val masterEnabled = notificationPrefs.isMasterEnabled()
        var totalDrop = 0
        val daemons = daemonDao.getAll()
        for (daemon in daemons) {
            val state = dialogueDao.daemonState(daemon.id) ?: continue
            val voice = VoicePreset.fromKey(daemon.voicePreset)
            val cfg = AttentionConfig.resolve(state, voice)
            val drop = decayFor(state.lastAttentionUpdateAt, now, cfg, masterEnabled, state.notificationsEnabled)
            if (drop > 0) {
                dialogueDao.applyDecay(daemon.id, drop, now)
                totalDrop += drop
            }
        }
        return totalDrop
    }

    companion object {
        const val DAY_MILLIS = 24L * 60L * 60L * 1000L

        /**
         * Pure function (companion so JVM tests don't need DAOs or a
         * Context) for call-sites that already loaded state.
         */
        fun decayFor(
            lastAttentionUpdateAt: Long?,
            now: Long,
            cfg: ResolvedAttentionConfig,
            masterNotificationsEnabled: Boolean,
            daemonNotificationsEnabled: Boolean,
        ): Int {
            if (cfg.decayDisabled) return 0
            if (cfg.decayPerDay <= 0) return 0
            if (!masterNotificationsEnabled && !daemonNotificationsEnabled) return 0
            val anchor = lastAttentionUpdateAt ?: return 0  // never anchored = never decay yet
            val daysSinceUpdate = ((now - anchor) / DAY_MILLIS).toInt()
            val effectiveDays = daysSinceUpdate - cfg.decayGraceDays
            if (effectiveDays <= 0) return 0
            return effectiveDays * cfg.decayPerDay
        }
    }
}
