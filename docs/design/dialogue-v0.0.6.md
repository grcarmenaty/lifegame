# Daemon Conversations — Design Plan

> **Version log**
> - v0 — pre-research, pre-council. Tree-based, naive.
> - v1 — post-Hades research, pre-council. Flat predicate pool.
> - v2 — post council round 1. Two consumers, affinity cut, cuts to
>   `choice_log`, ChoiceTone, conditional templating, wall-clock lock.
> - **v3 (current) — post council round 2.** Fixes the two-consumer
>   race bug (`preferredSurface`). Adds earned predicates for
>   intimacy gating (Believer). Adds `archetypeAllowsReactive`
>   whitelist (Skeptic). Drops DSL (Architect + Demolisher converge).
>   Drops animated thinking pauses (Demolisher). Tells move to
>   renderer (Architect). Adopts deprecation scaffolding now
>   (Ally + Skeptic).

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
  `StreakAtLeast(days: Int)`, `DaysSinceLastConversationAtLeast(n)`,
  `ConversationsHadAtLeast(n)`,
  `DaysSinceFirstConversationAtLeast(n)`,
  `BoonSpentToday`.

**Earned predicates** (round 2, Believer): the vulnerability chain and
similar "intimacy is earned" content gates on *costly action*, not
attendance:
- `MajorsClosedAtLeast(n: Int)` — only counts truly closed (not just
  attempted).
- `WishesSpentAtLeast(n: Int)`.

These are not gameable because the cost is real (the user has to
actually do quests, actually spend wishes). They replace what affinity
would have done in v0 / v1.

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
action-gating could leave a daemon mute forever — so there's an
absolute fallback: regardless of activity, the lock auto-releases
after **24 hours**. Refused state shows a small archetype-styled
banner ("`*pacing*` I've said my piece. Go.") with a single "Leave"
action.

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

### Shape (post-round-2 revision)

All 10 archetypes ship full coverage on the foreground surfaces:
- **6-8 OPENER lines** spanning state patterns (first-ever-conversation,
  post-apotheosis, after-lapse, morning, evening, with-wishes-pending,
  all-quests-clear, mid-streak, **mid-day-return** per Ally round 2)
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

## Polish status (post round 2)

- ~~Kotlin DSL builder~~ — **dropped**. Architect: IDE/compile risk
  at scale; Demolisher: designer's joy. Raw `listOf(DialogueLine())`.
- ~~Archetype-flavored animated thinking pauses~~ — **dropped**.
  Demolisher: animation system isn't earning its keep. Tells in text
  do the perceived work.
- **Tells library + per-archetype tell cooldown** — kept, moved to
  renderer (Architect).
- **`DialogueLintTest` Kotlin unit test** — kept (Ally pushed for
  this in round 2; cheaper than a Gradle plugin). Asserts id
  uniqueness, no dangling `requires` / `forbids` / `nextLineId`,
  every RESPONSE reachable, every archetype covers the 8 mandated
  topics, every non-deprecated line whose `requires` points at a
  deprecated id without `replacedBy`.
- **`deprecated: Boolean` + `replacedBy: String?`** — adopted now
  (Ally + Skeptic converge: impossible to retrofit safely).
- "last 3 exchanges" strip — deferred to v0.0.7.
- `@onSeen` side-effect hooks — deferred.

## Open questions for council round 3

1. **The `preferredSurface` solution to the race bug.** Does it cover
   every case, or are there leak paths where the inline surface still
   burns a screen-flagged ESSENTIAL? What about repeat-after-replay
   semantics — once a screen-only line plays in Consumer B, the
   inline never sees it (by design), but should it ever?
2. **Multi-beat chains in only 3 archetypes for v0.0.6.** Drill
   Sergeant + Gentle Mentor + Oracle. Right pick? Is the harsh /
   warm / mystic triad the most-likely-to-be-used coverage, or are
   we picking the most-fun-to-write archetypes for ourselves?
3. **The earned predicates.** `MajorsClosedAtLeast` + `WishesSpentAtLeast`
   replace what affinity would have done. Is this enough scaffolding
   for the vulnerability chain to feel earned, or does the user need
   to *see* the gate they're approaching (Believer's "punch-card"
   concern from round 2)?
4. **`archetypeWhitelist` on predicates as the surveillance guardrail.**
   Is this enough, or do we also need cap-per-day cooldowns on
   lapse-reactive lines so even Drill Sergeant can't compound shame?
5. **No `replacedBy` enforcement at runtime, only at lint time.** A
   line marked `deprecated = true, replacedBy = "ds_new_morning"` —
   what does the engine do if a user's chain has `requires =
   listOf("ds_old_morning")`? Currently the chain just breaks. Should
   the engine follow `replacedBy` chains?
6. **Mid-day-return ambush moment** (Ally round 2). Inline opener
   gated by `MinorsCompletedTodayAtLeast(0) + TimeOfDayIs(AFTERNOON)
   + DaysSinceLastConversationAtLeast(0)`. Worth designing as its
   own essential pattern, or just one more opener variant?
7. **The Demolisher's persisting concern.** Even after cuts, the
   Conversation screen carries 40% of remaining concern. Is there a
   structural move (e.g., the screen's first-launch shows a
   why-this-exists explainer that frames it as ritual not utility)
   that addresses the "opened twice" risk without cutting the
   feature?

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

Rounds 3–5+ will append below.
