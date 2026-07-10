package com.grcarmenaty.lifegame.domain.dialogue

import com.grcarmenaty.lifegame.domain.calendar.HolidayToken
import java.util.Calendar

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

object HasOpenMajors : Predicate {
    override fun isSatisfied(ctx: ConversationContext) =
        ctx.openMajors.isNotEmpty()
}

/**
 * v0.0.18: gates lines carrying the `{quest}` placeholder — satisfied
 * only when `buildContext` found an open minor quest whose title is
 * short enough to interpolate. Template-safe lines (no placeholder)
 * remain the fallback pool when this fails.
 */
object HasOpenQuest : Predicate {
    override fun isSatisfied(ctx: ConversationContext) =
        ctx.openQuestTitle != null
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
val MinorsToday_2 = MinorsCompletedTodayAtLeast(2)
val MinorsToday_3 = MinorsCompletedTodayAtLeast(3)
val MinorsToday_4 = MinorsCompletedTodayAtLeast(4)
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
val Level_1 = AtLevelAtLeast(1)
val Level_2 = AtLevelAtLeast(2)
val Level_3 = AtLevelAtLeast(3)
val Level_4 = AtLevelAtLeast(4)
// Legacy interns kept so older lines compile during the v0.0.11
// expansion; new content should prefer the Level_1..Level_4 set.
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

// ---- v0.0.11 calendar predicates ----

/**
 * Fires when today's resolved [HolidayToken] equals [token]. The
 * resolution priority (birthday > personal > cultural) is handled
 * upstream in the repository's `buildContext`; the predicate just
 * matches the single token that survived.
 */
class OnHoliday(private val token: HolidayToken) : Predicate {
    override fun isSatisfied(ctx: ConversationContext) = ctx.holidayToken == token
}
val OnSantJordi = OnHoliday(HolidayToken.SANT_JORDI)
val OnLaMerce = OnHoliday(HolidayToken.LA_MERCE)
val OnSantJoan = OnHoliday(HolidayToken.SANT_JOAN)
val OnDiada = OnHoliday(HolidayToken.DIADA)
val OnNadal = OnHoliday(HolidayToken.NADAL)
val OnCapDAny = OnHoliday(HolidayToken.CAP_D_ANY)
val OnReis = OnHoliday(HolidayToken.REIS)
val OnCarnaval = OnHoliday(HolidayToken.CARNAVAL)
val OnDivendresSant = OnHoliday(HolidayToken.DIVENDRES_SANT)
val OnPasqua = OnHoliday(HolidayToken.PASQUA)
val OnFestaDelTreball = OnHoliday(HolidayToken.FESTA_DEL_TREBALL)
val OnFestaMajorGracia = OnHoliday(HolidayToken.FESTA_MAJOR_GRACIA)
val OnAssumpcio = OnHoliday(HolidayToken.ASSUMPCIO)
val OnHispanitat = OnHoliday(HolidayToken.HISPANITAT)
val OnCastanyada = OnHoliday(HolidayToken.CASTANYADA)
val OnConstitucio = OnHoliday(HolidayToken.CONSTITUCIO)
val OnImmaculada = OnHoliday(HolidayToken.IMMACULADA)
val OnSantEsteve = OnHoliday(HolidayToken.SANT_ESTEVE)
val OnCapDAnyEve = OnHoliday(HolidayToken.CAP_D_ANY_EVE)
val IsBirthday = OnHoliday(HolidayToken.BIRTHDAY)
val IsPersonalDate = OnHoliday(HolidayToken.PERSONAL_DATE)

// ---- v0.0.11 day-of-week predicates ----

/**
 * Day-of-week predicate. [Calendar.DAY_OF_WEEK] semantics: SUNDAY=1,
 * MONDAY=2, ..., SATURDAY=7.
 */
class DayOfWeekIs(private val dow: Int) : Predicate {
    override fun isSatisfied(ctx: ConversationContext) = ctx.dayOfWeek == dow
}
val IsMonday = DayOfWeekIs(Calendar.MONDAY)
val IsFriday = DayOfWeekIs(Calendar.FRIDAY)
val IsSaturday = DayOfWeekIs(Calendar.SATURDAY)
val IsSunday = DayOfWeekIs(Calendar.SUNDAY)

object IsWeekend : Predicate {
    override fun isSatisfied(ctx: ConversationContext) =
        ctx.dayOfWeek == Calendar.SATURDAY || ctx.dayOfWeek == Calendar.SUNDAY
}

object IsWeekday : Predicate {
    override fun isSatisfied(ctx: ConversationContext) =
        ctx.dayOfWeek in Calendar.MONDAY..Calendar.FRIDAY
}

// ---- v0.0.11 level-exact predicates (transition gating) ----

/**
 * Exact-level match. Used for level-transition openers paired with
 * `lifeEvent = true` so they play once when the daemon hits that level.
 * Distinct from [AtLevelAtLeast], which keeps firing while ≥ N.
 */
class AtLevelExactly(private val n: Int) : Predicate {
    override fun isSatisfied(ctx: ConversationContext) = ctx.level == n
}
val LevelExactly_0 = AtLevelExactly(0)
val LevelExactly_1 = AtLevelExactly(1)
val LevelExactly_2 = AtLevelExactly(2)
val LevelExactly_3 = AtLevelExactly(3)
val LevelExactly_4 = AtLevelExactly(4)

// ---- v0.0.11 attention-loss predicates ----

/**
 * Fires when the daemon lost at least [n] attention points in the most
 * recent decay tick AND that tick happened within the last 24h. Used
 * by reactive lines that acknowledge the loss in the daemon's voice
 * instead of letting the bar drop silently.
 *
 * Whitelisted to archetypes that can carry the weight without becoming
 * shame-amplifiers (Drill Sergeant / Coach / Trickster / Stoic / Oracle).
 * Gentle Mentor / Therapist / Hermit have their own milder variants
 * that read attentionLost24h directly via a separate predicate.
 */
class AttentionLostAtLeast(private val n: Int) : Predicate {
    override val archetypeWhitelist = setOf(
        "DRILL_SERGEANT", "COACH", "TRICKSTER", "STOIC", "ORACLE",
    )
    override fun isSatisfied(ctx: ConversationContext) =
        ctx.attentionLost24h >= n
}
val AttentionLost_3 = AttentionLostAtLeast(3)
val AttentionLost_10 = AttentionLostAtLeast(10)

/**
 * Gentle variant — no whitelist, suitable for soft-stance archetypes.
 * They acknowledge the loss without weaponizing it.
 */
class AttentionLostAtLeastGentle(private val n: Int) : Predicate {
    override fun isSatisfied(ctx: ConversationContext) =
        ctx.attentionLost24h >= n
}
val AttentionLostGentle_3 = AttentionLostAtLeastGentle(3)
val AttentionLostGentle_10 = AttentionLostAtLeastGentle(10)
