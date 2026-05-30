package com.grcarmenaty.lifegame.domain.dialogue.lines

import com.grcarmenaty.lifegame.domain.dialogue.*

internal object StoicLines {
    private const val A = "STOIC"
    val all: List<DialogueLine> = listOf(
        DialogueLine("st_first_ever", A, "We begin. The work is the same regardless.",
            LineTier.ESSENTIAL, LineCategory.OPENER,
            lifeEvent = true,
            stateRequirements = listOf(FirstConversation)),
        DialogueLine("st_morning", A, "The day arrives. Do what you can. Accept the rest.",
            LineTier.CONTEXTUAL, LineCategory.OPENER,
            stateRequirements = listOf(TimeOfDay_Morning),
            cooldownGroup = "st_greet", cooldownPicks = 3),
        DialogueLine("st_after_lapse", A, "Yesterday is yesterday. Today is what you have.",
            LineTier.ESSENTIAL, LineCategory.OPENER,
            recencyKey = RecencyKey.TODAY,
            stateRequirements = listOf(AfterLapse_1),
            cooldownGroup = LAPSE_REACTIVE_COOLDOWN,
            cooldownPicks = 1,
            crossSurfaceCooldown = true),
        DialogueLine("st_post_apotheosis", A, "What you intended, you completed. That is enough.",
            LineTier.ESSENTIAL, LineCategory.OPENER,
            recencyKey = RecencyKey.TODAY,
            stateRequirements = listOf(AfterApotheosis_24h)),
        DialogueLine("st_wishes_pending", A, "Something is owed to you. Take it without ceremony.",
            LineTier.CONTEXTUAL, LineCategory.OPENER,
            stateRequirements = listOf(WishesAvailable_1),
            cooldownGroup = "st_wish_nudge", cooldownPicks = 4),
        DialogueLine("st_complete_1", A, "Done. Move on.",
            LineTier.FILLER, LineCategory.COMPLETION),
        DialogueLine("st_complete_2", A, "Acceptable.",
            LineTier.FILLER, LineCategory.COMPLETION),
        DialogueLine("st_complete_3", A, "One thing at peace. Continue.",
            LineTier.FILLER, LineCategory.COMPLETION),
        DialogueLine("st_apotheosis_1", A, "A chapter closes. The river runs on.",
            LineTier.CONTEXTUAL, LineCategory.APOTHEOSIS),
        DialogueLine("st_apotheosis_2", A, "The work was the reward. Yet more is given.",
            LineTier.CONTEXTUAL, LineCategory.APOTHEOSIS),
        DialogueLine("st_apotheosis_first_ever", A, "First passage. Remember: it is the same river.",
            LineTier.ESSENTIAL, LineCategory.APOTHEOSIS,
            lifeEvent = true,
            stateRequirements = listOf(MajorsClosed_1)),
    )
}
