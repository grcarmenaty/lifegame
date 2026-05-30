# Daemon Conversations — Design Plan

> **Version log**
> - v0 — pre-research, pre-council. Tree-based, naive.
> - v1 — post-Hades research, pre-council. Flat predicate pool.
> - **v2 (current) — post council round 1.** Two consumers of one
>   engine (inline surfaces + explicit screen), affinity deferred to
>   v0.0.7, `choice_log` cut, action-only same-encounter lock,
>   templating minimal, life-event-recency ordering within ESSENTIAL.

## Premise

A hand-authored dialogue system that makes each daemon feel like a
contextual conversation partner without an LLM. Inspired by Supergiant's
mechanics in Hades/Hades 2: a flat pool of voice lines, each gated by
predicates and ranked by priority. The system feels reactive because the
*filter* is paying attention to user state, not because the content is
dynamically generated.

User agency is achieved through **branching choices** layered on top of
the Hades pool model — once the engine picks an opener, the user steers
where the conversation goes by choosing among reply options.

**Constraints baked in:**
- No network, no API, no LLM.
- All content ships in the APK.
- Reuses the existing 10 voice presets as the *style anchor*.
- Persistent per-daemon: your history with Athleta accumulates.

## What Hades actually does (the parts we're copying)

(Unchanged from v1.) Flat predicate pool. Three priority tiers
(filler/contextual/essential). Two retirement axes (hard named
cooldowns + soft exhaust-before-repeat). Sequences as implicit chains
via `requires`/`forbids` on lineIds. Cheap rich state surface for
predicates.

What we're explicitly NOT copying: voice acting, 300k-word corpus,
one-direction monologue. Branching choices are our addition.

## Two consumers, one engine — the v2 reframe

The Demolisher's strongest point in round 1: "the daemon's voice should
ambush you in the completion toast, not wait behind a button." We honor
both this insight AND the user's explicit ask for conversations by
giving the predicate engine **two consumers** that share one corpus:

### Consumer A — inline surfaces (the "ambush track")

The engine is plugged into existing UI moments that already show daemon
voice:
- **Daily greeting line** on each daemon card. Currently picks from
  `VoicePreset.greeting(seed)`. v0.0.6: queries the engine with
  category=`OPENER` and the current `ConversationContext`. Falls back
  to the templated voice preset line if no eligible engine line.
- **Completion toast** when a minor quest is tapped. Currently shows
  `VoicePreset.completion(seed)`. v0.0.6: queries the engine with
  category=`COMPLETION` and the just-completed minor in context.
- **Apotheosis dialog** when a major closes. Currently shows
  `VoicePreset.apotheosis(seed)`. v0.0.6: queries with `APOTHEOSIS` +
  the closed major in context.

These give the user contextual daemon speech *in the foreground flow*
without requiring them to navigate anywhere. Most reactive lines
(referencing recent activity, lapses, streaks) land here.

### Consumer B — explicit Conversation screen (the user's ask)

Reached from a **Talk** button on the daemon card and on the daemon
detail header. Opens a `ConversationScreen` that uses the same engine
with category=`OPENER` and walks the branching choice graph. Multi-beat
story chunks live here because they need user-driven pacing.

Same corpus. A line marked `category = OPENER` may surface either as
the daily greeting OR as the conversation opener — both surfaces query
the same pool, the line plays in whichever surface picks it first.

## Architecture

### `DialogueLine`

```kotlin
data class DialogueLine(
    val id: String,                        // stable across versions
    val archetype: String,                 // VoicePreset key, or "ANY"
    val text: String,                      // templated; ${daemonName}, etc.
    val tier: LineTier,                    // FILLER | CONTEXTUAL | ESSENTIAL
    val category: LineCategory,            // OPENER | COMPLETION | APOTHEOSIS | RESPONSE
    val priority: Int = 0,                 // tiebreak within tier
    val recencyKey: RecencyKey? = null,    // for ESSENTIAL: how recent must the
                                           // triggering event be? lower = wins
    val requires: List<String> = emptyList(),
    val forbids: List<String> = emptyList(),
    val stateRequirements: List<Predicate> = emptyList(),
    val cooldownGroup: String? = null,
    val cooldownPicks: Int? = null,
    val tells: TellStyle = TellStyle.NONE, // archetype-flavored pause marker
    val choices: List<DialogueChoice> = emptyList(),
)

enum class LineTier { FILLER, CONTEXTUAL, ESSENTIAL }
enum class LineCategory { OPENER, COMPLETION, APOTHEOSIS, RESPONSE }
enum class TellStyle { NONE, ELLIPSIS, SNAP, FADE, PACE }
enum class RecencyKey { FIRST_EVER, TODAY, THIS_WEEK, THIS_MONTH, EVER }

data class DialogueChoice(
    val text: String,
    val nextLineId: String?,               // null = end conversation
    val gates: List<Predicate> = emptyList(),
)
```

