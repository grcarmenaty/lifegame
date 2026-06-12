package com.grcarmenaty.lifegame.domain

import com.grcarmenaty.lifegame.data.entities.MinorQuest
import java.util.Calendar
import java.util.TimeZone

/**
 * Pure cadence-window arithmetic, extracted from [PantheonRepository]
 * so the gating that decides whether a completion counts (and whether
 * the Daily view renders a minor as open) is unit-testable without a
 * database. The [TimeZone] parameter exists for tests; production
 * callers use the device default.
 */
object CadenceWindows {

    /**
     * Whether [last] and [now] fall in the same cadence window for the
     * given [minor]. The single source of truth used by
     * `completeMinor` and by the Daily VM's `isOpenNow`, so completion
     * gating and "open" rendering can't drift.
     *
     * WEEKLY with day-pinning uses per-day windows (one completion per
     * selected day); plain WEEKLY uses the local week. ONE_OFF never
     * shares a window — its single-shot gate is the `completed` flag.
     */
    fun sameWindow(
        minor: MinorQuest,
        last: Long,
        now: Long,
        tz: TimeZone = TimeZone.getDefault(),
    ): Boolean = when (minor.cadence) {
        MinorQuest.CADENCE_DAILY -> sameLocalDay(last, now, tz)
        MinorQuest.CADENCE_WEEKLY ->
            if (minor.cadenceDays.isNullOrBlank()) sameLocalWeek(last, now, tz)
            else sameLocalDay(last, now, tz)
        MinorQuest.CADENCE_MONTHLY -> sameLocalMonth(last, now, tz)
        else -> false
    }

    fun sameLocalDay(a: Long, b: Long, tz: TimeZone = TimeZone.getDefault()): Boolean {
        val ca = Calendar.getInstance(tz).apply { timeInMillis = a }
        val cb = Calendar.getInstance(tz).apply { timeInMillis = b }
        return ca.get(Calendar.YEAR) == cb.get(Calendar.YEAR) &&
            ca.get(Calendar.DAY_OF_YEAR) == cb.get(Calendar.DAY_OF_YEAR)
    }

    fun sameLocalWeek(a: Long, b: Long, tz: TimeZone = TimeZone.getDefault()): Boolean {
        val ca = Calendar.getInstance(tz).apply { timeInMillis = a }
        val cb = Calendar.getInstance(tz).apply { timeInMillis = b }
        // WEEK_OF_YEAR rolls at the Calendar's first-day-of-week (locale
        // default). Pair with year-of-week to avoid edge-of-year collisions.
        return ca.getWeekYear() == cb.getWeekYear() &&
            ca.get(Calendar.WEEK_OF_YEAR) == cb.get(Calendar.WEEK_OF_YEAR)
    }

    fun sameLocalMonth(a: Long, b: Long, tz: TimeZone = TimeZone.getDefault()): Boolean {
        val ca = Calendar.getInstance(tz).apply { timeInMillis = a }
        val cb = Calendar.getInstance(tz).apply { timeInMillis = b }
        return ca.get(Calendar.YEAR) == cb.get(Calendar.YEAR) &&
            ca.get(Calendar.MONTH) == cb.get(Calendar.MONTH)
    }
}
