package com.grcarmenaty.lifegame.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
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

    /**
     * Count of minors under any of [daemonId]'s majors whose
     * `lastCompletedAt` is on/after [sinceMillis]. Used by
     * `buildContext` to populate `minorsCompletedToday` /
     * `minorsCompletedThisWeek`. Indexed by `majorQuestId`; SQLite
     * resolves the IN-subquery cheaply for small fanouts.
     */
    @Query(
        """
        SELECT COUNT(*) FROM minor_quests
        WHERE majorQuestId IN (SELECT id FROM major_quests WHERE daemonId = :daemonId)
          AND lastCompletedAt IS NOT NULL
          AND lastCompletedAt >= :sinceMillis
        """
    )
    suspend fun countMinorsCompletedSince(daemonId: Long, sinceMillis: Long): Int

    /**
     * Count of DAILY minors under [daemonId]'s majors that have a
     * historical completion AND have NOT been completed today (i.e.
     * `lastCompletedAt < :startOfTodayMillis`). Drives `AfterLapse`.
     */
    @Query(
        """
        SELECT COUNT(*) FROM minor_quests
        WHERE majorQuestId IN (SELECT id FROM major_quests WHERE daemonId = :daemonId)
          AND cadence = 'DAILY'
          AND lastCompletedAt IS NOT NULL
          AND lastCompletedAt < :startOfTodayMillis
        """
    )
    suspend fun countDailyMinorsLapsed(daemonId: Long, startOfTodayMillis: Long): Int

    @Query("SELECT COUNT(*) FROM minor_quests WHERE majorQuestId = :majorId")
    suspend fun countMinorsForMajor(majorId: Long): Int

    @Insert
    suspend fun insertMajor(major: MajorQuest): Long

    /**
     * Undo-restore: re-insert a snapshot with its original id so child
     * minors keep a valid FK. IGNORE (returns -1) if the id was reused
     * by a newer row while the undo snackbar was showing.
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun restoreMajor(major: MajorQuest): Long

    @Update
    suspend fun updateMajor(major: MajorQuest)

    @Query("DELETE FROM major_quests WHERE id = :id")
    suspend fun deleteMajorById(id: Long)

    @Query("SELECT id FROM major_quests WHERE wishBoonId = :boonId")
    suspend fun majorIdsGrantingBoon(boonId: Long): List<Long>

    /**
     * Undo-restore for a deleted boon: the FK's SET_NULL wiped these
     * majors' `wishBoonId`; point them back at the restored boon.
     */
    @Query("UPDATE major_quests SET wishBoonId = :boonId WHERE id IN (:majorIds)")
    suspend fun relinkWishBoon(majorIds: List<Long>, boonId: Long)

    @Query("SELECT * FROM minor_quests WHERE majorQuestId = :majorId ORDER BY createdAt ASC")
    fun observeMinorsForMajor(majorId: Long): Flow<List<MinorQuest>>

    @Query("SELECT * FROM minor_quests WHERE majorQuestId = :majorId ORDER BY createdAt ASC")
    suspend fun getMinorsForMajor(majorId: Long): List<MinorQuest>

    @Query("SELECT * FROM minor_quests WHERE id = :id")
    suspend fun getMinorById(id: Long): MinorQuest?

    @Insert
    suspend fun insertMinor(minor: MinorQuest): Long

    /** Undo-restore with original id; -1 if the id was reused meanwhile. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun restoreMinor(minor: MinorQuest): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun restoreMinors(minors: List<MinorQuest>)

    @Update
    suspend fun updateMinor(minor: MinorQuest)

    @Query("DELETE FROM minor_quests WHERE id = :id")
    suspend fun deleteMinorById(id: Long)
}
