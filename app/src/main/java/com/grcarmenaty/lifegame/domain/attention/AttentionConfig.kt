package com.grcarmenaty.lifegame.domain.attention

import com.grcarmenaty.lifegame.data.entities.DaemonState
import com.grcarmenaty.lifegame.domain.VoicePreset

/**
 * Resolves per-daemon decay + accrual config from
 * (archetype default, per-daemon override). One place to ask
 * "what are this daemon's actual numbers" — Architect's round-1
 * recommendation to avoid scattering null-coalescing across worker
 * + UI + accrual.
 */
data class ResolvedAttentionConfig(
    val decayPerDay: Int,
    val decayGraceDays: Int,
    val minorsPerBoonAccrual: Int,
    val decayDisabled: Boolean,
)

object AttentionConfig {
    fun resolve(state: DaemonState, voicePreset: VoicePreset): ResolvedAttentionConfig =
        ResolvedAttentionConfig(
            decayPerDay = state.attentionDecayPerDay ?: voicePreset.decayPerDay,
            decayGraceDays = state.attentionDecayGraceDays ?: voicePreset.decayGraceDays,
            minorsPerBoonAccrual = state.minorsPerBoonAccrual ?: voicePreset.minorsPerBoonAccrual,
            decayDisabled = state.decayDisabled,
        )
}
