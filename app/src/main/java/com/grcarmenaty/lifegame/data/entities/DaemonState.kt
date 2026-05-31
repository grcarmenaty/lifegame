package com.grcarmenaty.lifegame.data.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

/**
 * Per-daemon dialogue / relationship state. v0.0.10 absorbs the
 * attention economy: `attentionPoints` is the central currency,
 * driving level (derived) and decaying when neglected. Decay rate
 * and grace period are nullable overrides — null falls back to the
 * archetype default on `VoicePreset`. Same for
 * `minorsPerBoonAccrual`.
 *
 * `lastSeenLevel` is the fire-once gate for the boon-level-up
 * prompt: when the computed level exceeds it, the UI surfaces the
 * "your relationship has grown" affordance, then bumps it.
 * Per Ally round 1 polish: `lastSeenLevel` only grows; decay never
 * rewinds it, so a level-down + level-back-up doesn't re-prompt.
 */
@Serializable
@Entity(
    tableName = "daemon_state",
    foreignKeys = [
        ForeignKey(
            entity = Daemon::class,
            parentColumns = ["id"],
            childColumns = ["daemonId"],
            onDelete = ForeignKey.CASCADE,
        )
    ],
)
data class DaemonState(
    @PrimaryKey val daemonId: Long,
    val lastConversationAt: Long? = null,
    val firstConversationAt: Long? = null,
    val conversationsHad: Int = 0,
    val majorsClosedTotal: Int = 0,
    val wishesSpentTotal: Int = 0,
    val screenOpenCount: Int = 0,
    val lastScreenOpenAt: Long? = null,
    /** Per-daemon notification toggle — added v0.0.7. Also gates decay. */
    val notificationsEnabled: Boolean = true,
    val lastNudgeAt: Long? = null,
    // v0.0.10 attention economy:
    val attentionPoints: Int = 0,
    val lastAttentionUpdateAt: Long? = null,
    /** Override; null = use [VoicePreset.decayPerDay]. */
    val attentionDecayPerDay: Int? = null,
    /** Override; null = use [VoicePreset.decayGraceDays]. */
    val attentionDecayGraceDays: Int? = null,
    /** User-driven kill switch for decay on this specific daemon. */
    val decayDisabled: Boolean = false,
    val minorsCompletedSinceAccrual: Int = 0,
    /** Override; null = use [VoicePreset.minorsPerBoonAccrual]. */
    val minorsPerBoonAccrual: Int? = null,
    /**
     * Greatest level we've shown the boon-level-up prompt for.
     * `currentLevel > lastSeenLevel` ⇒ pending prompt. Never rewinds
     * on decay (Ally polish, round 1).
     */
    val lastSeenLevel: Int = 0,
)
