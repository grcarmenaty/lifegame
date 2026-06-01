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
        com.grcarmenaty.lifegame.domain.dialogue.lines.themed
            .DrillSergeantFriendshipLines.all,
        com.grcarmenaty.lifegame.domain.dialogue.lines.themed
            .DrillSergeantGratitudeLines.all,
        com.grcarmenaty.lifegame.domain.dialogue.lines.themed
            .DrillSergeantHobbiesLines.all,
        com.grcarmenaty.lifegame.domain.dialogue.lines.themed
            .DrillSergeantOutdoorsLines.all,
        com.grcarmenaty.lifegame.domain.dialogue.lines.themed
            .DrillSergeantDigitalLines.all,
        com.grcarmenaty.lifegame.domain.dialogue.lines.themed
            .DrillSergeantAdminLines.all,
        com.grcarmenaty.lifegame.domain.dialogue.lines.themed
            .DrillSergeantRecoveryLines.all,
        com.grcarmenaty.lifegame.domain.dialogue.lines.themed
            .DrillSergeantBoundariesLines.all,
        // Gentle Mentor themed packs.
        com.grcarmenaty.lifegame.domain.dialogue.lines.themed
            .GentleMentorExerciseLines.all,
        com.grcarmenaty.lifegame.domain.dialogue.lines.themed
            .GentleMentorSleepLines.all,
        com.grcarmenaty.lifegame.domain.dialogue.lines.themed
            .GentleMentorNutritionLines.all,
        com.grcarmenaty.lifegame.domain.dialogue.lines.themed
            .GentleMentorHydrationLines.all,
        com.grcarmenaty.lifegame.domain.dialogue.lines.themed
            .GentleMentorTidynessLines.all,
        com.grcarmenaty.lifegame.domain.dialogue.lines.themed
            .GentleMentorFinancesLines.all,
        com.grcarmenaty.lifegame.domain.dialogue.lines.themed
            .GentleMentorCareerLines.all,
        com.grcarmenaty.lifegame.domain.dialogue.lines.themed
            .GentleMentorLearningLines.all,
        com.grcarmenaty.lifegame.domain.dialogue.lines.themed
            .GentleMentorWritingLines.all,
        com.grcarmenaty.lifegame.domain.dialogue.lines.themed
            .GentleMentorMeditationLines.all,
        com.grcarmenaty.lifegame.domain.dialogue.lines.themed
            .GentleMentorLoveLines.all,
        com.grcarmenaty.lifegame.domain.dialogue.lines.themed
            .GentleMentorFamilyLines.all,
        com.grcarmenaty.lifegame.domain.dialogue.lines.themed
            .GentleMentorFriendshipLines.all,
        com.grcarmenaty.lifegame.domain.dialogue.lines.themed
            .GentleMentorGratitudeLines.all,
        com.grcarmenaty.lifegame.domain.dialogue.lines.themed
            .GentleMentorHobbiesLines.all,
        com.grcarmenaty.lifegame.domain.dialogue.lines.themed
            .GentleMentorOutdoorsLines.all,
    ).flatten()
}
