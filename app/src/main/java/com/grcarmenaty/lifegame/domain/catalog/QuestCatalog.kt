package com.grcarmenaty.lifegame.domain.catalog

import com.grcarmenaty.lifegame.data.entities.MinorQuest
import com.grcarmenaty.lifegame.domain.LifeTheme

/**
 * A pre-authored minor quest in the [QuestCatalog]. [fragment] is the
 * quest-specific completion clause (lower-case, no trailing period,
 * e.g. "the run is logged") that the daemon's archetype frame wraps at
 * completion time — see
 * [com.grcarmenaty.lifegame.domain.dialogue.QuestCompletion].
 *
 * Repeating minors carry a sensible [cadence]; one-off minors use
 * [MinorQuest.CADENCE_ONE_OFF] and tend to carry a higher [weight]
 * because they are the harder, more ambitious acts.
 */
data class CatalogMinor(
    val templateId: String,
    val title: String,
    val fragment: String,
    val cadence: String = MinorQuest.CADENCE_DAILY,
    val cadenceCount: Int = 1,
    val cadenceDays: Set<Int> = emptySet(),
    val weight: Int = 1,
) {
    val isRepeating: Boolean get() = cadence != MinorQuest.CADENCE_ONE_OFF
}

/** A pre-authored major quest with its repeating + one-off minors. */
data class CatalogMajor(
    val templateId: String,
    val title: String,
    val fragment: String,
    val repeating: List<CatalogMinor>,
    val oneOff: List<CatalogMinor>,
) {
    val minors: List<CatalogMinor> get() = repeating + oneOff
}

/**
 * The library of suggested quests, keyed by [LifeTheme]. Each theme
 * offers at least six majors; each major offers at least six repeating
 * and six one-off minors. "Other" (theme == null) has no catalog — the
 * user authors everything by hand there.
 *
 * Theme content lives one-object-per-file under
 * `domain/catalog/themes/` so the corpus stays reviewable and a theme
 * can be edited in isolation.
 */
object QuestCatalog {

    private val byTheme: Map<LifeTheme, List<CatalogMajor>> = mapOf(
        LifeTheme.EXERCISE to ExerciseQuests.majors,
        LifeTheme.SLEEP to SleepQuests.majors,
        LifeTheme.NUTRITION to NutritionQuests.majors,
        LifeTheme.HYDRATION to HydrationQuests.majors,
        LifeTheme.TIDYNESS to TidynessQuests.majors,
        LifeTheme.FINANCES to FinancesQuests.majors,
        LifeTheme.CAREER to CareerQuests.majors,
        LifeTheme.LEARNING to LearningQuests.majors,
        LifeTheme.WRITING to WritingQuests.majors,
        LifeTheme.MEDITATION to MeditationQuests.majors,
        LifeTheme.LOVE to LoveQuests.majors,
        LifeTheme.FAMILY to FamilyQuests.majors,
        LifeTheme.FRIENDSHIP to FriendshipQuests.majors,
        LifeTheme.GRATITUDE to GratitudeQuests.majors,
        LifeTheme.HOBBIES to HobbiesQuests.majors,
        LifeTheme.OUTDOORS to OutdoorsQuests.majors,
        LifeTheme.DIGITAL to DigitalQuests.majors,
        LifeTheme.ADMIN to AdminQuests.majors,
        LifeTheme.RECOVERY to RecoveryQuests.majors,
        LifeTheme.BOUNDARIES to BoundariesQuests.majors,
    )

    /** Catalog majors for [theme], or empty for null/"Other". */
    fun majorsFor(theme: LifeTheme?): List<CatalogMajor> =
        if (theme == null) emptyList() else byTheme[theme].orEmpty()

    private val majorById: Map<String, CatalogMajor> by lazy {
        byTheme.values.flatten().associateBy { it.templateId }
    }
    private val minorById: Map<String, CatalogMinor> by lazy {
        byTheme.values.flatten().flatMap { it.minors }.associateBy { it.templateId }
    }

    fun major(templateId: String?): CatalogMajor? = templateId?.let { majorById[it] }
    fun minor(templateId: String?): CatalogMinor? = templateId?.let { minorById[it] }
    fun majorFragment(templateId: String?): String? = major(templateId)?.fragment
    fun minorFragment(templateId: String?): String? = minor(templateId)?.fragment
}
