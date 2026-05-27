package com.grcarmenaty.lifegame.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.grcarmenaty.lifegame.data.entities.MajorQuest
import com.grcarmenaty.lifegame.data.entities.MinorQuest
import kotlinx.coroutines.flow.Flow

@Dao
interface QuestDao {
    @Query("SELECT * FROM major_quests WHERE daemonId = :daemonId ORDER BY createdAt ASC")
    fun observeMajorsForDaemon(daemonId: Long): Flow<List<MajorQuest>>

    @Query("SELECT COUNT(*) FROM major_quests WHERE daemonId = :daemonId AND completed = 1")
    suspend fun countCompletedMajorsForDaemon(daemonId: Long): Int

    @Query("SELECT * FROM major_quests WHERE id = :id")
    suspend fun getMajorById(id: Long): MajorQuest?

    @Insert
    suspend fun insertMajor(major: MajorQuest): Long

    @Update
    suspend fun updateMajor(major: MajorQuest)

    @Query("SELECT * FROM minor_quests WHERE majorQuestId = :majorId ORDER BY createdAt ASC")
    fun observeMinorsForMajor(majorId: Long): Flow<List<MinorQuest>>

    @Query("SELECT * FROM minor_quests WHERE id = :id")
    suspend fun getMinorById(id: Long): MinorQuest?

    @Insert
    suspend fun insertMinor(minor: MinorQuest): Long

    @Update
    suspend fun updateMinor(minor: MinorQuest)
}
