package com.grcarmenaty.lifegame.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "daemons")
data class Daemon(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val archetype: String,
    val voicePreset: String,
    val createdAt: Long = System.currentTimeMillis(),
    /**
     * v0.0.12: the [com.grcarmenaty.lifegame.domain.LifeTheme.key]
     * that scopes the daemon's dialogue corpus. Null = "Other" was
     * picked; the engine falls back to the base per-archetype lines.
     */
    val theme: String? = null,
    /**
     * v0.0.13: the drawable resource *entry name* of the user-chosen
     * face (e.g. `"face_oracle_exercise_2"`), resolved back to a
     * resId at render time via
     * [com.grcarmenaty.lifegame.domain.DaemonFaceCatalog]. Null =
     * the user never picked one; render falls back to the
     * deterministic per-(archetype, theme) variant keyed on the
     * daemon's id. Stored as a stable name string rather than the
     * resId because R values are not stable across builds/backups.
     */
    val face: String? = null,
)
