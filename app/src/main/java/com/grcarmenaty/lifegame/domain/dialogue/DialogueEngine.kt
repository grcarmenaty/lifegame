package com.grcarmenaty.lifegame.domain.dialogue

/**
 * The selector. Reads `DialogueState` (one batched read upstream) plus
 * `ConversationContext` and picks the highest-ranked eligible line.
 *
 * Ordering inside the surviving tier:
 *   lifeEvent desc -> recencyKey asc -> priority desc
 *   -> playCount asc (Hades soft-exhaust) -> lastPlayedAt asc
 *
 * Pure CPU over an in-memory list — safe on the caller's dispatcher.
 */
class DialogueEngine(private val corpus: List<DialogueLine>) {

    fun pickFor(
        category: LineCategory,
        surface: Surface,
        ctx: ConversationContext,
        state: DialogueState,
    ): DialogueLine? {
        val played = state.played
        val cooldowns = state.cooldownsFor(surface)

        val eligible = corpus.asSequence()
            .filter { it.category == category }
            .filter { !it.deprecated }
            .filter { it.archetype == ctx.archetypeKey || it.archetype == "ANY" }
            .filter { surfaceMatches(it.preferredSurface, surface) }
            .filter { it.requires.all(played::contains) }
            .filter { it.forbids.none(played::contains) }
            .filter { satisfiedAndPermitted(it, ctx) }
            .filter { it.cooldownGroup == null || it.cooldownGroup !in cooldowns }
            .toList()

        if (eligible.isEmpty()) return null

        val byTier = eligible.groupBy { it.tier }
        val tier = listOf(LineTier.ESSENTIAL, LineTier.CONTEXTUAL, LineTier.FILLER)
            .firstOrNull { (byTier[it] ?: emptyList()).isNotEmpty() }
            ?: return null

        return byTier[tier]!!.sortedWith(
            compareByDescending<DialogueLine> { it.lifeEvent }
                .thenBy { it.recencyKey.ordinal }
                .thenByDescending { it.priority }
                .thenBy { state.playCounts[it.id] ?: 0 }
                .thenBy { state.lastPlayedAt[it.id] ?: 0L }
        ).first()
    }

    private fun surfaceMatches(pref: PreferredSurface, asking: Surface): Boolean = when (pref) {
        PreferredSurface.EITHER -> true
        PreferredSurface.INLINE -> asking == Surface.INLINE
        PreferredSurface.SCREEN -> asking == Surface.SCREEN
    }

    private fun satisfiedAndPermitted(line: DialogueLine, ctx: ConversationContext): Boolean {
        for (p in line.stateRequirements) {
            val whitelist = p.archetypeWhitelist
            if (whitelist != null && ctx.archetypeKey !in whitelist) return false
            if (!p.isSatisfied(ctx)) return false
        }
        return true
    }
}
