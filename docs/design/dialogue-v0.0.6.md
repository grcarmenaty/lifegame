# Daemon Conversations — Design Plan

> **Version log**
> - v0 — pre-research, pre-council. Tree-based, naive.
> - **v1 (current) — post-Hades research, pre-council.** Replaces tree
>   with a flat predicate pool, adopts Hades' three-tier priority and
>   named cooldown groups, drops chunk-position state in favor of
>   implicit chains via `requires`/`forbids`.

## Premise

A hand-authored dialogue system that makes each daemon feel like a
contextual conversation partner without an LLM. Inspired by Supergiant's
dialogue mechanics in Hades/Hades 2: a flat pool of voice lines, each
gated by predicates and ranked by priority. The system feels reactive
because the *filter* is paying attention to user state, not because the
content is dynamically generated.

User agency is achieved through **branching choices** layered on top of
the Hades pool model — once the engine picks an opener, the user steers
where the conversation goes by choosing among reply options.

**Constraints baked in:**
- No network, no API, no LLM.
- All content ships in the APK.
- Reuses the existing 10 voice presets as the *style anchor*.
- Persistent per-daemon: your history with Athleta accumulates.

## What Hades actually does (the parts we're copying)

1. **Flat pool + predicates, not a tree.** Each line has `Priority`,
   `GameStateRequirements`, `RequiredTextLines` (must have played),
   `RequiredFalseTextLines` (must NOT have played), `CooldownName`,
   `CooldownTime`. The engine filters all eligible lines, then picks
   the highest priority.
2. **Three priority tiers**: filler / contextual / essential. Essential
   story beats override everything else when their conditions hit.
3. **Two retirement axes**: hard named-cooldowns (so a *topic* goes
   quiet, not just one line) + soft "exhaust before repeat" (prefer
   the line with the lowest play count when picking among ties).
4. **Sequences as implicit chains.** Line B requires line A has played.
   There is no `currentChapter` state — the play log *is* the state.
5. **Cheap rich state surface.** Predicates can read anything: HP,
   weapon, deaths-this-run, gifts-given, NPCs-spoken-to. Reactivity
   comes from the breadth of the predicate language, not from clever
   selection logic.

