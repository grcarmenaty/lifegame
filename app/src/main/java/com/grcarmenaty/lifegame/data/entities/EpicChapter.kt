package com.grcarmenaty.lifegame.data.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

/**
 * A user-authored narrative beat on a daemon's running arc. Authored
 * (optionally) at major closure as part of apotheosis. The Scripture
 * view on daemon detail lists chapters in `position` order.
 *
 * `position` is the index in the daemon's chapter list; the writer
 * assigns it at insert time (currently = existing count for the
 * daemon).
 */
@Serializable
@Entity(
    tableName = "epic_chapter",
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
data class EpicChapter(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val daemonId: Long,
    val position: Int,
    val text: String,
    val createdAt: Long = System.currentTimeMillis(),
)
