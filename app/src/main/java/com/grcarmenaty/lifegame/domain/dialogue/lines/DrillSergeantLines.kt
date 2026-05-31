package com.grcarmenaty.lifegame.domain.dialogue.lines

import com.grcarmenaty.lifegame.domain.dialogue.*

/**
 * Drill Sergeant — harsh stance, one of the three deep archetypes for
 * v0.0.6. Foreground coverage + chains shipping in v0.0.6.1.
 *
 * Voice: terse, militant, no soft edges. Capable of reactive lapse
 * lines (archetypeWhitelist permits). Lapse-reactive lines all share
 * the LAPSE_REACTIVE_COOLDOWN per round-3 Skeptic guardrail.
 */
internal object DrillSergeantLines {

    private const val A = "DRILL_SERGEANT"

    val all: List<DialogueLine> = listOf(

        // -------- OPENER --------

        DialogueLine(
            id = "ds_first_ever",
            archetype = A,
            text = "Listen up. New regime starts now. Don't blink.",
            tier = LineTier.ESSENTIAL,
            category = LineCategory.OPENER,
            preferredSurface = PreferredSurface.EITHER,
            lifeEvent = true,
            stateRequirements = listOf(FirstConversation),
        ),
        DialogueLine(
            id = "ds_morning",
            archetype = A,
            text = "Morning. List's short. Get moving.",
            tier = LineTier.CONTEXTUAL,
            category = LineCategory.OPENER,
            stateRequirements = listOf(TimeOfDay_Morning),
            cooldownGroup = "ds_greet",
            cooldownPicks = 3,
        ),
        DialogueLine(
            id = "ds_afternoon",
            archetype = A,
            text = "Afternoon. The clock isn't your friend.",
            tier = LineTier.CONTEXTUAL,
            category = LineCategory.OPENER,
            stateRequirements = listOf(TimeOfDay_Afternoon),
            cooldownGroup = "ds_greet",
            cooldownPicks = 3,
        ),
        DialogueLine(
            id = "ds_evening",
            archetype = A,
            text = "Evening. Anything left undone reflects on both of us.",
            tier = LineTier.CONTEXTUAL,
            category = LineCategory.OPENER,
            stateRequirements = listOf(TimeOfDay_Evening),
            cooldownGroup = "ds_greet",
            cooldownPicks = 3,
        ),
        DialogueLine(
            id = "ds_post_apotheosis",
            archetype = A,
            text = "Promotion logged. Don't get soft.",
            tier = LineTier.ESSENTIAL,
            category = LineCategory.OPENER,
            preferredSurface = PreferredSurface.EITHER,
            recencyKey = RecencyKey.TODAY,
            stateRequirements = listOf(AfterApotheosis_24h),
        ),
        DialogueLine(
            id = "ds_after_lapse",
            archetype = A,
            text = "You skipped yesterday. Two ways to fix that. Pick one.",
            tier = LineTier.ESSENTIAL,
            category = LineCategory.OPENER,
            preferredSurface = PreferredSurface.SCREEN, // multi-choice
            lifeEvent = false,
            recencyKey = RecencyKey.TODAY,
            stateRequirements = listOf(AfterLapse_1),
            cooldownGroup = LAPSE_REACTIVE_COOLDOWN,
            cooldownPicks = 1,
            crossSurfaceCooldown = true,
            // Multi-beat chain destinations (`ds_lapse_engage`, `ds_lapse_breather`)
            // were deferred from v0.0.6.1 — for now the only honest choice is to
            // close the conversation. Re-add when the chain content ships.
            choices = listOf(
                DialogueChoice("Acknowledged", null),
            ),
        ),
        DialogueLine(
            id = "ds_mid_day_return",
            archetype = A,
            text = "Half the day's gone. The list isn't.",
            tier = LineTier.CONTEXTUAL,
            category = LineCategory.OPENER,
            preferredSurface = PreferredSurface.INLINE,
            recencyKey = RecencyKey.TODAY,
            stateRequirements = listOf(
                TimeOfDay_Afternoon,
                MinorsCompletedTodayIsZero,
                ForegroundFloor_3h,
            ),
            cooldownGroup = "mid_day_return",
            cooldownPicks = 2,
        ),
        DialogueLine(
            id = "ds_wishes_pending",
            archetype = A,
            text = "You earned something. Spend it or stop talking about it.",
            tier = LineTier.CONTEXTUAL,
            category = LineCategory.OPENER,
            stateRequirements = listOf(WishesAvailable_1),
            cooldownGroup = "ds_wish_nudge",
            cooldownPicks = 4,
        ),
        DialogueLine(
            id = "ds_mid_streak",
            archetype = A,
            text = "Streak holding. Don't notice it; just keep going.",
            tier = LineTier.CONTEXTUAL,
            category = LineCategory.OPENER,
            stateRequirements = listOf(Streak_3),
            cooldownGroup = "ds_streak",
            cooldownPicks = 5,
        ),

        // -------- COMPLETION --------

        DialogueLine(
            id = "ds_complete_1",
            archetype = A,
            text = "Good. Next.",
            tier = LineTier.FILLER,
            category = LineCategory.COMPLETION,
        ),
        DialogueLine(
            id = "ds_complete_2",
            archetype = A,
            text = "That's one. Keep moving.",
            tier = LineTier.FILLER,
            category = LineCategory.COMPLETION,
        ),
        DialogueLine(
            id = "ds_complete_3",
            archetype = A,
            text = "Done. Don't get comfortable.",
            tier = LineTier.FILLER,
            category = LineCategory.COMPLETION,
        ),
        DialogueLine(
            id = "ds_complete_first_today",
            archetype = A,
            text = "Good — you started. Half the war.",
            tier = LineTier.CONTEXTUAL,
            category = LineCategory.COMPLETION,
            stateRequirements = listOf(MinorsToday_1),
            cooldownGroup = "ds_first_today",
            cooldownPicks = 6,
        ),
        DialogueLine(
            id = "ds_complete_after_streak",
            archetype = A,
            text = "Day three. Pretend you don't notice.",
            tier = LineTier.CONTEXTUAL,
            category = LineCategory.COMPLETION,
            stateRequirements = listOf(Streak_3),
            cooldownGroup = "ds_streak_complete",
            cooldownPicks = 4,
        ),

        // -------- APOTHEOSIS --------

        DialogueLine(
            id = "ds_apotheosis_1",
            archetype = A,
            text = "Outstanding. You leveled up. Now harder.",
            tier = LineTier.CONTEXTUAL,
            category = LineCategory.APOTHEOSIS,
        ),
        DialogueLine(
            id = "ds_apotheosis_2",
            archetype = A,
            text = "That's how it's done. Set the next bar.",
            tier = LineTier.CONTEXTUAL,
            category = LineCategory.APOTHEOSIS,
        ),
        DialogueLine(
            id = "ds_apotheosis_first_ever",
            archetype = A,
            text = "First major closed. Don't forget what that costs.",
            tier = LineTier.ESSENTIAL,
            category = LineCategory.APOTHEOSIS,
            preferredSurface = PreferredSurface.EITHER,
            lifeEvent = true,
            stateRequirements = listOf(MajorsClosed_1),
            forbids = listOf("ds_apotheosis_1", "ds_apotheosis_2"),
        ),
        DialogueLine(
            id = "ds_apotheosis_level_5",
            archetype = A,
            text = "Five levels in. You're not a beginner anymore.",
            tier = LineTier.ESSENTIAL,
            category = LineCategory.APOTHEOSIS,
            preferredSurface = PreferredSurface.EITHER,
            lifeEvent = true,
            recencyKey = RecencyKey.TODAY,
            stateRequirements = listOf(Level_5),
        ),

        // -------- NUDGE (notifications) --------

        DialogueLine("ds_nudge_morning", A,
            "Morning. The list isn't going to do itself.",
            LineTier.CONTEXTUAL, LineCategory.NUDGE,
            stateRequirements = listOf(TimeOfDay_Morning, HasOpenMajors),
            cooldownGroup = "ds_nudge", cooldownPicks = 1, crossSurfaceCooldown = true),
        DialogueLine("ds_nudge_afternoon_zero", A,
            "Half the day. Nothing closed. Pick one.",
            LineTier.CONTEXTUAL, LineCategory.NUDGE,
            stateRequirements = listOf(TimeOfDay_Afternoon, MinorsCompletedTodayIsZero, HasOpenMajors),
            cooldownGroup = "ds_nudge", cooldownPicks = 1, crossSurfaceCooldown = true),
        DialogueLine("ds_nudge_evening", A,
            "Evening. Anything left undone reflects on both of us.",
            LineTier.CONTEXTUAL, LineCategory.NUDGE,
            stateRequirements = listOf(TimeOfDay_Evening, HasOpenMajors),
            cooldownGroup = "ds_nudge", cooldownPicks = 1, crossSurfaceCooldown = true),
        DialogueLine("ds_nudge_lapse", A,
            "You missed yesterday. Today fixes that. Move.",
            LineTier.ESSENTIAL, LineCategory.NUDGE,
            recencyKey = RecencyKey.TODAY,
            stateRequirements = listOf(AfterLapse_1),
            cooldownGroup = LAPSE_REACTIVE_COOLDOWN,
            cooldownPicks = 1, crossSurfaceCooldown = true),
    )
}
