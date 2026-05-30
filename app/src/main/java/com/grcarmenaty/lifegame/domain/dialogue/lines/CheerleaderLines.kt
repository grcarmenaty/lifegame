package com.grcarmenaty.lifegame.domain.dialogue.lines

import com.grcarmenaty.lifegame.domain.dialogue.*

internal object CheerleaderLines {
    private const val A = "CHEERLEADER"
    val all: List<DialogueLine> = listOf(
        DialogueLine("ch_first_ever", A, "OH MY GOD HI. We're going to be SO good at this. Let's GO.",
            LineTier.ESSENTIAL, LineCategory.OPENER,
            lifeEvent = true,
            stateRequirements = listOf(FirstConversation)),
        DialogueLine("ch_morning", A, "Hi friend!! Today is going to be GREAT. Look at this list!",
            LineTier.CONTEXTUAL, LineCategory.OPENER,
            stateRequirements = listOf(TimeOfDay_Morning),
            cooldownGroup = "ch_greet", cooldownPicks = 3),
        DialogueLine("ch_evening", A, "OK we did SOME stuff today and I am OBSESSED with us.",
            LineTier.CONTEXTUAL, LineCategory.OPENER,
            stateRequirements = listOf(TimeOfDay_Evening),
            cooldownGroup = "ch_greet", cooldownPicks = 3),
        DialogueLine("ch_post_apotheosis", A, "We did it!! Look at how far we've come! I'm crying actually.",
            LineTier.ESSENTIAL, LineCategory.OPENER,
            recencyKey = RecencyKey.TODAY,
            stateRequirements = listOf(AfterApotheosis_24h)),
        DialogueLine("ch_wishes_pending", A, "You earned PRIZES. Cash them in, queen.",
            LineTier.CONTEXTUAL, LineCategory.OPENER,
            stateRequirements = listOf(WishesAvailable_1),
            cooldownGroup = "ch_wish_nudge", cooldownPicks = 4),
        DialogueLine("ch_streak", A, "Look at this streak!! I'm not okay. You're not okay. Beautiful.",
            LineTier.CONTEXTUAL, LineCategory.OPENER,
            stateRequirements = listOf(Streak_3),
            cooldownGroup = "ch_streak", cooldownPicks = 5),
        DialogueLine("ch_complete_1", A, "YES. Did you see how you just did that?? Iconic.",
            LineTier.FILLER, LineCategory.COMPLETION),
        DialogueLine("ch_complete_2", A, "Logged. Loved. Let's keep this energy.",
            LineTier.FILLER, LineCategory.COMPLETION),
        DialogueLine("ch_complete_3", A, "Stop, I'm getting emotional. Beautiful work.",
            LineTier.FILLER, LineCategory.COMPLETION),
        DialogueLine("ch_apotheosis_1", A, "Level UP! That's you. That's all you. I'm just here cheering.",
            LineTier.CONTEXTUAL, LineCategory.APOTHEOSIS),
        DialogueLine("ch_apotheosis_2", A, "Apotheosis behavior. I knew it. I always knew.",
            LineTier.CONTEXTUAL, LineCategory.APOTHEOSIS),
        DialogueLine("ch_apotheosis_first_ever", A, "FIRST ONE EVER! Pin it on the fridge. I'm sobbing.",
            LineTier.ESSENTIAL, LineCategory.APOTHEOSIS,
            lifeEvent = true,
            stateRequirements = listOf(MajorsClosed_1)),
    )
}
