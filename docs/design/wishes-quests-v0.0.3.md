# v0.0.3 — Quest CRUD, Multi-boon Wishes, Decoupled Economics (post-council)

> Status: post-council revision. Iterates on `daemons-and-quests.md`
> v2. The pre-council draft proposed exposing every knob; the council
> (Demolisher, Skeptic, Architect in particular) argued that exposed
> knobs corrode the metaphor even when defaults are sane. This
> revision ships the user's six asks at the **model layer**, trims
> the configurability **surface**, and lays groundwork for v0.0.4
> without paying the loot-table tax today.

## User asks

1. Add major + minor quests as I wish.
2. Choose one-off vs repeating at minor creation.
3. Confirm before spending a wish.
4. Several wishes per daemon.
5. Customize how much a quest advances a major.
6. Level decoupled from wish count. Defaults customizable.

## What ships

### A. Quest CRUD — full
- Add major (title only — threshold hardcoded to 3 for added majors).
- Add minor (title + cadence + weight stepper).
- Delete major (cascade to minors). Confirm dialog states "X of Y
  minors complete" so the cost is visible.
- Delete minor. No refund of past contributions to the parent
  major's `progressCount` for now — see "Accepted limitations".
- **Blocked:** adding minors to a major that's already completed (the
  Architect's loop-hole guard).

### B. Cadence chooser — full
- Radio at minor creation: **ONE_OFF** (default) or **DAILY**.

### C. Customizable weight — partial
- **Ships:** minor weight stepper at creation (default 1, range 1–9).
- **Cut from UI:** major threshold stepper. Hardcoded 3 for added
  majors; summoning keeps its existing default (threshold = number of
  authored minors).
- **Rationale:** the council's Demolisher and Skeptic agree: a
  threshold stepper *teaches* the user the number is arbitrary,
  which collapses the ritual frame. Weight is conceptually cheaper
  ("how much does this matter?") and survives the test.

### D. Multiple boons per daemon — model + minimal UI
- New entity `Boon(id, daemonId, text, count, createdAt)`.
- Summoning still authors **one** boon (no change).
- Detail screen exposes a **Boons** section: add (text + initial
  count, default 0) and delete (confirm). No edit; delete + re-add.
- Spending picker shows every boon with `count > 0`. Even when a
  daemon has only one boon, the picker still appears as a single-row
  confirmation — per the Believer, the picker *is* the ritual surface.

### E. Level / wish decoupling — model only
- Columns ship: `major_quests.wishBoonId` (Long?, FK to `boons` with
  `ON DELETE SET NULL`) and `major_quests.wishRewardCount` (Int = 1).
- **Cut from UI:** per-major reward config. At major creation we
  default `wishBoonId = the daemon's first boon` and
  `wishRewardCount = 1`. No stepper, no boon dropdown, no "None"
  option in v0.0.3.
- **Rationale:** the Architect cut the stepper as a knob without a
  use case; the Demolisher called per-major reward config the worst
  Habitica-drift offender. The Believer's point — that decoupling
  matters — is honored by the column existing; v0.0.4 exposes it
  once a user actually asks for a no-wish major.

### F. Confirm before spend — full
- Tapping the wish chip opens a picker dialog listing every boon
  with `count > 0`, each row showing text + `N available` + an
  explicit **Spend** button.
- Apotheosis dialog names the granted boon ("As promised — *<boon
  text>*.") — Ally polish #5.
- Spend wrapped in a Room `@Transaction` with a `count > 0` guard
  per the Architect's race note.

## Data migration (1 → 2)

The Architect insisted: byte-for-byte match against Room's generated
v2 schema. We enable `exportSchema = true`, set
`ksp { arg("room.schemaLocation", "${projectDir}/schemas") }` and
commit the v2 JSON. The migration itself:

1. `PRAGMA foreign_keys = OFF` at top, `PRAGMA foreign_key_check` at
   bottom (the documented Room recipe).
2. Create `boons` (matches the entity's generated schema exactly).
3. Seed `boons` from each existing daemon's `(boonText,
   wishesAvailable, createdAt)`.
4. Recreate `major_quests` to add `wishBoonId` (FK SET NULL) and
   `wishRewardCount` (default 1); set `wishBoonId = first boon of
   the major's daemon` during copy.
5. Recreate `daemons` without `boonText` / `wishesAvailable`.

Manual table recreation (vs. naked `ALTER ... ADD ... REFERENCES`)
because SQLite's column-add with FK is inert on existing rows and
fails Room's schema validator.

## Polish adopted from the Ally

- Apotheosis dialog names the boon granted.
- Major card shows a subtle "→ grants *<boon text>*" line when the
  major has a `wishBoonId`.

## Polish deferred

- In-voice "Spend" copy (Ally #1) — needs a new voice slot per
  preset; defer to v0.0.4 alongside reconciliation beats.
- Daemon-voice prompt when authoring boons (Ally #2) — uses templated
  text in v0.0.3, voice integration later.

## Architect risks honored

- Migration uses `runInTransaction` + `PRAGMA foreign_keys=off`.
- Recreate-table for `daemons` and `major_quests`, both matching
  Room's generated v2 schema byte-for-byte.
- Spend wrapped in `@Transaction` with `count > 0` guard.
- Adding minors to a completed major is disabled in UI (`+ minor`
  button hidden when `major.completed`).
- Delete-major dialog states the progress that will be lost.

## Accepted limitations (deferred to v0.0.4)

- **Deleting a DAILY minor that has contributed** leaves prior
  contributions in `major.progressCount`. Clean refund requires a
  per-minor completion counter; out of scope.
- **No undo** on quest or boon delete. The Skeptic warned about
  history loss — for v0.0.3, the confirmation dialog is the safety
  net; soft-delete + restore is v0.0.4.
- **MigrationTestHelper instrumented test** — recommended by the
  Architect; not wired up in v0.0.3 because we don't yet have any
  test infrastructure. Tracked as a follow-up.

## Out of scope for v0.0.3 (Demolisher cuts honored)

- Per-major `wishRewardCount` UI stepper.
- Per-major `wishBoonId` UI dropdown.
- Per-major `thresholdCount` UI stepper for added majors.
- "None" wish-reward option.
- Cadences beyond ONE_OFF + DAILY.
- Editing quest titles, threshold, weight, or boon text.
- Recompute-on-write for `progressCount`.
- Cross-daemon wish references.
- Failure-handling tonal decay.
