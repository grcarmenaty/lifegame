package com.grcarmenaty.lifegame.domain

/**
 * Face-like icons per (voice-preset, life-theme) cell — at least three
 * per combination, drawn from a 12-deep portrait-emoji pool per
 * archetype.
 *
 * The pool is character / face / figure emoji rather than kaomoji so
 * each chip reads as a small portrait rather than a punctuation face.
 * The triple returned for a given theme rotates the pool by the
 * theme's enum index so adjacent themes within the same archetype
 * surface different portraits.
 *
 * Emoji rather than drawable resources because they cost nothing,
 * render natively on every Android 8+ device through the existing
 * string pipeline, and survive the JSON backup/restore round-trip
 * with no extra plumbing. Drop-in bespoke silhouette art would mean
 * shipping ~120 vector drawables and switching the return type to
 * resource IDs — worth doing once the asset set exists, but out of
 * scope here.
 */
object DaemonFaceSuggestions {

    private const val FACES_PER_CELL = 3

    fun forPair(preset: VoicePreset, theme: LifeTheme?): List<String> {
        if (theme == null) return emptyList()
        val pool = POOLS[preset] ?: return emptyList()
        val themeIdx = LifeTheme.entries.indexOf(theme)
        if (themeIdx < 0) return emptyList()
        return List(FACES_PER_CELL) { i -> pool[(themeIdx + i) % pool.size] }
    }

    private val POOLS: Map<VoicePreset, List<String>> = mapOf(
        VoicePreset.DRILL_SERGEANT to listOf(
            "💂‍♂️", "💂‍♀️", "👮‍♂️", "👮‍♀️", "🥷", "🤺",
            "🧐", "😠", "😡", "🤬", "🥸", "👨‍✈️",
        ),
        VoicePreset.COACH to listOf(
            "👨‍🏫", "👩‍🏫", "🏃‍♂️", "🏃‍♀️", "🤾‍♂️", "🤾‍♀️",
            "⛹️‍♂️", "⛹️‍♀️", "🏋️‍♂️", "🏋️‍♀️", "🏊‍♂️", "🏌️‍♂️",
        ),
        VoicePreset.HERMIT to listOf(
            "🧙‍♂️", "🧙‍♀️", "👴", "👵", "🧓", "🧘‍♂️",
            "🧘‍♀️", "🦉", "🐢", "🦫", "🐉", "🛖",
        ),
        VoicePreset.POET to listOf(
            "🎭", "👨‍🎨", "👩‍🎨", "🧚‍♂️", "🧚‍♀️", "👨‍🎤",
            "👩‍🎤", "🧑‍🎨", "🪶", "📜", "🌹", "🪐",
        ),
        VoicePreset.THERAPIST to listOf(
            "👩‍⚕️", "👨‍⚕️", "🧑‍⚕️", "🫂", "🤝", "🤲",
            "💚", "🌸", "🌿", "🕊️", "🧘‍♀️", "🪷",
        ),
        VoicePreset.GENTLE_MENTOR to listOf(
            "👨‍🏫", "👩‍🏫", "🧑‍🏫", "👴", "👵", "🧓",
            "👩‍🦳", "👨‍🦳", "🤝", "📚", "🌱", "🪴",
        ),
        VoicePreset.ORACLE to listOf(
            "🔮", "👁️", "🌙", "🧙‍♀️", "🧙‍♂️", "🧞‍♀️",
            "🧞‍♂️", "✨", "🃏", "🌟", "🪬", "🪐",
        ),
        VoicePreset.CHEERLEADER to listOf(
            "🤸‍♀️", "🤸‍♂️", "📣", "🎉", "🥳", "💃",
            "🕺", "⭐", "🌟", "✨", "🎊", "🌈",
        ),
        VoicePreset.STOIC to listOf(
            "🗿", "🏛️", "📜", "⚖️", "⛰️", "🪨",
            "🦅", "🧐", "👨‍🦳", "👴", "🛡️", "🏺",
        ),
        VoicePreset.TRICKSTER to listOf(
            "🃏", "🦊", "🎭", "😼", "🌀", "🧚‍♂️",
            "🦝", "👻", "🤡", "😏", "😈", "🥷",
        ),
    )
}
