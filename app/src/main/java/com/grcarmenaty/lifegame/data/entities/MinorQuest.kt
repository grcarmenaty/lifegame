package com.grcarmenaty.lifegame.data.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

/**
 * A small act on the daemon's quest. v0.0.12 extends the cadence model:
 *
 *   - [cadence] is the period — ONE_OFF / DAILY / WEEKLY / MONTHLY.
 *   - [cadenceCount] is `n` times per period. Default 1. Ignored for
 *     ONE_OFF (always 1).
 *   - [cadenceDays] applies only to WEEKLY and is a CSV of
 *     [java.util.Calendar.DAY_OF_WEEK] integers (Sunday = 1 … Saturday
 *     = 7). When non-empty the minor is openable only on those days
 *     and the count is implicitly 1 per selected day.
 *   - [completionsThisWindow] tracks how many times the minor has been
 *     completed inside the *current* window. The repository resets it
 *     when [lastCompletedAt] falls outside today / this-week / this-
 *     month before incrementing.
 *
 * Backed by additive Room migration v6 → v7 (the legacy single-string
 * `cadence` column survives intact; the three new columns default in
 * for existing rows).
 */
@Serializable
@Entity(
    tableName = "minor_quests",
    foreignKeys = [
        ForeignKey(
            entity = MajorQuest::class,
            parentColumns = ["id"],
            childColumns = ["majorQuestId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("majorQuestId")]
)
data class MinorQuest(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val majorQuestId: Long,
    val title: String,
    val cadence: String,
    val weight: Int = 1,
    val completed: Boolean = false,
    val lastCompletedAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    // v0.0.12 cadence enrichment:
    /** N times per period. Min 1, max 9. Ignored for ONE_OFF. */
    val cadenceCount: Int = 1,
    /**
     * CSV of [java.util.Calendar.DAY_OF_WEEK] ints for WEEKLY only.
     * Null or empty = "any day". When non-empty [cadenceCount] is
     * implicitly 1 per selected day.
     */
    val cadenceDays: String? = null,
    /**
     * Completions in the current window. The repository compares the
     * last-completed window key with `now`'s window key on each
     * completion; resets to 1 on rollover, increments otherwise.
     */
    val completionsThisWindow: Int = 0,
    /**
     * v0.0.14: stable id of the [com.grcarmenaty.lifegame.domain.catalog
     * .QuestCatalog] minor this was picked from, or null for custom
     * minors. Drives the quest-specific completion line (composed with
     * the daemon's archetype frame); custom minors fall back to a
     * generic per-archetype frame.
     */
    val templateId: String? = null,
    /**
     * v0.0.15: user-edited completion phrase, replacing the catalog
     * fragment in the composed completion line so an edited minor's
     * snackbar still reads naturally. Null = use the catalog fragment
     * (templateId), or the generic per-archetype line for custom minors.
     */
    val fragmentOverride: String? = null,
) {
    /** Parsed view of [cadenceDays]. Empty when null/blank. */
    fun parsedCadenceDays(): Set<Int> = parseDaysCsv(cadenceDays)

    /**
     * Effective per-window count. WEEKLY + days selected pins it to 1
     * (one completion per selected day). ONE_OFF is always 1.
     * Otherwise the authored [cadenceCount] wins.
     */
    fun effectiveCount(): Int = when {
        cadence == CADENCE_ONE_OFF -> 1
        cadence == CADENCE_WEEKLY && !cadenceDays.isNullOrBlank() -> 1
        else -> cadenceCount.coerceAtLeast(1)
    }

    companion object {
        const val CADENCE_ONE_OFF = "ONE_OFF"
        const val CADENCE_DAILY = "DAILY"
        const val CADENCE_WEEKLY = "WEEKLY"
        const val CADENCE_MONTHLY = "MONTHLY"

        /** Display order in pickers. One-off first; repeatable cadences ascending. */
        val ALL_CADENCES = listOf(CADENCE_ONE_OFF, CADENCE_DAILY, CADENCE_WEEKLY, CADENCE_MONTHLY)

        /** Period-only label — short. UI calls [cadenceLongLabel] for the rich form. */
        fun cadenceLabel(cadence: String): String = when (cadence) {
            CADENCE_ONE_OFF -> "One-off"
            CADENCE_DAILY -> "Daily"
            CADENCE_WEEKLY -> "Weekly"
            CADENCE_MONTHLY -> "Monthly"
            else -> cadence
        }

        /**
         * Reader-facing summary of a full cadence spec.
         * `(DAILY, 3, _)` → `"3× daily"`, `(WEEKLY, _, {Mon, Wed})` →
         * `"Weekly · Mon, Wed"`, `(ONE_OFF, _, _)` → `"One-off"`.
         */
        fun cadenceLongLabel(cadence: String, count: Int, days: Set<Int>): String {
            if (cadence == CADENCE_ONE_OFF) return "One-off"
            if (cadence == CADENCE_WEEKLY && days.isNotEmpty()) {
                val names = DAY_OF_WEEK_ORDER
                    .filter { it in days }
                    .joinToString(", ") { dayShortName(it) }
                return "Weekly · $names"
            }
            val periodWord = when (cadence) {
                CADENCE_DAILY -> "daily"
                CADENCE_WEEKLY -> "weekly"
                CADENCE_MONTHLY -> "monthly"
                else -> cadence.lowercase()
            }
            val n = count.coerceAtLeast(1)
            return if (n == 1) periodWord.replaceFirstChar { it.uppercase() } else "$n× $periodWord"
        }

        /** Day-chip display order: Mon first, Sun last. Values are Calendar.DAY_OF_WEEK. */
        val DAY_OF_WEEK_ORDER = listOf(2, 3, 4, 5, 6, 7, 1)

        fun dayShortName(dayOfWeek: Int): String = when (dayOfWeek) {
            1 -> "Sun"; 2 -> "Mon"; 3 -> "Tue"; 4 -> "Wed"
            5 -> "Thu"; 6 -> "Fri"; 7 -> "Sat"
            else -> "?"
        }

        fun parseDaysCsv(csv: String?): Set<Int> {
            if (csv.isNullOrBlank()) return emptySet()
            return csv.split(",").mapNotNull { it.trim().toIntOrNull() }.toSet()
        }

        fun encodeDays(days: Set<Int>): String? =
            if (days.isEmpty()) null else days.sorted().joinToString(",")
    }
}
