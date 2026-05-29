package com.grcarmenaty.lifegame.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import com.grcarmenaty.lifegame.data.entities.Boon
import kotlinx.coroutines.flow.Flow

@Dao
interface BoonDao {
    @Query("SELECT * FROM boons WHERE daemonId = :daemonId ORDER BY createdAt ASC")
    fun observeForDaemon(daemonId: Long): Flow<List<Boon>>

    @Query("SELECT * FROM boons WHERE daemonId = :daemonId ORDER BY createdAt ASC")
    suspend fun getForDaemon(daemonId: Long): List<Boon>

    @Query("SELECT * FROM boons WHERE id = :id")
    suspend fun getById(id: Long): Boon?

    @Insert
    suspend fun insert(boon: Boon): Long

    @Query("UPDATE boons SET count = count + :delta WHERE id = :id")
    suspend fun incrementCount(id: Long, delta: Int)

    /**
     * Spend one wish of this boon. Returns 1 if a row was decremented
     * (count was > 0), 0 otherwise — the caller treats 0 as "no wish
     * left, race lost".
     */
    @Query("UPDATE boons SET count = count - 1 WHERE id = :id AND count > 0")
    suspend fun decrementIfAvailable(id: Long): Int

    @Query("DELETE FROM boons WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Transaction
    suspend fun spend(id: Long): Boon? {
        val before = getById(id) ?: return null
        if (decrementIfAvailable(id) == 0) return null
        return before
    }
}
