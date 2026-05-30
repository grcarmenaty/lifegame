# Daemon Conversations — Design Plan (v0)

> Status: pre-research, pre-council. Will be revised once the Hades
> research lands and iterated ≥5 rounds with the council.

## Premise

A hand-authored, Hades-inspired dialogue system that makes each daemon
feel like a contextual conversation partner without an LLM. Branching
choices give the user agency; priority + eligibility gating makes lines
feel reactive to current state.

**Constraints baked in:**
- No network. No API key. No LLM.
- All content ships in the APK.
- Reuses the existing 10 voice presets as the *style anchor*.
- Persistent per-daemon (your history with Athleta accumulates).

## Architecture sketch

### `DialogueLine` (atomic content unit)

```kotlin
data class DialogueLine(
    val id: String,                       // stable across versions; used for "seen" tracking
    val archetype: String,                // e.g. "DRILL_SERGEANT" or "ANY"
    val text: String,                     // templated, references ${context.recentMajorTitle} etc.
    val priority: Int,                    // higher = preferred when multiple eligible
    val category: LineCategory,           // OPENER | RESPONSE | MILESTONE | FILLER | CHUNK_BEAT
    val triggers: List<Trigger>,          // ALL must be satisfied for eligibility
    val oneShot: Boolean = false,         // retires after first delivery
    val cooldownDays: Int? = null,        // won't re-play inside this window
    val chunkId: String? = null,          // for multi-beat sequences
    val chunkPosition: Int? = null,
    val choices: List<DialogueChoice> = emptyList(),
)

data class DialogueChoice(
    val text: String,                     // what the user "says"
    val nextLineId: String?,              // the daemon's reply; null = end conversation
    val tone: ChoiceTone = ChoiceTone.NEUTRAL,  // shifts affinity
    val gates: List<Trigger> = emptyList(),     // hide choice unless gates pass
)
```

### `ConversationContext` (what predicates can see)

```kotlin
data class ConversationContext(
    val daemonId: Long,
    val daemonName: String,
    val archetypeKey: String,
    val level: Int,
    val openMajors: List<MajorQuest>,
    val recentlyClosedMajor: MajorQuest?,    // within last 24h
    val minorsCompletedToday: Int,
    val dailyMinorsLapsedCount: Int,         // DAILY minors with lastCompletedAt > 1 day ago
    val totalWishesAvailable: Int,
    val conversationsHad: Int,
    val daysSinceLastConversation: Int?,
    val affinity: Int,                       // running tally from past choices
    val recentlySpentBoonText: String?,      // within last 24h
)
```

### Engine

```
fun openConversation(daemonId): DialogueLine {
    val ctx = buildContext(daemonId)
    val candidates = ALL_LINES
        .filter { it.category == OPENER }
        .filter { it.archetype == ctx.archetypeKey || it.archetype == "ANY" }
        .filter { it.triggers.all { t -> t.satisfied(ctx) } }
        .filter { !alreadyRetired(it) }
        .filter { !inCooldown(it) }
        .filter { chunkOrderingSatisfied(it, ctx) }
    return candidates.maxByOrNull { it.priority }
        ?: FALLBACK_OPENER_FOR(ctx.archetypeKey)
}

fun continueConversation(choice): DialogueLine? {
    markChoiceTaken(choice)
    return choice.nextLineId?.let { lookup(it) }
}
```

## Content corpus target

Per archetype, minimum:
- **6-8 openers**, each gated by a distinct state pattern (first-ever
  conversation; right after apotheosis; with lapsed dailies; with
  wishes pending; etc.)
- **3-5 multi-beat "story chunks"** that develop over multiple
  conversations (the daemon talks about why the work matters; the
  daemon shares a memory; etc.)
- **8-12 response branches** the user can pick on most lines.
- **5-10 milestone one-shots** for big moments (first apotheosis, level
  5, first lapse, daemon's first conversation, etc.)

Total target: **40-60 unique lines per archetype × 10 archetypes ≈
400-600 lines**, plus ~50 archetype-agnostic structural lines.

This is the "very comprehensive" target. Adjustable based on council
review of authoring cost vs. perceived depth.

## Persistence

New Room table:
```
line_seen (
  daemonId INTEGER NOT NULL,
  lineId TEXT NOT NULL,
  lastSeenAt INTEGER NOT NULL,
  seenCount INTEGER NOT NULL,
  PRIMARY KEY (daemonId, lineId),
  FOREIGN KEY(daemonId) REFERENCES daemons(id) ON DELETE CASCADE
)
```

Optional affinity column on `daemons` (or a separate table).

## UI

- New **Talk** button on each daemon card in Daily, plus on the detail
  screen header.
- `ConversationScreen`:
  - Daemon name + archetype across the top
  - Center: current line (large type, in voice)
  - Bottom: up to 4 response choices as cards
  - Persistent "Leave" / "End conversation" affordance
- Conversation ends → return to Daily/Detail.
- Conversation log per daemon is **not** displayed in v0.0.6 — only
  the live thread plus the "seen" book-keeping under the hood. Showing
  history is v0.0.7.

## Open questions (seeds for council)

1. **Authoring format.** Hand-Kotlin (vibe-loyal, but rebuild for every
   content tweak), or JSON loaded from assets (faster iteration, but
   strings drift from code)? Or a Kotlin DSL?
2. **Comprehensive target.** Is 40-60 lines per archetype enough? Hades
   shipped ~300k. We're not Hades, but where's the floor for "feels
   alive"?
3. **Affinity mechanic.** Does it earn its complexity? Or does the
   priority + recency system already make daemons feel responsive
   without a running relationship score?
4. **Free-text fallback.** If no eligible line matches, do we show a
   generic "I have nothing new to say today" or do we pad with filler?
5. **Cross-daemon references.** Out of scope for v0.0.6, but worth
   considering whether to add a `mentionedBy: List<archetypeKey>` field
   now so we don't paint into a corner.
6. **Where to enter.** Should "Talk" be on every Daily card, or just
   the daemon detail screen? Daily-card placement makes conversation
   the foreground; detail-only makes it intentional.

## Hades research integration

TBD when the research agent reports back. Expect updates to the
priority system semantics, the story-chunks structure, and any
authoring-tooling insights worth borrowing.

## Council iteration log

Will be appended to this doc as rounds 1–5+ complete.
