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
 * Supported regions for the holiday calendar. Only one value ships in
 * v0.0.11 — the calendar is hardcoded to Catalan / Barcelona defaults.
 * The enum + the UI dropdown make the location-driven shape of the
 * calendar visible and leave a real extension point for later
 * regions; today, picking anything else would surface no holidays at
 * all, so the picker rejects it.
 */
enum class SupportedRegion(val key: String, val display: String) {
    BARCELONA("BARCELONA", "Barcelona / Catalonia"),
    ;
    companion object {
        val DEFAULT = BARCELONA
        fun fromKey(key: String?): SupportedRegion = entries.firstOrNull { it.key == key } ?: DEFAULT
    }
}

/**
 * Single-user identity prefs. Stores the user's birthday (MM-DD
 * string) and the active region for the holiday calendar.
 *
 * Birthday is stored as a string rather than two ints so the
 * "not set" case is just an absent key, no sentinel pair to remember.
 * Region defaults to [SupportedRegion.DEFAULT].
 */
class UserPrefs(private val context: Context) {

    private val birthdayKey = stringPreferencesKey("user_birthday_mmdd")
    private val regionKey = stringPreferencesKey("user_region")

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

    /** Active region. Defaults to [SupportedRegion.DEFAULT] when unset. */
    val region: Flow<SupportedRegion> = context.userPrefs.data
        .map { SupportedRegion.fromKey(it[regionKey]) }

    suspend fun getRegion(): SupportedRegion = region.first()

    suspend fun setRegion(value: SupportedRegion) {
        context.userPrefs.edit { prefs -> prefs[regionKey] = value.key }
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
