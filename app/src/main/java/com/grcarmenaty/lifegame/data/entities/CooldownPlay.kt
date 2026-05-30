package com.grcarmenaty.lifegame.data.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import kotlinx.serialization.Serializable

/**
 * `surface` is one of "INLINE", "SCREEN", "BOTH". The engine reads
 *   WHERE surface = ? OR surface = 'BOTH'
 * so a cross-surface cooldown blocks both consumers.
 */
@Serializable
@Entity(
    tableName = "cooldown_play",
    primaryKeys = ["daemonId", "cooldownGroup", "surface"],
    foreignKeys = [
        ForeignKey(
            entity = Daemon::class,
            parentColumns = ["id"],
            childColumns = ["daemonId"],
            onDelete = ForeignKey.CASCADE,
        )
    ],
    indices = [Index("daemonId")],
)
data class CooldownPlay(
    val daemonId: Long,
    val cooldownGroup: String,
    val surface: String,
    val expiresAtPicks: Int,
)
