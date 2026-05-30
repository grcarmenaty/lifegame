package com.grcarmenaty.lifegame.domain.dialogue

import com.grcarmenaty.lifegame.data.entities.MajorQuest
import com.grcarmenaty.lifegame.data.entities.MinorQuest

enum class TimeOfDay { MORNING, AFTERNOON, EVENING, NIGHT }
enum class Surface { INLINE, SCREEN }

/**
 * Everything predicates can see. Built once per `DialogueEngine.pickFor`
 * call by the repository; never mutated thereafter.
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
)
