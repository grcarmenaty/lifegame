package com.grcarmenaty.lifegame.data.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "major_quests",
    foreignKeys = [
        ForeignKey(
            entity = Daemon::class,
            parentColumns = ["id"],
            childColumns = ["daemonId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("daemonId")]
)
data class MajorQuest(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val daemonId: Long,
    val title: String,
    val description: String? = null,
    val thresholdCount: Int,
    val progressCount: Int = 0,
    val completed: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
