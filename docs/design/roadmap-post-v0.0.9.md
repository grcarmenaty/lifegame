# Roadmap, post-v0.0.9

> Where lifegame goes from here. This doc supersedes the bullet lists
> in CLAUDE.md and the deferred-items log in
> `docs/design/dialogue-v0.0.6.md` for things that haven't shipped.
> Conceptual model has shifted enough that several deferred items are
> now moot, several survive, and four new mechanics need spec'ing.

---

## 1. Why epics haven't shipped

Honest answer: I deferred them across three design rounds.

- v2 design (`docs/design/daemons-and-quests.md`) demoted epics to
  "decorative, opt-in scripture view, never a gate" after Demolisher
  called the original framing "designer's joy / pure authorial
  fan-service."
- v0.0.3 and v0.0.4 reaffirmed deferral.
- The dialogue council reaffirmed deferral again.

Each time I leaned on the council's argument that *epics aren't
gameplay-required*. That was a defensible call when you hadn't asked
for them. You're asking now. The argument flips: epics are the
*narrative payoff* for the new model where major closure no longer
grants boons. **Epics ship in v0.0.11.**

---

## 2. New conceptual model

Earlier rules:
- Level = count of closed majors
- Boon += wishRewardCount on major closure
- Minors gate major closure via thresholdCount (REMOVED in v0.0.10
  source, branch only)

New rules:

| Concept | Mechanic |
|---|---|
| **Attention** | Stored point total per daemon. Earned by minor completions and major closures. Subject to decay. |
| **Level** | Derived: `1 + (attention / pointsPerLevel)`. No upper bound. |
| **Boons** | Accrued by minor completions, *not* by major closures. Per-daemon counter; at threshold, the daemon's first boon gets +1 and the counter resets. |
| **Major closure** | One large attention bonus + advances the daemon's epic by one chapter (if epics are enabled for that daemon). No boon deposit. |
| **Decay** | Attention drops over time if the daemon is neglected (no minor completions). Rate is per-archetype default, overridable per-daemon. |
| **Epics** | A user-authored chapter log per daemon. Advances one chapter per major closure. Optional — daemon can run without one. |

This makes attention the central currency. Boons are an *output* of
attention. Level is a *display* of attention. Closing a major is
emotional + narrative (epic chapter, in-voice dialog), not
transactional.

---

## 3. Mechanic specifications

### 3.1 Attention & level

**Storage** (schema v4 → v5):
```
ALTER TABLE daemon_state ADD COLUMN attentionPoints INTEGER NOT NULL DEFAULT 0;
ALTER TABLE daemon_state ADD COLUMN lastAttentionUpdateAt INTEGER;
ALTER TABLE daemon_state ADD COLUMN attentionDecayPerDay INTEGER;          -- null = use archetype default
ALTER TABLE daemon_state ADD COLUMN attentionDecayGraceDays INTEGER;       -- null = use archetype default
ALTER TABLE daemon_state ADD COLUMN minorsCompletedSinceAccrual INTEGER NOT NULL DEFAULT 0;
ALTER TABLE daemon_state ADD COLUMN minorsPerBoonAccrual INTEGER;          -- null = use archetype default (5)
```

**Rules:**
- Minor completion: `attentionPoints += minor.weight` (so default
  weight 1 = 1 attention; user-set weight 9 = 9).
- Major closure: `attentionPoints += 25` (large bonus reflecting
  month-scale work).
- Level derivation: `level = 1 + attentionPoints / 50`. So 50 minor
  completions of weight 1 = level 2; major closure (25) + 25 minors
  = level 2; etc.
- `levelOf(daemonId)` switches from major-count to attention-derived.

**Migration:** existing daemons get an attention backfill equal to
their current completed-major count × 25 + sum of minor weights
already credited via progressCount. Existing level stays roughly
where it was.

### 3.2 Decay

**Rate** is per-archetype default, overridable per-daemon.

Per-archetype defaults (lower-pressure archetypes decay slower; the
existing relational-stance personality carries into the punishment
register too):

| Archetype | Decay/day | Grace days | Effect |
|---|---|---|---|
| DRILL_SERGEANT | 5 | 1 | Aggressive — expects daily check-in |
| COACH | 4 | 1 | High — practice habit |
| TRICKSTER | 3 | 2 | Moderate — sardonic about absence |
| CHEERLEADER | 3 | 2 | Moderate — gentle nudges |
| ORACLE | 2 | 3 | Slow — patient |
| POET | 2 | 3 | Slow — observational |
| GENTLE_MENTOR | 2 | 5 | Slow — softest |
| STOIC | 1 | 5 | Very slow — accepts what is |
| THERAPIST | 1 | 7 | Very slow — patient |
| HERMIT | 0 | 14 | Effectively none — quiet, doesn't pressure |

