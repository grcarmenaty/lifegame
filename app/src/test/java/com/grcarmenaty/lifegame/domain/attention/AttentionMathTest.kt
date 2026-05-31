package com.grcarmenaty.lifegame.domain.attention

import com.grcarmenaty.lifegame.data.entities.MajorQuest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pure-function tests for the v0.0.10 attention math. Architect's
 * MigrationTestHelper round-trip on the actual SQL migration remains
 * a tracked follow-up (would need androidTest infra in CI). These
 * tests cover the math the migration depends on.
 */
class AttentionMathTest {

    @Test fun levelFor_returns_0_below_threshold_1() {
        assertEquals(0, AttentionMath.levelFor(0))
        assertEquals(0, AttentionMath.levelFor(9))
    }

    @Test fun levelFor_returns_1_at_threshold_1() {
        assertEquals(1, AttentionMath.levelFor(10))
        assertEquals(1, AttentionMath.levelFor(34))
    }

    @Test fun levelFor_returns_2_at_threshold_2() {
        assertEquals(2, AttentionMath.levelFor(35))
        assertEquals(2, AttentionMath.levelFor(84))
    }

    @Test fun levelFor_returns_3_at_threshold_3() {
        assertEquals(3, AttentionMath.levelFor(85))
        assertEquals(3, AttentionMath.levelFor(159))
    }

    @Test fun levelFor_caps_at_4() {
        assertEquals(4, AttentionMath.levelFor(160))
        assertEquals(4, AttentionMath.levelFor(1_000))
        assertEquals(4, AttentionMath.levelFor(Int.MAX_VALUE))
    }

    @Test fun levelProgress_is_0_at_level_threshold() {
        assertEquals(0f, AttentionMath.levelProgress(0))
        assertEquals(0f, AttentionMath.levelProgress(10))
        assertEquals(0f, AttentionMath.levelProgress(35))
        assertEquals(0f, AttentionMath.levelProgress(85))
        // At 160, the bar starts the buffer cycle from 0.
        assertEquals(0f, AttentionMath.levelProgress(160))
    }

    @Test fun levelProgress_climbs_toward_1_within_tier() {
        // 35 → 85 is a span of 50; halfway at 60.
        val halfWay = AttentionMath.levelProgress(60)
        assertTrue("halfway should be ~0.5, was $halfWay", halfWay in 0.4f..0.6f)
    }

    @Test fun levelProgress_above_max_cycles_in_buffer() {
        // Every +100 attention above 160 is one full buffer cycle.
        assertEquals(0.5f, AttentionMath.levelProgress(210), 0.001f)
        assertEquals(0f, AttentionMath.levelProgress(260), 0.001f)
    }

    // ---- AttentionBackfill ----

    @Test fun backfill_returns_0_for_empty_pantheon() {
        assertEquals(0, AttentionBackfill.compute(emptyList()))
    }

    @Test fun backfill_credits_25_per_closed_major() {
        val majors = listOf(
            majorOf(closed = true, progress = 0, threshold = 3),
            majorOf(closed = true, progress = 0, threshold = 3),
        )
        // 2 × 25 (closures) + min(0,3) + min(0,3) = 50.
        assertEquals(50, AttentionBackfill.compute(majors))
    }

    @Test fun backfill_credits_progress_up_to_threshold() {
        val majors = listOf(majorOf(closed = false, progress = 3, threshold = 5))
        assertEquals(3, AttentionBackfill.compute(majors))
    }

    @Test fun backfill_caps_progress_at_threshold_to_avoid_daily_overflow() {
        // Architect round 1 fix #2: a DAILY minor can push progressCount
        // past thresholdCount over time — don't over-credit on backfill.
        val majors = listOf(majorOf(closed = false, progress = 50, threshold = 3))
        assertEquals(3, AttentionBackfill.compute(majors))
    }

    @Test fun backfill_double_counts_closed_majors_intentionally() {
        // A closed major credits BOTH the ×25 bonus AND
        // min(progress, threshold). Documented as intentional.
        val majors = listOf(majorOf(closed = true, progress = 5, threshold = 3))
        assertEquals(25 + 3, AttentionBackfill.compute(majors))
    }

    private fun majorOf(closed: Boolean, progress: Int, threshold: Int): MajorQuest =
        MajorQuest(
            id = 0,
            daemonId = 1,
            title = "test",
            thresholdCount = threshold,
            progressCount = progress,
            completed = closed,
            wishBoonId = null,
            wishRewardCount = 1,
        )
}
