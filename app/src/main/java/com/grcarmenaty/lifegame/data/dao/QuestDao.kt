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

    @Query("SELECT * FROM major_quests WHERE daemonId = :daemonId ORDER BY createdAt ASC")
    suspend fun getMajorsForDaemon(daemonId: Long): List<MajorQuest>

    @Query("SELECT COUNT(*) FROM major_quests WHERE daemonId = :daemonId AND completed = 1")
    suspend fun countCompletedMajorsForDaemon(daemonId: Long): Int

    @Query("SELECT * FROM major_quests WHERE id = :id")
    suspend fun getMajorById(id: Long): MajorQuest?

    @Query("SELECT COUNT(*) FROM minor_quests WHERE majorQuestId = :majorId AND completed = 1")
    suspend fun countCompletedMinorsForMajor(majorId: Long): Int

    @Query("SELECT COUNT(*) FROM minor_quests WHERE majorQuestId = :majorId")
    suspend fun countMinorsForMajor(majorId: Long): Int

    @Insert
    suspend fun insertMajor(major: MajorQuest): Long

    @Update
    suspend fun updateMajor(major: MajorQuest)

    @Query("DELETE FROM major_quests WHERE id = :id")
    suspend fun deleteMajorById(id: Long)

    @Query("SELECT * FROM minor_quests WHERE majorQuestId = :majorId ORDER BY createdAt ASC")
    fun observeMinorsForMajor(majorId: Long): Flow<List<MinorQuest>>

    @Query("SELECT * FROM minor_quests WHERE majorQuestId = :majorId ORDER BY createdAt ASC")
    suspend fun getMinorsForMajor(majorId: Long): List<MinorQuest>

    @Query("SELECT * FROM minor_quests WHERE id = :id")
    suspend fun getMinorById(id: Long): MinorQuest?

    @Insert
    suspend fun insertMinor(minor: MinorQuest): Long

    @Update
    suspend fun updateMinor(minor: MinorQuest)

    @Query("DELETE FROM minor_quests WHERE id = :id")
    suspend fun deleteMinorById(id: Long)
}
