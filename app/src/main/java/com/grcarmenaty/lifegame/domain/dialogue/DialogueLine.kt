package com.grcarmenaty.lifegame.domain.dialogue

/**
 * Atomic content unit. Inspired by Supergiant's Hades approach: a flat
 * predicate-gated pool, three-tier priority, implicit sequencing via
 * `requires`/`forbids` on lineIds, named cooldown groups. See the design
 * doc at `docs/design/dialogue-v0.0.6.md` for council history.
 *
 * Authoring rules enforced by `DialogueLintTest`:
 * - `id` is stable across releases (rename via deprecate-and-replace only).
 * - ESSENTIAL + lifeEvent must declare an explicit `preferredSurface`.
 * - INLINE-only lines may not carry `choices`.
 * - Any line with `AfterLapse(_)` in predicates must carry the
 *   `LAPSE_REACTIVE_COOLDOWN` group with `crossSurfaceCooldown = true`.
 */
data class DialogueLine(
    val id: String,
    val archetype: String,
    val text: String,
    val tier: LineTier,
    val category: LineCategory,
    val preferredSurface: PreferredSurface = PreferredSurface.EITHER,
    val priority: Int = 0,
    val lifeEvent: Boolean = false,
    val recencyKey: RecencyKey = RecencyKey.EVER,
    val requires: List<String> = emptyList(),
    val forbids: List<String> = emptyList(),
    val stateRequirements: List<Predicate> = emptyList(),
    val cooldownGroup: String? = null,
    val cooldownPicks: Int? = null,
    val crossSurfaceCooldown: Boolean = false,
    val tellHint: TellStyle = TellStyle.NONE,
    val choices: List<DialogueChoice> = emptyList(),
    val deprecated: Boolean = false,
    val replacedBy: String? = null,
)

enum class LineTier { FILLER, CONTEXTUAL, ESSENTIAL }
enum class LineCategory { OPENER, COMPLETION, APOTHEOSIS, RESPONSE, NUDGE }
enum class PreferredSurface { EITHER, INLINE, SCREEN }
enum class TellStyle { NONE, ELLIPSIS, SNAP, FADE, PACE }

/**
 * Locked enum order. Ally round 4 caught that silent reorder would
 * invert the sort. [DialogueLineSortTest] asserts TODAY.ordinal <
 * EVER.ordinal etc.
 */
enum class RecencyKey { TODAY, THIS_WEEK, THIS_MONTH, EVER }

data class DialogueChoice(
    val text: String,
    val nextLineId: String?,
    val gates: List<Predicate> = emptyList(),
)

/** Shared cooldown group name — referenced by lint and engine. */
const val LAPSE_REACTIVE_COOLDOWN = "lapse_reactive_24h"
