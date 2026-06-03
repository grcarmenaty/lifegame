package com.grcarmenaty.lifegame.domain

/**
 * Common life-themes the user can pick from in the summoning ritual.
 *
 * Each theme bundles a stable storage [key], a human-readable
 * [display] for the dropdown, and the [archetypeText] used as the
 * default `Daemon.archetype` ("what part of life this daemon
 * represents") when the user accepts the theme without overriding it
 * later. The archetype text is intentionally first-person-possessive
 * so the daemon reads as the user's own — "my body", "my craft".
 *
 * Picking "Other" in the summoning UI bypasses [LifeTheme] entirely
 * and leaves `Daemon.theme = null`, in which case the dialogue engine
 * falls back to the base per-archetype corpus.
 */
enum class LifeTheme(
    val key: String,
    val display: String,
    val archetypeText: String,
    val helper: String,
) {
    EXERCISE("EXERCISE", "Body & exercise", "my body",
        "Strength, runs, mobility, the gym."),
    SLEEP("SLEEP", "Sleep & rest", "my rest",
        "Bedtimes, wind-downs, sleep debt."),
    NUTRITION("NUTRITION", "Nutrition & cooking", "what I eat",
        "Meals, prep, real food, what you cook."),
    HYDRATION("HYDRATION", "Hydration & body basics", "my body's small needs",
        "Water, sunscreen, vitamins, small acts that compound."),
    TIDYNESS("TIDYNESS", "Tidyness & home", "my home",
        "Cleaning, putting away, the room around you."),
    FINANCES("FINANCES", "Money & finances", "my finances",
        "Budget, savings, tracking, what's owed and what's set aside."),
    CAREER("CAREER", "Career & craft", "my craft",
        "Focused work in your trade. Deep work, the studio."),
    LEARNING("LEARNING", "Learning & study", "my learning",
        "Reading, courses, languages, what you didn't know yesterday."),
    WRITING("WRITING", "Writing & creativity", "my creative work",
        "Journals, art, music, what wants to come out."),
    MEDITATION("MEDITATION", "Meditation & stillness", "my stillness",
        "Sitting, breath, presence, the cushion."),
    LOVE("LOVE", "Relationships & love", "my partner / my love life",
        "The other person. Intimacy, dating, the conversations that matter."),
    FAMILY("FAMILY", "Family & kin", "my family",
        "Parents, siblings, kids — the people you can't choose."),
    FRIENDSHIP("FRIENDSHIP", "Friendship & community", "my people",
        "The chosen ones. Texts, calls, showing up."),
    GRATITUDE("GRATITUDE", "Gratitude & journaling", "my gratitude",
        "What you noticed today. What didn't go wrong."),
    HOBBIES("HOBBIES", "Hobbies & play", "my play",
        "Non-productive joy. The thing you do for no reason."),
    OUTDOORS("OUTDOORS", "Outdoors & nature", "my time outside",
        "Walks, sun, fresh air, the path."),
    DIGITAL("DIGITAL", "Digital hygiene", "my attention",
        "Screen time, the feed, the small dopamine, what's actually here."),
    ADMIN("ADMIN", "Admin & life logistics", "my admin",
        "Paperwork, appointments, what you've been putting off."),
    RECOVERY("RECOVERY", "Recovery & healing", "my healing",
        "The slow road. Physio, therapy, the body asking for time."),
    BOUNDARIES("BOUNDARIES", "Boundaries & voice", "my voice",
        "Saying no, asking for what you want, holding the line."),
    ;

    companion object {
        /** Returns the matching theme or null when [key] is blank/"OTHER"/unknown. */
        fun fromKey(key: String?): LifeTheme? {
            if (key.isNullOrBlank() || key.equals("OTHER", ignoreCase = true)) return null
            return entries.firstOrNull { it.key.equals(key, ignoreCase = true) }
        }
    }
}
