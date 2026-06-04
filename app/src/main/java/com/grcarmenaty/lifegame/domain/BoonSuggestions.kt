package com.grcarmenaty.lifegame.domain

/**
 * Suggested boons for the summoning ritual's reward step — small,
 * self-granted favours the user can tap instead of writing their own.
 * Theme-flavoured where it helps, with a universal fallback that suits
 * any daemon. Suggestions are starting points; the field stays free-text.
 */
object BoonSuggestions {

    private val universal = listOf(
        "A guilt-free rest day",
        "An hour for whatever you want",
        "A small treat you've been eyeing",
        "A lazy morning, alarm off",
        "An episode of your show",
        "A long, hot bath",
    )

    private val byTheme: Map<LifeTheme, List<String>> = mapOf(
        LifeTheme.EXERCISE to listOf("A guilt-free rest day", "A new piece of kit", "A sports massage"),
        LifeTheme.SLEEP to listOf("A slow weekend lie-in", "A new pillow", "An afternoon nap, no guilt"),
        LifeTheme.NUTRITION to listOf("A meal out, anything you like", "A fancy ingredient", "Dessert, no second-guessing"),
        LifeTheme.HYDRATION to listOf("A nice water bottle", "A fancy coffee", "A spa afternoon"),
        LifeTheme.TIDYNESS to listOf("Something nice for the home", "A candle or plant", "A cleaner for a day"),
        LifeTheme.FINANCES to listOf("A small splurge, fully earned", "Money toward something you want", "A nice dinner out"),
        LifeTheme.CAREER to listOf("An afternoon fully off", "A tool or course you've wanted", "A coffee-shop work day"),
        LifeTheme.LEARNING to listOf("A new book", "A course you've wanted", "An afternoon down a rabbit hole"),
        LifeTheme.WRITING to listOf("A new notebook", "An afternoon to create freely", "A gallery or gig ticket"),
        LifeTheme.MEDITATION to listOf("A long walk, no agenda", "A retreat afternoon", "An hour of doing nothing"),
        LifeTheme.LOVE to listOf("A date night", "A weekend away together", "A small gift for them"),
        LifeTheme.FAMILY to listOf("A shared meal out", "A day trip together", "A call with no clock"),
        LifeTheme.FRIENDSHIP to listOf("A night out with friends", "A round on you", "A games night"),
        LifeTheme.GRATITUDE to listOf("A quiet moment with tea", "A photo printed and framed", "A small kindness to yourself"),
        LifeTheme.HOBBIES to listOf("An afternoon for play", "Something for the hobby", "A guilt-free deep dive"),
        LifeTheme.OUTDOORS to listOf("A day hike", "New outdoor gear", "A picnic somewhere green"),
        LifeTheme.DIGITAL to listOf("Guilt-free screen time", "A new game", "An evening fully offline, your way"),
        LifeTheme.ADMIN to listOf("A treat for clearing the backlog", "An afternoon off the to-do list", "A nice lunch"),
        LifeTheme.RECOVERY to listOf("A gentle, slow day", "A comfort you've denied yourself", "A soothing treat"),
        LifeTheme.BOUNDARIES to listOf("An evening that's only yours", "A solo treat", "Time with no obligations"),
    )

    /** Up to [limit] suggestions for [theme], padded with universal ones. */
    fun forTheme(theme: LifeTheme?, limit: Int = 6): List<String> {
        val themed = theme?.let { byTheme[it] }.orEmpty()
        return (themed + universal).distinct().take(limit)
    }
}
