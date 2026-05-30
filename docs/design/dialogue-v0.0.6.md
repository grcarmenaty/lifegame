# Daemon Conversations — Design Plan

> **Version log**
> - v0 — pre-research, pre-council. Tree-based, naive.
> - v1 — post-Hades research, pre-council. Flat predicate pool.
> - v2 — post council round 1. Two consumers, affinity cut, cuts to
>   `choice_log`, ChoiceTone, conditional templating, wall-clock lock.
> - v3 — post council round 2. preferredSurface, earned predicates,
>   archetypeWhitelist, DSL out, tells to renderer, deprecation scaffolding.
> - v4 — post council round 3. preferredSurface leak fixes, surface-
>   scoped cooldowns, replacedBy load-time rewrite, predicate
>   singletons, 24h fallback bug fix, LAPSE_REACTIVE cooldown, no
>   first-launch explainer, mid-day-return pattern.
> - v5 — post council round 4. replacedBy runtime cut, PantheonBackup v2
>   carrying dialogue state, rigorous v0.0.7 trigger, threading model,
>   6-PR order.
> - **v6 (current) — post council round 5. SIGNED OFF.** Three "ship
>   it" verdicts (Believer, Architect, Demolisher); two precise
>   additions accepted: Skeptic's immutable-id lint rule (j),
>   Ally's "trigger cannot fire against v0.0.6" guarantee + PR4
>   placeholder Talk affordance, Architect's `onResume` re-pick +
>   source-level lint rule (i) implementation, Believer's PR4
>   callout for the 180-min foreground guard. **Council process
>   complete.** Implementation begins.

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
    val preferredSurface: PreferredSurface = PreferredSurface.EITHER,  // round 2 fix
    val priority: Int = 0,                 // tiebreak within tier
    val lifeEvent: Boolean = false,        // ESSENTIAL first-ever beats today's
    val recencyKey: RecencyKey = RecencyKey.EVER,  // secondary recency sort
    val requires: List<String> = emptyList(),
    val forbids: List<String> = emptyList(),
    val stateRequirements: List<Predicate> = emptyList(),
    val cooldownGroup: String? = null,
    val cooldownPicks: Int? = null,
    val crossSurfaceCooldown: Boolean = false,  // round 3: shared across surfaces
    val tellHint: TellStyle = TellStyle.NONE,   // hint to renderer; renderer picks the tell
    val choices: List<DialogueChoice> = emptyList(),

    // Deprecation scaffolding (round 2, Ally + Skeptic converge)
    val deprecated: Boolean = false,       // engine skips for new picks
    val replacedBy: String? = null,        // chain rewrites preserve continuity
)

enum class LineTier { FILLER, CONTEXTUAL, ESSENTIAL }
enum class LineCategory { OPENER, COMPLETION, APOTHEOSIS, RESPONSE }
enum class PreferredSurface { EITHER, INLINE, SCREEN }
enum class TellStyle { NONE, ELLIPSIS, SNAP, FADE, PACE }
enum class RecencyKey { TODAY, THIS_WEEK, THIS_MONTH, EVER }

data class DialogueChoice(
    val text: String,
    val nextLineId: String?,               // null = end conversation
    val gates: List<Predicate> = emptyList(),
)
```

No `ChoiceTone`, no `affinityDelta` — both cut with affinity in v2.

**Why `preferredSurface`** (round 2, Architect + Skeptic converge):
the two-consumer architecture creates a race — if the daily greeting
plays `ds_after_lapse` (ESSENTIAL) at 8am, the Conversation screen at
8:01 sees it as played and opens with a filler. `preferredSurface =
SCREEN` reserves life-event one-shots and multi-beat chains for the
screen; inline queries filter them out. Same line ID, no fragmentation.

**Round 3 (Architect): leak fix.** `EITHER`-flagged ESSENTIAL lines
can still burn inline before the screen sees them. Fix:
**`ESSENTIAL` + `lifeEvent = true` lines default to
`preferredSurface = SCREEN`** unless explicitly overridden by the
author. Enforced by the lint test, not by the data class default
(which stays `EITHER` for clarity). Authors who want a life-event line
in the inline track must explicitly write `preferredSurface = INLINE`
or `EITHER` — making the intent visible at the call site.

**Why `lifeEvent` + `recencyKey` split** (round 2, Architect): the v2
ordinal-as-priority was brittle — "first apotheosis ever" should always
beat "first lapse today" regardless of recency. Now sorted by
`lifeEvent desc`, then `recencyKey asc`, then `priority desc`.

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
  `MinorsCompletedTodayIsZero`,            // for mid-day-return ambush
  `StreakAtLeast(days: Int)`, `DaysSinceLastConversationAtLeast(n)`,
  `ConversationsHadAtLeast(n)`,
  `DaysSinceFirstConversationAtLeast(n)`,
  `BoonSpentToday`.

**Round 3 (Architect): predicate instances are object singletons** —
`object AfterLapse1 : Predicate` not `class AfterLapse(1)` allocated
per line. Saves thousands of redundant objects across the corpus. For
parameterized predicates, intern small-arity instances in a registry:
```kotlin
internal val AfterLapse_1 = AfterLapse(1)
internal val AfterLapse_2 = AfterLapse(2)
internal val AfterLapse_3 = AfterLapse(3)
```
Authors reference the interned values; the engine never sees a fresh
predicate allocation.

**Earned predicates** (round 2, Believer): the vulnerability chain and
similar "intimacy is earned" content gates on *costly action*, not
attendance:
- `MajorsClosedAtLeast(n: Int)` — only counts truly closed (not just
  attempted).
- `WishesSpentAtLeast(n: Int)`.

These are not gameable because the cost is real (the user has to
actually do quests, actually spend wishes). They replace what affinity
would have done in v0 / v1.

**Round 3 (Architect): denormalized counters**. These predicates read
`ctx.majorsClosedTotal: Int` and `ctx.wishesSpentTotal: Int`, both
stored on `daemon_state` and incremented on the actual completion /
spend event in the repository. Avoids N aggregate queries per Daily
recomposition.

**Reactive-predicate archetype gates** (round 2, Skeptic): some
predicates carry a shame-amplifier risk. They declare which archetypes
may match them:

```kotlin
class AfterLapse(val min: Int) : Predicate {
    override val archetypeWhitelist = setOf(
        "DRILL_SERGEANT", "COACH", "TRICKSTER", "STOIC"
    )
    override fun isSatisfied(ctx) = ctx.dailyMinorsLapsedCount >= min
}
```

Engine checks `predicate.archetypeWhitelist?.contains(ctx.archetypeKey) ?: true`
during eligibility. Drill Sergeant can react to lapses; Gentle Mentor,
Therapist, Hermit, Poet cannot — at the engine layer, by construction.
Author cannot accidentally hand a shame line to the wrong daemon.

**Round 3 (Skeptic): compound-shame fix.** `archetypeWhitelist` alone
prevents the wrong archetype from reacting, but doesn't stop Drill
Sergeant from firing four lapse-reactive lines in one session via
different `cooldownGroup`s. Add a synthetic, cross-surface cooldown:
```kotlin
const val LAPSE_REACTIVE_COOLDOWN = "lapse_reactive_24h"
```
Every line whose predicates include `AfterLapse(_)` must declare
`cooldownGroup = LAPSE_REACTIVE_COOLDOWN, cooldownPicks = 1,
crossSurface = true`. The lint test enforces membership. Even Drill
Sergeant cannot compound shame across one day or across surfaces.

**Round 3 (Architect): predicate `archetypeWhitelist` shape.**
`Set<String>?` on the predicate impl is acceptable for v0.0.6 even
though it's "data masquerading as code." When a third reactive
predicate appears (v0.0.7+), promote to a `ReactivePolicy` registry
that maps predicate type → allowed archetype keys, loaded
declaratively. Until then, the inline `Set<String>?` default-null is
the cheapest correct shape.

Add as content demands. Never grow the algebra speculatively.

### Engine

```kotlin
enum class Surface { INLINE, SCREEN }

