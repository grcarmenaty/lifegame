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
        // v0.0.12 themed packs (Drill Sergeant ships first).
        com.grcarmenaty.lifegame.domain.dialogue.lines.themed
            .DrillSergeantExerciseLines.all,
    ).flatten()
}
