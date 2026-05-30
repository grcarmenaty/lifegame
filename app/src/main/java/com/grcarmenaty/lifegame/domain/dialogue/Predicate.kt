package com.grcarmenaty.lifegame.domain.dialogue

/**
 * Engine-checkable gate. Implementations are `object` singletons (or
 * top-level interned `val` instances for parameterized variants) per the
 * Architect's hard rule — no per-line allocation. Linted by
 * `DialogueLintTest`.
 *
 * `archetypeWhitelist` is the Skeptic's surveillance guard: predicates
 * carrying shame-amplifier risk (e.g. `AfterLapse`) declare which
 * archetypes may match them.
 */
interface Predicate {
    fun isSatisfied(ctx: ConversationContext): Boolean
    val archetypeWhitelist: Set<String>? get() = null
}

// ---- Singletons ----

object FirstConversation : Predicate {
    override fun isSatisfied(ctx: ConversationContext) = ctx.conversationsHad == 0
}

object BoonSpentToday : Predicate {
    override fun isSatisfied(ctx: ConversationContext) =
        ctx.recentlySpentBoonText != null
}

object MinorsCompletedTodayIsZero : Predicate {
    override fun isSatisfied(ctx: ConversationContext) =
        ctx.minorsCompletedToday == 0
}

// ---- Time of day ----

class TimeOfDayIs(private val t: TimeOfDay) : Predicate {
    override fun isSatisfied(ctx: ConversationContext) = ctx.timeOfDay == t
}
val TimeOfDay_Morning = TimeOfDayIs(TimeOfDay.MORNING)
val TimeOfDay_Afternoon = TimeOfDayIs(TimeOfDay.AFTERNOON)
val TimeOfDay_Evening = TimeOfDayIs(TimeOfDay.EVENING)
val TimeOfDay_Night = TimeOfDayIs(TimeOfDay.NIGHT)

// ---- Interned small-arity predicates ----

class MinorsCompletedTodayAtLeast(private val n: Int) : Predicate {
    override fun isSatisfied(ctx: ConversationContext) =
        ctx.minorsCompletedToday >= n
}
val MinorsToday_1 = MinorsCompletedTodayAtLeast(1)
val MinorsToday_3 = MinorsCompletedTodayAtLeast(3)
val MinorsToday_5 = MinorsCompletedTodayAtLeast(5)

class StreakAtLeast(private val days: Int) : Predicate {
    override fun isSatisfied(ctx: ConversationContext) = ctx.streakDays >= days
}
val Streak_3 = StreakAtLeast(3)
val Streak_7 = StreakAtLeast(7)
val Streak_14 = StreakAtLeast(14)

class WithWishesAvailable(private val min: Int) : Predicate {
    override fun isSatisfied(ctx: ConversationContext) =
        ctx.totalWishesAvailable >= min
}
val WishesAvailable_1 = WithWishesAvailable(1)

class AtLevelAtLeast(private val n: Int) : Predicate {
    override fun isSatisfied(ctx: ConversationContext) = ctx.level >= n
}
val Level_2 = AtLevelAtLeast(2)
val Level_5 = AtLevelAtLeast(5)
val Level_10 = AtLevelAtLeast(10)

class ConversationsHadAtLeast(private val n: Int) : Predicate {
    override fun isSatisfied(ctx: ConversationContext) = ctx.conversationsHad >= n
}
val Conversations_3 = ConversationsHadAtLeast(3)
val Conversations_7 = ConversationsHadAtLeast(7)
val Conversations_15 = ConversationsHadAtLeast(15)

class DaysSinceFirstConversationAtLeast(private val n: Int) : Predicate {
    override fun isSatisfied(ctx: ConversationContext) =
        (ctx.daysSinceFirstConversation ?: 0) >= n
}
val DaysSinceFirst_7 = DaysSinceFirstConversationAtLeast(7)
val DaysSinceFirst_30 = DaysSinceFirstConversationAtLeast(30)

class AfterApotheosisWithin(private val hours: Int) : Predicate {
    override fun isSatisfied(ctx: ConversationContext): Boolean {
        val closed = ctx.recentlyClosedMajor ?: return false
        return (System.currentTimeMillis() - closed.createdAt) <=
            hours.toLong() * 3600_000L
    }
}
val AfterApotheosis_24h = AfterApotheosisWithin(24)

class MinutesSinceLastForegroundAtLeast(private val minutes: Int) : Predicate {
    override fun isSatisfied(ctx: ConversationContext) =
        (ctx.minutesSinceLastForeground ?: Int.MAX_VALUE) >= minutes
}
/** Believer round 4: the 3-hour floor below which mid-day-return reads
 *  as surveillance for a power user reopening 5x/day. Do NOT lower. */
val ForegroundFloor_3h = MinutesSinceLastForegroundAtLeast(180)

// ---- Reactive predicates (Skeptic's archetypeWhitelist guard) ----

class AfterLapse(private val min: Int) : Predicate {
    override val archetypeWhitelist = setOf(
        "DRILL_SERGEANT", "COACH", "TRICKSTER", "STOIC",
    )
    override fun isSatisfied(ctx: ConversationContext) =
        ctx.dailyMinorsLapsedCount >= min
}
val AfterLapse_1 = AfterLapse(1)
val AfterLapse_3 = AfterLapse(3)

// ---- Earned predicates (Believer round 2) ----

class MajorsClosedAtLeast(private val n: Int) : Predicate {
    override fun isSatisfied(ctx: ConversationContext) =
        ctx.majorsClosedTotal >= n
}
val MajorsClosed_1 = MajorsClosedAtLeast(1)
val MajorsClosed_3 = MajorsClosedAtLeast(3)
val MajorsClosed_10 = MajorsClosedAtLeast(10)

class WishesSpentAtLeast(private val n: Int) : Predicate {
    override fun isSatisfied(ctx: ConversationContext) =
        ctx.wishesSpentTotal >= n
}
val WishesSpent_1 = WishesSpentAtLeast(1)
val WishesSpent_5 = WishesSpentAtLeast(5)
