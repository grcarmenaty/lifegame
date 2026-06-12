package com.grcarmenaty.lifegame.domain

import com.grcarmenaty.lifegame.data.entities.MinorQuest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

/**
 * Window arithmetic tests for the cadence gating shared by
 * `completeMinor` and the Daily view's `isOpenNow`. A wrong answer here
 * either lets a daily be completed twice in one day or hides an open
 * quest — both user-visible loop bugs.
 *
 * Week assertions deliberately avoid the Sunday/Monday boundary: the
 * implementation uses the locale's first-day-of-week, so tests only
 * pair days (Tue/Wed) that fall in the same week under either
 * convention.
 */
class CadenceWindowsTest {

    private val tz = TimeZone.getTimeZone("Europe/Madrid")

    private fun at(
        year: Int, month: Int, day: Int,
        hour: Int = 12, minute: Int = 0,
    ): Long = Calendar.getInstance(tz).run {
        clear()
        set(year, month - 1, day, hour, minute, 0)
        timeInMillis
    }

    private fun minor(cadence: String, days: String? = null) = MinorQuest(
        majorQuestId = 1L,
        title = "test",
        cadence = cadence,
        cadenceDays = days,
    )

    // ---- DAILY ----

    @Test fun daily_same_day_is_same_window() {
        val m = minor(MinorQuest.CADENCE_DAILY)
        assertTrue(CadenceWindows.sameWindow(m, at(2026, 6, 10, 0, 1), at(2026, 6, 10, 23, 59), tz))
    }

    @Test fun daily_rolls_over_at_local_midnight() {
        val m = minor(MinorQuest.CADENCE_DAILY)
        assertFalse(CadenceWindows.sameWindow(m, at(2026, 6, 10, 23, 59), at(2026, 6, 11, 0, 1), tz))
    }

    @Test fun daily_year_boundary_is_a_rollover() {
        val m = minor(MinorQuest.CADENCE_DAILY)
        assertFalse(CadenceWindows.sameWindow(m, at(2025, 12, 31), at(2026, 1, 1), tz))
    }

    @Test fun daily_same_calendar_day_different_year_is_not_same_window() {
        val m = minor(MinorQuest.CADENCE_DAILY)
        assertFalse(CadenceWindows.sameWindow(m, at(2025, 6, 10), at(2026, 6, 10), tz))
    }

    // ---- WEEKLY (unpinned) ----

    @Test fun weekly_midweek_days_share_a_window() {
        val m = minor(MinorQuest.CADENCE_WEEKLY)
        // Tue 2026-06-09 and Wed 2026-06-10: same week in any locale.
        assertTrue(CadenceWindows.sameWindow(m, at(2026, 6, 9), at(2026, 6, 10), tz))
    }

    @Test fun weekly_seven_days_apart_is_a_new_window() {
        val m = minor(MinorQuest.CADENCE_WEEKLY)
        assertFalse(CadenceWindows.sameWindow(m, at(2026, 6, 9), at(2026, 6, 16), tz))
    }

    @Test fun weekly_same_week_number_different_year_is_not_same_window() {
        val m = minor(MinorQuest.CADENCE_WEEKLY)
        assertFalse(CadenceWindows.sameWindow(m, at(2025, 6, 10), at(2026, 6, 10), tz))
    }

    // ---- WEEKLY (day-pinned): per-day windows ----

    @Test fun pinned_weekly_uses_per_day_windows() {
        val m = minor(MinorQuest.CADENCE_WEEKLY, days = "3,4") // Tue, Wed
        // Same day → same window.
        assertTrue(CadenceWindows.sameWindow(m, at(2026, 6, 9, 8), at(2026, 6, 9, 20), tz))
        // Tue → Wed of the same week: a NEW window (one per selected day).
        assertFalse(CadenceWindows.sameWindow(m, at(2026, 6, 9), at(2026, 6, 10), tz))
    }

    // ---- MONTHLY ----

    @Test fun monthly_first_and_last_day_share_a_window() {
        val m = minor(MinorQuest.CADENCE_MONTHLY)
        assertTrue(CadenceWindows.sameWindow(m, at(2026, 1, 1), at(2026, 1, 31), tz))
    }

    @Test fun monthly_rolls_over_on_the_first() {
        val m = minor(MinorQuest.CADENCE_MONTHLY)
        assertFalse(CadenceWindows.sameWindow(m, at(2026, 1, 31), at(2026, 2, 1), tz))
    }

    @Test fun monthly_same_month_different_year_is_not_same_window() {
        val m = minor(MinorQuest.CADENCE_MONTHLY)
        assertFalse(CadenceWindows.sameWindow(m, at(2025, 3, 15), at(2026, 3, 15), tz))
    }

    // ---- ONE_OFF ----

    @Test fun one_off_never_shares_a_window() {
        val m = minor(MinorQuest.CADENCE_ONE_OFF)
        // Gate for one-offs is the `completed` flag, not the window.
        assertFalse(CadenceWindows.sameWindow(m, at(2026, 6, 10, 8), at(2026, 6, 10, 9), tz))
    }
}