No `ChoiceTone`, no `affinityDelta` — both cut with affinity.

### `ConversationContext`

```kotlin
data class ConversationContext(
    val daemonId: Long,
    val daemonName: String,
    val archetypeKey: String,
    val level: Int,
    val openMajors: List<MajorQuest>,
    val recentlyClosedMajor: MajorQuest?,
    val recentlyCompletedMinors: List<MinorQuest>,
    val minorsCompletedToday: Int,
    val minorsCompletedThisWeek: Int,
    val dailyMinorsLapsedCount: Int,
    val streakDays: Int,
    val totalWishesAvailable: Int,
    val recentlySpentBoonText: String?,
    val conversationsHad: Int,
    val daysSinceLastConversation: Int?,
    val daysSinceFirstConversation: Int?,
    val dayOfWeek: Int,
    val timeOfDay: TimeOfDay,
)
```

Note: no `affinity` field. Predicates that previously would have read
affinity now read `conversationsHad` and `daysSinceFirstConversation`
as play-history-only proxies for intimacy. These can't be gamed.

### Predicates

A small algebra of named predicates implementing
`fun isSatisfied(ctx: ConversationContext): Boolean`. Initial set:

- `FirstConversation`, `AfterApotheosis(withinHours: Int)`,
  `AfterLapse(min: Int)`, `WithWishesAvailable(min: Int)`,
  `AtLevelAtLeast(n: Int)`, `TimeOfDayIs(t)`,
  `MinorsCompletedTodayAtLeast(n)`,
  `StreakAtLeast(days: Int)`, `DaysSinceLastConversationAtLeast(n)`,
  `ConversationsHadAtLeast(n)`, `BoonSpentToday`.

Add as content demands. Never grow the algebra speculatively.

### Engine

```kotlin
class DialogueEngine(
    private val corpus: List<DialogueLine>,
    private val store: DialogueStateStore,
) {
    suspend fun pickFor(
        category: LineCategory,
        ctx: ConversationContext,
    ): DialogueLine? {
        // Single-batch fetch — Architect's note: materialize maps once.
        val played = store.playedLineIds(ctx.daemonId)
        val playCounts = store.playCountsByLineId(ctx.daemonId)
        val lastPlayedAt = store.lastPlayedAtByLineId(ctx.daemonId)
        val cooldowns = store.activeCooldownGroups(ctx.daemonId)

        val eligible = corpus.asSequence()
            .filter { it.category == category }
            .filter { it.archetype == ctx.archetypeKey || it.archetype == "ANY" }
            .filter { it.requires.all(played::contains) }
            .filter { it.forbids.none(played::contains) }
            .filter { it.stateRequirements.all { p -> p.isSatisfied(ctx) } }
            .filter { it.cooldownGroup == null || it.cooldownGroup !in cooldowns }
            .toList()

        val byTier = eligible.groupBy { it.tier }
        val tier = listOf(LineTier.ESSENTIAL, LineTier.CONTEXTUAL, LineTier.FILLER)
            .firstOrNull { byTier[it]?.isNotEmpty() == true }
            ?: return null

        return byTier[tier]!!.sortedWith(
            compareBy<DialogueLine>(
                { it.recencyKey?.ordinal ?: Int.MAX_VALUE },  // life-event recency wins
                { -it.priority },                              // higher priority next
                { playCounts[it.id] ?: 0 },                    // exhaust before repeat
                { lastPlayedAt[it.id] ?: 0L },                 // oldest first
            )
        ).first()
    }

    suspend fun markPlayed(daemonId: Long, line: DialogueLine) {
        store.markPlayed(daemonId, line.id, System.currentTimeMillis())
        line.cooldownGroup?.let { group ->
            store.startCooldown(daemonId, group, expiresAtPicks = line.cooldownPicks ?: 8)
        }
    }
}
```

The same-encounter lock for Consumer B (Conversation screen): the
screen refuses to open if `(now - lastConversationAt) <
SAME_ENCOUNTER_WINDOW` **AND** no real activity happened
(completed-minor or major or spent-wish) between the two opens.
Action-only, no wall clock — Believer + Architect agreed on this.

### Persistence

v2 → v3 migration is pure additive (Architect's note: no recreation,
no FK-add on existing rows).

