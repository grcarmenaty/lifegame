# Roadmap, post-v0.0.9 (revision 2 — user-decisions encoded)

> Where lifegame goes from here. Revised after the user's decisions
> on the v1 open questions. v0.0.10 absorbs what was previously
> v0.0.10 + v0.0.11 (the attention/decay/boons/epics work). Per-version
> split below is now a sequential list of v0.0.10 contents + a
> separate v0.0.11+ QoL track.

---

## 1. Why epics haven't shipped

Honest answer: I deferred them across three design rounds, leaning
on the council's "epics are decorative, not gameplay-required"
argument. That was defensible when you hadn't asked for them.
You're asking now, and the argument flips: epics are the narrative
payoff for the new model where major closure no longer grants boons.
**Epics ship in v0.0.10**, alongside the attention rework.

---

## 2. New conceptual model

| Concept | Mechanic |
|---|---|
| **Attention** | Stored point total per daemon. Earned by minor completions and major closures. Subject to decay. |
| **Level** | Tier-derived from attention. Max **4**. Can display **0** if attention decays to nothing. |
| **Boons** | Accrued by minor completions, not by major closures. At each daemon level-up, the user is prompted to add or grow a boon — they expand in scope as the relationship deepens. |
| **Major closure** | One large attention bonus (+25) + advances the daemon's epic by one chapter (if the user writes one). No boon deposit. |
| **Decay** | Attention drops over time if the daemon is neglected. Per-archetype default, overridable per-daemon. |
| **Epics** | A user-authored chapter log per daemon. Advances one chapter per major closure. Optional — daemon can run without one. |

Attention is the central currency. Boons are an output. Level is a
display. Closing a major is emotional + narrative (epic chapter,
in-voice dialog), not transactional.

---

## 3. Mechanic specifications

### 3.1 Attention & level (TIERED, MAX LEVEL 4)

**Tier thresholds** (cumulative attention required to reach each
level):

| Level | Cumulative attention | Marginal cost to reach |
|---|---|---|
| 0 | 0 (decayed) | — |
| 1 | 10 | 10 |
| 2 | 35 | 25 |
| 3 | 85 | 50 |
| 4 (max) | 160 | 75 |

Level derivation:
```kotlin
fun levelFor(attention: Int): Int = when {
    attention < 10  -> 0
    attention < 35  -> 1
    attention < 85  -> 2
    attention < 160 -> 3
    else            -> 4
}
```

**Attention above 160 is allowed.** It acts as a decay buffer —
continuing to work keeps the daemon safe from level drop. Level stays
pinned at 4 regardless of how much attention accumulates.

**Earning attention:**
- Minor completion: `attentionPoints += minor.weight`
- Major closure: `attentionPoints += 25`

**Storage** (schema v4 → v5):
```sql
ALTER TABLE daemon_state ADD COLUMN attentionPoints INTEGER NOT NULL DEFAULT 0;
ALTER TABLE daemon_state ADD COLUMN lastAttentionUpdateAt INTEGER;
ALTER TABLE daemon_state ADD COLUMN attentionDecayPerDay INTEGER;      -- null = archetype default
ALTER TABLE daemon_state ADD COLUMN attentionDecayGraceDays INTEGER;   -- null = archetype default
ALTER TABLE daemon_state ADD COLUMN minorsCompletedSinceAccrual INTEGER NOT NULL DEFAULT 0;
ALTER TABLE daemon_state ADD COLUMN minorsPerBoonAccrual INTEGER;      -- null = archetype default
ALTER TABLE daemon_state ADD COLUMN lastSeenLevel INTEGER NOT NULL DEFAULT 0;
```

`lastSeenLevel` is the trigger for the level-up boon prompt: when the
computed level exceeds `lastSeenLevel`, the UI shows the
"author/grow a boon" dialog, then updates `lastSeenLevel = current`.

**Migration backfill** for existing daemons:
```
attentionPoints = (completedMajorCount * 25)
                + sum(major.progressCount for major in daemon.majors)
lastSeenLevel = levelFor(attentionPoints)
```
This preserves rank for users coming from the old model and credits
past minor work.

### 3.2 Decay

