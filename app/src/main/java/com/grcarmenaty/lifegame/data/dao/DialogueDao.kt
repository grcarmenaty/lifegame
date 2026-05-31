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

    @Query("SELECT * FROM daemon_state WHERE daemonId = :daemonId")
    fun observeDaemonState(daemonId: Long): kotlinx.coroutines.flow.Flow<DaemonState?>

    @Query("SELECT * FROM daemon_state")
    fun observeAllDaemonState(): kotlinx.coroutines.flow.Flow<List<DaemonState>>

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

    // ---- v0.0.10 attention economy ----

    @Query("""
        UPDATE daemon_state
        SET attentionPoints = MAX(0, attentionPoints + :delta),
            lastAttentionUpdateAt = :at
        WHERE daemonId = :daemonId
    """)
    suspend fun addAttention(daemonId: Long, delta: Int, at: Long)

    @Query("""
        UPDATE daemon_state
        SET attentionPoints = MAX(0, attentionPoints - :amount),
            lastAttentionUpdateAt = :at
        WHERE daemonId = :daemonId
    """)
    suspend fun applyDecay(daemonId: Long, amount: Int, at: Long)

    @Query("UPDATE daemon_state SET lastSeenLevel = :level WHERE daemonId = :daemonId AND lastSeenLevel < :level")
    suspend fun bumpLastSeenLevel(daemonId: Long, level: Int)

    @Query("UPDATE daemon_state SET minorsCompletedSinceAccrual = :value WHERE daemonId = :daemonId")
    suspend fun setMinorsCompletedSinceAccrual(daemonId: Long, value: Int)

    @Query("UPDATE daemon_state SET attentionDecayPerDay = :decay, attentionDecayGraceDays = :grace WHERE daemonId = :daemonId")
    suspend fun setDecayOverride(daemonId: Long, decay: Int?, grace: Int?)

    @Query("UPDATE daemon_state SET decayDisabled = :disabled WHERE daemonId = :daemonId")
    suspend fun setDecayDisabled(daemonId: Long, disabled: Boolean)

    @Query("UPDATE daemon_state SET minorsPerBoonAccrual = :value WHERE daemonId = :daemonId")
    suspend fun setMinorsPerBoonOverride(daemonId: Long, value: Int?)

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
