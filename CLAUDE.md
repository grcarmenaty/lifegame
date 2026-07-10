# CLAUDE.md

Guidance for AI assistants (Claude Code and others) working in this repository.

## What Personae is

**Personae** (user-facing name; the GitHub repository and internal
Gradle/package identifiers remain `lifegame` for upgrade compatibility
— see "Naming" below) — "A customizable app to gamify habits and
objectives." A native Android app whose reward language is
**relationship**, not points. The user authors a small pantheon of
personifications (**daemons**) that ask things of them in a voice
they wrote themselves.

The product thesis, entity model, loop, MVP cut, and risks live in
[`docs/design/daemons-and-quests.md`](docs/design/daemons-and-quests.md).
**Read it before changing core mechanics.** If something in this CLAUDE.md
conflicts with the design doc, the design doc wins for product
decisions; this file wins for engineering conventions.

## Project status

Scaffold is in place. The app builds, runs, and ships:

- Guided 6-step **summoning ritual** (state preserved across rotation /
  process death via `rememberSaveable`) that creates a daemon + its
  major quests + minor quests + boon. Steps 3–4 pick from the **quest
  library** (multi-select, pre-tick 3 repeating + 1 one-off per major)
  and/or author custom quests. Single-choice steps (theme, voice)
  auto-advance on tap (v0.0.16); the major picker shows no minor counts.
- **Quest library** (v0.0.14, expanded v0.0.15) in `domain/catalog/`: a
  pre-authored corpus, one file per theme under `catalog/themes/` —
  every `LifeTheme` ships **6 major quests**, each with **12 repeating**
  minors (sensible cadence) + **12 one-off** minors (harder, higher
  weight). 120 majors / 2 880 minors total, globally-unique
  `templateId`s. A daemon can be summoned with several majors at once
  (the picker hides per-major minor counts); the detail screen's
  **+ Library** button adds more later (theme-scoped). Picked quests
  persist their `templateId` on `major_quests` / `minor_quests`
  (DB v9→v10, additive). **Completion dialogue** is composed
  (`domain/dialogue/QuestCompletion.kt`): each catalog quest carries a
  quest-specific *fragment*; each archetype owns voiced *frames* with a
  `{0}` slot, so every quest reads in the daemon's voice, with several
  rotating variants for repeatables. Minor completions surface the line
  in a **Daily snackbar**; major closures use it in the apotheosis
  dialog. Custom quests fall back to the archetype's generic completion.
- **Quest editing** (v0.0.15): the detail screen edits any major/minor
  (title, cadence, weight) via pencil actions. A `fragmentOverride`
  column on both quest tables (DB v10→v11, additive) lets the user
  retune the **completion phrase** while the `templateId` — and thus the
  voiced dialogue frames — is preserved; the override also gives custom
  quests a voiced line. Summoning's reward step offers **boon
  suggestions** (`domain/BoonSuggestions.kt`, theme-flavoured). The
  face chooser is now an **expanded scrollable grid panel**, not a
  one-line ribbon.
- **Dialogue voice pass + specificity** (v0.0.18): the Daily card
  greeting is now picked from the **dialogue engine** (`OPENER`
  category, one pick per daemon per local day, cached in the VM) with
  the `VoicePreset` bank as instant render + fallback — before this,
  the entire contextual OPENER corpus only ever surfaced in
  notifications. All 10 base archetype files got a **full rewrite**
  for per-archetype cadence (the old corpus was uniformly staccato and
  narrated its own trigger predicates). New `{quest}` placeholder
  interpolates an open minor's title into lines gated on
  `HasOpenQuest` (3 new lines per archetype: 1 opener + 2 nudges);
  `DialogueLintTest` enforces gate ⇔ placeholder and that the
  placeholder is always quoted (“{quest}”) so user-authored titles
  can't break the daemon's voice — template-safe lines remain the
  fallback pool. `QuestCompletion.fill` now capitalizes fragments at
  sentence-start slots (was emitting lowercase sentence openers);
  `QuestCompletionTest` guards it. `buildContext` picks the `{quest}`
  candidate with the same `isMinorOpenNow` the Daily list uses, so the
  greeting never names a quest the list wouldn't show.