**Mechanism:**
- New WorkManager periodic worker `AttentionDecayWorker` (~12-hour
  interval). Runs alongside the existing `NudgeWorker` but in its
  own scheduler.
- On each run, for each daemon:
  - Compute `daysSinceLastUpdate` from `lastAttentionUpdateAt`.
  - If `daysSinceLastUpdate > graceDays`:
    - Apply `(daysSinceLastUpdate - graceDays) × decayPerDay` to
      `attentionPoints`, floored at 0.
    - `lastAttentionUpdateAt = now`.

Any time the user completes a minor or closes a major, we also
update `lastAttentionUpdateAt = now` so the grace period resets.

**Per-daemon override UI:** on the daemon detail screen, a new
"Decay" subsection in the edit form exposes two number inputs (per
day + grace days). Empty = use archetype default (with the resolved
value shown as a hint).

### 3.3 Boon accrual from minors

**Rule:** every N minor completions on a daemon, +1 to that
daemon's first boon. N defaults to 5; per-archetype default;
overridable per-daemon (`daemon_state.minorsPerBoonAccrual`).

Per-archetype defaults:

| Archetype | Minors per boon |
|---|---|
| DRILL_SERGEANT, COACH | 7 (harder to earn — expects more reps) |
| CHEERLEADER, TRICKSTER | 5 |
| GENTLE_MENTOR, THERAPIST, POET, ORACLE | 4 (gentler economy) |
| STOIC | 5 |
| HERMIT | 3 (small rewards from a quiet daemon) |

**Recipient policy (v0.0.10):** auto-credit the daemon's **first
boon** (the one created at summoning). The user can name it
accordingly ("a guilt-free rest day" → that's what your minor work
buys you).

**Future iteration (v0.0.12 or beyond):** when a boon-accrual fires,
optionally show a picker dialog to let the user pick which boon
receives the credit. For v0.0.10 we ship the simple version.

### 3.4 Epics (v0.0.11)

**Storage** (schema v5 → v6):
```
CREATE TABLE epic_chapter (
  id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
  daemonId INTEGER NOT NULL,
  position INTEGER NOT NULL,
  text TEXT NOT NULL,
  createdAt INTEGER NOT NULL,
  FOREIGN KEY(daemonId) REFERENCES daemons(id) ON DELETE CASCADE
);
CREATE INDEX index_epic_chapter_daemonId ON epic_chapter(daemonId);
```

**Rules:**
- Optional: a daemon can have zero chapters and never write any.
- On major closure: the apotheosis dialog gains a "Write a chapter"
  affordance (optional skip). User writes 1-3 lines describing what
  this closure means in the daemon's narrative.
- Detail screen gains a "Scripture" section showing all chapters in
  order with their dates. Collapsible by default to avoid bloating
  the screen.
- Authoring tip per archetype: at the chapter prompt, suggest
  template language ("Athleta speaks of the run she demanded…")
  matching the daemon's voice. Skippable.

**Round-trip:** epics travel in `PantheonBackup` (bump format to v3
in v0.0.11, but adding a new optional list is backward-compatible —
existing v2 backups still import).

---

## 4. Version roadmap

Estimated complexity → which release.

### v0.0.10 — Attention economy

**Ships:**
- Schema v4 → v5 (5 new columns on `daemon_state`).
- Attention model: minor completion adds weight; major closure adds
  25. `levelOf` becomes attention-derived.
- Boon-from-minors auto-accrual (first boon, archetype-default
  threshold).
- Attention decay (periodic worker, archetype defaults).
- UI: per-daemon decay + boon-threshold override fields on detail
  screen edit form.
- UI: Daily card level bar repurposed — shows progress toward next
  level (i.e., `(attention % 50) / 50`) rather than progress on
  the leading open major.
- Removes the "thresholdCount" display from the major card (already
  did this on the branch); thresholdCount column stays unused, will
  be dropped in v0.0.12 with the recreate-table migration that drops
  several legacy columns at once.

**Risk:** the migration from major-count level to attention-derived
level needs to feel coherent for existing data. The backfill formula
gives daemons "credit" for prior majors so users don't suddenly drop
ranks.

### v0.0.11 — Epics

**Ships:**
- Schema v5 → v6 (new `epic_chapter` table).
- Apotheosis dialog → optional "Write a chapter" branch.
- Detail screen → collapsible Scripture section.
- `PantheonBackup` format v3 (carries chapters).
- Per-archetype chapter prompt suggestions.

