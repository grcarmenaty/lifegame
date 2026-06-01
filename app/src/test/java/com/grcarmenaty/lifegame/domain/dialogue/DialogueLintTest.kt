package com.grcarmenaty.lifegame.domain.dialogue

import com.grcarmenaty.lifegame.domain.dialogue.lines.DialogueCorpus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

/**
 * The shipped corpus must pass these checks. Spec from design v6, Ally
 * round 3 + round 5. See `docs/design/dialogue-v0.0.6.md`.
 */
class DialogueLintTest {

    private val corpus = DialogueCorpus.all
    private val byId = corpus.associateBy { it.id }

    /** (a) id uniqueness across all archetype files */
    @Test fun ids_are_unique() {
        val seen = mutableSetOf<String>()
        for (line in corpus) {
            if (!seen.add(line.id)) fail("Duplicate line id: ${line.id}")
        }
    }

    /** (b) no dangling requires / forbids / nextLineId / replacedBy */
    @Test fun references_resolve() {
        for (line in corpus) {
            for (ref in line.requires) {
                assertNotNull("Line ${line.id} requires unknown id $ref", byId[ref])
            }
            for (ref in line.forbids) {
                assertNotNull("Line ${line.id} forbids unknown id $ref", byId[ref])
            }
            for (choice in line.choices) {
                choice.nextLineId?.let { ref ->
                    assertNotNull("Line ${line.id} choice → unknown id $ref", byId[ref])
                }
            }
            line.replacedBy?.let { ref ->
                assertNotNull("Line ${line.id} replacedBy unknown id $ref", byId[ref])
            }
        }
    }

    /**
     * (e) archetypeWhitelist-bearing predicates not attached to lines
     * whose archetype is outside the whitelist
     */
    @Test fun reactive_predicates_respect_archetype_whitelist() {
        for (line in corpus) {
            for (predicate in line.stateRequirements) {
                val whitelist = predicate.archetypeWhitelist ?: continue
                if (line.archetype == "ANY") continue // ANY is always allowed
                assertTrue(
                    "Line ${line.id} (archetype=${line.archetype}) uses a predicate " +
                        "(${predicate.javaClass.simpleName}) whose whitelist is $whitelist",
                    line.archetype in whitelist,
                )
            }
        }
    }

    /**
     * (f) chain integrity through deprecation — if A requires B and B
     * is deprecated, B must declare replacedBy. (Lint catches the case
     * the engine no longer rewrites at runtime.)
     */
    @Test fun deprecated_requires_replacedBy_when_chain_references_it() {
        for (line in corpus) {
            if (!line.deprecated) continue
            val referencedBy = corpus.any { other -> line.id in other.requires }
            if (referencedBy) {
                assertNotNull(
                    "Deprecated line ${line.id} is referenced by live chains but " +
                        "has no replacedBy",
                    line.replacedBy,
                )
            }
        }
    }

    /** Live requires/forbids must not target deprecated ids (round 4 Architect). */
    @Test fun live_lines_dont_reference_deprecated() {
        for (line in corpus) {
            if (line.deprecated) continue
            for (ref in line.requires + line.forbids) {
                val target = byId[ref] ?: continue
                assertTrue(
                    "Live line ${line.id} references deprecated id $ref " +
                        "without going through deprecate-and-replace",
                    !target.deprecated,
                )
            }
        }
    }

    /** (g) INLINE-preferred lines may not carry choices. */
    @Test fun inline_lines_have_no_choices() {
        for (line in corpus) {
            if (line.preferredSurface != PreferredSurface.INLINE) continue
            assertTrue(
                "INLINE-preferred line ${line.id} carries ${line.choices.size} " +
                    "choices — inline surfaces cannot render branching",
                line.choices.isEmpty(),
            )
        }
    }