What we're NOT copying:
- Voice acting (we're silent text).
- The 300k-word corpus (we ship maybe ~500).
- One-direction monologue (Hades' player rarely chooses what Zagreus
  says back). We add **branching choices** because in lifegame the
  user *is* the protagonist and needs agency.

## Architecture

### `DialogueLine` (atomic content unit)

```kotlin
data class DialogueLine(
    val id: String,                      // stable across versions; "seen" tracking
    val archetype: String,               // a VoicePreset key, or "ANY"
    val text: String,                    // templated; references ${context.X}
    val tier: LineTier,                  // FILLER | CONTEXTUAL | ESSENTIAL
    val priority: Int = 0,               // tiebreak within a tier; higher first
    val requires: List<String> = emptyList(),   // lineIds that must have played
    val forbids: List<String> = emptyList(),    // lineIds that must NOT have played
    val stateRequirements: List<Predicate> = emptyList(),
    val cooldownGroup: String? = null,   // named topic; shared across lines
    val cooldownTurns: Int? = null,      // # of intervening picks before group unlocks
    val choices: List<DialogueChoice> = emptyList(),  // user's replies (may be empty)
)

enum class LineTier { FILLER, CONTEXTUAL, ESSENTIAL }

data class DialogueChoice(
    val text: String,                    // what the user "says"
    val nextLineId: String?,             // daemon's reply; null = end conversation
    val tone: ChoiceTone = ChoiceTone.NEUTRAL,
    val gates: List<Predicate> = emptyList(),  // hide unless gates pass
)

enum class ChoiceTone { NEUTRAL, WARM, CHALLENGING }
```

### `ConversationContext` (what predicates see)

```kotlin
data class ConversationContext(
    val daemonId: Long,
    val daemonName: String,
    val archetypeKey: String,
    val level: Int,
    val openMajors: List<MajorQuest>,
    val recentlyClosedMajor: MajorQuest?,    // last 24h
    val recentlyCompletedMinors: List<MinorQuest>,  // last 24h
    val minorsCompletedToday: Int,
    val minorsCompletedThisWeek: Int,
    val dailyMinorsLapsedCount: Int,         // DAILY minors with lastCompletedAt > 1d ago
    val streakDays: Int,                     // consecutive days with at least one completion
    val totalWishesAvailable: Int,
    val recentlySpentBoonText: String?,      // last 24h
    val conversationsHad: Int,
    val daysSinceLastConversation: Int?,
    val affinity: Int,                       // running tally; -10..+10 typical
    val dayOfWeek: Int,
    val timeOfDay: TimeOfDay,                // MORNING | AFTERNOON | EVENING | NIGHT
)

interface Predicate {
    fun isSatisfied(ctx: ConversationContext): Boolean
}
// Implemented as a small algebra of named predicates:
//   FirstConversation, AfterLapse(min: Int), AfterApotheosis,
//   WithWishesAvailable(min: Int), AtLevel(n: Int), TimeOfDayIs(t),
//   AffinityAtLeast(n), MinorsCompletedTodayAtLeast(n), etc.
```

### Engine

```kotlin
class DialogueEngine(
    private val corpus: List<DialogueLine>,
    private val store: DialogueStateStore,
) {
    fun openConversation(ctx: ConversationContext): DialogueLine {
        val played = store.playedLineIds(ctx.daemonId)
        val playCounts = store.playCountsByLineId(ctx.daemonId)
        val onCooldown = store.cooldownGroupsOnCooldown(ctx.daemonId)

        val eligible = corpus
            .filter { it.archetype == ctx.archetypeKey || it.archetype == "ANY" }
            .filter { it.requires.all(played::contains) }
            .filter { it.forbids.none(played::contains) }
            .filter { it.stateRequirements.all { p -> p.isSatisfied(ctx) } }
            .filter { it.cooldownGroup == null || it.cooldownGroup !in onCooldown }

        val byTier = eligible.groupBy { it.tier }
        val tier = listOf(LineTier.ESSENTIAL, LineTier.CONTEXTUAL, LineTier.FILLER)
            .firstOrNull { byTier[it]?.isNotEmpty() == true }
            ?: return FALLBACK_FOR(ctx.archetypeKey)

        return byTier[tier]!!
            .sortedWith(
                compareByDescending<DialogueLine> { it.priority }
                    .thenBy { playCounts[it.id] ?: 0 }                  // exhaust before repeat
                    .thenBy { store.lastPlayedAt(ctx.daemonId, it.id) ?: 0 }
            )
            .first()
            .also { store.markPlayed(ctx.daemonId, it) }
    }

    fun continueWith(ctx: ConversationContext, choice: DialogueChoice): DialogueLine? {
        store.recordChoice(ctx.daemonId, choice)        // updates affinity, etc.
        val nextId = choice.nextLineId ?: return null
        val line = corpus.firstOrNull { it.id == nextId } ?: return null
        store.markPlayed(ctx.daemonId, line)
        return line
    }
}
```

### Same-encounter lock

Hades disallows re-poking an NPC for new lines in one hub visit. lifegame
equivalent: after a conversation ends, the engine refuses to open a fresh
conversation with the same daemon **until at least one of**:
- A minor or major quest is completed.
- 4 hours of wall time pass.
- A wish is spent.

If the user opens "Talk" before any of those, they get a short
archetype-specific "I have nothing new" beat and a "Leave" choice.

### Persistence

```
-- New tables, v2 → v3 migration
CREATE TABLE line_seen (
  daemonId INTEGER NOT NULL,
  lineId TEXT NOT NULL,
  lastPlayedAt INTEGER NOT NULL,
  playCount INTEGER NOT NULL,
  PRIMARY KEY (daemonId, lineId),
  FOREIGN KEY (daemonId) REFERENCES daemons(id) ON DELETE CASCADE
);

CREATE TABLE choice_log (
  id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
  daemonId INTEGER NOT NULL,
  choiceText TEXT NOT NULL,
  tone TEXT NOT NULL,
  takenAt INTEGER NOT NULL,
  FOREIGN KEY (daemonId) REFERENCES daemons(id) ON DELETE CASCADE
);

CREATE TABLE cooldown_play (
  daemonId INTEGER NOT NULL,
  cooldownGroup TEXT NOT NULL,
  startedAt INTEGER NOT NULL,
  PRIMARY KEY (daemonId, cooldownGroup),
  FOREIGN KEY (daemonId) REFERENCES daemons(id) ON DELETE CASCADE
);

-- Affinity stored on daemons (or in a daemons_state table) — TBD
```

## Content corpus

### Target

Per archetype:
- **8-10 openers**, each gated by a distinct state pattern
- **3-5 multi-beat chains** (sequences via `requires`) on archetype themes
- **15-20 branched response lines** for user-driven topics
- **5-8 essential one-shots** (first conversation ever, first apotheosis,
  level milestones, first lapse, first wish spent)
- **10-15 filler lines** with thin or no requirements

**Per archetype: ~45-55 lines × 10 archetypes ≈ 500 lines**
plus ~30-50 archetype-shared structural lines.

### Topics every archetype must cover

These are the **content axes** the engine selects from. Each archetype
should have at least one line in each:

1. **Greeting** (state-aware: first-ever, post-apotheosis, after lapse,
   morning, evening, with-wishes, all-quests-clear, mid-streak)
2. **Progress check-in** ("How am I doing?" branch)
3. **Negotiation** ("I want to skip", "I'm overwhelmed", "Push me harder")
4. **Reactive comment** on a recently completed minor or closed major
5. **Reactive comment** on a recently spent boon
6. **Confession / vulnerability** (chain, unlocks at affinity ≥ 5)
7. **Philosophical stance** ("Why does this matter?")
8. **Same-encounter lock** ("I've said my piece. Go.")

### Authoring format

For v0.0.6 we hand-author in Kotlin as `val lines = listOf(...)` per
archetype, one file each under `domain/dialogue/`. This is verbose but
keeps content discoverable and compile-checked. A DSL is tempting but
would consume implementation budget; revisit in v0.0.7.

## UI

- **Talk** button on each daemon card in Daily (right of the wish chip
  / details icon) and on the daemon detail header.
- **`ConversationScreen`**:
  - Top: daemon name + level + a single archetype-styled greeting bar.
  - Center: current line in large type. Optional in-voice attribution
    "*— Athleta*" beneath.
  - Bottom: up to 4 choice cards. If no choices, a single "Continue" or
    "Leave" affordance.
  - Persistent back arrow exits the conversation cleanly.
- After conversation ends, return to where you came from.

## Affinity (kept, scoped tightly)

A running per-daemon integer, range [-10, +10], clamped. WARM choices
+1, CHALLENGING -1. Some lines gate on `AffinityAtLeast(N)` or
`AffinityAtMost(N)`. This is the *only* hidden state the engine tracks
that isn't already derivable from existing tables. We accept the
complexity because it's the cheapest way to express "Drill Sergeant
softens up after you keep showing up" without writing per-archetype
state machines.

If the council pushes back, we cut affinity and rely purely on
play-history sequencing.

## Open questions for council

1. **Comprehensive target — 500 lines a defensible floor?** Or is the
   real floor more like 200, with later patches?
2. **Affinity earns its keep?** It's the only hidden state. Worth the
   complexity, or can play-history-via-`requires` do the same work?
3. **Same-encounter lock specifics.** Is the 4-hour wall-clock window
   right, or should it be purely action-gated (only unlocks after a
   real activity, no clock)?
4. **Templating language.** Just `${context.field}` substitution, or do
   we need conditionals (`${if openMajors.size > 0}…${endif}`)?
   Conditionals are powerful but tend to balloon authoring complexity.
5. **Where the choice branches live.** Embedding `choices` directly on
   the line (current sketch) keeps everything in one place; a separate
   `branches` table makes large branching topics easier to edit. The
   Hades approach (everything inline as Lua tables) suggests inline.
6. **Daemon-can-go-silent edge case.** When zero eligible lines exist,
   what do we show? A canned "I have nothing new" in the daemon's
   voice, or hide the Talk button entirely?
7. **Content tiers within tier.** Hades has just 3 tiers, but a single
   "ESSENTIAL" tier might still need ordering (first-apotheosis vs.
   first-level-5 vs. first-lapse all eligible at once). Use `priority`
   integer within tier as the tiebreak — sketched above. Sufficient?

## Out of scope for v0.0.6

- Cross-daemon references (Athleta mentioning Sage)
- Audio / voice acting
- Conversation history view (the log accumulates but isn't displayed)
- Live-service content patches
- LLM integration of any kind

## Council iteration log

Rounds will append below.