### v0.0.12 — Quality of life

**Pick from:**
- Edit quest / boon titles (current model: delete + re-add).
- Soft-delete + undo for destructive ops (vanish, delete major,
  delete minor, delete boon).
- Per-daemon notifications toggle UI on the detail screen
  (the repo + DAO + VM method already ship from v0.0.7).
- Boon-pick dialog when accrual fires (replace auto-credit-first
  behavior).
- WEEKLY cadence for minors.
- Schema cleanup: drop unused `thresholdCount`, drop `wishRewardCount`
  from major_quests (now meaningless). One big recreate-table
  migration.
- `MigrationTestHelper` round-trip tests for v1 → v2, v2 → v3,
  v3 → v4, v4 → v5, v5 → v6. Tracked since v0.0.3.

### v0.0.13 — Conversation screen + chains (deferred from v0.0.6.1)

The Conversation screen + multi-beat chains for Drill Sergeant /
Gentle Mentor / Oracle were the v0.0.6.1 plan but didn't ship. The
Demolisher's v0.0.7-cutdown trigger said "if screen open rate <2/
active daemon/week, cut." But the screen never shipped, so the
trigger never could fire. Status: re-evaluate against current model
before committing.

### Backlog (no version yet)

- Cross-daemon mechanics + `@token` flavor references
- LLM-backed daemon conversations (BYOK Anthropic API — your call)
- Failure-handling tonal decay in dialogue lines (lapse-aware voice
  shifts beyond the attention decay system)

---

## 5. What's now moot

Several deferred items are obsoleted by the new model:

| Deferred item | Status |
|---|---|
| Per-major threshold configuration UI | **Moot** — majors no longer auto-close, threshold is meaningless |
| Per-major wish reward configuration UI | **Moot** — major closure no longer grants boons |
| Boon-pick dialog (the "Believer wanted legibility" point) | Survives — moves to v0.0.12 as boon-accrual picker |
| Refund of contributions when a DAILY minor is deleted | **Less critical** — progressCount is just a tracking signal now, not gating anything |

---

## 6. Per-archetype defaults — one summary table

All the new per-archetype constants in one place for review before
they go into source:

| Archetype | Decay/day | Grace days | Minors per boon |
|---|---|---|---|
| DRILL_SERGEANT | 5 | 1 | 7 |
| COACH | 4 | 1 | 7 |
| TRICKSTER | 3 | 2 | 5 |
| CHEERLEADER | 3 | 2 | 5 |
| ORACLE | 2 | 3 | 4 |
| POET | 2 | 3 | 4 |
| GENTLE_MENTOR | 2 | 5 | 4 |
| STOIC | 1 | 5 | 5 |
| THERAPIST | 1 | 7 | 4 |
| HERMIT | 0 | 14 | 3 |

The numbers encode a relationship stance: aggressive daemons demand
high-frequency check-ins and dole rewards parsimoniously; gentle
daemons are slow to penalize and quick to thank.

---

## 7. Open questions for you

Before I implement v0.0.10:

1. **Attention bonus on major closure**: I proposed +25 (= half a
   level). Is that right? Larger (+50, a full level) makes major
   closure feel monumental. Smaller (+10) makes it feel modest
   relative to the ongoing minor grind.

2. **`pointsPerLevel = 50`**: I picked this so a daemon with default
   weight-1 minors and one daily completion levels every ~50 days.
   Too slow? Too fast?

3. **Boon-accrual recipient**: ship v0.0.10 with auto-credit-first-
   boon, defer picker to v0.0.12 — or hold v0.0.10 until the picker
   ships?

4. **Decay floor**: I floored attention at 0. Should level also have
   a floor of 1 (can never drop below "you exist") or should the
   level number itself be allowed to display 0?

5. **Backfill formula on migration**: should existing daemons get
   credit for past minor completions too (via `progressCount` on
   their majors), or only major-closure credit? `progressCount` is a
   running sum already; multiplying by 1 gives a rough attention
   estimate. Or skip and let existing users start at the major-count
   level they had.

6. **Should the council weigh in?** Most of this is mechanic-shape,
   not product-thesis. I think one round of council critique on
   §3 and §4 specifically (not §5/§7) would catch holes I'm not
   seeing. Skip if you'd rather iterate via me directly.

---

## 8. Suggested next step

Tell me which open questions to resolve which way, and I'll start
v0.0.10 implementation. If you want a council round on §3 + §4
first, say so and I'll dispatch.
