package com.grcarmenaty.lifegame.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.grcarmenaty.lifegame.data.entities.EpicChapter
import kotlinx.coroutines.flow.Flow

@Dao
interface EpicChapterDao {

    @Query("SELECT * FROM epic_chapter WHERE daemonId = :daemonId ORDER BY position ASC")
    fun observeForDaemon(daemonId: Long): Flow<List<EpicChapter>>

    @Query("SELECT * FROM epic_chapter WHERE daemonId = :daemonId ORDER BY position ASC")
    suspend fun forDaemon(daemonId: Long): List<EpicChapter>

    @Query("SELECT COUNT(*) FROM epic_chapter WHERE daemonId = :daemonId")
    suspend fun countForDaemon(daemonId: Long): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(chapter: EpicChapter): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(chapters: List<EpicChapter>)

    @Query("SELECT * FROM epic_chapter")
    suspend fun all(): List<EpicChapter>

    @Query("DELETE FROM epic_chapter WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM epic_chapter")
    suspend fun deleteAll()
}
