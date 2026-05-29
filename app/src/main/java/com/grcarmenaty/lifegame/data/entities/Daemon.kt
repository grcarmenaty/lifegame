package com.grcarmenaty.lifegame.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "daemons")
data class Daemon(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val archetype: String,
    val voicePreset: String,
    val createdAt: Long = System.currentTimeMillis(),
)
