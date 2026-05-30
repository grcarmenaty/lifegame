package com.grcarmenaty.lifegame.domain.notify

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.notificationPrefs by preferencesDataStore(name = "notification_prefs")

/**
 * Global notification preferences. Per-daemon toggle lives on
 * `daemon_state.notificationsEnabled`. Quiet hours are stored as
 * two integer hours (24h clock); they may wrap midnight
 * (e.g., `quietStart = 22, quietEnd = 8` ⇒ 10pm-8am quiet).
 */
class NotificationPrefs(private val context: Context) {

    private val masterKey = booleanPreferencesKey("master_enabled")
    private val quietStartKey = intPreferencesKey("quiet_start_hour")
    private val quietEndKey = intPreferencesKey("quiet_end_hour")

    val masterEnabled: Flow<Boolean> = context.notificationPrefs.data
        .map { it[masterKey] ?: true }

    val quietStart: Flow<Int> = context.notificationPrefs.data
        .map { it[quietStartKey] ?: 22 }

    val quietEnd: Flow<Int> = context.notificationPrefs.data
        .map { it[quietEndKey] ?: 8 }

    suspend fun isMasterEnabled(): Boolean = masterEnabled.first()

    suspend fun getQuietHours(): Pair<Int, Int> =
        quietStart.first() to quietEnd.first()

    suspend fun setMaster(enabled: Boolean) {
        context.notificationPrefs.edit { it[masterKey] = enabled }
    }

    suspend fun setQuietHours(start: Int, end: Int) {
        val s = start.coerceIn(0, 23)
        val e = end.coerceIn(0, 23)
        context.notificationPrefs.edit {
            it[quietStartKey] = s
            it[quietEndKey] = e
        }
    }

    companion object {
        /** Is [hour] inside the quiet window `[start, end)`, supporting wrap. */
        fun isQuietHour(hour: Int, start: Int, end: Int): Boolean =
            if (start == end) false
            else if (start < end) hour in start until end
            else hour >= start || hour < end
    }
}
