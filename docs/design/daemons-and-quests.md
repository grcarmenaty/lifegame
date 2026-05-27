# Daemons & Quests — Design Plan (v1)

> Status: initial draft, pre-council review. Subject to revision.

## Premise

The user's life is modeled as a small pantheon of **daemons** —
personifications of the life-aspects the user chooses to track (e.g.,
"Athleta" for fitness, "Sage" for learning, "Hearth" for relationships,
"Coin" for finances). Each daemon is a questgiver with its own voice,
arc, and powers. The user's productivity becomes a negotiation with this
inner pantheon.

The whole system is **user-authored**: there are no shipped quests. The
app provides structure, voice, and ritual; the user fills in the content.

## Core Entities

### Daemon
- A user-defined personification of one life-aspect.
- Carries: name, archetype/description, voice/tone, current **level**,
  current **epic chapter**, active major quests, active minor quests,
  available **wishes**.
- Daemons are the narrative anchor — quests are framed in their voice.

### Epic
- The daemon's overarching story arc, advanced chapter-by-chapter.
- Authored by the user (optionally seeded by the daemon's archetype).
- Each chapter unlocks at daemon level-up.
- Not gameplay-required — narrative texture, not a gate.

### Major Quest
- An overarching goal tied to one daemon (e.g., "Run a half-marathon",
  "Read 12 books this year", "Reach $X savings").
- Composed of one or more **minor quests**.
- User defines the completion threshold (count of minor quests, weighted
  points, deadline, etc.).
- Completion triggers daemon level-up + epic advancement + a revision
  prompt for the next round of minor quests.

### Minor Quest
- A concrete task that feeds a major quest.
- Two flavors:
  - **One-off**: a single act ("Sign up for the race")
  - **Repeatable**: recurring across days/weeks ("Run 3x this week")
- Each completion contributes weighted progress toward the parent major
  quest.
- Editable or replaceable at major-quest completion checkpoints.

### Wish
- A daemon's "power" — a benefit the user can spend.
- Both the **count** and the **nature** of available wishes scale with
  daemon level.
- Indicative examples: skip-day token, reroll a minor quest,
  double-progress on next task, unlock a cosmetic narrative beat,
  "borrow" progress from another daemon.
- Spending wishes is part of the gameplay loop, not just a reward.

## The Loop

1. **Summon**: user creates a daemon — names it, sets archetype, writes
   (or seeds) the opening epic chapter, defines an initial major quest
   and a few minor quests.
2. **Quest**: user completes minor quests; progress accrues on the parent
   major quest in the daemon's voice.
3. **Apotheosis**: major quest completes →
   1. Daemon levels up.
   2. Next epic chapter unlocks.
   3. User is prompted to retire, refine, or replace the minor-quest
      slate.
   4. Wish slots refresh; new wish types may unlock at this tier.
4. **Wish**: user spends wishes on QoL or narrative beats.
5. Loop continues; the daemon's identity deepens with each cycle.

## What This Is NOT

- Not a habit tracker with extra steps. The narrative framing and the
  user-authored content are the product, not chrome.
- Not multiplayer / social (for now).
- Not tied to specific external integrations (calendar, fitness
  trackers) in v1.
- Not gamification-by-points-and-badges. Daemons, epics, and wishes are
  the entire reward language.

## Design Tensions (Known, Unresolved)

- **Authoring burden.** How much must the user write upfront before the
  app feels useful? Templates? AI-assisted archetype generation? A
  "starter daemon" wizard?
- **Quest hierarchy depth.** Is two tiers (major/minor) enough, or do
  some users need explicit sub-tasks under minor quests?
- **Repeatable-quest fatigue.** How to keep daily-habit-style quests
  from feeling like the same checklist the user already abandoned in
  three other apps.
- **Wish economy.** Wishes must feel meaningful but not exploitable.
  Who designs the wish catalog — the app, the user, or both?
- **Daemon count.** Should there be a soft cap to discourage shallow
  proliferation? Or trust the user?
- **Failure handling.** What happens when a minor quest lapses? Penalty,
  neutral, narrative consequence ("the daemon grows distant")? The
  choice sets the emotional tone of the entire app.
- **Cross-daemon interaction.** Can daemons reference each other,
  conflict, cooperate? Tempting but probably v2.
- **Onboarding.** First-run: one daemon and grow, or build the pantheon
  upfront? The former is gentler; the latter is more committal.

## Deliberately Out of Scope for v1

- Multiplayer / social features
- External integrations beyond export
- Monetization model
- Cross-daemon mechanics
- A shipped quest catalog
