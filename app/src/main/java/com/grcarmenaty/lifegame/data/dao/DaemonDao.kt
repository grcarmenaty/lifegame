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

    @Query("SELECT * FROM daemons ORDER BY createdAt ASC")
    suspend fun getAll(): List<Daemon>

    @Query("SELECT * FROM daemons WHERE id = :id")
    fun observe(id: Long): Flow<Daemon?>

    @Query("SELECT COUNT(*) FROM daemons")
    suspend fun count(): Int

    @Query("SELECT * FROM daemons WHERE id = :id")
    suspend fun getById(id: Long): Daemon?

    @Insert
    suspend fun insert(daemon: Daemon): Long

    @Update
    suspend fun update(daemon: Daemon)

    @Query("DELETE FROM daemons WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM daemons")
    suspend fun deleteAll()
}