class DialogueEngine(
    private val corpus: List<DialogueLine>,
    private val store: DialogueStateStore,
) {
    suspend fun pickFor(
        category: LineCategory,
        surface: Surface,                  // round 2: who's asking?
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
            .filter { !it.deprecated }                                   // round 2
            .filter { surfaceMatches(it.preferredSurface, surface) }     // round 2
            .filter { it.requires.all(played::contains) }
            .filter { it.forbids.none(played::contains) }
            .filter { it.stateRequirements.all { p -> archetypeMatches(p, ctx) && p.isSatisfied(ctx) } }
            .filter { it.cooldownGroup == null || it.cooldownGroup !in cooldowns }
            .toList()

        val byTier = eligible.groupBy { it.tier }
        val tier = listOf(LineTier.ESSENTIAL, LineTier.CONTEXTUAL, LineTier.FILLER)
            .firstOrNull { byTier[it]?.isNotEmpty() == true }
            ?: return null

        return byTier[tier]!!.sortedWith(
            compareByDescending<DialogueLine> { it.lifeEvent }              // life-events first
                .thenBy { it.recencyKey.ordinal }                            // TODAY before EVER
                .thenByDescending { it.priority }
                .thenBy { playCounts[it.id] ?: 0 }                           // exhaust before repeat
                .thenBy { lastPlayedAt[it.id] ?: 0L }
        ).first()
    }

    private fun surfaceMatches(pref: PreferredSurface, asking: Surface) = when (pref) {
        PreferredSurface.EITHER -> true
        PreferredSurface.INLINE -> asking == Surface.INLINE
        PreferredSurface.SCREEN -> asking == Surface.SCREEN
    }

    private fun archetypeMatches(p: Predicate, ctx: ConversationContext): Boolean =
        p.archetypeWhitelist?.contains(ctx.archetypeKey) ?: true

    suspend fun markPlayed(daemonId: Long, line: DialogueLine) {
        store.markPlayed(daemonId, line.id, System.currentTimeMillis())
        line.cooldownGroup?.let { group ->
            store.startCooldown(daemonId, group, expiresAtPicks = line.cooldownPicks ?: 8)
        }
    }
}
```

**`ConversationsHad` increments on first user choice taken, not on
screen open** (round 2, Architect): tap-and-bounce no longer accrues
intimacy credit. The flag is set in `DialogueEngine.continueWith`,
not in `pickFor`.

The same-encounter lock for Consumer B (Conversation screen): the
screen refuses a fresh open if no real activity (completed-minor,
closed-major, spent-wish, summoned-daemon) has happened since
`lastConversationAt`. Architect (round 2) flagged that pure
action-gating could leave a daemon mute forever — so there's a
release condition:

```
canOpenConversation =
    activitySinceLastConversation
    OR conversationsHad == 0           // round 3 (Skeptic): first-ever always passes
```

The round-3 Skeptic killed the unconditional 24h fallback because it
rewarded absence — a user who completes zero quests for 23 hours then
opens the screen would have gotten filler. The first-ever exception
covers the only edge case where a daemon would otherwise be silent at
the start (no activity history).

Refused state shows a small archetype-styled banner
("`*pacing*` I've said my piece. Go.") with a single "Leave" action.

### Persistence

v2 → v3 migration is pure additive (Architect's note: no recreation,
no FK-add on existing rows).

```sql
CREATE TABLE line_seen (
  daemonId     INTEGER NOT NULL,
  lineId       TEXT    NOT NULL,
  lastPlayedAt INTEGER NOT NULL,
  playCount    INTEGER NOT NULL,
  PRIMARY KEY (daemonId, lineId),
  FOREIGN KEY (daemonId) REFERENCES daemons(id) ON DELETE CASCADE
);
CREATE INDEX idx_line_seen_daemon ON line_seen(daemonId);

