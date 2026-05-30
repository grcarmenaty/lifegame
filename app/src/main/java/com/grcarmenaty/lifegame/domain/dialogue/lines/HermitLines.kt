package com.grcarmenaty.lifegame.domain.dialogue.lines

import com.grcarmenaty.lifegame.domain.dialogue.*

internal object HermitLines {
    private const val A = "HERMIT"
    val all: List<DialogueLine> = listOf(
        DialogueLine("he_first_ever", A, "You found the door. Sit, if you like. The kettle is on.",
            LineTier.ESSENTIAL, LineCategory.OPENER,
            lifeEvent = true,
            stateRequirements = listOf(FirstConversation)),
        DialogueLine("he_morning", A, "I am here. The work is here. Begin when you wish.",
            LineTier.CONTEXTUAL, LineCategory.OPENER,
            stateRequirements = listOf(TimeOfDay_Morning),
            cooldownGroup = "he_greet", cooldownPicks = 3),
        DialogueLine("he_evening", A, "The lamps. The list. The quiet. You came back.",
            LineTier.CONTEXTUAL, LineCategory.OPENER,
            stateRequirements = listOf(TimeOfDay_Evening),
            cooldownGroup = "he_greet", cooldownPicks = 3),
        DialogueLine("he_post_apotheosis", A, "A passage. I have witnessed it. The room is still.",
            LineTier.ESSENTIAL, LineCategory.OPENER,
            recencyKey = RecencyKey.TODAY,
            stateRequirements = listOf(AfterApotheosis_24h)),
        DialogueLine("he_wishes_pending", A, "There is a kindness on the shelf. Take it down.",
            LineTier.CONTEXTUAL, LineCategory.OPENER,
            stateRequirements = listOf(WishesAvailable_1),
            cooldownGroup = "he_wish_nudge", cooldownPicks = 4),
        DialogueLine("he_complete_1", A, "I noticed. That's enough.",
            LineTier.FILLER, LineCategory.COMPLETION),
        DialogueLine("he_complete_2", A, "Yes. The small thing is the whole thing.",
            LineTier.FILLER, LineCategory.COMPLETION),
        DialogueLine("he_complete_3", A, "Done. The room is still.",
            LineTier.FILLER, LineCategory.COMPLETION),
        DialogueLine("he_apotheosis_1", A, "Something is finished. We will not speak loudly of it.",
            LineTier.CONTEXTUAL, LineCategory.APOTHEOSIS),
        DialogueLine("he_apotheosis_2", A, "The lamp glows differently now. That is all.",
            LineTier.CONTEXTUAL, LineCategory.APOTHEOSIS),
        DialogueLine("he_apotheosis_first_ever", A, "Your first one. I'll keep its quiet for you.",
            LineTier.ESSENTIAL, LineCategory.APOTHEOSIS,
            lifeEvent = true,
            stateRequirements = listOf(MajorsClosed_1)),
    )
}
