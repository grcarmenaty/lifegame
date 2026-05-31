# CLAUDE.md

Guidance for AI assistants (Claude Code and others) working in this repository.

## What lifegame is

**lifegame** — "A customizable app to gamify habits and objectives." A
native Android app whose reward language is **relationship**, not
points. The user authors a small pantheon of personifications
(**daemons**) that ask things of them in a voice they wrote themselves.

The product thesis, entity model, loop, MVP cut, and risks live in
[`docs/design/daemons-and-quests.md`](docs/design/daemons-and-quests.md).
**Read it before changing core mechanics.** If something in this CLAUDE.md
conflicts with the design doc, the design doc wins for product
decisions; this file wins for engineering conventions.

## Project status

Scaffold is in place. The app builds, runs, and ships:

- Guided 6-step **summoning ritual** (state preserved across rotation /
  process death via `rememberSaveable`) that creates a daemon + first
  major quest + minor quests + boon.
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

Not yet implemented (deliberately deferred per design v2 / v0.0.3
council):
- Epic chapters / scripture view
- Per-major wish reward configuration UI (the `wishBoonId` and
  `wishRewardCount` columns exist; v0.0.4 exposes them)
- Per-major threshold configuration UI for added majors (hardcoded 3)
- Editing quest/boon text (delete + re-add for now)
- Refund of contributions when a DAILY minor that has contributed is
  deleted (clean refund needs per-minor completion-count tracking)
- Cadences beyond ONE_OFF + DAILY
- Cross-daemon mechanics (including `@token` flavor references)
- Failure-handling tonal decay (greeting/completion lines stay neutral
  for now; reconciliation beats not wired up)
- Soft-delete + undo for destructive ops
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
│       │   ├── domain/dialogue/        # predicate-pool dialogue engine
│       │   │   ├── DialogueLine.kt     # line/choice/enum types
│       │   │   ├── Predicate.kt        # predicate algebra + singletons
│       │   │   ├── DialogueEngine.kt   # selector (lifeEvent → priority → exhaust)
│       │   │   ├── DialogueStateStore.kt  # one batched DAO read per pick
│       │   │   └── lines/              # per-archetype line files
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
- Build release APK (debug-signed for now): `./gradlew :app:assembleRelease`
- JVM unit tests: `./gradlew test`
- Lint: `./gradlew :app:lint`
- Install on connected device: `./gradlew :app:installDebug`

**Wrapper note:** `gradlew` and `gradle/wrapper/gradle-wrapper.jar` are
committed. If they go missing, regenerate with `gradle wrapper
--gradle-version 8.11.1 --distribution-type bin`.

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
  variants per slot at 3+ to avoid repetition fatigue. Lines must be
  template-safe — no user-content interpolation in v1.
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
