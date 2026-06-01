package com.grcarmenaty.lifegame.data.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(
    tableName = "minor_quests",
    foreignKeys = [
        ForeignKey(
            entity = MajorQuest::class,
            parentColumns = ["id"],
            childColumns = ["majorQuestId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("majorQuestId")]
)
data class MinorQuest(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val majorQuestId: Long,
    val title: String,
    val cadence: String,
    val weight: Int = 1,
    val completed: Boolean = false,
    val lastCompletedAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
) {
    companion object {
        const val CADENCE_ONE_OFF = "ONE_OFF"
        const val CADENCE_DAILY = "DAILY"
        const val CADENCE_WEEKLY = "WEEKLY"
        const val CADENCE_MONTHLY = "MONTHLY"

        /** Display order in pickers. One-off first; the repeatable cadences ascending. */
        val ALL_CADENCES = listOf(CADENCE_ONE_OFF, CADENCE_DAILY, CADENCE_WEEKLY, CADENCE_MONTHLY)

        fun cadenceLabel(cadence: String): String = when (cadence) {
            CADENCE_ONE_OFF -> "One-off"
            CADENCE_DAILY -> "Daily"
            CADENCE_WEEKLY -> "Weekly"
            CADENCE_MONTHLY -> "Monthly"
            else -> cadence
        }
    }
}
