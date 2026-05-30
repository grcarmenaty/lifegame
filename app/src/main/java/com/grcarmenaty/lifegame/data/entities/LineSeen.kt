package com.grcarmenaty.lifegame.data.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import kotlinx.serialization.Serializable

@Serializable
@Entity(
    tableName = "line_seen",
    primaryKeys = ["daemonId", "lineId"],
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
data class LineSeen(
    val daemonId: Long,
    val lineId: String,
    val lastPlayedAt: Long,
    val playCount: Int,
)
