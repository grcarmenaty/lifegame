package com.grcarmenaty.lifegame.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.grcarmenaty.lifegame.data.entities.Daemon
import kotlinx.coroutines.flow.Flow

@Dao
interface DaemonDao {
    @Query("SELECT * FROM daemons ORDER BY createdAt ASC")
    fun observeAll(): Flow<List<Daemon>>

    @Query("SELECT COUNT(*) FROM daemons")
    suspend fun count(): Int

    @Query("SELECT * FROM daemons WHERE id = :id")
    suspend fun getById(id: Long): Daemon?

    @Insert
    suspend fun insert(daemon: Daemon): Long

    @Update
    suspend fun update(daemon: Daemon)
}
