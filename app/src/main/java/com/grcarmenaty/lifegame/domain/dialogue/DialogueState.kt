package com.grcarmenaty.lifegame.domain.dialogue

/**
 * The snapshot a single `DialogueEngine.pickFor` call needs. Built by
 * `DialogueStateStore.loadState` in ONE batched DAO call (Architect's
 * round-4 threading fix). No in-memory cache; every pick re-reads.
 */
data class DialogueState(
    val played: Set<String>,
    val playCounts: Map<String, Int>,
    val lastPlayedAt: Map<String, Long>,
    val activeCooldownsInline: Set<String>,
    val activeCooldownsScreen: Set<String>,
    val activeCooldownsBoth: Set<String>,
) {
    fun cooldownsFor(surface: Surface): Set<String> {
        val surfaceSpecific = if (surface == Surface.INLINE) activeCooldownsInline
                              else activeCooldownsScreen
        return surfaceSpecific + activeCooldownsBoth
    }
}
