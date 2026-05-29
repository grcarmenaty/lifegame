# v0.0.3 — Quest CRUD, Multi-boon Wishes, Decoupled Economics

> Status: pre-council. Iterates on `daemons-and-quests.md` v2. The product
> thesis is unchanged; this doc covers six concrete mechanic changes the
> user requested after using v0.0.2.

## Feature set

### A. Quest CRUD on demand
- **Add** a major quest at any time from the daemon detail screen.
- **Add** a minor quest under any open major from detail.
- **Delete** a major (cascades to minors) or a single minor.
- Edit deferred — for v0.0.3, delete + re-add is the workflow for renames.

### B. Cadence chooser at minor creation
- ONE_OFF (default) or DAILY (recurring once per local day).
- Cadence already exists on the entity; this just exposes it in the form.

### C. Customizable quest weight
- Each minor contributes `weight` (default **1**) to its parent major's
  progress on completion.
- Each major has `thresholdCount` (default **3**). The summoning ritual
  keeps its existing override (threshold = count of minors authored).

### D. Multiple boons per daemon
- New entity `Boon(id, daemonId, text, count, createdAt)`. A daemon
  has 1+ boons.
- The summoning ritual still authors **one** boon (no change there).
- Detail screen exposes a Boons section with add/delete.
- Spending UI shows all boons with `count > 0`; the user picks one.

### E. Level / wish decoupling
- Level stays derived from completed-major count. **No change.**
- Each major specifies its **wish reward** at creation:
  - `wishBoonId: Long?` — which boon completing this major fills.
    `NULL` = no wish granted, level-up only.
  - `wishRewardCount: Int = 1` — how many.
- Defaults at major creation: `wishBoonId = the daemon's first boon`,
  `wishRewardCount = 1`. Identical visible behavior to v0.0.2 unless
  the user overrides.

### F. Confirm before spending
- Tapping the wish chip opens a picker listing every boon with
  `count > 0`, each row showing `boon text · N available`.
- Tapping a boon row reveals an explicit **Spend** button. No
  silent-on-tap.

## Data migration (1 → 2)

Non-destructive. Runs once on first launch after the upgrade.

```sql
CREATE TABLE boons (
  id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
  daemonId INTEGER NOT NULL,
  text TEXT NOT NULL,
  count INTEGER NOT NULL,
  createdAt INTEGER NOT NULL,
  FOREIGN KEY(daemonId) REFERENCES daemons(id) ON DELETE CASCADE
);
CREATE INDEX index_boons_daemonId ON boons(daemonId);

INSERT INTO boons (daemonId, text, count, createdAt)
  SELECT id, boonText, wishesAvailable, createdAt FROM daemons;

ALTER TABLE major_quests ADD COLUMN wishBoonId INTEGER REFERENCES boons(id) ON DELETE SET NULL;
ALTER TABLE major_quests ADD COLUMN wishRewardCount INTEGER NOT NULL DEFAULT 1;

UPDATE major_quests SET wishBoonId = (
  SELECT id FROM boons WHERE boons.daemonId = major_quests.daemonId LIMIT 1
);

-- Recreate daemons sans the legacy boon columns
CREATE TABLE daemons_new (
  id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
  name TEXT NOT NULL,
  archetype TEXT NOT NULL,
  voicePreset TEXT NOT NULL,
  createdAt INTEGER NOT NULL
);
INSERT INTO daemons_new SELECT id, name, archetype, voicePreset, createdAt FROM daemons;
DROP TABLE daemons;
ALTER TABLE daemons_new RENAME TO daemons;
```

`exportSchema = true` from here forward; v2 schema JSON committed.

## UX defaults — explicit list

| Surface | Default | Override |
|---|---|---|
| Minor cadence at creation | ONE_OFF | Radio: ONE_OFF / DAILY |
| Minor weight | 1 | Number stepper |
| Major threshold | 3 | Number stepper |
| Major wish reward boon | daemon's first boon | Dropdown incl. "None" |
| Major wish reward count | 1 | Number stepper |
| Boon initial count | 0 | Number stepper |
| Wish spend | requires explicit Spend tap | n/a (no skip) |

## Open questions for council

1. **Picker for one boon** — when a daemon has only one boon with
   `count > 0`, do we still force the boon-picker step or fast-path to
   confirm? Forcing is consistent; fast-path is friendlier. Lean
   towards forcing for predictability.
2. **Major threshold default of 3** — is this empirically right, or
   should it match "number of minors authored so far on this major"
   like summoning does?
3. **"None" reward option** — is it useful enough to expose, or does
   it just confuse the form? (Use case: a major that's pure narrative
   /level chase, no wish.) Lean towards keeping; it's the entire
   reason for decoupling.
4. **Boon `count` is unbounded** — could the user inflate wishes by
   adding boons with high initial counts? Yes, intentionally — this
   is a user-authored economy, not a guarded one. Per design v2 we
   accepted self-cheatability.

## Out of scope for v0.0.3
- Editing quest titles, thresholds, or cadences (delete + re-add)
- Cadences beyond ONE_OFF + DAILY (WEEKLY, etc.)
- Daily quest targets (e.g., "3× per day")
- Cross-daemon wish references
- Failure-handling tonal decay
- Multi-language voice-preset variants
