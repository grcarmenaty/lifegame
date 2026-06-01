package com.grcarmenaty.lifegame.domain.dialogue

import com.grcarmenaty.lifegame.data.entities.MajorQuest
import com.grcarmenaty.lifegame.data.entities.MinorQuest
import com.grcarmenaty.lifegame.domain.calendar.HolidayToken

enum class TimeOfDay { MORNING, AFTERNOON, EVENING, NIGHT }
enum class Surface { INLINE, SCREEN }

/**
 * Everything predicates can see. Built once per `DialogueEngine.pickFor`
 * call by the repository; never mutated thereafter.
 *
 * v0.0.11 additions: calendar awareness (holidayToken / birthday /
 * personalDateLabel), attention-loss surface (attentionPoints +
 * attentionLost24h), and the previously-stubbed lapse/completion
 * counters are now actually populated by buildContext.
 */
data class ConversationContext(
    val daemonId: Long,
    val daemonName: String,
    val archetypeKey: String,
    val level: Int,
    val openMajors: List<MajorQuest>,
    val recentlyClosedMajor: MajorQuest?,         // within last 24h
    val recentlyCompletedMinors: List<MinorQuest>, // within last 24h
    val minorsCompletedToday: Int,
    val minorsCompletedThisWeek: Int,
    val dailyMinorsLapsedCount: Int,
    val streakDays: Int,
    val totalWishesAvailable: Int,
    val recentlySpentBoonText: String?,           // within last 24h
    val conversationsHad: Int,
    val daysSinceLastConversation: Int?,
    val daysSinceFirstConversation: Int?,
    val majorsClosedTotal: Int,                   // denormalized, on daemon_state
    val wishesSpentTotal: Int,                    // denormalized, on daemon_state
    val minutesSinceLastForeground: Int?,         // for mid-day-return guard
    val dayOfWeek: Int,
    val timeOfDay: TimeOfDay,
    // v0.0.11 attention surface — predicates that need to react to
    // decay loss read these. attentionLost24h is the amount taken in
    // the most recent decay tick if that tick happened within the
    // last 24h, else 0.
    val attentionPoints: Int,
    val attentionLost24h: Int,
    // v0.0.11 calendar awareness — one token at most per pick.
    // Resolution order applied by the repository: BIRTHDAY >
    // PERSONAL_DATE > cultural holidays. personalDateLabel is the
    // user's own text for the matched personal date (templated into
    // the line at output site — safe because the user wrote it).
    val holidayToken: HolidayToken?,
    val personalDateLabel: String?,
    /**
     * v0.0.12 themed dialogue: the daemon's chosen LifeTheme key
     * (e.g. "EXERCISE"), or null when the user picked "Other" on
     * summoning. The engine filters theme-tagged lines against this.
     */
    val theme: String?,
)
