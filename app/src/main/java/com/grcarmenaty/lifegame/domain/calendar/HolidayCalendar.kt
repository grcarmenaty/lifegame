package com.grcarmenaty.lifegame.domain.calendar

import com.grcarmenaty.lifegame.domain.SupportedRegion
import java.util.Calendar
import java.util.GregorianCalendar
import java.util.TimeZone

/**
 * Maps a local date to a single [HolidayToken], if any. Pure function —
 * no IO, no DAO reads. Region selection is exposed for future
 * expansion, but [SupportedRegion.BARCELONA] is the only valid value in
 * v0.0.11; the Settings dropdown won't let the user pick anything else.
 *
 * Priority: BIRTHDAY > PERSONAL_DATE > loaded six > lesser. The caller
 * (PantheonRepository.buildContext) layers user-supplied dates over the
 * cultural calendar.
 */
object HolidayCalendar {

    /**
     * Returns the cultural [HolidayToken] for the local date represented
     * by [calendar] under [region], or null if no holiday matches.
     * Caller must pre-set [calendar] to the desired local date.
     *
     * Does NOT return BIRTHDAY / PERSONAL_DATE — those are layered in by
     * the repository with higher priority.
     */
    fun tokenFor(
        calendar: Calendar,
        region: SupportedRegion = SupportedRegion.DEFAULT,
    ): HolidayToken? {
        // Only Barcelona has a holiday set wired up in v0.0.11. Adding
        // a region means: extend this switch with its calendar and let
        // the Settings dropdown surface it.
        if (region != SupportedRegion.BARCELONA) return null
        val month = calendar.get(Calendar.MONTH) + 1   // 1-12
        val day = calendar.get(Calendar.DAY_OF_MONTH)  // 1-31
        val year = calendar.get(Calendar.YEAR)

        // Fixed-date loaded six first (so they outrank fixed-date lesser
        // ones that may share the day — e.g., Aug 15 is both Assumpció
        // and the start of the Gràcia festival window; we prefer the
        // festival framing for the daemons).
        when {
            month == 4 && day == 23 -> return HolidayToken.SANT_JORDI
            month == 9 && day == 24 -> return HolidayToken.LA_MERCE
            month == 6 && day == 24 -> return HolidayToken.SANT_JOAN
            month == 9 && day == 11 -> return HolidayToken.DIADA
            month == 12 && day == 25 -> return HolidayToken.NADAL
            month == 1 && day == 1 -> return HolidayToken.CAP_D_ANY
        }

        // Festa Major de Gràcia: Aug 15-21 window. Assumpció (Aug 15)
        // is subsumed by the festival's opening day.
        if (month == 8 && day in 15..21) return HolidayToken.FESTA_MAJOR_GRACIA

        // Lesser fixed-date holidays.
        when {
            month == 1 && day == 6 -> return HolidayToken.REIS
            month == 5 && day == 1 -> return HolidayToken.FESTA_DEL_TREBALL
            month == 10 && day == 12 -> return HolidayToken.HISPANITAT
            month == 11 && day == 1 -> return HolidayToken.CASTANYADA
            month == 12 && day == 6 -> return HolidayToken.CONSTITUCIO
            month == 12 && day == 8 -> return HolidayToken.IMMACULADA
            month == 12 && day == 26 -> return HolidayToken.SANT_ESTEVE
            month == 12 && day == 31 -> return HolidayToken.CAP_D_ANY_EVE
        }

        // Easter-relative (computed for current year).
        val easter = easterSunday(year)
        if (sameMonthDay(calendar, easter, daysOffset = -47)) return HolidayToken.CARNAVAL
        if (sameMonthDay(calendar, easter, daysOffset = -2)) return HolidayToken.DIVENDRES_SANT
        if (sameMonthDay(calendar, easter, daysOffset = +1)) return HolidayToken.PASQUA

        return null
    }

    /**
     * Convenience for the repository. Takes today's millis + TZ; returns
     * a [Calendar] positioned at that local date, ready for [tokenFor].
     */
    fun localCalendar(nowMillis: Long, tz: TimeZone = TimeZone.getDefault()): Calendar {
        val c = Calendar.getInstance(tz)
        c.timeInMillis = nowMillis
        return c
    }

    private fun sameMonthDay(
        target: Calendar,
        anchor: Calendar,
        daysOffset: Int,
    ): Boolean {
        val shifted = anchor.clone() as Calendar
        shifted.add(Calendar.DAY_OF_MONTH, daysOffset)
        return target.get(Calendar.MONTH) == shifted.get(Calendar.MONTH) &&
            target.get(Calendar.DAY_OF_MONTH) == shifted.get(Calendar.DAY_OF_MONTH)
    }

    /**
     * Gauss's algorithm for Western (Gregorian) Easter. Returns a
     * Calendar at midnight on Easter Sunday of [year].
     */
    private fun easterSunday(year: Int): Calendar {
        val a = year % 19
        val b = year / 100
        val c = year % 100
        val d = b / 4
        val e = b % 4
        val f = (b + 8) / 25
        val g = (b - f + 1) / 3
        val h = (19 * a + b - d - g + 15) % 30
        val i = c / 4
        val k = c % 4
        val l = (32 + 2 * e + 2 * i - h - k) % 7
        val m = (a + 11 * h + 22 * l) / 451
        val month = (h + l - 7 * m + 114) / 31  // 3=March, 4=April
        val day = ((h + l - 7 * m + 114) % 31) + 1
        return GregorianCalendar(year, month - 1, day)
    }
}