Per-archetype defaults, overridable per-daemon. Lower-pressure
archetypes decay slower:

| Archetype | Decay/day | Grace days |
|---|---|---|
| DRILL_SERGEANT | 5 | 1 |
| COACH | 4 | 1 |
| TRICKSTER | 3 | 2 |
| CHEERLEADER | 3 | 2 |
| ORACLE | 2 | 3 |
| POET | 2 | 3 |
| GENTLE_MENTOR | 2 | 5 |
| STOIC | 1 | 5 |
| THERAPIST | 1 | 7 |
| HERMIT | 0 | 14 |

**Mechanism:** `AttentionDecayWorker` (WorkManager, ~12-hour
interval). For each daemon:
- Compute `daysSinceLastUpdate` from `lastAttentionUpdateAt`.
- If `daysSinceLastUpdate > graceDays`:
  - `attentionPoints -= (daysSinceLastUpdate - graceDays) * decayPerDay`, floored at 0.
  - `lastAttentionUpdateAt = now`.

Completing a minor or closing a major also updates
`lastAttentionUpdateAt = now`, resetting the grace clock.

**Note on level drop:** if decay drops attention below the level's
threshold, level drops. If attention drops to 0, level displays 0.
The level-up boon prompt does **not** fire on level drop (only on
gain) — the relationship survives a setback without re-asking the
user to design new boons.

**Per-daemon override UI:** "Decay" subsection in the daemon detail
edit form. Two number inputs: per-day, grace days. Empty = use
archetype default, with the resolved value shown as a hint.

### 3.3 Boon accrual from minors

Every N minor completions → +1 to the daemon's first boon. N is
per-archetype default, overridable per-daemon
(`daemon_state.minorsPerBoonAccrual`).

| Archetype | Minors per boon |
|---|---|
| DRILL_SERGEANT, COACH | 7 |
| CHEERLEADER, TRICKSTER, STOIC | 5 |
| GENTLE_MENTOR, THERAPIST, POET, ORACLE | 4 |
| HERMIT | 3 |

Auto-credit to the first boon for v0.0.10. The boon-picker dialog
(let user choose which boon receives credit) is the v0.0.12 polish.

### 3.4 Boon level-up — NEW

When the computed daemon level exceeds `lastSeenLevel`:
1. Apotheosis dialog (the existing one for major closure) gains a
   secondary path: "[Daemon] has grown."
2. The user is prompted: "Your relationship deepens. Add a larger
   favor, or grow an existing one."
3. Two affordances:
   - **Add a new boon** — opens the existing AddBoonDialog with a
     hint reminding the user "this favor matches your daemon now —
     scope it accordingly."
   - **Grow an existing boon** — opens an inline rename dialog on
     the daemon's existing boons. User edits the text in place.
4. The user can also skip — the level-up still applies; no boon
   change happens.
5. After dismissal: `lastSeenLevel = current`.

This mechanic is the explicit answer to "stay small with boons when
starting out" — the system naturally encourages restraint early,
because the user knows they'll get to grow boons later.

### 3.5 Summoning guidance

