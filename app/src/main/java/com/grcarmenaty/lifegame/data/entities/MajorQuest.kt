package com.grcarmenaty.lifegame.data.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(
    tableName = "major_quests",
    foreignKeys = [
        ForeignKey(
            entity = Daemon::class,
            parentColumns = ["id"],
            childColumns = ["daemonId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = Boon::class,
            parentColumns = ["id"],
            childColumns = ["wishBoonId"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [Index("daemonId"), Index("wishBoonId")],
)
data class MajorQuest(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val daemonId: Long,
    val title: String,
    val description: String? = null,
    val thresholdCount: Int,
    val progressCount: Int = 0,
    val completed: Boolean = false,
    val wishBoonId: Long? = null,
    val wishRewardCount: Int = 1,
    val createdAt: Long = System.currentTimeMillis(),
    /**
     * v0.0.14: when this major was picked from the pre-authored
     * [com.grcarmenaty.lifegame.domain.catalog.QuestCatalog], its stable
     * catalog id — used to look up its quest-specific completion
     * fragment for the apotheosis line. Null for user-authored majors,
     * which fall back to the daemon's voice/engine line.
     */
    val templateId: String? = null,
)