    /**
     * (h) every line whose predicates include AfterLapse(_) must carry
     * `cooldownGroup = LAPSE_REACTIVE_COOLDOWN` and
     * `crossSurfaceCooldown = true` (Skeptic round 3 compound-shame fix).
     */
    @Test fun lapse_reactive_lines_carry_cross_surface_cooldown() {
        for (line in corpus) {
            val touchesLapse = line.stateRequirements.any { it is AfterLapse }
            if (!touchesLapse) continue
            assertEquals(
                "Line ${line.id} reacts to lapses but isn't in the cross-surface " +
                    "compound-shame guard",
                LAPSE_REACTIVE_COOLDOWN, line.cooldownGroup,
            )
            assertTrue(
                "Line ${line.id} reacts to lapses but isn't crossSurfaceCooldown",
                line.crossSurfaceCooldown,
            )
        }
    }

    /**
     * Believer round 5 protect-list: the 180-min foreground floor on
     * mid-day-return is load-bearing. Any line that uses
     * `MinutesSinceLastForegroundAtLeast` must use the canonical
     * 3-hour singleton, never an arbitrary value.
     */
    @Test fun mid_day_return_uses_canonical_3h_floor() {
        for (line in corpus) {
            val p = line.stateRequirements
                .firstOrNull { it is MinutesSinceLastForegroundAtLeast }
                ?: continue
            assertTrue(
                "Line ${line.id} uses a non-canonical foreground floor — Believer " +
                    "round 5 protect-list demands ForegroundFloor_3h (180 min). " +
                    "Lowering this turns the daemon into surveillance.",
                p === ForegroundFloor_3h,
            )
        }
    }

    /**
     * Ally round 4: RecencyKey enum order is load-bearing for the sort
     * (TODAY before EVER). A silent reorder would invert the ordering.
     */
    @Test fun recency_key_order_is_locked() {
        assertTrue("TODAY must come before THIS_WEEK",
            RecencyKey.TODAY.ordinal < RecencyKey.THIS_WEEK.ordinal)
        assertTrue("THIS_WEEK must come before THIS_MONTH",
            RecencyKey.THIS_WEEK.ordinal < RecencyKey.THIS_MONTH.ordinal)
        assertTrue("THIS_MONTH must come before EVER",
            RecencyKey.THIS_MONTH.ordinal < RecencyKey.EVER.ordinal)
    }

    /**
     * v0.0.11 calendar guard: holiday-gated lines must carry
     * `RecencyKey.TODAY`. Cultural dates burn for a single calendar day;
     * a stale EVER key could be picked again weeks later via the
     * playCount tiebreaker.
     */
    @Test fun holiday_lines_use_today_recency() {
        for (line in corpus) {
            val touchesHoliday = line.stateRequirements.any { it is OnHoliday }
            if (!touchesHoliday) continue
            assertEquals(
                "Holiday line ${line.id} must use RecencyKey.TODAY so it doesn't " +
                    "leak past its date through the playCount tiebreaker.",
                RecencyKey.TODAY, line.recencyKey,
            )
        }
    }

    /**
     * v0.0.11 templating guard: any line whose predicate is
     * `IsPersonalDate` must carry the `{label}` placeholder. Without
     * it the repository's `renderLine` is a no-op and the line reads
     * generically when it should reference the user's authored text.
     */
    @Test fun personal_date_lines_contain_label_placeholder() {
        for (line in corpus) {
            val isPersonalDate = line.stateRequirements.any { it === IsPersonalDate }
            if (!isPersonalDate) continue
            assertTrue(
                "Personal-date line ${line.id} must contain the {label} placeholder " +
                    "so the user's own text is interpolated; otherwise the line is " +
                    "generic and the predicate wasted.",
                "{label}" in line.text,
            )
        }
    }

    /**
     * v0.0.11 archetype-coverage floor: each per-archetype file must
     * carry at least 30 distinct lines. Below that the cooldown system
     * runs out of fresh content within a couple of weeks and the
     * daemon's voice flattens. ANY lines don't count toward this floor
     * (they're a shared fallback pool).
     */
    @Test fun each_per_archetype_file_has_at_least_30_lines() {
        val perArchetype = corpus.filter { it.archetype != "ANY" }
            .groupBy { it.archetype }
        for ((arch, lines) in perArchetype) {
            assertTrue(
                "Archetype $arch has only ${lines.size} lines; floor is 30. " +
                    "Below this the engine recycles content too quickly.",
                lines.size >= 30,
            )
        }
    }
}
