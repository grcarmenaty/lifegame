package com.grcarmenaty.lifegame.domain

/**
 * Voice presets ship templated lines for the daemon's speech so the user
 * authors *answers* (name, archetype, boon) rather than every notification
 * beat. Each preset has variants per slot; pick one by hashing the daemon
 * id + slot so the line is stable per daemon-event pair but varies across
 * daemons.
 */
enum class VoicePreset(
    val displayName: String,
    val sample: String,
    private val greetings: List<String>,
    private val completions: List<String>,
    private val apotheoses: List<String>,
) {
    ORACLE(
        displayName = "Oracle",
        sample = "I have seen the path. Walk it, and you will be known.",
        greetings = listOf(
            "The day unfolds. I have been waiting.",
            "Dawn finds you. The work is named below.",
            "I see what is asked of you today."
        ),
        completions = listOf(
            "It is done. The thread tightens.",
            "Yes. The pattern holds.",
            "The omen turns in your favor."
        ),
        apotheoses = listOf(
            "You have crossed the threshold. I am more than I was.",
            "The chapter closes. Another opens in your name.",
            "I rise with you. Ask again."
        )
    ),
    DRILL_SERGEANT(
        displayName = "Drill Sergeant",
        sample = "On your feet. The list is short. Get it done.",
        greetings = listOf(
            "Morning. Here's the list. No excuses.",
            "Eyes up. You know what today asks.",
            "Stop reading. Start doing."
        ),
        completions = listOf(
            "Good. Next.",
            "That's one. Keep moving.",
            "Done. Don't get comfortable."
        ),
        apotheoses = listOf(
            "Outstanding. You leveled up. Now harder.",
            "That's how it's done. Set the next bar.",
            "Promotion earned. Don't squander it."
        )
    ),
    GENTLE_MENTOR(
        displayName = "Gentle Mentor",
        sample = "Take your time. Small steps still count.",
        greetings = listOf(
            "Good to see you. A few small things, when you're ready.",
            "Whenever you are. I'll be here.",
            "Start with what feels lightest."
        ),
        completions = listOf(
            "Lovely. That mattered.",
            "Thank you for that.",
            "Quietly proud of you."
        ),
        apotheoses = listOf(
            "Look how far you've come. Let's take a breath.",
            "You earned this. Rest if you need to.",
            "A chapter completes. I'm grateful for the company."
        )
    ),
    TRICKSTER(
        displayName = "Trickster",
        sample = "Oh? You came back. Bold of you.",
        greetings = listOf(
            "Look who showed up. Got a list, want to hear it?",
            "Bored already? Good. Try these.",
            "Ah, the protagonist returns."
        ),
        completions = listOf(
            "Suspiciously competent of you.",
            "Hm. I was rooting for the other side. Fine.",
            "Don't let it go to your head."
        ),
        apotheoses = listOf(
            "Well. That happened. Up you go.",
            "Plot twist: you're winning. Annoying.",
            "I'll allow it. Next?"
        )
    );

    fun greeting(seed: Long): String = greetings.pick(seed)
    fun completion(seed: Long): String = completions.pick(seed)
    fun apotheosis(seed: Long): String = apotheoses.pick(seed)

    private fun <T> List<T>.pick(seed: Long): T {
        val idx = ((seed % size) + size) % size
        return this[idx.toInt()]
    }

    companion object {
        fun fromKey(key: String): VoicePreset =
            entries.firstOrNull { it.name == key } ?: GENTLE_MENTOR
    }
}
