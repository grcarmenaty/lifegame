package com.grcarmenaty.lifegame.domain.attention

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Pure-function tests for the decay computation the
 * AttentionDecayWorker and NudgeWorker apply. The user-facing contract
 * (v0.0.10 council): never decay inside the grace window, never decay
 * when the user has opted out (kill switch, or notifications off both
 * globally and for the daemon), and decay linearly per elapsed day
 * after grace.
 */
class AttentionDecayTest {

    private val day = AttentionDecay.DAY_MILLIS

    private fun cfg(
        decayPerDay: Int = 5,
        graceDays: Int = 1,
        disabled: Boolean = false,
    ) = ResolvedAttentionConfig(
        decayPerDay = decayPerDay,
        decayGraceDays = graceDays,
        minorsPerBoonAccrual = 5,
        decayDisabled = disabled,
    )

    private fun decay(
        anchor: Long?,
        now: Long,
        cfg: ResolvedAttentionConfig,
        master: Boolean = true,
        perDaemon: Boolean = true,
    ) = AttentionDecay.decayFor(anchor, now, cfg, master, perDaemon)

    @Test fun kill_switch_blocks_decay() {
        assertEquals(0, decay(0L, 10 * day, cfg(disabled = true)))
    }

    @Test fun zero_rate_never_decays() {
        // Hermit archetype: 0/day by default.
        assertEquals(0, decay(0L, 10 * day, cfg(decayPerDay = 0)))
    }

    @Test fun notifications_off_everywhere_pauses_decay() {
        assertEquals(0, decay(0L, 10 * day, cfg(), master = false, perDaemon = false))
    }

    @Test fun decay_runs_if_either_notification_channel_is_on() {
        assertEquals(45, decay(0L, 10 * day, cfg(), master = false, perDaemon = true))
        assertEquals(45, decay(0L, 10 * day, cfg(), master = true, perDaemon = false))
    }

    @Test fun unanchored_state_never_decays() {
        assertEquals(0, decay(null, 10 * day, cfg()))
    }

    @Test fun no_decay_within_grace() {
        // 1 grace day: nothing through the end of day 1.
        assertEquals(0, decay(0L, day - 1, cfg()))
        assertEquals(0, decay(0L, day, cfg()))
        assertEquals(0, decay(0L, 2 * day - 1, cfg()))
    }

    @Test fun decay_starts_after_grace_and_scales_per_day() {
        assertEquals(5, decay(0L, 2 * day, cfg()))
        assertEquals(10, decay(0L, 3 * day, cfg()))
        assertEquals(45, decay(0L, 10 * day, cfg()))
    }

    @Test fun partial_days_do_not_count() {
        assertEquals(5, decay(0L, 2 * day + day / 2, cfg()))
    }

    @Test fun zero_grace_decays_from_the_first_full_day() {
        assertEquals(0, decay(0L, day - 1, cfg(graceDays = 0)))
        assertEquals(5, decay(0L, day, cfg(graceDays = 0)))
    }
}