CREATE TABLE cooldown_play (
  daemonId       INTEGER NOT NULL,
  cooldownGroup  TEXT    NOT NULL,
  surface        TEXT    NOT NULL,    -- round 3 (Architect): scoped per surface
  expiresAtPicks INTEGER NOT NULL,
  PRIMARY KEY (daemonId, cooldownGroup, surface),
  FOREIGN KEY (daemonId) REFERENCES daemons(id) ON DELETE CASCADE
);
CREATE INDEX idx_cooldown_play_daemon ON cooldown_play(daemonId);

CREATE TABLE daemon_state (
  daemonId            INTEGER PRIMARY KEY NOT NULL,
  lastConversationAt  INTEGER,
  firstConversationAt INTEGER,
  conversationsHad    INTEGER NOT NULL DEFAULT 0,
  majorsClosedTotal   INTEGER NOT NULL DEFAULT 0,   -- round 3 (Architect): denormalized
  wishesSpentTotal    INTEGER NOT NULL DEFAULT 0,   -- round 3 (Architect): denormalized
  -- Excluded from PantheonBackup so backup format doesn't churn.
  FOREIGN KEY (daemonId) REFERENCES daemons(id) ON DELETE CASCADE
);
```

**Surface in `cooldown_play.PRIMARY KEY`** (round 3, Architect): an
`INLINE` pick of a cooldown group does NOT block a `SCREEN` line in
the same group unless the line is `crossSurfaceCooldown = true`
(in which case the engine writes one row with `surface = 'BOTH'`,
consulted via `WHERE surface = ? OR surface = 'BOTH'` — single index
hit, no NULL semantics, confirmed cleanest in round 4 Architect).

**Synthetic `LAPSE_REACTIVE` cooldown** (round 3, Skeptic): when a
line carrying `AfterLapse(_)` plays in either surface, the engine
inserts `(daemonId, 'lapse_reactive_24h', 'BOTH', expiresAtPicks=N)`
where `N` is sized to outlast a day. Lint enforces that any line with
`AfterLapse` in its predicates also carries `cooldownGroup =
LAPSE_REACTIVE_COOLDOWN, crossSurfaceCooldown = true`.

**Import / Reset behavior**:
- **Reset**: all three tables wipe via FK CASCADE from `daemons`. The
  same-encounter lock's `OR conversationsHad == 0` (round 3 Skeptic
  fix) is *correct because reset is reset* — a fresh-summoned daemon
  with the same name passes the lock unconditionally on first open.
- **Import** (round 4, Skeptic uncovered the voice-continuity bug):
  v4's "clear three tables on import" left a load-bearing problem —
  restoring `Athleta.boons + quests` while wiping her `conversationsHad`
  meant she greeted the user as a stranger. v5 fixes by **bumping
  `PantheonBackup` to format v2**, carrying `line_seen` +
  `cooldown_play` + `daemon_state` rows. The dialogue state is part of
  the daemon, the way play-log is part of the relationship (Believer
  round 1: "the play log IS the relationship"). Old v1 backups
  restore daemons without dialogue history — acceptable degradation.

**Schema migration v2 → v3** (Architect, confirmed round 3):
- Three `CREATE TABLE` + indices.
- `idx_line_seen_daemon` and `idx_cooldown_play_daemon` explicit even
  though PK covers them (Room generates explicit lookups).
- `daemon_state` `exportSchema = true` emits `app/schemas/3.json` —
  schema is in the dump, **rows are now in the backup** (v2 format).

No `choice_log` table (round 1 cut, write-only without UI). No
`affinity` column anywhere.

## Content corpus

### Shape (post-round-2 revision)

All 10 archetypes ship full coverage on the foreground surfaces:
- **6-8 OPENER lines** spanning state patterns (first-ever-conversation,
  post-apotheosis, after-lapse, morning, evening, with-wishes-pending,
  all-quests-clear, mid-streak, **mid-day-return** per Ally round 2
  with **`MinutesSinceLastForegroundAtLeast(180)` guard** added in
  round 4 by Believer — below the 3-hour floor, "noticing" reads as
  surveillance for a power user reopening five times a day)
- **5-7 COMPLETION lines** with state predicates (close-streak,
  weak-streak, first-of-day, late-evening)
- **4-5 APOTHEOSIS lines** spanning level milestones + recent-pattern
- **10-15 RESPONSE lines** as targets of branching choices
- **5-8 ESSENTIAL one-shots** with `lifeEvent = true` (first
  conversation ever, first apotheosis, first lapse, first wish spent,
  level-5 ever)

**3 archetypes go deep with multi-beat CHAINS** in v0.0.6 (round 2
compromise on Skeptic's vertical-slice argument):
- **Drill Sergeant** (harsh), **Gentle Mentor** (warm), **Oracle**
  (mystic) — covering three relational stances.
- Each: 3 chains × ~3 beats = ~9 chain lines.
- Other 7 archetypes get chains in v0.0.7 if engagement data warrants.

**Corpus math:**
- Foreground coverage: ~35-40 lines × 10 = 350-400
- Chain content: ~27 lines × 3 archetypes = ~80
- Total: ~430-480 lines

Honors the user's "very comprehensive" ask within a budget one author
can hold in their head.

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

**DSL dropped from v0.0.6** (round 2: Architect flagged IDE/compile
risk with mega `init {}` blocks; Demolisher called it designer's joy
when there is no second author). Use raw `listOf(DialogueLine(...))`
per archetype, one file per archetype under
`domain/dialogue/lines/<ArchetypeName>Lines.kt`. Compile-checked,
IDE-friendly, refactor-clean. Revisit DSL when a second author shows
up.

```kotlin
internal object DrillSergeantLines {
    val all: List<DialogueLine> = listOf(
        DialogueLine(
            id = "ds_morning",
            archetype = "DRILL_SERGEANT",
            text = "Morning. List's short. Get moving.",
            tier = LineTier.CONTEXTUAL,
            category = LineCategory.OPENER,
            stateRequirements = listOf(TimeOfDayIs(TimeOfDay.MORNING)),
            cooldownGroup = "ds_greet",
            cooldownPicks = 4,
        ),
        DialogueLine(
            id = "ds_after_lapse",
            archetype = "DRILL_SERGEANT",
            text = "You skipped yesterday. Two ways to fix that. Pick one.",
            tier = LineTier.ESSENTIAL,
            category = LineCategory.OPENER,
            preferredSurface = PreferredSurface.SCREEN,    // multi-choice — needs the screen
            recencyKey = RecencyKey.TODAY,
            stateRequirements = listOf(AfterLapse(min = 1)),
            choices = listOf(
                DialogueChoice("I'll do one now", "ds_lapse_engage"),
                DialogueChoice("I need a breather", "ds_lapse_breather"),
                DialogueChoice("Leave me alone", null),    // end conversation
            ),
        ),
        // ... chain lines linked by `requires` / `forbids`
    )
}
```

Top-level `DialogueCorpus.all` flattens all archetype lists at
startup. One file per archetype keeps incremental compile cheap.

### Tells (renderer concern, not engine)

Per archetype, a small set of in-voice "tells" — micro-stage-directions
that interleave or precede lines. E.g.:
- Oracle: `*long pause*`, `*sees past you*`
- Drill Sergeant: `*snaps*`, `*claps once*`
- Gentle Mentor: `*soft smile*`, `*sets cup down*`
- Hermit: `*does not look up*`

The `DialogueLine.tellHint` is a *hint* the renderer interprets.
**Architect (round 2): the tell library lives next to the UI**, not
inside the engine corpus, and the renderer holds per-archetype
**tell cooldown state** (don't repeat the same tell back-to-back per
Ally polish #3). This keeps `DialogueEngine` corpus-pure and lets the
tells evolve without touching engine logic.

## UI

- **Talk** button on each daemon card in Daily and on the detail
  header.
- **`ConversationScreen`**:
  - Top: daemon name + level + back arrow.
  - Center: current line in large type. Tells are baked into the
    `text` at render time via `tellHint`; no separate animation
    system (Demolisher round 2: animations are designer's joy here).
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

## Polish status (post round 3)

- ~~Kotlin DSL builder~~ — dropped (Architect + Demolisher).
- ~~Animated thinking pauses~~ — dropped (Demolisher).
- ~~First-launch explainer~~ — dropped (round 3 — Believer + Skeptic
  + Demolisher converge: "daemons don't explain themselves").
- **Tells library** in renderer (Architect). Per-archetype
  tell-cooldown lives in **DataStore Preferences** (round 3, Ally:
  survives process death, no new Room table — this isn't
  relationship state, just anti-repetition).
- **Fallback render seed** (round 4, Ally): when `pickFor` returns
  null and the renderer falls back to `VoicePreset.greeting(seed)`,
  seed = `daemonId XOR epochDay` so the fallback line is stable
  across recompositions within a day. Without this, the daily card
  flickers between fallback variants on every state change.
- **`recencyKey.ordinal` lock** (round 4, Ally): a unit test asserts
  `RecencyKey.TODAY.ordinal < RecencyKey.EVER.ordinal` etc. Catches
  silent sort inversion if anyone reorders the enum.
- **`DialogueLintTest`** — Ally's round-3 spec adopted:
  - (a) id uniqueness across all archetype files
  - (b) no dangling `requires` / `forbids` / `nextLineId` /
    `replacedBy` references
  - (c) every RESPONSE line reachable from some OPENER via choice
    graph (BFS)
  - (d) every archetype covers all 8 mandated topics
  - (e) `archetypeWhitelist`-bearing predicates not attached to
    lines whose archetype is outside the whitelist
  - (f) chain integrity through deprecation — if A `requires` B and
    B is `deprecated`, B must declare `replacedBy`
  - (g) no `preferredSurface = INLINE` line carrying `choices`
    (inline can't render branching)
  - (h) every line with `AfterLapse(_)` in predicates carries
    `cooldownGroup = LAPSE_REACTIVE_COOLDOWN` and
    `crossSurfaceCooldown = true`
  - (i) every ESSENTIAL + lifeEvent line declares an explicit
    `preferredSurface` (default `EITHER` would be a bug here).
    **Round 5 (Architect): implemented as source-level check** (KSP
    or regex over line files), not runtime reflection — Kotlin can't
    distinguish "author wrote `EITHER`" from "author omitted the
    field." Lint runs against source, not against constructed
    `DialogueLine` instances.
  - (j) **Round 5 (Skeptic): `DialogueLine.id` is immutable across
    releases.** Lint compares ids against `app/dialogue-ids/<prev>.txt`
    and rejects any disappearance of a once-live id that wasn't
    formally deprecated.
- **`deprecated: Boolean` + `replacedBy: String?`** — kept as data;
  **runtime rewrite cut in v5** (round 4: Architect + Demolisher
  converge). Architect's argument was decisive: the lint already
  forbids `replacedBy` pointing at a deprecated line, so transitive
  resolution is unreachable by construction. Engine-side "follow N
  hops" code is dead code that pretends a scenario exists. Lint-only
  is provably sufficient given the deprecation playbook below.

**Deprecation playbook** (round 4, Architect — make this explicit):
1. Author marks line `B` `deprecated = true, replacedBy = "C"`.
2. Lint must reject the PR if any **live** (non-deprecated) line still
   has `replacedBy = "B"`. Author resolves by either updating the
   in-flight `replacedBy` references to `C` or undeprecating `B`.
3. Lint must reject any line whose `requires` references a deprecated
   id (force authors to rewrite chains, not stack indirection).
4. **Round 5 (Skeptic): `DialogueLine.id` is immutable across
   releases.** Renames happen via the deprecate-and-replace pipeline,
   never via in-place edit. Lint must compare ids against the
   previous-release snapshot (committed as `app/dialogue-ids/<v>.txt`)
   and reject any disappearance of a once-live id that wasn't
   deprecated. Without this, `line_seen` rows from old backups become
   orphans pointing at a vanished id — the engine treats them as
   "never played" and supposedly-retired lines replay.
5. With these three rules, transitive `replacedBy` is unreachable AND
   backup voice continuity holds across version upgrades.
6. The engine never reads `replacedBy` — it's pure lint metadata.
- **`LineProvenance` debug overlay** in dev builds (round 3, Ally) —
  long-press a daemon line to see the resolved id and any
  `replacedBy` hops. Costs nothing, makes the deprecation pipeline
  auditable from day one.
- "last 3 exchanges" strip — deferred to v0.0.7.
- `@onSeen` side-effect hooks — deferred.

## v0.0.7 screen-cutdown trigger (encoded Demolisher concession)

The Conversation screen ships in v0.0.6 because the user explicitly
asked for conversations. The Demolisher's case for cutting it is
structurally sound and goes uncontested at the product level. Instead
of pre-cutting, we commit to a **rigorously defined**, **instrumented**,
**pre-staged** trigger.

### Trigger condition (round 4: Skeptic + Demolisher converge)

> If, over a 14-day window starting after a 7-day settle-in,
> the **median** Conversation-screen opens per **active daemon** per
> week is < 2, then v0.0.7 cuts: the screen, the `RESPONSE` category,
> the multi-beat chain content, and the `crossSurfaceCooldown` flag
> on `DialogueLine`.

- **Median, not mean** (Demolisher + Skeptic): a mean hides a dead
  screen behind one obsessive user.
- **"Active daemon" definition**: a daemon with ≥1 minor quest
  completion in the trailing 7 days AND created ≥7 days ago. Tied
  to the quest loop, not the Talk button's own opens — the screen
  cannot drag its own denominator up.
- **7-day settle-in + 14-day measurement**: covers the novelty
  ramp + the routinized usage that actually matters.

### Instrumentation (round 4 Believer's round-5 hill)

If v0.0.6 ships without the meter, the trigger is theater. **v0.0.6
must ship**:
- `daemon_state.screenOpenCount: Int` (denormalized counter)
- `daemon_state.lastScreenOpenAt: Long?`
- Daily window aggregation visible in the dev-build `LineProvenance`
  overlay (long-press a daemon card → see screen-opens / active-week
  / current median).
- A read-only `SettingsScreen → "Conversation metrics"` row (dev build
  only) so the author can see drift without attaching a debugger.

### Pre-commit (round 4 Demolisher's honest-trigger move)

A draft file `docs/design/v0.0.7-cutdown.draft.md` ships alongside
v0.0.6 and enumerates **exactly** what gets deleted if the trigger
fires:
- `ui/conversation/` package
- `RESPONSE` enum value + every line carrying it
- `DialogueLine.choices` field
- `DialogueLine.crossSurfaceCooldown` field
- `cooldown_play.surface` column (migration v3 → v4 recreate)
- The 80-ish chain lines in `DrillSergeantLines` / `GentleMentorLines` /
  `OracleLines` files
- The same-encounter lock infrastructure
- The `LAPSE_REACTIVE_COOLDOWN` constant + lint rule

When v0.0.7 work starts and the metric has fired, the draft becomes
an actual PR — no debate, no sunk-cost gravity, just executing a
decision we already made under v0.0.6's clearer eyes.

## Implementation order (round 4: Ally + Architect converge)

Six PRs, each shippable / revertable independently:

| # | Scope | ~Effort | Ships as |
|---|---|---|---|
| 1 | **Lint + skeleton**: `DialogueLine`, `Predicate`, `ConversationContext` data classes; empty per-archetype files; `DialogueLintTest` running green against zero lines. | 1d | infra-only PR |
| 2 | **Schema + DAOs + migration**: three new tables, v2→v3 migration, `MigrationTestHelper` round-trip, `PantheonBackup` v2 format incl. dialogue state. | 2d | infra-only PR |
| 3 | **Engine + Drill Sergeant full content + Daily-greeting integration**: `DialogueEngine.pickFor`, state batched in one DAO call, predicate object singletons, fallback seed `daemonId + epochDay`. One archetype proves the loop end-to-end. | 3d | v0.0.6-alpha internal |
| 4 | **Remaining 9 archetypes' foreground content** (parallelizable authoring) + completion-toast + apotheosis-dialog integration. Lint covers every line. | 5-7d | **v0.0.6 PUBLIC RELEASE** |
| 5 | **`ConversationScreen` + RESPONSE category + chains for Drill Sergeant / Gentle Mentor / Oracle** + same-encounter lock + `LAPSE_REACTIVE` cross-surface. | 4d | **v0.0.6.1 PUBLIC RELEASE** |
| 6 | **`LineProvenance` dev overlay + tell-cooldown DataStore + screen-open metrics view** + `v0.0.7-cutdown.draft.md` committed. | 1d | shipped alongside v0.0.6.1 |

**Phased release** (round 4: Skeptic + Ally + Architect all converge):
- **v0.0.6** = engine + ambush track + all 10 archetypes' foreground
  (~350 lines authored). The thing the Demolisher actually likes.
- **v0.0.6.1** = `ConversationScreen` + RESPONSE + chains for 3 deep
  archetypes (~80 more lines). The thing the user asked for.
- Cleanly separates the trigger metric: v0.0.6 baseline = pure
  inline; v0.0.6.1 adds the screen, screen_open_count starts
  ticking.

**Round 5 (Ally) — explicit guarantee**: **the v0.0.7 cutdown trigger
cannot fire against v0.0.6.** The trigger clock starts at v0.0.6.1
ship. v0.0.6 contains no Conversation screen and therefore generates
no screen-open data; comparing pre-screen telemetry against the
trigger threshold would be incoherent. The 7-day settle-in + 14-day
measurement window both begin at v0.0.6.1 release.

**Round 5 (Ally) — placeholder Talk affordance in PR4**: v0.0.6
ships the Talk button (on Daily card and detail header) as a visible
affordance routing to a single archetype-styled card:
`*the daemon is gathering thought. return soon.*` — no chat, no
choices, no `markPlayed`. Sets the user's expectation that
conversations exist *somewhere* without leaking a half-feature.
Implementation: a `PlaceholderConversationScreen.kt` that v0.0.6.1
replaces with `ConversationScreen.kt`. The button doesn't move; the
behind-the-button screen swaps.

**Round 5 (Architect) — additional implementation notes**:
- **`ConversationScreen.onResume` re-pick after backgrounding**: if
  the screen has been backgrounded > 30 seconds, on resume call
  `pickFor` fresh so the daemon doesn't reply to a stale beat. Add
  to PR #5 acceptance criteria.
- **PR1 lint rule (i) implementation**: source-level KSP processor
  or regex over `domain/dialogue/lines/*.kt` files, asserting any
  line declared with `tier = LineTier.ESSENTIAL` *and* `lifeEvent =
  true` carries an explicit `preferredSurface =` token in source.
  Not runtime reflection.

Author rate (round 4, Skeptic empirical estimate): 30-50 voice-loyal
lines/day. ~3 weeks calendar to complete content authoring across
both phases, plus chain coherence pass.

## DI / layering / threading model (round 4, Architect)

Hadn't been raised through round 3. Locked here:

1. **`DialogueEngine` lives on `LifegameApplication.dialogueEngine: by lazy`**,
   same manual-DI pattern as `repository`. No Hilt. Constructed with
   the corpus + `DialogueStateStore`.
2. **Corpus loading**: `DialogueCorpus.all` is a `val` flattening 10
   archetype `object` declarations at class-init. Predicates are
   **`object` declarations** (`object AfterLapse_1 : Predicate`)
   interned at module load — **hard rule, not convention**. Lint
   catches `class AfterLapse(1)`-style allocations in line files.
3. **Threading**: `DialogueEngine.pickFor` is `suspend`, but the body
   is pure CPU over an in-memory list plus **ONE batched DAO read**:
   ```kotlin
   data class DialogueState(
       val played: Set<String>,
       val playCounts: Map<String, Int>,
       val lastPlayedAt: Map<String, Long>,
       val activeCooldowns: Set<Pair<String, Surface>>,
   )
   suspend fun loadState(daemonId: Long): DialogueState  // one DAO trip
   ```
   The store batches all four into one transaction. `pickFor` then
   runs entirely on the caller's dispatcher with no awaits inside the
   filter chain. Toast click-handler completes in single-digit ms.
4. **No in-memory cache** (resolves Architect's cache-invalidation
   contract): every `pickFor` reads fresh from Room. With the batched
   read, this is cheap enough. The race the Architect worried about
   (user closes a major while Conversation screen is paged) can't
   exist because the next `pickFor` sees the updated state.
5. **Tell-cooldown** (renderer-side, round 3 Ally) lives in
   DataStore Preferences keyed by `archetypeKey → lastUsedTellId`.
   Lazy-read into a per-archetype `StateFlow`, write-through on each
   tell pick. No new Room table.

## Open questions for council round 5 (FINAL)

Round 5 is the **sign-off round**. The design is implementation-ready;
the only remaining job is confirming nothing structural is still
wrong. Specific questions:

1. **The `PantheonBackup` v2 format bump** (round 4 Skeptic-uncovered
   bug, v5 fix). Including dialogue state in the backup honors
   Believer's "play log IS the relationship" but it's a backup format
   change after just shipping v1 last release. Worth it, or accept
   degradation on restore as Skeptic's "backup-as-incarnation"
   alternative?
2. **The trigger threshold of < 2 median screen opens / active daemon /
   week**. Round 4 (Skeptic + Demolisher) said the *shape* is right
   (median, well-defined active). Is the number — 2 — calibrated?
   Should it be 1? 3? What's the basis other than designer's gut?
3. **The pre-committed cutdown draft file**. Is committing
   `v0.0.7-cutdown.draft.md` actually a forcing function, or is it
   theater that will rot? What keeps it honest?
4. **The 6-PR sequence and 3-week author calendar**. Realistic, or
   am I about to discover that line 250 of voice-loyal authoring is
   when prose actually flattens?
5. **Anything quietly wrong** that's been sitting in someone's
   peripheral vision through rounds 1-4.

Round 5 deliverable: either "ship it" or "one specific thing to fix
before implementation starts."

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

### Round 2 — synthesis

Believer conceded the affinity cut but disputed the cut going *too*
far. Vulnerability chains on `conversationsHad` alone were called a
"punch-card." Demanded ONE earned predicate (`MajorsClosedAtLeast`).
Round 3 must protect: the Conversation screen against the Demolisher's
push to cut it; multi-beat chains; ESSENTIAL `FIRST_EVER` lines.

Ally pushed back on deferring the linter — graph integrity at 500 lines
is invisible at line level. Proposed `deprecated` flag and tell-cooldown
adoption now. Designed the mid-day-return ambush moment.

Architect found the **two-consumer race bug**: inline plays line at
8am, screen sees it as played at 8:01, opens with filler. Independently
discovered by the Skeptic. Resolution: `preferredSurface`. Architect
also flagged DSL IDE risk (mega init blocks), recommended `lifeEvent
desc, recencyKey asc, priority desc` ordering instead of ordinal-as-
priority, moved tells to renderer, added 24h fallback on same-encounter
lock to prevent permanent mute, and pinned `ConversationsHad`
increment to first-choice-taken not screen-open.

Skeptic strengthened the vertical-slice argument given the doubled
surface, proposed `archetypeAllowsReactive` predicate-layer whitelist
as the surveillance guardrail, demanded `replacedBy` + lint for the
deprecation strategy, and converged with Architect on the race bug.

Demolisher conceded the ambush track is the real win (40% concern
reduction) but still argued for cutting the screen entirely on
procedural grounds. Called the DSL designer's joy. Targeted: branching
choices, chains, same-encounter lock, animated pauses, tells-as-system,
choice cards.

**Resolutions encoded into v3:**
- `preferredSurface` fixes the two-consumer race (Architect + Skeptic)
- Earned predicates (`MajorsClosedAtLeast`, `WishesSpentAtLeast`) for
  intimacy gating (Believer)
- `archetypeWhitelist` on reactive predicates (Skeptic)
- `ConversationsHad` increments on first choice taken (Architect)
- 24h fallback on same-encounter lock (Architect)
- `lifeEvent` + `recencyKey` two-axis ordering replaces ordinal hack
- DSL dropped, raw `listOf(DialogueLine())` per archetype file
  (Architect + Demolisher converge)
- Animated thinking pauses dropped (Demolisher)
- Tells move to renderer (Architect); tell cooldown in renderer (Ally)
- `deprecated` + `replacedBy` adopted now (Ally + Skeptic)
- `DialogueLintTest` unit test ships (Ally pushed)
- Chains in 3 archetypes only (Drill Sergeant + Gentle Mentor + Oracle)
  — partial concession to Skeptic's vertical-slice
- Conversation screen kept (user explicit ask) but all decorative
  scaffolding around it cut (Demolisher's spirit, not letter)

### Round 3 — synthesis

Believer accepted the v3 earned predicates as satisfying the
punch-card concern; demanded the mid-day-return be its own essential
pattern (not just an opener variant); rejected the first-launch
explainer as "the screen earns its second open by what the daemon
says the first time."

Ally specced `DialogueLintTest` coverage (9 items now); recommended
DataStore Preferences for tell-cooldown (not Room); fleshed out the
mid-day-return predicate stack with three example archetype lines;
proposed `LineProvenance` debug overlay.

Architect found three new leak paths in v3: (1) EITHER-flagged
ESSENTIAL still races; (2) cooldown bleed across surfaces; (3)
`replacedBy` chains break user data if engine lint-only. Recommended
predicate singletons / interning. Flagged denormalized counter need
for earned predicates. Confirmed `daemon_state` exclusion from
backup but caught the import-leaves-stale-state bug.

Skeptic killed the unconditional 24h same-encounter fallback (rewards
absence); demanded synthetic `LAPSE_REACTIVE` cross-surface cooldown
so even whitelisted archetypes can't compound shame; rejected the
first-launch explainer as polish dressed as structure; stayed firm
on full vertical-slice (one archetype deep, others stubbed).

Demolisher delivered the harshest revisit: "decorations are gone,
the mistake is naked." Argued earned predicates are structurally
identical to affinity. Argued the 3-deep / 7-shallow split is the
ceasefire that loses both fronts. Lightly-revised epitaph: same
verdict, two-percent improvement.

**Resolutions encoded into v4:**
- `EITHER`-flagged ESSENTIAL+lifeEvent default to SCREEN via lint
- Surface-scoped cooldowns (`cooldown_play.surface` column,
  `crossSurfaceCooldown` flag on the line)
- Engine resolves `requires` through one hop of `replacedBy` at load
- Predicate singletons + small-arity interning (coding convention)
- Denormalized `majorsClosedTotal` / `wishesSpentTotal` on
  `daemon_state`, updated on event
- Import-replaces-pantheon also wipes `daemon_state`
- 24h unconditional fallback dropped; `OR conversationsHad == 0`
- Synthetic `LAPSE_REACTIVE_COOLDOWN` cross-surface, lint-enforced
- First-launch explainer dropped (3 seats converged)
- Mid-day-return promoted to its own essential pattern
- Tell-cooldown lives in DataStore Preferences (not Room)
- `LineProvenance` dev-build debug overlay adopted
- v0.0.7 cutdown trigger documented: < 2 screen opens/active
  daemon/week ⇒ cut screen + RESPONSE + chain content

**Tensions remaining unresolved (round 4 to test):**
- Skeptic's full vertical-slice still rejected — v0.0.7 trigger is
  the deferred concession.
- Believer's legibility-vs-opacity on earned predicates — fix would
  require surfacing gates to user; documented as v0.0.7 tension.
- Demolisher's "cut the screen" — rejected per user ask; v0.0.7
  cutdown trigger encodes the deferred concession.

### Round 4 — synthesis

Believer: v0.0.7 cutdown trigger is honest IF instrumented in v0.0.6
(otherwise theater). Mid-day-return needs `MinutesSinceLastForeground
AtLeast(180)` WHEN-NOT guard. Accepted earned-predicate deferral.
Round-5 hill: instrumentation must ship.

Ally: 6-PR implementation sequence. Phased release (v0.0.6 = engine
+ ambush + foreground; v0.0.6.1 = screen + chains). Fallback seed
`daemonId + epochDay` (recomposition flicker fix). Lock
`recencyKey` enum order with unit test.

Architect: cut `replacedBy` runtime rewrite — lint already forbids
the unreachable case. Document deprecation playbook. `surface =
'BOTH'` row pattern confirmed cleanest. **DI / layering / threading
not raised through round 3** — engine on `LifegameApplication`,
predicate `object` declarations as hard rule, batched single-DAO
state read in `pickFor`, no in-memory cache. Engine cache
invalidation contract resolved by "always re-read, batched, fast."

Skeptic: stood down on full vertical-slice. **Surfaced backup-voice-
continuity bug** — daemon_state excluded from backup means restoring
gives stranger-Athleta. Defined active daemon (≥1 minor completion
trailing 7d AND created ≥7d ago) and threshold form (median over 14d
after 7d settle-in). Author rate empirical: 30-50 lines/day, ~3
weeks calendar.

Demolisher: trigger papered over without pre-staged cut PR. Single
veto for round 5: `replacedBy` runtime rewrite (converges with
Architect on this). Median not mean (converges with Skeptic).
Active-daemon def must protect denominator from self-gaming.
Verdict revised: "quiet uninstall of a feature, not the app" —
progress acknowledged.

**Resolutions encoded into v5:**
- `replacedBy` runtime rewrite **CUT** (Architect + Demolisher
  converge); lint-only sufficient given deprecation playbook now
  documented inline
- `PantheonBackup` bumps to format v2, carries `line_seen` +
  `cooldown_play` + `daemon_state` (Skeptic backup-voice bug)
- Trigger threshold: **median** opens / active daemon / week, ≥ 2,
  over 14d after 7d settle-in (Skeptic + Demolisher)
- Active daemon definition: ≥1 minor completion trailing 7d AND
  created ≥7d ago (Skeptic + Demolisher)
- `screen_open_count` + `last_screen_open_at` on `daemon_state`,
  surfaced in dev `LineProvenance` overlay AND in a dev-build
  Settings row (Believer instrumentation demand)
- `v0.0.7-cutdown.draft.md` committed alongside v0.0.6, enumerating
  exactly what gets deleted (Demolisher's honest-trigger move)
- Engine threading: `pickFor` batches state into ONE DAO call, no
  cache (Architect)
- Predicate `object` singletons as hard lint rule (Architect)
- `MinutesSinceLastForegroundAtLeast(180)` on mid-day-return
  (Believer)
- Fallback render seed `daemonId XOR epochDay` (Ally)
- `recencyKey.ordinal` lock unit test (Ally)
- Deprecation playbook documented in-line (Architect)
- 6-PR implementation order locked (Ally + Architect)
- Phased release v0.0.6 + v0.0.6.1 (Skeptic + Ally + Architect)

**Tensions remaining unresolved (round 5 to test):**
- Whether `PantheonBackup` format bump is right call vs "backup-as-
  incarnation" (Skeptic's alternative).
- Whether 2-opens threshold is calibrated or arbitrary.
- Whether pre-committed cutdown draft is forcing function or theater.

### Round 5 — sign-off

**Final tally: three "ship it" verdicts, two precise additions.**

- **Believer**: SHIP IT. Earned predicates over affinity held the
  thesis. Backup format bump is right — Skeptic's alternative would
  have re-stranger'd the daemon every restore. Protect-during-
  implementation: the 180-min foreground guard on mid-day-return is
  load-bearing; flag in PR4 description so it survives implementation
  pressure.

- **Ally**: ONE SPECIFIC THING. Make trigger-window cannot-fire-against
  -v0.0.6 explicit. Add placeholder Talk affordance in PR4 so v0.0.6
  doesn't read as unfulfilled promise. Calendar is 3.5 weeks, not 3
  (PR4 needs a coherence-buffer + author rate math). **All adopted in
  v6.**

- **Architect**: SHIP IT. `PantheonBackup` v2 path clean (Daemon
  entity unchanged, daemon_state is sibling). Single-DAO-call batching
  works because `pickFor` runs per beat, not per screen-open — no
  stale-read window beyond current line. Two implementation notes
  added to PR scope (`onResume` re-pick, source-level lint).

- **Skeptic**: ONE SPECIFIC THING. Add lint rule (j): `DialogueLine.id`
  immutable across releases; rename via deprecate-and-replace only.
  Prevents `line_seen` rows from old backups becoming orphans across
  upgrades. **Adopted in v6.** Prediction for v0.0.7: median lands
  0.6-1.2, cutdown fires, ~110 lines deleted, ambush survives as the
  actual product.

- **Demolisher**: SHIP IT. v5 did the two things demanded (kill
  `replacedBy` runtime, pre-stage cutdown). Predicts screen dies in
  v0.0.7 because ambush eats its reason to exist. Updated epitaph:
  *"Built the screen with the cremation papers pre-signed. Either it
  earns its keep or it gets the cleanest deletion in this repo's
  history. Both outcomes are wins."*

**v6 additions encoded:**
- Lint rule (j) — immutable line ids; previous-release snapshot at
  `app/dialogue-ids/<v>.txt`
- Trigger-window guarantee: cannot fire against v0.0.6
- Placeholder Talk affordance in PR4 routing to
  `PlaceholderConversationScreen.kt`
- PR1: lint rule (i) implementation must be source-level (KSP/regex)
- PR5: `ConversationScreen.onResume` re-pick after > 30s background
- PR4 description: flag 180-min foreground guard as load-bearing
- Calendar revised to 3.5 weeks (PR4 + coherence pass)

**No structural changes from v5. Council process complete.
Implementation begins.**