Step 5 of the summoning ritual (currently "What favor will it grant
you for the work?") gains explicit copy:

> *"Start small. As your relationship with this daemon deepens,
> you'll be invited to grow this boon or add larger ones. The first
> boon is meant to be modest — a 10-minute break, a small kindness."*

No mechanic change to summoning; just helper text.

### 3.6 Epics

**Storage** (schema v5 → v6 — combined into the one v4→v5 migration
since both ship together; revised schema is v4 → v5):
```sql
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
  this closure means in the daemon's narrative arc.
- Detail screen gains a "Scripture" section showing all chapters in
  order with their dates. Collapsible by default.
- Authoring tip per archetype: at the chapter prompt, suggest
  template language matching the daemon's voice. Skippable.

**Backup format:** `PantheonBackup` bumps to v3 to carry chapters.
v2 backups still import (new list is empty, no breakage).

---

## 4. v0.0.10 — single coherent release

**All of the following ship together:**
- Schema v4 → v5: 7 new columns on `daemon_state`, 1 new table (`epic_chapter`).
- Migration backfill of attention from past minor work + closed majors.
- Attention model: minor +weight, major +25.
- Tiered level computation (max 4, can display 0).
- Decay (`AttentionDecayWorker`, per-archetype defaults).
- Per-daemon decay override UI (detail screen edit form).
- Boon-from-minors auto-accrual (first boon, archetype-default threshold).
- Per-daemon boon-accrual-threshold override UI (detail screen edit form).
- Boon level-up prompt (on level gain only).
- Summoning step 5 guidance copy.
- Epics: `epic_chapter` table, apotheosis "write a chapter" branch,
  Scripture section on detail screen, `PantheonBackup` v3.
- Daily card level bar shows progress to next level
  (`(attention - levelThreshold) / (nextLevelThreshold - levelThreshold)`)
  rather than progress on the leading open major. Bar empty when
  level 4 (no further to climb).

Estimated risk: **high**, because of the size. Two big risks:
1. Migration backfill — getting the math wrong destroys existing
   ranks. Council needs to scrutinize §3.1.
2. The boon level-up flow introduces a new modal moment that fires
   from completion events. Easy to misfire (double-fire on
   simultaneous level + apotheosis events).

---

## 5. v0.0.11+ — QoL and remaining deferred

Not part of v0.0.10. Picked up in order of value:

- Edit quest / boon titles (current model: delete + re-add).
- Soft-delete + undo for destructive ops.
- Per-daemon notifications toggle UI on the detail screen
  (the repo + DAO + VM method already ship from v0.0.7).
- Boon-pick dialog at accrual (replace auto-credit-first behavior).
- WEEKLY cadence for minors.
- Schema cleanup: drop unused `thresholdCount`, `wishRewardCount`.
- `MigrationTestHelper` round-trip tests.
- Conversation screen + multi-beat chains (deferred from v0.0.6.1).
- Cross-daemon mechanics + `@token` flavor references.

---

## 6. What's now moot (under the new model)

| Deferred item | Status |
|---|---|
| Per-major threshold configuration UI | **Moot** — majors don't auto-close |
| Per-major wish reward configuration UI | **Moot** — major closure doesn't grant boons |
| Refund of contributions when a DAILY minor is deleted | **Lower priority** — progressCount is informational, not gating |

---

## 7. Per-archetype defaults — one summary table

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

## 8. User decisions encoded (v1 open questions resolved)

| Question | Answer |
|---|---|
| Attention bonus on major closure | +25 (kept) |
| Points per level | Tiered: 10, 25, 50, 75 → max level 4 |
| Boon-accrual recipient | Auto-credit first boon; picker is v0.0.11+ |
| Level floor | Can display 0 |
| Backfill | Credit past minor work + closed majors |
| Scope | One version (v0.0.10) covers attention/decay/boons/epics/boon-level-up |
| Council round | **Required before any code** |

---

## 9. Council brief (this round)

A council pass on this whole plan before implementation. Focus areas:

1. **Tier curve coherence** (§3.1): does 10 / 25 / 50 / 75 feel
   right? Is max level 4 a stunting cap, a focusing constraint, or a
   missed opportunity?
2. **Decay calibration** (§3.2 + §7): a Drill Sergeant daemon at
   5/day decay, 1 grace day — does that read as "tough love" or
   "shame amplifier" given the v0.0.6 dialogue council's surveillance
   guard?
3. **Boon level-up mechanic** (§3.4): a new modal moment tied to
   level changes. Authoring burden, fatigue risk, interaction with
   apotheosis dialog. Believer might fight for this hardest; Demolisher
   might call it ceremony.
4. **Migration backfill** (§3.1): existing daemons get
   `attentionPoints = completedMajorCount × 25 + sum(progressCount)`.
   Does this produce defensible ranks for current users? Risk of
   surprise demotion or sudden over-promotion?
5. **One-version-vs-split** (§4): user wants everything in v0.0.10.
   Architect: real implementation/regression risk? Skeptic: vertical
   slice would have been smaller — accept or fight?
6. **Summoning guidance copy** (§3.5): "stay small with boons" is
   product-tone advice in the ritual. Manipulative or helpful?
   Believer's call.

Five-seat round, one round, then synthesize and report.
