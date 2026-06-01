package com.grcarmenaty.lifegame.domain.dialogue.lines

import com.grcarmenaty.lifegame.domain.dialogue.*

/**
 * Cross-archetype fallback pool. Lines tagged `archetype = "ANY"` are
 * eligible regardless of the daemon's voice — the dialogue engine's
 * filter accepts them alongside the per-archetype set.
 *
 * v0.0.11 ships the lesser-holiday set here: holidays where the
 * cultural weight is light enough that per-archetype voicing felt like
 * over-engineering. The loaded six (Sant Jordi, La Mercè, Sant Joan,
 * Diada, Nadal, Cap d'Any) plus birthday and personal-date stay
 * per-archetype.
 *
 * Believer round 4 outcome: share these but keep them date-recognition
 * lines, not personality-flat copy. Each acknowledges the day in a
 * neutral but warm register.
 */
internal object AnyLines {

    private const val A = "ANY"

    val all: List<DialogueLine> = listOf(

        // -------- Lesser Catalan / Barcelona holidays --------

        DialogueLine("any_reis", A, "Reis. The three kings, the gifts, the small ceremony. Honor the day. Begin small.",
            LineTier.ESSENTIAL, LineCategory.OPENER,
            recencyKey = RecencyKey.TODAY, lifeEvent = true,
            stateRequirements = listOf(OnReis),
            cooldownGroup = "any_holiday", cooldownPicks = 1),
        DialogueLine("any_carnaval", A, "Carnaval. The mask comes off as much as it goes on. Either way — you're seen.",
            LineTier.ESSENTIAL, LineCategory.OPENER,
            recencyKey = RecencyKey.TODAY, lifeEvent = true,
            stateRequirements = listOf(OnCarnaval),
            cooldownGroup = "any_holiday", cooldownPicks = 1),
        DialogueLine("any_divendres_sant", A, "Divendres Sant. A still day. The work can be quiet today.",
            LineTier.ESSENTIAL, LineCategory.OPENER,
            recencyKey = RecencyKey.TODAY, lifeEvent = true,
            stateRequirements = listOf(OnDivendresSant),
            cooldownGroup = "any_holiday", cooldownPicks = 1),
        DialogueLine("any_pasqua", A, "Pasqua. A long weekend. The small acts still count.",
            LineTier.ESSENTIAL, LineCategory.OPENER,
            recencyKey = RecencyKey.TODAY, lifeEvent = true,
            stateRequirements = listOf(OnPasqua),
            cooldownGroup = "any_holiday", cooldownPicks = 1),
        DialogueLine("any_festa_treball", A, "Primer de Maig. Workers' day. Rest is part of the work too.",
            LineTier.ESSENTIAL, LineCategory.OPENER,
            recencyKey = RecencyKey.TODAY, lifeEvent = true,
            stateRequirements = listOf(OnFestaDelTreball),
            cooldownGroup = "any_holiday", cooldownPicks = 1),
        DialogueLine("any_festa_major_gracia", A, "Festa Major de Gràcia. The streets are dressed. Wander them. The list will wait.",
            LineTier.ESSENTIAL, LineCategory.OPENER,
            recencyKey = RecencyKey.TODAY, lifeEvent = true,
            stateRequirements = listOf(OnFestaMajorGracia),
            cooldownGroup = "any_holiday", cooldownPicks = 1),
        DialogueLine("any_assumpcio", A, "L'Assumpció. A high summer pause. Take the day in slowly.",
            LineTier.ESSENTIAL, LineCategory.OPENER,
            recencyKey = RecencyKey.TODAY, lifeEvent = true,
            stateRequirements = listOf(OnAssumpcio),
            cooldownGroup = "any_holiday", cooldownPicks = 1),
        DialogueLine("any_hispanitat", A, "Hispanitat. A holiday some hold, some don't. Whichever you do — be honest about it.",
            LineTier.ESSENTIAL, LineCategory.OPENER,
            recencyKey = RecencyKey.TODAY, lifeEvent = true,
            stateRequirements = listOf(OnHispanitat),
            cooldownGroup = "any_holiday", cooldownPicks = 1),
        DialogueLine("any_castanyada", A, "Castanyada. Chestnuts, panellets, the soft turn into the dark months. Take care of yourself.",
            LineTier.ESSENTIAL, LineCategory.OPENER,
            recencyKey = RecencyKey.TODAY, lifeEvent = true,
            stateRequirements = listOf(OnCastanyada),
            cooldownGroup = "any_holiday", cooldownPicks = 1),
        DialogueLine("any_constitucio", A, "Dia de la Constitució. A long weekend ahead. Save some work for it; rest in the rest.",
            LineTier.ESSENTIAL, LineCategory.OPENER,
            recencyKey = RecencyKey.TODAY, lifeEvent = true,
            stateRequirements = listOf(OnConstitucio),
            cooldownGroup = "any_holiday", cooldownPicks = 1),
        DialogueLine("any_immaculada", A, "La Immaculada. The Pont de la Puríssima — a long bridge weekend. Let it be long.",
            LineTier.ESSENTIAL, LineCategory.OPENER,
            recencyKey = RecencyKey.TODAY, lifeEvent = true,
            stateRequirements = listOf(OnImmaculada),
            cooldownGroup = "any_holiday", cooldownPicks = 1),
        DialogueLine("any_sant_esteve", A, "Sant Esteve. The day after. Leftovers, slow walks. Light load.",
            LineTier.ESSENTIAL, LineCategory.OPENER,
            recencyKey = RecencyKey.TODAY, lifeEvent = true,
            stateRequirements = listOf(OnSantEsteve),
            cooldownGroup = "any_holiday", cooldownPicks = 1),
        DialogueLine("any_cap_d_any_eve", A, "Cap d'Any Eve. Twelve grapes at midnight. Whatever today's list is, end it before then.",
            LineTier.ESSENTIAL, LineCategory.OPENER,
            recencyKey = RecencyKey.TODAY, lifeEvent = true,
            stateRequirements = listOf(OnCapDAnyEve),
            cooldownGroup = "any_holiday", cooldownPicks = 1),
    )
}
