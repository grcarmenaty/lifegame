package com.grcarmenaty.lifegame.domain.dialogue.lines

import com.grcarmenaty.lifegame.domain.dialogue.DialogueLine

/**
 * Flat corpus aggregated from per-archetype line files. Loaded once at
 * app start as a `val`. Lint enforces id uniqueness; engine sees a
 * single read-only list.
 */
object DialogueCorpus {
    val all: List<DialogueLine> = listOf(
        DrillSergeantLines.all,
        OracleLines.all,
        GentleMentorLines.all,
        TricksterLines.all,
        StoicLines.all,
        CheerleaderLines.all,
        PoetLines.all,
        TherapistLines.all,
        CoachLines.all,
        HermitLines.all,
        // v0.0.12 themed packs. Each pack mirrors the base archetype's
        // structure (60 lines) with theme-specific vocabulary; the
        // engine filters by `DialogueLine.theme`.
        com.grcarmenaty.lifegame.domain.dialogue.lines.themed
            .DrillSergeantExerciseLines.all,
        com.grcarmenaty.lifegame.domain.dialogue.lines.themed
            .DrillSergeantSleepLines.all,
        com.grcarmenaty.lifegame.domain.dialogue.lines.themed
            .DrillSergeantNutritionLines.all,
        com.grcarmenaty.lifegame.domain.dialogue.lines.themed
            .DrillSergeantHydrationLines.all,
        com.grcarmenaty.lifegame.domain.dialogue.lines.themed
            .DrillSergeantTidynessLines.all,
        com.grcarmenaty.lifegame.domain.dialogue.lines.themed
            .DrillSergeantFinancesLines.all,
        com.grcarmenaty.lifegame.domain.dialogue.lines.themed
            .DrillSergeantCareerLines.all,
        com.grcarmenaty.lifegame.domain.dialogue.lines.themed
            .DrillSergeantLearningLines.all,
        com.grcarmenaty.lifegame.domain.dialogue.lines.themed
            .DrillSergeantWritingLines.all,
        com.grcarmenaty.lifegame.domain.dialogue.lines.themed
            .DrillSergeantMeditationLines.all,
        com.grcarmenaty.lifegame.domain.dialogue.lines.themed
            .DrillSergeantLoveLines.all,
        com.grcarmenaty.lifegame.domain.dialogue.lines.themed
            .DrillSergeantFamilyLines.all,
    ).flatten()
}
