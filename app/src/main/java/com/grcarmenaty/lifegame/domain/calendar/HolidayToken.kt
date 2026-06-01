package com.grcarmenaty.lifegame.domain.calendar

/**
 * Identifies a culturally-loaded date the dialogue engine can react to.
 *
 * Tokens are intentionally specific (NADAL, not CHRISTMAS): Personae's
 * default region is Catalonia/Barcelona, and the daemon voices use the
 * Catalan names. Tokens map to fixed-date checks (or Easter-relative
 * for Carnaval/Good-Friday/Easter-Monday) in [HolidayCalendar].
 *
 * BIRTHDAY and PERSONAL_DATE are user-supplied and outrank all
 * cultural holidays when both match (see priority order in
 * [HolidayCalendar.tokenFor]).
 */
enum class HolidayToken {
    // ---- User-supplied (highest priority) ----
    BIRTHDAY,
    PERSONAL_DATE,

    // ---- Catalan / Barcelona loaded six (per-archetype voicing) ----
    SANT_JORDI,           // Apr 23 — books and roses, not a public holiday
    LA_MERCE,             // Sep 24 — Barcelona patron saint, city festival
    SANT_JOAN,            // Jun 24 — bonfires, midsummer
    DIADA,                // Sep 11 — National Day of Catalonia
    NADAL,                // Dec 25 — Christmas
    CAP_D_ANY,            // Jan 1  — New Year

    // ---- Catalan / Barcelona lesser (shared ANY pool, light per-archetype) ----
    REIS,                 // Jan 6  — Three Kings
    CARNAVAL,             // Easter -47d (Shrove Tuesday)
    DIVENDRES_SANT,       // Easter -2d  (Good Friday)
    PASQUA,               // Easter Monday
    FESTA_DEL_TREBALL,    // May 1  — Labor Day
    FESTA_MAJOR_GRACIA,   // Aug 15-21 window — Gràcia neighborhood festival
    ASSUMPCIO,            // Aug 15 — Assumption
    HISPANITAT,           // Oct 12
    CASTANYADA,           // Nov 1  — All Saints / chestnut night
    CONSTITUCIO,          // Dec 6
    IMMACULADA,           // Dec 8
    SANT_ESTEVE,          // Dec 26 — St. Stephen
    CAP_D_ANY_EVE,        // Dec 31 — New Year's Eve
}
