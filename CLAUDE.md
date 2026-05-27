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

Scaffold is in place. The app builds, runs, and ships the v1 MVP loop:

- Guided 6-step **summoning ritual** that creates a daemon + first major
  quest + minor quests + boon.
- **Daily view** listing today's open minor quests grouped by daemon,
  each greeted in the daemon's voice. Tap to complete.
- **Apotheosis** on major-quest completion: derived level-up, +1 wish,
  in-voice dialog.
- **Wish/boon** spending via the chip in the daemon's header.

Not yet implemented (deliberately deferred per design v2):
- Epic chapters / scripture view
- Multiple wish types per daemon, wish-nature scaling
- Cross-daemon mechanics (including `@token` flavor references)
- Repeatable-minor cadence beyond `DAILY`
- Failure-handling tonal decay (greeting/completion lines stay neutral
  for now; reconciliation beats not wired up)
- Settings, daemon editing/archiving, more than one major-quest workflow
- Unit + UI tests (only the scaffolding directories exist)

## Tech stack

- **Kotlin 2.0.21** with the Kotlin **Compose** compiler plugin (not the
  old AGP-managed `composeOptions { kotlinCompilerExtensionVersion }`).
- **AGP 8.7.3**, **Gradle 8.11.1** (pinned in
  `gradle/wrapper/gradle-wrapper.properties`).
- **Jetpack Compose** with Material 3, Compose BOM `2024.12.01`.
- **Navigation Compose** for the (currently 3-route) graph.
- **Room 2.6.1** via **KSP** (`com.google.devtools.ksp`) — *not* kapt.
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
│       │   └── ui/                      # theme + per-screen packages
│       │       ├── nav/                 # navigation graph
│       │       ├── summoning/           # onboarding ritual
│       │       └── daily/               # today's quests
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
- **`.github/workflows/release.yml`** runs on tag push matching `v*`
  (e.g., `v0.1.0`). Builds the release variant, attaches the APK to a
  GitHub Release with auto-generated notes.

**Signing:** the `release` build type is currently wired to the **debug
keystore** (see `app/build.gradle.kts`). That keeps CI builds installable
without secrets, but the resulting APKs are not Play-Store-signable.
When you're ready for real release signing:

1. Generate a keystore: `keytool -genkey -v -keystore release.jks -keyalg RSA -keysize 2048 -validity 10000 -alias lifegame`
2. Base64-encode it and store as the GitHub secret `RELEASE_KEYSTORE_B64`.
3. Store `RELEASE_KEYSTORE_PASSWORD`, `RELEASE_KEY_ALIAS`,
   `RELEASE_KEY_PASSWORD` as secrets.
4. Add a `signingConfigs.release { ... }` block in
   `app/build.gradle.kts` that reads from those secrets via env vars,
   and switch `buildTypes.release.signingConfig` to it.
5. Decode the keystore in `release.yml` before `assembleRelease`.

To cut a release from a branch:

```
git tag v0.1.0
git push origin v0.1.0
```

The workflow does the rest.

## Conventions

- **Language:** Kotlin, official style. ktlint/detekt not yet wired up
  — propose one when adding it; commit the config alongside.
- **UI:** Jetpack Compose only. Don't introduce XML layouts.
- **Async:** coroutines + `Flow`. No RxJava.
- **DI:** manual via `LifegameApplication`. Don't add Hilt yet.
- **Persistence:** Room for structured data; DataStore (Preferences)
  for settings when we need them. No raw `SharedPreferences`.
- **Schema migrations:** v1 ships with `exportSchema = false` and one
  database version. Before bumping the schema version, set
  `exportSchema = true`, commit the schema JSON, and write a
  `Migration`. Don't ship destructive migrations.
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
