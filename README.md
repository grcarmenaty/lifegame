# lifegame

A customizable app to gamify habits and objectives — but the reward
language is **relationship**, not points.

You author a small pantheon of **daemons**: personifications of the
parts of your life you want to hear from. Each daemon is a questgiver
with its own voice, its own goals, and its own favors to grant. Your
productivity becomes a negotiation with this inner pantheon, not a
checklist with confetti.

The full product thesis, entity model, and design history (post-council
critique and revision) live in
[`docs/design/daemons-and-quests.md`](docs/design/daemons-and-quests.md).

## Status

Android scaffold is in place. The v1 MVP loop ships:

- 6-step **summoning ritual** that creates a daemon + first major quest
  + minor quests + a boon, in under 3 minutes
- **Daily view** of today's open minor quests, grouped by daemon,
  greeted in the daemon's voice
- **Apotheosis** on major-quest completion: derived level-up, wish
  granted, in-voice dialog
- **Wish/boon** spending from the daemon's header chip

Not yet shipped: epic chapters, multi-wish economies, cross-daemon
mechanics, tonal failure-handling. Tracked in the design doc.

## Download

- **Tagged releases (signed APK):**
  https://github.com/grcarmenaty/lifegame/releases — once a `v*` tag is
  pushed, the [release workflow](.github/workflows/release.yml) builds
  the APK and attaches it. No release has been cut yet; the link will
  populate as soon as the first one ships.
- **Debug APK from CI:** every push to `main` or `claude/**` builds a
  debug APK and uploads it as a workflow artifact. Grab the latest from
  the [Actions tab](https://github.com/grcarmenaty/lifegame/actions)
  (artifact `lifegame-debug-<sha>`, retained 14 days, requires being
  signed in to GitHub).

Both APKs are currently signed with the debug keystore — they install
via sideload but are not Play-Store-acceptable. Real release signing
will be wired in before the first public release; see
[`CLAUDE.md`](CLAUDE.md#cirelease) for the upgrade path.

## Build from source

Requires JDK 17 and the Android SDK (API 35).

```
./gradlew :app:assembleDebug          # debug APK at app/build/outputs/apk/debug/
./gradlew :app:installDebug           # install on a connected device
./gradlew test                        # JVM unit tests
./gradlew :app:lint                   # lint
```

## Tech stack

Kotlin 2.0.21 · Jetpack Compose + Material 3 · Room via KSP · Navigation
Compose · AGP 8.7.3 · Gradle 8.11.1 · `minSdk 26` / `targetSdk 35`.
Single `:app` module, manual DI through the `Application` class. Full
conventions in [`CLAUDE.md`](CLAUDE.md).

## License

[MIT](LICENSE).
