package com.grcarmenaty.lifegame.domain

/**
 * Face-like icons (kaomoji) per (voice-preset, life-theme) cell — at
 * least three per combination, drawn in the preset's mood.
 *
 * Each preset has a pool of distinct face variants in its voice; the
 * triple returned for a given theme rotates through the pool by the
 * theme's enum index so adjacent themes within the same archetype
 * surface different faces. Pool size (12) is larger than the chip
 * count (3) so every cell has a usable mix.
 *
 * Kaomoji rather than emoji bitmaps because they're text — they cost
 * nothing, render through the existing string pipeline, and survive
 * the JSON backup/restore round-trip with no extra plumbing.
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
            "(¬_¬)", "(ಠ_ಠ)", "(◣_◢)", "(◣д◢)", "(-_-)凸",
            "(눈‸눈)", "(╬ಠ益ಠ)", ">:|", "(¬‿¬)凸", "(-‿-)ゞ",
            "(눈_눈)", "(¬_¬;)",
        ),
        VoicePreset.COACH to listOf(
            "(•̀ᴗ•́)", "(ง •̀_•́)ง", "(>‿◠)✌", "ᕦ(ò_óˇ)ᕤ", "(ᗒᗨᗕ)",
            "(´•‿•`)", "(•‿•)ゞ", "(•́ω•̀)", "(•̀⤙•́)", "(ง'̀-'́)ง",
            "(•̀_•́)", "(>_<)9",
        ),
        VoicePreset.HERMIT to listOf(
            "(˘◡˘)", "(-‿-)", "(•‿•)", "ʕ•ᴥ•ʔ", "(◡‿◡)",
            "(-_-)", "(・_・)", "(◞‸◟)", "(¯﹃¯)", "(˘•_•˘)",
            "ʕ-ᴥ-ʔ", "(˘ω˘)",
        ),
        VoicePreset.POET to listOf(
            "(˘◡˘)", "(◕‿◕✿)", "٩(◕‿◕)۶", "(◡‿◡✿)", "(˘ω˘)",
            "(◔‿◔)", "(✿◕‿◕)", "(◠‿◠✿)", "(◍•ᴗ•◍)", "(⌒‿⌒)",
            "(♡‿♡)", "(◔◡◔)",
        ),
        VoicePreset.THERAPIST to listOf(
            "(◕‿◕)", "(◠‿◠)", "(˘◡˘)", "(・◡・)", "(◡‿◡)",
            "(^‿^)", "(◖ω◗)", "(◍•ᴗ•◍)", "(◡‿◡✿)", "(◕ᴗ◕)",
            "(´◡`)", "(◠‿◠)♡",
        ),
        VoicePreset.GENTLE_MENTOR to listOf(
            "(◡‿◡)", "(˘◡˘)", "(•‿•)", "(◠ω◠)", "(◠‿◠)",
            "(◕‿◕)", "(◕ᴗ◕)", "(´◡`)", "(◍•ᴗ•◍)", "(⌒‿⌒)",
            "(◠ᴗ◠)", "(^_^)",
        ),
        VoicePreset.ORACLE to listOf(
            "(◉_◉)", "(⊙_⊙)", "(☉‿☉)", "ʘ‿ʘ", "(◑▂◐)",
            "(◕‿◕)✧", "(¬_¬✧)", "(◔_◔)", "(∗`▿´∗)", "(◐‿◑)",
            "(◕ᴗ◕✧)", "(⊙‿⊙)",
        ),
        VoicePreset.CHEERLEADER to listOf(
            "(＾▽＾)", "٩(^‿^)۶", "(★‿★)", "(≧◡≦)", "(◕‿◕)♡",
            "(☆▽☆)", "٩(◕‿◕)۶", "(✿◠‿◠)", "(＾◡＾)", "ヽ(>‿<)ノ",
            "(≧▽≦)", "(◕‿◕)✿",
        ),
        VoicePreset.STOIC to listOf(
            "(-_-)", "(¬_¬)", "(◔_◔)", "(◕_◕)", "ಠ_ಠ",
            "(・ω・)", "(¬‿¬)", "(-‿-)", "(◑_◐)", "(¬_¬¬)",
            "(-_- )", "(._.)",
        ),
        VoicePreset.TRICKSTER to listOf(
            "(¬‿¬)", "(◕‿↼)", "(≖‿≖)", "(◑‿◐)", ">:3",
            "(^_<)~☆", "(≧ω≦)", "(¬‿¬✿)", "( ͡° ͜ʖ ͡°)", "(¬‿¬)b",
            "(◕ ω ◕)", "(≧◡≦)",
        ),
    )
}