```sql
CREATE TABLE line_seen (
  daemonId  INTEGER NOT NULL,
  lineId    TEXT    NOT NULL,
  lastPlayedAt INTEGER NOT NULL,
  playCount INTEGER NOT NULL,
  PRIMARY KEY (daemonId, lineId),
  FOREIGN KEY (daemonId) REFERENCES daemons(id) ON DELETE CASCADE
);

CREATE TABLE cooldown_play (
  daemonId       INTEGER NOT NULL,
  cooldownGroup  TEXT    NOT NULL,
  expiresAtPicks INTEGER NOT NULL,   -- Architect: expiry on the row, not derived
  PRIMARY KEY (daemonId, cooldownGroup),
  FOREIGN KEY (daemonId) REFERENCES daemons(id) ON DELETE CASCADE
);

CREATE TABLE daemon_state (
  daemonId            INTEGER PRIMARY KEY NOT NULL,
  lastConversationAt  INTEGER,
  conversationsHad    INTEGER NOT NULL DEFAULT 0,
  firstConversationAt INTEGER,
  -- Architect's note: NOT on daemons (which is @Serializable / exported).
  -- This table is intentionally excluded from PantheonBackup.
  FOREIGN KEY (daemonId) REFERENCES daemons(id) ON DELETE CASCADE
);
```

No `choice_log` table (Architect cut — write-only without UI). No
`affinity` column anywhere. `daemon_state` is excluded from the
PantheonBackup export so the backup format doesn't churn.

## Content corpus

### Shape (post-round-1 revision)

Per archetype:
- **8-10 OPENER lines** spanning state patterns (first-ever-conversation,
  post-apotheosis, after-lapse, morning, evening, with-wishes-pending,
  all-quests-clear, mid-streak)
- **6-8 COMPLETION lines** with state predicates (close-streak,
  weak-streak, first-of-day, late-evening)
- **4-6 APOTHEOSIS lines** spanning level milestones + recent-pattern
- **3-5 multi-beat CHAIN sequences** (each chain ~3 beats; chains live
  exclusively in Consumer B)
- **15-20 RESPONSE lines** as targets of branching choices
- **5-8 ESSENTIAL one-shots** keyed by `RecencyKey.FIRST_EVER` /
  `TODAY` (first conversation ever, first apotheosis, first lapse,
  first wish spent, level-5 ever, etc.)

**Per archetype: ~45-55 lines × 10 archetypes ≈ 500 lines.**

### Topics every archetype must cover

Same eight axes as v1:
1. Greeting (state-aware)
2. Progress check-in ("How am I doing?")
3. Negotiation ("I want to skip", "Push me harder")
4. Reactive comment on recently completed minor or closed major
5. Reactive comment on recently spent boon
6. Vulnerability chain (now gated by `ConversationsHadAtLeast(N)` +
   `DaysSinceFirstConversationAtLeast(D)`, NOT by affinity — chain
   requires the user to have returned, not to have "won" warmth)
7. Philosophical stance ("Why does this matter?")
8. Same-encounter "I have nothing new" beat

### Authoring format

Adopting Ally's polish #1 — a thin Kotlin DSL builder:

```kotlin
object DrillSergeantLines : ArchetypeLines("DRILL_SERGEANT") {
    init {
        opener("ds_morning") {
            tier = CONTEXTUAL
            requires(TimeOfDayIs(MORNING))
            cooldownGroup("greet", picks = 4)
            says("Morning. List's short. Get moving.")
        }
        opener("ds_after_lapse") {
            tier = ESSENTIAL
            recencyKey = TODAY
            requires(AfterLapse(min = 1))
            says("You skipped yesterday. Two ways to fix that. Pick one.")
            choices {
                "I'll do one now"   -> "ds_lapse_engage"
                "I need a breather" -> "ds_lapse_breather"
                "Leave me alone"    -> null  // end
            }
        }
        chain("ds_why_matters", beats = 3) {
            beat(1) { says("You want to know why I bark. Sit down.") }
            beat(2) { says("Everyone has a body. Most people let it go to seed.") }
            beat(3) { says("I'm the part of you that won't.") }
        }
    }
}
```

DSL compiles to `List<DialogueLine>`. ~30% fewer keystrokes than raw
`data class` literals; remains compile-checked.

### Tells (Ally polish #5)

Per archetype, a small set of in-voice "tells" — micro-stage-directions
that interleave or precede lines. E.g.:
- Oracle: `*long pause*`, `*sees past you*`
- Drill Sergeant: `*snaps*`, `*claps once*`
- Gentle Mentor: `*soft smile*`, `*sets cup down*`
- Hermit: `*does not look up*`

A line declares `tells = ELLIPSIS` (or whichever) and the renderer
prepends/appends the chosen tell from that archetype's tell library.
20 extra tokens of authoring per archetype, big perceived-variety
multiplier.

## UI

- **Talk** button on each daemon card in Daily and on the detail
  header.
- **`ConversationScreen`**:
  - Top: daemon name + level + back arrow.
  - Center: current line in large type, archetype-flavored thinking
    pause (Ally polish #3: 600-900ms before the daemon's reply,
    with archetype-flavored indicator — Oracle fades in, Drill
    Sergeant snaps in instantly with `*snap*`, Hermit appears as
    if always there).
  - Bottom: up to 4 choice cards; if no choices, "Leave" only.
  - Persistent back-arrow exits cleanly.

