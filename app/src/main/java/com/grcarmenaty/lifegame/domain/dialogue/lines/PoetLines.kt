package com.grcarmenaty.lifegame.domain.dialogue.lines

import com.grcarmenaty.lifegame.domain.dialogue.*

internal object PoetLines {
    private const val A = "POET"
    val all: List<DialogueLine> = listOf(
        DialogueLine("pt_first_ever", A, "The page is blank and asks your name. Write back.",
            LineTier.ESSENTIAL, LineCategory.OPENER,
            lifeEvent = true,
            stateRequirements = listOf(FirstConversation)),
        DialogueLine("pt_morning", A, "Morning unfolds, a page that asks to be marked.",
            LineTier.CONTEXTUAL, LineCategory.OPENER,
            stateRequirements = listOf(TimeOfDay_Morning),
            cooldownGroup = "pt_greet", cooldownPicks = 3),
        DialogueLine("pt_evening", A, "Light leaves. What you wrote here is staying.",
            LineTier.CONTEXTUAL, LineCategory.OPENER,
            stateRequirements = listOf(TimeOfDay_Evening),
            cooldownGroup = "pt_greet", cooldownPicks = 3),
        DialogueLine("pt_post_apotheosis", A, "The arc bends. You are no longer who you were.",
            LineTier.ESSENTIAL, LineCategory.OPENER,
            recencyKey = RecencyKey.TODAY,
            stateRequirements = listOf(AfterApotheosis_24h)),
        DialogueLine("pt_wishes_pending", A, "There is a gift unopened. It is yours.",
            LineTier.CONTEXTUAL, LineCategory.OPENER,
            stateRequirements = listOf(WishesAvailable_1),
            cooldownGroup = "pt_wish_nudge", cooldownPicks = 4),
        DialogueLine("pt_complete_1", A, "A stanza, finished. The poem grows.",
            LineTier.FILLER, LineCategory.COMPLETION),
        DialogueLine("pt_complete_2", A, "The line lands. The room is changed.",
            LineTier.FILLER, LineCategory.COMPLETION),
        DialogueLine("pt_complete_3", A, "Small word, large echo.",
            LineTier.FILLER, LineCategory.COMPLETION),
        DialogueLine("pt_apotheosis_1", A, "A chapter closes — you wrote it well.",
            LineTier.CONTEXTUAL, LineCategory.APOTHEOSIS),
        DialogueLine("pt_apotheosis_2", A, "An ascension, quiet as ink drying.",
            LineTier.CONTEXTUAL, LineCategory.APOTHEOSIS),
        DialogueLine("pt_apotheosis_first_ever", A, "Your first finished thing. Keep it where you can read it.",
            LineTier.ESSENTIAL, LineCategory.APOTHEOSIS,
            lifeEvent = true,
            stateRequirements = listOf(MajorsClosed_1)),
    )
}
