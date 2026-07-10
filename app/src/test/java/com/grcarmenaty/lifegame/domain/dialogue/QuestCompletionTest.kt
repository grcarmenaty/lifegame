package com.grcarmenaty.lifegame.domain.dialogue

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Guards the frame-composition seams: fragments are authored lowercase,
 * so slots that open a sentence must capitalize and mid-sentence slots
 * must not. Frame banks themselves are linted for exactly one slot.
 */
class QuestCompletionTest {

    @Test fun fragment_is_capitalized_at_line_start() {
        assertEquals(
            "The run is logged. The pattern holds.",
            QuestCompletion.fill("{0}. The pattern holds.", "the run is logged"),
        )
    }

    @Test fun fragment_is_capitalized_after_sentence_end() {
        assertEquals(
            "It is done. The run is logged, as I foresaw.",
            QuestCompletion.fill("It is done. {0}, as I foresaw.", "the run is logged"),
        )
        assertEquals(
            "Oh? The run is logged. Didn't think you had it in you.",
            QuestCompletion.fill("Oh? {0}. Didn't think you had it in you.", "the run is logged"),
        )
        assertEquals(
            "YES! The run is logged! Incredible!",
            QuestCompletion.fill("YES! {0}! Incredible!", "the run is logged"),
        )
    }

    @Test fun fragment_stays_lowercase_mid_sentence() {
        assertEquals(
            "And so, the run is logged — the day softens.",
            QuestCompletion.fill("And so, {0} — the day softens.", "the run is logged"),
        )
        assertEquals(
            "There: the run is logged, and the light shifts.",
            QuestCompletion.fill("There: {0}, and the light shifts.", "the run is logged"),
        )
    }

    @Test fun every_frame_carries_exactly_one_slot() {
        val banks = QuestCompletion.MINOR_FRAMES.entries + QuestCompletion.MAJOR_FRAMES.entries
        for ((voice, frames) in banks) {
            assertTrue("Voice $voice has an empty frame bank", frames.isNotEmpty())
            for (frame in frames) {
                val slots = Regex("\\{0}").findAll(frame).count()
                assertEquals(
                    "Frame for $voice must carry exactly one {0} slot: “$frame”",
                    1, slots,
                )
            }
        }
    }
}
