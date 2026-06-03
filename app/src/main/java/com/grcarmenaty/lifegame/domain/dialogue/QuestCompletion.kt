package com.grcarmenaty.lifegame.domain.dialogue

import com.grcarmenaty.lifegame.domain.VoicePreset

/**
 * Composes a quest's completion line in the daemon's voice.
 *
 * Per the v0.0.14 council decision, per-archetype variety is realized by
 * **composition** rather than ~28k hand-written lines: each catalog
 * quest carries a quest-specific `fragment` (e.g. "the run is logged"),
 * and each archetype owns a small bank of voiced *frames* with a `{0}`
 * slot. The line shown on completion is `frame(fragment)`, and
 * repeatable minors rotate through the frames on successive completions
 * so the same act doesn't read identically every day.
 *
 * Custom (user-authored) quests have no fragment; they fall back to the
 * archetype's generic completion line so every completion still speaks
 * in voice.
 */
object QuestCompletion {

    fun minorLine(voice: VoicePreset, fragment: String?, rotation: Long): String {
        if (fragment.isNullOrBlank()) return voice.completion(rotation)
        val frames = MINOR_FRAMES.getValue(voice)
        return frames[floorMod(rotation, frames.size)].replace("{0}", fragment)
    }

    fun majorLine(voice: VoicePreset, fragment: String?, rotation: Long): String? {
        if (fragment.isNullOrBlank()) return null
        val frames = MAJOR_FRAMES.getValue(voice)
        return frames[floorMod(rotation, frames.size)].replace("{0}", fragment)
    }

    private fun floorMod(x: Long, m: Int): Int = ((x % m + m) % m).toInt()

    private val MINOR_FRAMES: Map<VoicePreset, List<String>> = mapOf(
        VoicePreset.ORACLE to listOf(
            "It is done. {0}, as I foresaw.",
            "{0}. The pattern holds.",
            "I saw this thread. {0}.",
            "{0} — one more step on the path I showed you.",
            "The vision narrows. {0}.",
            "{0}. You move as the omens said you would.",
        ),
        VoicePreset.DRILL_SERGEANT to listOf(
            "Done. {0}. Next.",
            "{0}. Logged. Move.",
            "Good. {0}. Don't stop.",
            "{0}. That's the standard. Again tomorrow.",
            "Acceptable. {0}.",
            "{0}. No excuses today. Noted.",
        ),
        VoicePreset.GENTLE_MENTOR to listOf(
            "Well done. {0}.",
            "{0} — that counts, you know.",
            "Lovely. {0}.",
            "{0}. Small steps, and you took one.",
            "There it is. {0}.",
            "{0}. Be a little proud of that.",
        ),
        VoicePreset.TRICKSTER to listOf(
            "Oh? {0}. Didn't think you had it in you.",
            "{0}. Bold.",
            "Well, well. {0}.",
            "{0}. Suspiciously competent of you.",
            "Look at that. {0}.",
            "{0}. I'll allow it.",
        ),
        VoicePreset.STOIC to listOf(
            "{0}. That is enough.",
            "It is handled. {0}.",
            "{0}. Nothing more was required.",
            "{0}. The deed, not the applause.",
            "Complete. {0}.",
            "{0}. As it should be.",
        ),
        VoicePreset.CHEERLEADER to listOf(
            "YES! {0}! Incredible!",
            "{0}! Look at you GO!",
            "Amazing — {0}!",
            "{0}! That's what I'm talking about!",
            "Woo! {0}!",
            "{0}! You're on FIRE!",
        ),
        VoicePreset.POET to listOf(
            "And so, {0} — the day softens.",
            "{0}, like a line finally scanning.",
            "Quietly, {0}.",
            "{0}. A small bell, rung.",
            "{0} — and the page turns.",
            "There: {0}, and the light shifts.",
        ),
        VoicePreset.THERAPIST to listOf(
            "Notice that: {0}.",
            "{0}. How does that sit with you?",
            "You followed through — {0}.",
            "{0}. That's worth pausing on.",
            "{0}. You showed up for yourself.",
            "{0}. Small, and real.",
        ),
        VoicePreset.COACH to listOf(
            "Rep logged. {0}.",
            "{0}. On schedule.",
            "Good work — {0}.",
            "{0}. Stack another tomorrow.",
            "{0}. That's the game plan.",
            "{0}. Clean execution.",
        ),
        VoicePreset.HERMIT to listOf(
            "{0}. Good.",
            "{0}.",
            "Noted. {0}.",
            "{0}. The work is small and good.",
            "{0}. Enough.",
            "{0}. I saw.",
        ),
    )

    private val MAJOR_FRAMES: Map<VoicePreset, List<String>> = mapOf(
        VoicePreset.ORACLE to listOf(
            "The prophecy closes. {0}, and a door opens.",
            "{0}. What was foretold is now behind you.",
            "I named this end before you began. {0}.",
            "{0} — the path bends upward from here.",
        ),
        VoicePreset.DRILL_SERGEANT to listOf(
            "Mission complete. {0}. Outstanding.",
            "{0}. You held the line. Dismissed.",
            "That's how it's done. {0}.",
            "{0}. Earned, not given.",
        ),
        VoicePreset.GENTLE_MENTOR to listOf(
            "Look how far you came. {0}.",
            "{0}. I'm so glad you stayed with it.",
            "You did the whole thing. {0}.",
            "{0} — that was never small.",
        ),
        VoicePreset.TRICKSTER to listOf(
            "Ha! {0}. You actually finished. Shocking.",
            "{0}. Fine, fine — I'm impressed.",
            "You pulled it off. {0}. Sneaky.",
            "{0}. Colour me surprised.",
        ),
        VoicePreset.STOIC to listOf(
            "{0}. The work was the reward.",
            "It is finished. {0}.",
            "{0}. You acted; the rest is noise.",
            "{0}. Carry it lightly.",
        ),
        VoicePreset.CHEERLEADER to listOf(
            "HUGE! {0}! I'm so proud!",
            "{0}! You absolute legend!",
            "You DID it — {0}!",
            "{0}! Confetti, immediately!",
        ),
        VoicePreset.POET to listOf(
            "{0}. The whole stanza, complete.",
            "And it closes — {0} — like dusk.",
            "{0}. You wrote yourself an ending.",
            "{0}, and the book leans shut.",
        ),
        VoicePreset.THERAPIST to listOf(
            "{0}. Take a moment with that.",
            "You completed it — {0}. What changed?",
            "{0}. That took something. Honour it.",
            "{0}. You kept a promise to yourself.",
        ),
        VoicePreset.COACH to listOf(
            "Game won. {0}. Great campaign.",
            "{0}. You ran the whole play.",
            "That's the win — {0}.",
            "{0}. Championship habits.",
        ),
        VoicePreset.HERMIT to listOf(
            "{0}. A long thing, finished.",
            "{0}. Quietly done.",
            "{0}. That was worth the silence.",
            "{0}. Rest now.",
        ),
    )
}
