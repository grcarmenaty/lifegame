package com.grcarmenaty.lifegame.domain.attention

import com.grcarmenaty.lifegame.data.entities.MajorQuest

/**
 * Tiered level thresholds. Max 4. Attention above 160 is allowed and
 * acts as a decay buffer (Ally polish: shimmer pip in UI). Level can
 * display 0 when attention decays below 10.
 */
object AttentionMath {

    const val LEVEL_THRESHOLD_1 = 10
    const val LEVEL_THRESHOLD_2 = 35
    const val LEVEL_THRESHOLD_3 = 85
    const val LEVEL_THRESHOLD_4 = 160
    const val MAX_LEVEL = 4
    /** Attention bonus deposited on user-driven major closure. */
    const val MAJOR_CLOSURE_ATTENTION = 25

    fun levelFor(attentionPoints: Int): Int = when {
        attentionPoints < LEVEL_THRESHOLD_1 -> 0
        attentionPoints < LEVEL_THRESHOLD_2 -> 1
        attentionPoints < LEVEL_THRESHOLD_3 -> 2
        attentionPoints < LEVEL_THRESHOLD_4 -> 3
        else -> 4
    }

    /** Cumulative attention required to reach [level]. */
    fun thresholdFor(level: Int): Int = when (level) {
        1 -> LEVEL_THRESHOLD_1
        2 -> LEVEL_THRESHOLD_2
        3 -> LEVEL_THRESHOLD_3
        4 -> LEVEL_THRESHOLD_4
        else -> 0
    }

    /**
     * Progress fraction toward the next level, 0..1. At level 4
     * returns the buffer fraction (overflow above 160, scaled
     * generously so a daemon at attention 250 shows ~half-full
     * shimmer). UI uses this for the bar fill.
     */
    fun levelProgress(attentionPoints: Int): Float {
        val level = levelFor(attentionPoints)
        if (level >= MAX_LEVEL) {
            // Shimmer pip semantics: every 100 extra points = one
            // "buffer cycle" full. Capped at 1f for display.
            val overflow = attentionPoints - LEVEL_THRESHOLD_4
            return ((overflow % 100) / 100f).coerceIn(0f, 1f)
        }
        val floor = thresholdFor(level)
        val ceiling = thresholdFor(level + 1)
        val span = (ceiling - floor).coerceAtLeast(1)
        return ((attentionPoints - floor).toFloat() / span).coerceIn(0f, 1f)
    }
}

/**
 * Computes attention backfill from past data for an existing daemon
 * during the v4→v5 migration or v2 → v3 backup import. Shared helper
 * per Architect's round-1 recommendation.
 *
 * Formula:
 * ```
 * attention = (closedMajors × MAJOR_CLOSURE_ATTENTION)
 *           + sum over majors of min(progressCount, thresholdCount)
 * ```
 *
 * The cap inside the sum (Architect's round-1 fix #2) prevents DAILY
 * minors that ran past thresholdCount from over-crediting; that
 * overflow was never gating anything and shouldn't backfill into
 * attention either.
 *
 * The double-credit on a closed major (it counts via both the
 * `×25 bonus` and `min(progressCount, threshold)`) is intentional:
 * a closed major's worth of minor work was real, and the closure
 * itself is its own event.
 */
object AttentionBackfill {

    fun compute(majors: List<MajorQuest>): Int {
        val closedCount = majors.count { it.completed }
        val cappedProgress = majors.sumOf {
            minOf(it.progressCount, it.thresholdCount)
        }
        return closedCount * AttentionMath.MAJOR_CLOSURE_ATTENTION + cappedProgress
    }
}
