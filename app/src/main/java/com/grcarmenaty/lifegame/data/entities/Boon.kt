package com.grcarmenaty.lifegame.data.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(
    tableName = "boons",
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
data class Boon(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val daemonId: Long,
    val text: String,
    val count: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
)
