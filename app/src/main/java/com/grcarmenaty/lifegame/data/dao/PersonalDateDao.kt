package com.grcarmenaty.lifegame.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.grcarmenaty.lifegame.data.entities.PersonalDate
import kotlinx.coroutines.flow.Flow

@Dao
interface PersonalDateDao {

    @Query("SELECT * FROM personal_date ORDER BY month, day")
    fun observeAll(): Flow<List<PersonalDate>>

    @Query("SELECT * FROM personal_date ORDER BY month, day")
    suspend fun getAll(): List<PersonalDate>

    @Query("SELECT * FROM personal_date WHERE month = :month AND day = :day LIMIT 1")
    suspend fun forMonthDay(month: Int, day: Int): PersonalDate?

    @Insert
    suspend fun insert(row: PersonalDate): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(rows: List<PersonalDate>)

    @Query("DELETE FROM personal_date WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM personal_date")
    suspend fun deleteAll()
}
