package com.grcarmenaty.lifegame.domain.dialogue.lines

import com.grcarmenaty.lifegame.domain.dialogue.*

/**
 * Drill Sergeant — harsh stance.
 *
 * Voice: terse, militant, no soft edges. Capable of reactive lapse
 * lines and attention-loss callouts (archetypeWhitelist permits).
 * Lapse-reactive lines all share the LAPSE_REACTIVE_COOLDOWN per
 * round-3 Skeptic guardrail.
 *
 * v0.0.11 expansion: ~45 lines covering per-level transitions,
 * attention-loss callouts, per-completion-count escalation, day-of-week
 * markers, holidays (Sant Jordi, La Mercè, Sant Joan, Diada, Nadal,
 * Cap d'Any), birthday, personal-date interpolation, and an
 * early-morning / late-night pair sized to the WFH schedule.
 */
internal object DrillSergeantLines {

    private const val A = "DRILL_SERGEANT"

    val all: List<DialogueLine> = listOf(

        // -------- OPENER · time of day --------

        DialogueLine(
            id = "ds_first_ever",
            archetype = A,
            text = "Listen up. New regime starts now. Don't blink.",
            tier = LineTier.ESSENTIAL,
            category = LineCategory.OPENER,
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
            cooldownGroup = "ds_greet", cooldownPicks = 3,
        ),
        DialogueLine(
            id = "ds_morning_early",
            archetype = A,
            text = "Up before the sun. Good. The rest of them aren't.",
            tier = LineTier.CONTEXTUAL, category = LineCategory.OPENER,
            stateRequirements = listOf(TimeOfDay_Morning, IsWeekday),
            cooldownGroup = "ds_greet", cooldownPicks = 3,
        ),
        DialogueLine(
            id = "ds_afternoon",
            archetype = A,
            text = "Afternoon. The clock isn't your friend.",
            tier = LineTier.CONTEXTUAL, category = LineCategory.OPENER,
            stateRequirements = listOf(TimeOfDay_Afternoon),
            cooldownGroup = "ds_greet", cooldownPicks = 3,
        ),
        DialogueLine(
            id = "ds_evening",
            archetype = A,
            text = "Evening. Anything left undone reflects on both of us.",
            tier = LineTier.CONTEXTUAL, category = LineCategory.OPENER,
            stateRequirements = listOf(TimeOfDay_Evening),
            cooldownGroup = "ds_greet", cooldownPicks = 3,
        ),
        DialogueLine(
            id = "ds_night_late",
            archetype = A,
            text = "Late. Either finish strong or shut it down clean. No half measures.",
            tier = LineTier.CONTEXTUAL, category = LineCategory.OPENER,
            stateRequirements = listOf(TimeOfDay_Night),
            cooldownGroup = "ds_greet", cooldownPicks = 3,
        ),
        DialogueLine(
            id = "ds_post_apotheosis",
            archetype = A,
            text = "Promotion logged. Don't get soft.",
            tier = LineTier.ESSENTIAL, category = LineCategory.OPENER,
            recencyKey = RecencyKey.TODAY,
            stateRequirements = listOf(AfterApotheosis_24h),
        ),

        // -------- OPENER · day of week (WFH rhythm) --------

        DialogueLine(
            id = "ds_monday",
            archetype = A,
            text = "Monday. Set the tone for the week. Nobody's coming to do it for you.",
            tier = LineTier.CONTEXTUAL, category = LineCategory.OPENER,
            stateRequirements = listOf(IsMonday, TimeOfDay_Morning),
            cooldownGroup = "ds_weekmarker", cooldownPicks = 1,
            recencyKey = RecencyKey.THIS_WEEK,
        ),
        DialogueLine(
            id = "ds_friday",
            archetype = A,
            text = "Friday. Close the loops you opened. Don't drag them into the weekend.",
            tier = LineTier.CONTEXTUAL, category = LineCategory.OPENER,
            stateRequirements = listOf(IsFriday),
            cooldownGroup = "ds_weekmarker", cooldownPicks = 1,
            recencyKey = RecencyKey.THIS_WEEK,
        ),
        DialogueLine(
            id = "ds_weekend",
            archetype = A,
            text = "Weekend. Discipline doesn't take days off. Lighter load, same standard.",
            tier = LineTier.CONTEXTUAL, category = LineCategory.OPENER,
            stateRequirements = listOf(IsWeekend),
            cooldownGroup = "ds_weekmarker", cooldownPicks = 1,
            recencyKey = RecencyKey.TODAY,
        ),

        // -------- OPENER · per-level transitions (lifeEvent) --------

        DialogueLine(
            id = "ds_level_1_reached",
            archetype = A,
            text = "Level one. You're on the board. Don't read into it.",
            tier = LineTier.ESSENTIAL, category = LineCategory.OPENER,
            lifeEvent = true, recencyKey = RecencyKey.TODAY,
            stateRequirements = listOf(LevelExactly_1),
            cooldownGroup = "ds_level_xform", cooldownPicks = 999,
        ),
        DialogueLine(
            id = "ds_level_2_reached",
            archetype = A,
            text = "Level two. The easy gains end here. Now it costs.",
            tier = LineTier.ESSENTIAL, category = LineCategory.OPENER,
            lifeEvent = true, recencyKey = RecencyKey.TODAY,
            stateRequirements = listOf(LevelExactly_2),
            cooldownGroup = "ds_level_xform", cooldownPicks = 999,
        ),
        DialogueLine(
            id = "ds_level_3_reached",
            archetype = A,
            text = "Level three. You've earned the right to be tired. Not to stop.",
            tier = LineTier.ESSENTIAL, category = LineCategory.OPENER,
            lifeEvent = true, recencyKey = RecencyKey.TODAY,
            stateRequirements = listOf(LevelExactly_3),
            cooldownGroup = "ds_level_xform", cooldownPicks = 999,
        ),
        DialogueLine(
            id = "ds_level_4_reached",
            archetype = A,
            text = "Top rank. Means nothing if you slip tomorrow. Hold the line.",
            tier = LineTier.ESSENTIAL, category = LineCategory.OPENER,
            lifeEvent = true, recencyKey = RecencyKey.TODAY,
            stateRequirements = listOf(LevelExactly_4),
            cooldownGroup = "ds_level_xform", cooldownPicks = 999,
        ),

        // -------- OPENER · attention loss --------

        DialogueLine(
            id = "ds_attention_loss_mild",
            archetype = A,
            text = "Ground lost yesterday. Not catastrophic. Take it back today.",
            tier = LineTier.CONTEXTUAL, category = LineCategory.OPENER,
            recencyKey = RecencyKey.TODAY,
            stateRequirements = listOf(AttentionLost_3),
            cooldownGroup = "ds_atten_loss", cooldownPicks = 2,
        ),
        DialogueLine(
            id = "ds_attention_loss_hard",
            archetype = A,
            text = "You bled out a week of work. We're rebuilding from the floor. Start with one.",
            tier = LineTier.ESSENTIAL, category = LineCategory.OPENER,
            recencyKey = RecencyKey.TODAY,
            stateRequirements = listOf(AttentionLost_10),
            cooldownGroup = "ds_atten_loss", cooldownPicks = 2,
        ),

        // -------- OPENER · lapse (existing, kept) --------

        DialogueLine(
            id = "ds_after_lapse",
            archetype = A,
            text = "You skipped yesterday. Two ways to fix that. Pick one.",
            tier = LineTier.ESSENTIAL, category = LineCategory.OPENER,
            preferredSurface = PreferredSurface.SCREEN,
            recencyKey = RecencyKey.TODAY,
            stateRequirements = listOf(AfterLapse_1),
            cooldownGroup = LAPSE_REACTIVE_COOLDOWN,
            cooldownPicks = 1, crossSurfaceCooldown = true,
            choices = listOf(DialogueChoice("Acknowledged", null)),
        ),
        DialogueLine(
            id = "ds_mid_day_return",
            archetype = A,
            text = "Half the day's gone. The list isn't.",
            tier = LineTier.CONTEXTUAL, category = LineCategory.OPENER,
            preferredSurface = PreferredSurface.INLINE,
            recencyKey = RecencyKey.TODAY,
            stateRequirements = listOf(
                TimeOfDay_Afternoon, MinorsCompletedTodayIsZero, ForegroundFloor_3h,
            ),
            cooldownGroup = "mid_day_return", cooldownPicks = 2,
        ),
        DialogueLine(
            id = "ds_wishes_pending",
            archetype = A,
            text = "You earned something. Spend it or stop talking about it.",
            tier = LineTier.CONTEXTUAL, category = LineCategory.OPENER,
            stateRequirements = listOf(WishesAvailable_1),
            cooldownGroup = "ds_wish_nudge", cooldownPicks = 4,
        ),
        DialogueLine(
            id = "ds_mid_streak",
            archetype = A,
            text = "Streak holding. Don't notice it; just keep going.",
            tier = LineTier.CONTEXTUAL, category = LineCategory.OPENER,
            stateRequirements = listOf(Streak_3),
            cooldownGroup = "ds_streak", cooldownPicks = 5,
        ),

        // -------- OPENER · holidays (loaded six, per-archetype voice) --------

        DialogueLine(
            id = "ds_sant_jordi",
            archetype = A,
            text = "Books and roses. Sentimental. The list is still the list — pick a small one and finish it before noon.",
            tier = LineTier.ESSENTIAL, category = LineCategory.OPENER,
            recencyKey = RecencyKey.TODAY, lifeEvent = true,
            stateRequirements = listOf(OnSantJordi),
            cooldownGroup = "ds_holiday", cooldownPicks = 1,
        ),
        DialogueLine(
            id = "ds_la_merce",
            archetype = A,
            text = "Mercè week. The city's going to lose itself in fireworks. You don't get to. One rep today, then go.",
            tier = LineTier.ESSENTIAL, category = LineCategory.OPENER,
            recencyKey = RecencyKey.TODAY, lifeEvent = true,
            stateRequirements = listOf(OnLaMerce),
            cooldownGroup = "ds_holiday", cooldownPicks = 1,
        ),
        DialogueLine(
            id = "ds_sant_joan",
            archetype = A,
            text = "Sant Joan. You'll be up all night with the rest of them. Lock in your work before dusk or skip it honestly.",
            tier = LineTier.ESSENTIAL, category = LineCategory.OPENER,
            recencyKey = RecencyKey.TODAY, lifeEvent = true,
            stateRequirements = listOf(OnSantJoan),
            cooldownGroup = "ds_holiday", cooldownPicks = 1,
        ),
        DialogueLine(
            id = "ds_diada",
            archetype = A,
            text = "Diada. History earned this day with discipline. Don't waste it.",
            tier = LineTier.ESSENTIAL, category = LineCategory.OPENER,
            recencyKey = RecencyKey.TODAY, lifeEvent = true,
            stateRequirements = listOf(OnDiada),
            cooldownGroup = "ds_holiday", cooldownPicks = 1,
        ),
        DialogueLine(
            id = "ds_nadal",
            archetype = A,
            text = "Christmas. Family duty is duty too. One light rep, then go. Skip and tell me — I'll respect it. Vanish without telling me, I won't.",
            tier = LineTier.ESSENTIAL, category = LineCategory.OPENER,
            recencyKey = RecencyKey.TODAY, lifeEvent = true,
            stateRequirements = listOf(OnNadal),
            cooldownGroup = "ds_holiday", cooldownPicks = 1,
        ),
        DialogueLine(
            id = "ds_cap_d_any",
            archetype = A,
            text = "New year. The clock resets. Your standards don't. Today's rep is the first of the year — don't fumble it.",
            tier = LineTier.ESSENTIAL, category = LineCategory.OPENER,
            recencyKey = RecencyKey.TODAY, lifeEvent = true,
            stateRequirements = listOf(OnCapDAny),
            cooldownGroup = "ds_holiday", cooldownPicks = 1,
        ),

        // -------- OPENER · birthday + personal date --------

        DialogueLine(
            id = "ds_birthday",
            archetype = A,
            text = "Birthday. Older. Stronger isn't automatic. Light load today; full standard tomorrow.",
            tier = LineTier.ESSENTIAL, category = LineCategory.OPENER,
            recencyKey = RecencyKey.TODAY, lifeEvent = true,
            stateRequirements = listOf(IsBirthday),
            cooldownGroup = "ds_birthday", cooldownPicks = 1,
        ),
        DialogueLine(
            id = "ds_personal_date",
            archetype = A,
            text = "You marked today: {label}. Noted. Doesn't change the work, but I see it.",
            tier = LineTier.ESSENTIAL, category = LineCategory.OPENER,
            recencyKey = RecencyKey.TODAY, lifeEvent = true,
            stateRequirements = listOf(IsPersonalDate),
            cooldownGroup = "ds_personal", cooldownPicks = 1,
        ),

        // -------- COMPLETION · escalating count --------

        DialogueLine(id = "ds_complete_1", archetype = A,
            text = "Good. Next.",
            tier = LineTier.FILLER, category = LineCategory.COMPLETION),
        DialogueLine(id = "ds_complete_2", archetype = A,
            text = "That's one. Keep moving.",
            tier = LineTier.FILLER, category = LineCategory.COMPLETION),
        DialogueLine(id = "ds_complete_3", archetype = A,
            text = "Done. Don't get comfortable.",
            tier = LineTier.FILLER, category = LineCategory.COMPLETION),
        DialogueLine(id = "ds_complete_first_today", archetype = A,
            text = "Good — you started. Half the war.",
            tier = LineTier.CONTEXTUAL, category = LineCategory.COMPLETION,
            stateRequirements = listOf(MinorsToday_1),
            cooldownGroup = "ds_first_today", cooldownPicks = 6),
        DialogueLine(id = "ds_complete_second_today", archetype = A,
            text = "Two down. You're in the rhythm now. Don't break it.",
            tier = LineTier.CONTEXTUAL, category = LineCategory.COMPLETION,
            stateRequirements = listOf(MinorsToday_2),
            cooldownGroup = "ds_2_today", cooldownPicks = 6),
        DialogueLine(id = "ds_complete_third_today", archetype = A,
            text = "Three. That's a real day. Most people stop here.",
            tier = LineTier.CONTEXTUAL, category = LineCategory.COMPLETION,
            stateRequirements = listOf(MinorsToday_3),
            cooldownGroup = "ds_3_today", cooldownPicks = 6),
        DialogueLine(id = "ds_complete_fourth_today", archetype = A,
            text = "Four. You're past the easy ones. Pick the next one anyway.",
            tier = LineTier.CONTEXTUAL, category = LineCategory.COMPLETION,
            stateRequirements = listOf(MinorsToday_4),
            cooldownGroup = "ds_4_today", cooldownPicks = 6),
        DialogueLine(id = "ds_complete_fifth_today", archetype = A,
            text = "Five in one day. That's the standard, not the ceiling. Bank it.",
            tier = LineTier.ESSENTIAL, category = LineCategory.COMPLETION,
            recencyKey = RecencyKey.TODAY,
            stateRequirements = listOf(MinorsToday_5),
            cooldownGroup = "ds_5_today", cooldownPicks = 1),
        DialogueLine(id = "ds_complete_after_streak", archetype = A,
            text = "Day three. Pretend you don't notice.",
            tier = LineTier.CONTEXTUAL, category = LineCategory.COMPLETION,
            stateRequirements = listOf(Streak_3),
            cooldownGroup = "ds_streak_complete", cooldownPicks = 4),

        // -------- APOTHEOSIS · per-level --------

        DialogueLine(id = "ds_apotheosis_1", archetype = A,
            text = "Outstanding. You leveled up. Now harder.",
            tier = LineTier.CONTEXTUAL, category = LineCategory.APOTHEOSIS),
        DialogueLine(id = "ds_apotheosis_2", archetype = A,
            text = "That's how it's done. Set the next bar.",
            tier = LineTier.CONTEXTUAL, category = LineCategory.APOTHEOSIS),
        DialogueLine(id = "ds_apotheosis_3", archetype = A,
            text = "Closed and signed. Tomorrow's plan?",
            tier = LineTier.CONTEXTUAL, category = LineCategory.APOTHEOSIS),
        DialogueLine(id = "ds_apotheosis_first_ever", archetype = A,
            text = "First major closed. Don't forget what that costs.",
            tier = LineTier.ESSENTIAL, category = LineCategory.APOTHEOSIS,
            lifeEvent = true,
            stateRequirements = listOf(MajorsClosed_1),
            forbids = listOf("ds_apotheosis_1", "ds_apotheosis_2", "ds_apotheosis_3")),
        DialogueLine(id = "ds_apotheosis_at_level_2", archetype = A,
            text = "Promoted. Don't soft-pedal the next campaign.",
            tier = LineTier.ESSENTIAL, category = LineCategory.APOTHEOSIS,
            stateRequirements = listOf(Level_2),
            cooldownGroup = "ds_apotheosis_levelband", cooldownPicks = 3),
        DialogueLine(id = "ds_apotheosis_at_level_3", archetype = A,
            text = "Senior rank. You're not learning the basics anymore. Act it.",
            tier = LineTier.ESSENTIAL, category = LineCategory.APOTHEOSIS,
            stateRequirements = listOf(Level_3),
            cooldownGroup = "ds_apotheosis_levelband", cooldownPicks = 3),
        DialogueLine(id = "ds_apotheosis_at_level_4", archetype = A,
            text = "Top of the chart. The only direction now is sideways or down. Pick sideways.",
            tier = LineTier.ESSENTIAL, category = LineCategory.APOTHEOSIS,
            stateRequirements = listOf(Level_4),
            cooldownGroup = "ds_apotheosis_levelband", cooldownPicks = 3),

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
        DialogueLine("ds_nudge_attention_loss", A,
            "You lost ground. Coming back beats explaining why.",
            LineTier.ESSENTIAL, LineCategory.NUDGE,
            recencyKey = RecencyKey.TODAY,
            stateRequirements = listOf(AttentionLost_10),
            cooldownGroup = "ds_loss_nudge",
            cooldownPicks = 2, crossSurfaceCooldown = true),
    )
}
