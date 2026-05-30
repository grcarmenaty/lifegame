package com.grcarmenaty.lifegame.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.grcarmenaty.lifegame.data.entities.CooldownPlay
import com.grcarmenaty.lifegame.data.entities.DaemonState
import com.grcarmenaty.lifegame.data.entities.LineSeen

@Dao
interface DialogueDao {

    // ---- line_seen ----

    @Query("SELECT * FROM line_seen WHERE daemonId = :daemonId")
    suspend fun lineSeenForDaemon(daemonId: Long): List<LineSeen>

    @Query("SELECT * FROM line_seen WHERE daemonId = :daemonId AND lineId = :lineId")
    suspend fun lineSeen(daemonId: Long, lineId: String): LineSeen?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertLineSeen(row: LineSeen)

    @Query("DELETE FROM line_seen")
    suspend fun deleteAllLineSeen()

    @Query("SELECT * FROM line_seen")
    suspend fun allLineSeen(): List<LineSeen>

    // ---- cooldown_play ----

    @Query("SELECT * FROM cooldown_play WHERE daemonId = :daemonId")
    suspend fun cooldownsForDaemon(daemonId: Long): List<CooldownPlay>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCooldown(row: CooldownPlay)

    @Query("DELETE FROM cooldown_play WHERE daemonId = :daemonId AND cooldownGroup = :group AND surface = :surface")
    suspend fun clearCooldown(daemonId: Long, group: String, surface: String)

    @Query("DELETE FROM cooldown_play")
    suspend fun deleteAllCooldowns()

    @Query("SELECT * FROM cooldown_play")
    suspend fun allCooldowns(): List<CooldownPlay>

    // ---- daemon_state ----

    @Query("SELECT * FROM daemon_state WHERE daemonId = :daemonId")
    suspend fun daemonState(daemonId: Long): DaemonState?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertDaemonState(row: DaemonState)

    @Query("UPDATE daemon_state SET majorsClosedTotal = majorsClosedTotal + 1 WHERE daemonId = :daemonId")
    suspend fun incrementMajorsClosed(daemonId: Long)

    @Query("UPDATE daemon_state SET wishesSpentTotal = wishesSpentTotal + 1 WHERE daemonId = :daemonId")
    suspend fun incrementWishesSpent(daemonId: Long)

    @Query("UPDATE daemon_state SET screenOpenCount = screenOpenCount + 1, lastScreenOpenAt = :at WHERE daemonId = :daemonId")
    suspend fun recordScreenOpen(daemonId: Long, at: Long)

    @Query("UPDATE daemon_state SET notificationsEnabled = :enabled WHERE daemonId = :daemonId")
    suspend fun setNotificationsEnabled(daemonId: Long, enabled: Boolean)

    @Query("UPDATE daemon_state SET lastNudgeAt = :at WHERE daemonId = :daemonId")
    suspend fun recordNudge(daemonId: Long, at: Long)

    @Query("DELETE FROM daemon_state")
    suspend fun deleteAllDaemonState()

    @Query("SELECT * FROM daemon_state")
    suspend fun allDaemonState(): List<DaemonState>

    // ---- bulk insert (import path) ----

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllLineSeen(rows: List<LineSeen>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllCooldowns(rows: List<CooldownPlay>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllDaemonState(rows: List<DaemonState>)
}