- **Inline surfaces** (Consumer A) require no new UI; they're behind
  the existing greeting/completion/apotheosis displays.

## What's cut from v1 (and why)

- **Affinity counter.** Skeptic + Demolisher: gating intimacy on a
  counter turns vulnerability into a vending machine. Architect's
  "pattern of choices" argument is valid but marginal — replaced by
  play-history proxies (`conversationsHad`, `daysSinceFirstConversation`).
  May add in v0.0.7 if patterns emerge.
- **`ChoiceTone` enum + `affinityDelta`.** Dead with affinity.
- **`choice_log` table.** Architect: write-only without UI. Defer.
- **Same-encounter wall-clock window.** Believer + Architect: drop the
  4-hour clock, action-gated only.
- **Conditional templating.** Architect: predicates are the conditional
  language; don't duplicate at the wrong layer. Plain `${}`
  substitution, ~5 tokens.

## Polish adopted from Ally

1. Kotlin DSL builder for authoring (above).
2. Archetype-flavored thinking pauses (above).
3. Per-archetype "tells" library (above).
- Gradle linter task and "last 3 exchanges" strip deferred to v0.0.7.
- `@onSeen` side-effect hooks deferred (state mutation from content is
  scope creep for v0.0.6).

## Open questions for council round 2

1. **The Demolisher's "ambush in toast vs. dedicated screen" critique.**
   Resolved by shipping both consumers — but is the dedicated screen
   genuinely earning its weight, or should we ship Consumer A only and
   see if anyone misses the screen?
2. **Corpus shape.** Skeptic argued one-archetype vertical-slice;
   Believer argued 800; the current revision goes for 500 (50 × 10).
   Is this calibrated to what one human can author without prose
   flattening? Should we go vertical-slice for v0.0.6 (one archetype
   deep, others get a 10-line stub) and round out in v0.0.7?
3. **Reactive-as-surveillance failure mode** (Skeptic).
   `dailyMinorsLapsedCount` plus Drill Sergeant = shame amplifier.
   What guardrails sit between predicate availability and content?
   Should ESSENTIAL-tier reactive lines on lapses be archetype-filtered
   (Drill Sergeant can have them, Gentle Mentor must not)?
4. **Maintenance cliff** (Skeptic). No deprecation strategy. Should
   `DialogueLine` carry a `deprecated: Boolean` flag, with the engine
   skipping deprecated lines but preserving `requires` chains? Cheap
   to add now; impossible to retrofit safely.
5. **DSL ergonomics.** The proposed DSL is compile-checked Kotlin —
   any concerns about IDE friction or future content-editor tooling?
   Should we plan for JSON-from-assets as an eventual escape hatch?
6. **"Tells" library scope.** Are 4-5 tells per archetype enough, or
   does this need a richer system (multiple tells per slot, weighted
   sampling)?

## Council iteration log

### Round 1 — synthesis

Believer wanted to protect affinity + confession-gating, push to ~800
lines, add conditional templating, and drop the wall-clock part of the
same-encounter lock.

Ally proposed a thin Kotlin DSL, a Gradle linter, archetype-flavored
thinking pauses, side-effect hooks, per-archetype tells, and a
last-3-exchanges strip.

Architect cut `choice_log`, recommended `daemon_state` instead of
extending `daemons`, pinned `expiresAtPicks` on `cooldown_play`,
endorsed plain `${}` substitution, kept affinity + tier system +
requires/forbids as the spine.

Skeptic argued 500 lines straddle two bad numbers (30 = author ceiling
without bleed, 200 = floor to feel alive), called affinity-gated
intimacy a vending machine, flagged reactive-on-lapses as
surveillance/shame-amplifier, and proposed a vertical-slice
methodology.

Demolisher delivered the strongest single critique: dialogue engine
in search of a product; the daemon's voice should ambush you in the
completion toast, not wait behind a button. Branching with
WARM/CHALLENGING tones = JRPG dating sim. Affinity = XP in costume.
Epitaph: "Beautifully written, opened twice, replaced by a Notion
checklist."

**Resolutions encoded into v2:**
- Two consumers, one engine — both ambush (inline) and screen
- Affinity + ChoiceTone + choice_log all cut
- Wall-clock same-encounter window cut
- Plain `${}` templating (no conditionals)
- Life-event-recency ordering within ESSENTIAL tier
- Kotlin DSL, thinking pauses, per-archetype tells adopted
- daemon_state table separate from daemons (export-clean)
- Corpus target 500 lines, calibrated against Skeptic's caution

Rounds 2–5+ will append below.
