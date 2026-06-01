package com.grcarmenaty.lifegame.domain

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.userPrefs by preferencesDataStore(name = "user_prefs")

/**
 * Single-user identity prefs. Currently the daemon birthday in MM-DD
 * format (e.g. "04-23"). Stored as a string rather than two ints so the
 * "not set" case is just an absent key, no sentinel pair to remember.
 *
 * Region defaults to Catalonia/Barcelona — a region picker is planned
 * but not in this pass; HolidayCalendar uses Catalan defaults for now.
 */
class UserPrefs(private val context: Context) {

    private val birthdayKey = stringPreferencesKey("user_birthday_mmdd")

    /** "MM-DD" (e.g. "04-23") or null if unset. */
    val birthdayMonthDay: Flow<String?> = context.userPrefs.data
        .map { it[birthdayKey] }

    suspend fun getBirthdayMonthDay(): String? = birthdayMonthDay.first()

    suspend fun setBirthdayMonthDay(value: String?) {
        context.userPrefs.edit { prefs ->
            if (value == null) prefs.remove(birthdayKey)
            else prefs[birthdayKey] = value
        }
    }

    companion object {
        /** Returns true when [mmdd] is a sane "MM-DD" — Feb-30 etc. fail. */
        fun isValidMmDd(mmdd: String): Boolean {
            val parts = mmdd.split("-")
            if (parts.size != 2) return false
            val m = parts[0].toIntOrNull() ?: return false
            val d = parts[1].toIntOrNull() ?: return false
            if (m !in 1..12) return false
            val maxDay = when (m) {
                1, 3, 5, 7, 8, 10, 12 -> 31
                4, 6, 9, 11 -> 30
                2 -> 29 // permissive for leap birthdays
                else -> 31
            }
            return d in 1..maxDay
        }

        /** "MM-DD" → Pair(month1to12, day) or null if invalid. */
        fun parseMmDd(mmdd: String?): Pair<Int, Int>? {
            if (mmdd == null) return null
            if (!isValidMmDd(mmdd)) return null
            val parts = mmdd.split("-")
            return parts[0].toInt() to parts[1].toInt()
        }
    }
}
