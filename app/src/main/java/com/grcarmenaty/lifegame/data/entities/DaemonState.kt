package com.grcarmenaty.lifegame.data.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

/**
 * Per-daemon dialogue/relationship state. Architect round 3 split this
 * off `Daemon` because `Daemon` is `@Serializable` and exported in
 * `PantheonBackup` — dialogue state was getting churned into the export
 * format. v5 (Skeptic): backup voice continuity demands this state
 * survive restore — so the v2 backup format now does include it, just
 * via a parallel `DaemonStateBackup` shape, not by reshaping `Daemon`.
 *
 * `screenOpenCount` and `lastScreenOpenAt` are the v0.0.7 cutdown
 * trigger's instrumentation (Believer's round-5 hill).
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
    /** Per-daemon notification toggle — added v0.0.7. */
    val notificationsEnabled: Boolean = true,
    /** Last time a nudge was actually shown; used by the worker to rate-limit. */
    val lastNudgeAt: Long? = null,
)
