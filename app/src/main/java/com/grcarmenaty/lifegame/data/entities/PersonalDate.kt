package com.grcarmenaty.lifegame.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

/**
 * A user-authored calendar date the daemons should acknowledge — e.g.
 * an anniversary, a recurring deadline, a remembrance day. Stored
 * month/day-of-month (year-agnostic) so the line fires every year.
 *
 * Month is 1-12 (NOT [java.util.Calendar.MONTH]'s 0-11 convention) so
 * the backup wire format reads naturally. The DAO and lookup helpers
 * normalize between the two.
 *
 * Backup format v4 carries this table (Skeptic: opt-in via the future
 * per-record `includeInBackup` flag; v0.0.11 ships always-included
 * since the records are tiny and user-authored).
 */
@Serializable
@Entity(tableName = "personal_date")
data class PersonalDate(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val label: String,
    val month: Int,   // 1-12
    val day: Int,     // 1-31
    val createdAt: Long = System.currentTimeMillis(),
)
