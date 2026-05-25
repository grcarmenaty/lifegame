# CLAUDE.md

Guidance for AI assistants (Claude Code and others) working in this repository.

## Project status

**This repository is a greenfield scaffold.** As of the most recent commit it
contains only:

- `README.md` — one-line description of the project
- `LICENSE` — MIT
- `.gitignore` — Android / Gradle / Android Studio / IntelliJ ignores

There is **no application code, build configuration, or test infrastructure
yet**. Anything below describing structure, workflows, or conventions is
either (a) inferred from the `.gitignore` and README or (b) a convention to
adopt as soon as real code lands. When the codebase grows, update this file
accordingly — do not let it drift.

## What the project is

**lifegame** — "A customizable app to gamify habits and objectives." (per
`README.md`).

Beyond that one line, product scope, target users, and feature set are not
yet documented. If you need to make assumptions to move forward, surface
them in your reply and confirm with the user rather than baking them into
code silently.

## Inferred tech stack

The `.gitignore` is the only signal about intended tooling. It ignores:

- `.gradle/`, `build/` — **Gradle** build system
- `local.properties` — Android SDK path file
- `captures/`, `.externalNativeBuild/`, `.cxx/`, `*.aab`, `*.apk`,
  `output-metadata.json` — **Android Studio** outputs
- `*.iml`, `.idea/`, `misc.xml`, `deploymentTargetDropDown.xml`,
  `render.experimental.xml` — **IntelliJ / Android Studio** project files
- `*.jks`, `*.keystore` — Android signing keystores
- `google-services.json` — Firebase / Google APIs config
- `*.hprof` — Android profiler heap dumps

**Conclusion:** the project is set up for **native Android development**
(likely Kotlin, possibly with Firebase). When scaffolding, default to:

- Kotlin + Jetpack Compose unless the user says otherwise
- Gradle (Kotlin DSL — `build.gradle.kts`) over Groovy
- Android Studio's standard module layout (`app/`, root `settings.gradle.kts`,
  `gradle/libs.versions.toml` version catalog)

Confirm before introducing anything else (Flutter, React Native, KMP, etc.)
— the `.gitignore` does not anticipate those toolchains.

## Repository layout

```
lifegame/
├── .gitignore     # Android-oriented ignores
├── LICENSE        # MIT, © 2026 grcarmenaty
├── README.md      # One-line project description
└── CLAUDE.md      # This file
```

Once Android scaffolding is added, the expected layout is roughly:

```
lifegame/
├── app/
│   ├── build.gradle.kts
│   └── src/
│       ├── main/        # Production code + resources + AndroidManifest.xml
│       ├── test/        # JVM unit tests
│       └── androidTest/ # Instrumented tests
├── gradle/
│   └── libs.versions.toml
├── build.gradle.kts
├── settings.gradle.kts
└── gradlew, gradlew.bat
```

Update this section when real directories exist.

## Development workflows

None are wired up yet. The placeholders below are what to use **once** an
Android Gradle project is initialized. Do not invent passing commands before
the files exist.

- Build debug APK: `./gradlew assembleDebug`
- Run JVM unit tests: `./gradlew test`
- Run instrumented tests (device/emulator required): `./gradlew connectedAndroidTest`
- Lint: `./gradlew lint`
- Static checks / formatting: not yet chosen — propose **ktlint** or
  **detekt** when adding them, and document the chosen command here.

If you add or change any of these, edit this section in the same commit.

## Conventions to adopt

These are starting defaults; revise as the project takes shape.

- **Language:** Kotlin, target the latest stable JDK supported by AGP.
- **UI:** Jetpack Compose; avoid mixing with legacy View XML unless there's
  a concrete reason.
- **Architecture:** Prefer a single-module start; split into feature/data
  modules only when there's real pressure to (don't pre-architect).
- **Async:** Kotlin coroutines + `Flow`. Avoid RxJava.
- **DI:** Hilt is the default Android choice; don't introduce one until a
  second component needs it.
- **Persistence:** Room for structured local data; DataStore (Preferences)
  for simple key-value.
- **Testing:** JUnit4 + Truth/AssertJ for unit tests, Compose UI testing
  framework for screens. Write tests alongside features, not after.
- **Style:** Follow the official Kotlin style guide; let ktlint/detekt
  enforce it once installed.

## Files never to commit

These are already in `.gitignore` — do not bypass it:

- `local.properties` (contains absolute SDK paths)
- `*.jks`, `*.keystore` (signing material)
- `google-services.json` (Firebase config — may contain API keys)
- Anything under `build/`, `.gradle/`, `.idea/`

If a secret needs to be shared between contributors, route it through
Gradle properties or a secrets manager — never check it in.

## Git workflow

- `main` is the default branch.
- Feature work happens on topic branches; for AI-generated work the
  harness assigns a `claude/...` branch (e.g. `claude/claude-md-docs-Q8bUL`).
- **Do not push to `main` directly.** Push to the assigned branch and let
  the user open the PR — do not open PRs automatically unless explicitly
  asked.
- Write commit messages that explain *why*, not *what*. The diff covers
  *what*.

## When updating this file

- Keep it short and current. Delete sections that no longer apply rather
  than letting stale guidance accumulate.
- If you discover an established pattern in the code that contradicts
  something here, **change the doc** (or ask before changing the code).
- Don't list every file or every command — list the ones an AI assistant
  is likely to get wrong without guidance.