- **Undo for destructive deletes** (v0.0.17): deleting a minor, a major
  (with all its minors), or a boon on the detail screen shows an Undo
  snackbar. Repository deletes return in-memory snapshots; restores
  re-insert with original row ids (`INSERT OR IGNORE` guards the
  id-reuse race) and re-point `wishBoonId` FKs that the delete's
  SET_NULL wiped. Only the latest delete is undoable; snapshots don't
  survive process death. Vanish daemon keeps its confirm dialog.
- **Per-major threshold configuration** (v0.0.17): add/edit major
  dialogs expose "contributions to close" (was hardcoded 3 for added
  majors); the major card shows `progress / threshold` plus a
  "ready to close" marker. Closing stays a user act — the threshold is
  guidance and the attention-migration cap, never an auto-close.
- **Daily view** listing today's open minor quests grouped by daemon,
  each greeted in the daemon's voice. Tap to complete. Top-bar `+`
  re-enters the summoning ritual to add another daemon.
- **Per-daemon level bar** in the Daily card and on the detail screen:
  fill ratio = the most-progressed open major's `progressCount /
  thresholdCount` (closing it = +1 level). Empty when no open majors.
- **Daemon detail screen** (tap the tune icon on a daemon card): edit
  name / archetype / voice preset; full quest history (all majors +
  minors, completed and open); destructive **Vanish daemon** with
  confirmation (cascades to quests via the existing FK).
- **Quest CRUD** on the detail screen: add major (title; threshold
  defaults to 3); add minor under any open major (title, cadence
  one-off/daily, weight 1–9); delete major (confirm shows progress
  loss); delete minor (no refund of past contributions yet).
- **Multiple boons per daemon** via a Boons section on detail: add
  (text + initial count) and delete (confirm). Summoning still
  authors one boon. Each major implicitly grants +1 of the daemon's
  first boon on completion (per-major reward configuration column
  exists but is not exposed yet).
- **Apotheosis** on major-quest completion: derived level-up + reward
  deposited into the configured boon, in-voice dialog names the boon.
- **Wish spending** via a picker dialog reached from the chip on the
  daemon's header: lists every boon with `count > 0` and an explicit
  Spend button per row. Transactional `count > 0` guard against
  double-spend.
- **Settings screen** (gear icon on the Daily top bar): **Export**
  pantheon to a user-picked JSON file via Storage Access Framework
  (`ACTION_CREATE_DOCUMENT`); **Import** from a JSON backup (replaces
  current pantheon, confirm dialog up-front, `ACTION_OPEN_DOCUMENT`);
  **Reset** wipes the database (confirm dialog). **Backup format v2**
  (since v0.0.6) carries dialogue state (`line_seen`, `cooldown_play`,
  `daemon_state`) so a restore preserves the user's relationship
  history with each daemon, not just the authored content.
- **Hades-inspired dialogue engine** in `domain/dialogue/`: flat
  predicate-gated pool, three-tier priority (FILLER / CONTEXTUAL /
  ESSENTIAL), `requires` / `forbids` for implicit sequencing, named
  cooldown groups with surface scoping. Voice presets remain as the
  fallback when the engine can't pick a line. Inline-surface (Daily
  greeting, apotheosis dialog) wired in v0.0.6; Conversation screen
  + multi-beat chains for Drill Sergeant / Gentle Mentor / Oracle
  ship in v0.0.6.1. Council-iterated design lives in
  [`docs/design/dialogue-v0.0.6.md`](docs/design/dialogue-v0.0.6.md).
  `DialogueLintTest` (JVM unit test) enforces id uniqueness, dangling
  references, archetype-whitelist on shame-amplifier predicates,
  cross-surface cooldown on lapse-reactive lines, recencyKey enum
  order, and the load-bearing 180-min foreground floor on
  mid-day-return openers.
- **Daemon notifications** (v0.0.7) in `domain/notify/`: WorkManager
  periodic `NudgeWorker` (~2-hour interval, posts at most one
  notification per run for human-friendly rate-limiting). Notification
  text is picked from the dialogue engine via the new `NUDGE`
  `LineCategory`, so each archetype's voice carries through into its
  notifications — Drill Sergeant nags, Hermit barely speaks. Settings
  has a master toggle + quiet hours (DataStore prefs). Per-daemon
  `notificationsEnabled` lives on `daemon_state` (UI toggle added in
  v0.0.10's Tuning section). Android 13+ POST_NOTIFICATIONS permission
  requested in-app when the master toggle is first turned on. Tapping
  a notification deep-links to the daemon's detail screen via
  `MainActivity.intent` extras.
- **Attention economy** (v0.0.10) in `domain/attention/`: the central
  currency replacing major-count-as-level. Earned by minor
  completions (`+= minor.weight`) and major closures (`+= 25`).
  Tiered levels (10/25/50/75 cumulative; max level **4**; level **0**
  displayable when decayed below threshold). Above 160, attention
  accumulates as a decay buffer with a shimmer pip on the Daily card.
  **Decay**: periodic `AttentionDecayWorker` (~12h) + inline first
  step of `NudgeWorker`. Per-archetype defaults on `VoicePreset`
  (Hermit 0/day, Drill Sergeant 5/day after 1 grace day). Pauses
  when notifications are off for the daemon AND globally;
  user-toggleable per-daemon kill switch (`decayDisabled` column).
  **Boons from minors**: every N minor completions auto-credits the
  daemon's first boon; N per-archetype default, overridable per-daemon.
  Major closure **no longer** deposits boons.
- **Boon level-up prompt** (v0.0.10): when a daemon's computed level
  exceeds its `lastSeenLevel`, the Daily screen shows a persistent
  banner ("X relationships have grown"). User opens the daemon to
  add/grow a boon, or dismisses. Counter `lastSeenLevel` never
  rewinds on decay so a level-down + level-back-up doesn't re-prompt.
  Multi-level jumps fire once with the final level.
- **Epics / Scripture** (v0.0.10) in `data/entities/EpicChapter` +
  detail screen's Scripture section. Optional — daemon can run
  without any. Apotheosis dialog offers "Write a chapter" (skippable);
  text pre-fills with date + closed major title as scaffolding so
  it's edit-mode, not blank page.
- **Summoning step 5** voice variant: per-archetype "stay small with
  boons" advice (`VoicePreset.staySmallBoonAdvice`) voiced by the
  daemon being authored, not in product tone.
- **Dialogue expansion v0.0.11** (`domain/calendar/`,
  `domain/dialogue/`, `domain/UserPrefs.kt`): per-archetype line
  corpus grew from ~12 to **56-62 lines each** across all 10
  archetypes (~585 lines total — all holidays voiced per-archetype,
  no shared ANY pool). Coverage: per-level transition openers
  (`AtLevelExactly` × 4 with `lifeEvent = true`), per-completion-count
  escalation (1st through 5th minor today), attention-loss reactive
  lines (`AttentionLostAtLeast` whitelisted to harsh archetypes;
  `AttentionLostAtLeastGentle` for soft ones), day-of-week markers
  (Monday / Friday / Weekend), early-morning + late-night variants
  shaped to a WFH schedule. New `HolidayCalendar` (Catalan defaults,
  fixed dates + Gauss-Easter for Carnaval / Divendres Sant / Pasqua)
  returns a single `HolidayToken` per pick. **All 19 holidays carry
  per-archetype voicing** — the originally-loaded six (Sant Jordi,
  La Mercè, Sant Joan, Diada, Nadal, Cap d'Any) plus the 13 the
  council had originally flagged as "lesser" but the user flagged as
  important (Reis, Carnaval, Divendres Sant, Pasqua, Festa del
  Treball, Festa Major de Gràcia, Assumpció, Hispanitat, Castanyada,
  Constitució, Immaculada, Sant Esteve, Cap d'Any Eve). All
  holiday lines are **secular in framing** even where the holiday's
  origin is religious — `Divendres Sant` reads as "a quiet day, the
  city pauses", `Reis` as "gift day", `Nadal` as family / winter, no
  references to deity, prayer, or scripture in any line. **Location
  is user-selectable** via a Settings dropdown backed by
  `SupportedRegion` enum; v0.0.11 ships with `BARCELONA` as the only
  valid value and `HolidayCalendar.tokenFor` returns null for any
  other region. The dropdown surface makes the extension shape
  visible without faking unsupported coverage. Birthday is opt-in
  via `UserPrefs` (MM-DD); personal dates via new `personal_date`
  Room table with Settings list (add/delete). Personal-date lines
  carry `{label}` and the repository's `renderLine` interpolates the
  user's authored text. Backup format bumped v3 → v4 (additive —
  older backups still load). DB schema v5 → v6 adds the table + two
  `daemon_state` columns (`lastDecayAmount`, `lastDecayAt`) so
  attention-loss predicates can apply a 24h freshness window.
  `DialogueLintTest` gains three new asserts: holiday lines must use
  `RecencyKey.TODAY`, personal-date lines must contain `{label}`,
  each per-archetype file ≥ 30 lines. `buildContext` finally wires
  `minorsCompletedToday`, `minorsCompletedThisWeek`, and
  `dailyMinorsLapsedCount` from real DAO reads (the v0.0.6.1
  deferrals).

Not yet implemented (deliberately deferred per design v2 / v0.0.3
council):
- Epic chapters / scripture view
- Per-major wish reward configuration UI (the `wishBoonId` and
  `wishRewardCount` columns exist; v0.0.4 exposes them)
- Editing quest/boon text (delete + re-add for now)
- Refund of contributions when a DAILY minor that has contributed is
  deleted (clean refund needs per-minor completion-count tracking)
- Custom-interval cadences (every N days) — v0.0.11 ships
  ONE_OFF / DAILY / WEEKLY / MONTHLY; custom intervals deferred
- Cross-daemon mechanics (including `@token` flavor references)
- Failure-handling tonal decay (greeting/completion lines stay neutral
  for now; reconciliation beats not wired up)
- Undo for Vanish daemon (quest/boon deletes got undo in v0.0.17; the
  daemon's graph also spans dialogue state + epics, and its undo window
  would have to survive the navigation pop — needs a design pass)
- Unit + UI tests (only the scaffolding directories exist;
  `MigrationTestHelper` round-trip for 1 → 2 is a tracked follow-up)

## Tech stack

- **Kotlin 2.0.21** with the Kotlin **Compose** compiler plugin (not the
  old AGP-managed `composeOptions { kotlinCompilerExtensionVersion }`).
- **AGP 8.7.3**, **Gradle 8.11.1** (pinned in
  `gradle/wrapper/gradle-wrapper.properties`).
- **Jetpack Compose** with Material 3, Compose BOM `2024.12.01`.
- **Navigation Compose** for the (currently 3-route) graph.
- **Room 2.6.1** via **KSP** (`com.google.devtools.ksp`) — *not* kapt.
- **kotlinx.serialization 1.7.3** for the JSON backup format. Room
  entities carry `@Serializable` directly (single source of truth);
  `BuildConfig.VERSION_NAME` stamps the export.
- **Manual DI**: `LifegameApplication.repository` is the single source.
  No Hilt yet. Don't introduce it until a second component needs it.
- **JDK 17** target; the CI matrix is JDK 17 on `ubuntu-latest`.
- **minSdk 26**, **target/compileSdk 35**.

All versions live in `gradle/libs.versions.toml`. Add new dependencies
there with a named alias and reference them via `libs.x.y` in
`app/build.gradle.kts`.

## Repository layout

```
lifegame/
├── app/                                 # the only module
│   ├── build.gradle.kts
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/com/grcarmenaty/lifegame/
│       │   ├── LifegameApplication.kt   # owns the DB + repository
│       │   ├── MainActivity.kt          # single activity, Compose entry
│       │   ├── data/                    # Room entities + DAOs + DB
│       │   ├── domain/                  # VoicePreset, PantheonRepository
│       │   ├── domain/calendar/        # holiday token + Catalan calendar
│       │   ├── domain/dialogue/        # predicate-pool dialogue engine
│       │   │   ├── DialogueLine.kt     # line/choice/enum types
│       │   │   ├── Predicate.kt        # predicate algebra + singletons
│       │   │   ├── DialogueEngine.kt   # selector (lifeEvent → priority → exhaust)
│       │   │   ├── DialogueStateStore.kt  # one batched DAO read per pick
│       │   │   └── lines/              # per-archetype line files (+ AnyLines)
│       │   └── ui/                      # theme + per-screen packages
│       │       ├── nav/                 # navigation graph
│       │       ├── common/              # cross-screen composables
│       │       ├── summoning/           # onboarding ritual
│       │       ├── daily/               # today's quests
│       │       ├── detail/              # daemon edit + history + vanish
│       │       └── settings/            # export / import / reset
│       └── res/
├── docs/design/                         # design docs (v1, v2…)
├── gradle/
│   ├── libs.versions.toml               # version catalog
│   └── wrapper/                         # wrapper + JAR (committed)
├── .github/workflows/
│   ├── ci.yml                           # debug APK on push/PR
│   └── release.yml                      # release APK on `v*` tag
└── CLAUDE.md
```

## Development workflows

All commands assume the repo root.

- Configure / sanity-check: `./gradlew :app:tasks`
- Build debug APK: `./gradlew :app:assembleDebug`
- Build release APK (signed with the committed keystore): `./gradlew :app:assembleRelease`
- Kotlin compile only (faster than full build): `./gradlew :app:compileDebugKotlin`
- JVM unit tests: `./gradlew test`
- Lint: `./gradlew :app:lint`
- Install on connected device: `./gradlew :app:installDebug`

**Wrapper note:** `gradlew` and `gradle/wrapper/gradle-wrapper.jar` are
committed. If they go missing, regenerate with `gradle wrapper
--gradle-version 8.11.1 --distribution-type bin`.

### MANDATORY pre-push build discipline

**`./gradlew :app:tasks` only validates Gradle configuration. It does
NOT run the Kotlin compiler, KSP, or any Room schema validation.** It
will happily pass while the codebase has dangling references, missing
imports, removed types still being called, or schema-vs-entity drift.
The v0.0.10 release blew up on `compileReleaseKotlin` with seven
errors after I refactored a data class — `:app:tasks` had said the
project was "clean."

**Rule: before every push that touches `.kt`, `.kts`, schema, or
manifest files, run at minimum:**

```
./gradlew :app:compileDebugKotlin
```

This is fast (~30s incremental, ~2min cold) and catches the entire
class of "I renamed/removed a type but missed a caller" errors that
`:app:tasks` misses. For changes touching Room entities/DAOs/migrations,
also run `./gradlew :app:kspDebugKotlin` (Room schema validation) and
`./gradlew :app:testDebugUnitTest` (the dialogue/attention lint tests).
For changes touching Compose, prefer `:app:assembleDebug` which
exercises resource processing too.

**Android SDK requirement:** the SDK is not always present in the
ephemeral sandbox. Install with:
```
mkdir -p /tmp/android-sdk/cmdline-tools && cd /tmp/android-sdk/cmdline-tools
curl -sSL https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip -o cmdline.zip
unzip -q cmdline.zip && mv cmdline-tools latest
export ANDROID_HOME=/tmp/android-sdk
export PATH="$ANDROID_HOME/cmdline-tools/latest/bin:$PATH"
yes | sdkmanager --licenses >/dev/null
sdkmanager "platform-tools" "platforms;android-35" "build-tools;35.0.0"
echo "sdk.dir=/tmp/android-sdk" > local.properties
```
The `local.properties` is gitignored. Clean it up before commit if
needed.

**Common refactor-time failure modes the rule catches:**
1. Removed a data class field → all read sites are stale references.
2. Renamed a property → call sites unresolved.
3. Removed a top-level type → composables still take it as a parameter.
4. Removed an enum value → `when` statements lose exhaustiveness.
5. Renamed/moved a function → callers fail import resolution.
6. Added a Room entity field without a matching migration ALTER →
   schema mismatch (caught by `kspDebugKotlin` + runtime validator).

**This rule is unconditional. There is no class of edit small enough
to skip it for.** A one-line cosmetic edit can break compilation
(e.g., changing a `when` branch on a sealed class). The build cost is
trivial compared to a failed release and the recovery turn it forces.

### Naming

- **User-facing app name** (Android launcher label, README headline,
  CLAUDE.md headline): **Personae**.
- **GitHub repository slug**: `lifegame` (stays — renaming the repo
  would break every commit URL, the release workflow's existing tags,
  every previously-shipped APK download link).
- **Gradle root project name** (`settings.gradle.kts`): `personae`
  (renamed v0.0.11). Internal-only; affects `:app` module paths in
  `./gradlew` invocations? No — `:app` stays `:app`. Affects nothing
  user-visible.
- **Android `applicationId` and Kotlin package** (`com.grcarmenaty.lifegame`):
  unchanged. Changing them would make the new APK a different app
  from Android's perspective (different launcher icon, no upgrade
  path over installed v0.0.10 builds) — which directly fights the
  in-place-update guarantee we set up with the stable keystore in
  v0.0.8. If a full package rename is wanted later, it'd be a
  deliberate one-time uninstall cost like v0.0.7 → v0.0.8 was.
- **Database file** (`lifegame.db`): unchanged for the same upgrade-
  compat reason.
- **Keystore file** (`app/lifegame.keystore`): unchanged. The
  filename doesn't appear in the APK; renaming it would require
  updating `signingConfigs.release.storeFile` and adds zero value.

## CI / Release

- **`.github/workflows/ci.yml`** runs on every push to `main` and
  `claude/**`, and on every PR. Builds a debug APK and uploads it as a
  workflow artifact (`lifegame-debug-<sha>`).
- **`.github/workflows/release.yml`** cuts releases two ways:
  - **Auto** — on every push to `main`, it reads `versionName` from
    `app/build.gradle.kts`; if tag `v<version>` doesn't exist yet, it
    builds the release variant and publishes a GitHub Release, creating
    the tag from inside Actions via `GITHUB_TOKEN`. So bumping the
    version and merging to `main` *is* the release.
  - **Manual** — pushing a `v*` tag yourself always releases that
    commit (fallback path; useful for re-releasing or tagging an
    arbitrary ref).
  Either way the APK is attached with auto-generated notes.

**Signing:** the `release` build type is wired to a **stable keystore
committed to the repo** at `app/lifegame.keystore` (since v0.0.8). All
credentials are intentionally public (`storePassword = keyPassword =
"lifegame"`, alias `lifegame`). This is fine because:

- The app is distributed via GitHub Releases, not Play Store.
- The keystore lets Android verify "this update is from the same
  app" — that's the whole purpose. The previous setup used the AGP
  auto-generated `~/.android/debug.keystore`, which on CI was a fresh
  random keystore per run, breaking in-place updates.
- A stable keystore committed in the open is exactly equivalent in
  threat model to Android's standard `debug.keystore` (everyone has
  it, password is "android"). Anyone who wants to sign a malicious
  APK can do so trivially with their own keystore anyway; the
  signature only ever matters to the user's existing install
  accepting an update.

**To upgrade from v0.0.7 to v0.0.8 you must uninstall once** — the
signature differs from earlier CI-debug-keystore builds. v0.0.8
onward installs in place forever.

**If you ever ship to Play Store**, swap to a private keystore via
GitHub Actions secrets:

1. Generate a fresh keystore: `keytool -genkey -v -keystore release.jks -keyalg RSA -keysize 2048 -validity 10000 -alias lifegame`
2. Base64-encode it and store as the GitHub secret `RELEASE_KEYSTORE_B64`.
3. Store `RELEASE_KEYSTORE_PASSWORD`, `RELEASE_KEY_ALIAS`,
   `RELEASE_KEY_PASSWORD` as secrets.
4. In `app/build.gradle.kts` `signingConfigs.release`, read from
   `System.getenv("RELEASE_KEYSTORE_PATH")` etc.
5. Decode the keystore in `release.yml` before `assembleRelease`,
   delete `app/lifegame.keystore`, re-ignore it.

To cut a release, bump the version and merge to `main`:

```
# edit app/build.gradle.kts: versionName = "0.0.2"
git commit -am "Bump versionName to 0.0.2"
git push origin <branch>   # then merge/push to main
```

The push to `main` triggers the release. Pushing a `v*` tag manually
still works as a fallback.

## Conventions

- **Language:** Kotlin, official style. ktlint/detekt not yet wired up
  — propose one when adding it; commit the config alongside.
- **UI:** Jetpack Compose only. Don't introduce XML layouts.
- **Async:** coroutines + `Flow`. No RxJava.
- **DI:** manual via `LifegameApplication`. Don't add Hilt yet.
- **Persistence:** Room for structured data; DataStore (Preferences)
  for settings when we need them. No raw `SharedPreferences`.
- **Schema migrations:** from v2 onwards `exportSchema = true` and the
  KSP arg `room.schemaLocation = $projectDir/schemas` writes JSON
  schemas under `app/schemas/`. Commit the v_N_ JSON when bumping the
  schema version and write a hand-rolled `Migration` that matches it
  byte-for-byte (recreate-table for column drops or FK-add, since
  SQLite < 3.35 can't drop columns natively and `ALTER ... ADD ...
  REFERENCES` is inert on existing rows). Don't ship destructive
  migrations.
- **Architecture:** single-module. Split into feature modules only when
  build time or ownership demands it.
- **Voice presets:** new presets go in `domain/VoicePreset.kt`. Keep
  variants per slot at 3+ to avoid repetition fatigue. Lines are
  template-safe by default; the only sanctioned interpolations are
  `{label}` (personal dates) and `{quest}` (open minor title, must be
  quoted “{quest}” and gated on `HasOpenQuest` — lint-enforced).
- **Comments:** explain *why*, not *what*. Named identifiers describe
  what the code does.

## Files never to commit

Already in `.gitignore`; do not bypass it:

- `local.properties` (absolute SDK paths)
- `*.jks`, `*.keystore` (signing material — use GitHub secrets)
- `google-services.json` (Firebase config — may contain API keys)
- Anything under `build/`, `.gradle/`, `.idea/`

If a secret needs to be shared, route it through GitHub Actions secrets
or Gradle properties — never check it in.

## Git workflow

- `main` is the default branch.
- AI-generated work goes on the harness-assigned `claude/...` branch.
- **Do not push to `main` directly.** Push to the assigned branch; the
  user opens the PR.
- Don't open PRs automatically unless explicitly asked.
- Commit messages explain *why*; the diff covers *what*.

## When updating this file

- Keep it short and current. Delete sections that no longer apply.
- If reality contradicts something here, **fix the doc** (or ask before
  changing code that's load-bearing).
- The design doc (`docs/design/daemons-and-quests.md`) is the source of
  truth for product decisions; this file is the source of truth for
  engineering conventions.
