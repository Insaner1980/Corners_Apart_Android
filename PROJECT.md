<!-- generated-by: gsd-doc-writer -->
# Corners Apart Android — Current Project Reference

Last source audit: 2026-07-28.

This document describes the live checkout at `C:\Dev\Corners_Apart_Android`. It is intended as a source-backed reference for exact code-review questions, maintenance work, and possible UI redesign planning. Repository-relative paths and concrete symbols are included where they materially narrow the implementation boundary.

A direct verification command covering ktlint, detekt, Android lint, all debug JVM tests, debug assembly, and Android-test compilation completed successfully on 2026-07-28: `.\gradlew.bat :app:ktlintMainSourceSetCheck :app:ktlintTestSourceSetCheck :app:detekt :app:lintDebug :app:testDebugUnitTest :app:assembleDebug :app:compileDebugAndroidTestKotlin --no-configuration-cache --console=plain`. It reported `BUILD SUCCESSFUL in 19m 18s` with 81 actionable tasks; the generated XML records 279 passing JVM tests with no failures, errors, or skips. Targeted Compose tests were also executed on a Pixel 9 Pro API 36 emulator. No security, Sonar, release, physical-device, two-device Nearby, store, signing, or project-local wrapper command was run, so those claims remain manual or unverified where applicable.

## Source-of-truth order

Use the following order when this document and another artifact disagree:

1. Production source under `app/src/main/java/com/finnvek/cornersapart/`.
2. Tests under `app/src/test/java/com/finnvek/cornersapart/` and `app/src/androidTest/java/com/finnvek/cornersapart/`.
3. Gradle, manifest, resources, CI, and analyzer configuration.
4. `AGENTS.md`, `README.md`, `PRIVACY-POLICY.md`, and this file as maintained architecture and product context.
5. Historical specifications, reviewed plans, screenshots, and prototypes.

The project is an original Corners Apart product. Historical rule references and similarly themed commercial games are not implementation authorities and must not leak into user-facing wording, identity, or assets.

## Executive summary

Corners Apart is a single-module native Android polyomino strategy game written in Kotlin. The app uses Jetpack Compose and Material 3 for UI, Hilt for dependency injection, DataStore with kotlinx.serialization JSON for local persistence, pure Kotlin model/engine code for game rules, local rule-based computer opponents, and Google Play services Nearby Connections for peer-to-peer sessions.

The current checkout implements:

- Five base modes: Solo, Two-color duel, Compact duel, Three players, and Four players.
- A 21-piece, 89-cell catalog, placement validation, deterministic bonus layouts, scoring, ranking, pass/turn/game-over rules, and immutable publication snapshots.
- Six computer difficulty tiers and three play styles.
- Local play, automatic Solo opponents, fixed-seed challenge levels, a date-seeded daily challenge, and a 12-character Rivals ladder.
- Local profiles, settings, saves, history, statistics, challenge stars, achievements, daily bests/streaks, Rivals records, and a device-wide Top 20.
- A host-authoritative Nearby protocol and concrete Google Play services adapter.
- A fixed dark “candy” visual system, custom splash assembly animation, responsive compact/expanded layouts, Canvas board interaction, drag/drop placement previews, dialogs, raw sound effects, haptics, and accessibility announcements.
- JVM unit tests, two instrumented Compose test classes, build/release policy tests, GitHub Actions, dependency verification, static-analysis configuration, and repository-local wrapper entry points.

The implementation is still not a verified release candidate. Compact Duel is explicitly marked as requiring play testing; physical multi-device Nearby behavior is not proven by this documentation pass; no navigation graph exists; release signing values are external; and several newly added challenge/Rivals/profile progression paths have less direct test coverage than the core engine and Nearby coordinator.

## Repository and product identity

| Item | Current value | Authority |
|---|---|---|
| Root project | `CornersApart` | `settings.gradle.kts` |
| Included modules | `:app` only | `settings.gradle.kts` |
| Namespace | `com.finnvek.cornersapart` | `app/build.gradle.kts` |
| Application ID | `com.finnvek.cornersapart` | `app/build.gradle.kts` |
| Nearby service ID | `com.finnvek.cornersapart` | `NearbyConnectionsCoordinator.SERVICE_ID` |
| App label | `Corners Apart` | `app/src/main/res/values/strings.xml` |
| Version code | `1` | `app/build.gradle.kts` |
| Version name | `1.0.0` | `app/build.gradle.kts` |
| Minimum SDK | 26 | `app/build.gradle.kts` |
| Compile/target SDK | 37 / 37 | `app/build.gradle.kts` |
| Java/Kotlin target | JVM 17 | `app/build.gradle.kts` |
| License | MIT, copyright 2026 Finnvek | `LICENSE` |

Current checked-in Kotlin source inventory:

- 111 production `.kt` files under `app/src/main/java`.
- 70 JVM test `.kt` files under `app/src/test/java`.
- Two instrumented-test `.kt` files under `app/src/androidTest/java`.

These counts are a snapshot, not an architectural limit.

## Implemented, deferred, and unverified state

### Implemented in source

- Android launcher, Hilt application, Compose activity, system splash theme, and custom four-piece launch overlay.
- Pure domain/engine pipeline with no Android, Compose, DataStore, Hilt, Play Services, wall-clock, locale, transport, opponent, or app-layer dependency reachable from `engine/`; the intended restriction is checked by `EngineDependencyBoundaryTest`.
- Local and Nearby `GameSession` implementations.
- Repository-backed `GameViewModel` with local save/history behavior and active Nearby session delegation.
- Candy-styled game UI, settings/profile/help/history/game-over dialogs, challenges, daily streak UI, Rivals gallery/intro/results, achievements, and Top 20.
- Source-set security settings that disable backup, device transfer, and cleartext traffic.
- Dependency and build-tool policy configuration described later in this document.

### Explicitly deferred or not part of v1

- Room; persistence is JSON DataStore.
- Raw Bluetooth, Wi-Fi Direct, Wi-Fi Aware, sockets, or other transport fallbacks; Nearby Connections is the v1 transport boundary.
- Remote avatar/image services.
- Navigation Compose and a multi-screen navigation graph.
- App-owned analytics, ads, crash reporting, and developer-controlled game/profile servers. Google Play services Nearby SDK behavior is a separate third-party platform boundary described under Security and privacy.
- User-facing “AI” claims; opponents are deterministic/rule-based local code.

### Manual or unverified in this audit

- Fresh forced test execution, test pass counts from a newly generated report, lint/detekt/stability/Sonar/security results, and coverage percentages. The direct up-to-date unit-test gate is recorded above and in the Tests section.
- Connected/emulator Compose behavior and screenshot fidelity.
- Physical two-device or multi-client Nearby advertising, discovery, authentication, ownership, disconnect, timeout, and reconnection.
- Compact Duel balance/usability; `GameModeConfig.requiresPlayTesting` is `true`.
- Release keystore availability, signed APK/AAB production, shrinker behavior, store upload, Play data-safety declarations, and production distribution.
- External Android-check wrapper behavior beyond the thin repository scripts, because most wrappers delegate to `C:\Dev\Android-check\tools\InvokeProjectCheck.ps1`.

## Build system and toolchain

### Gradle layout

- `settings.gradle.kts` declares `CornersApart`, includes `:app`, configures the Foojay toolchain resolver, filters plugin repositories, and enforces `RepositoriesMode.FAIL_ON_PROJECT_REPOS`.
- Root `build.gradle.kts` owns root plugin aliases, forced build-tool security overrides, ktlint filtering, Sonar property bridging, and the root `sonar` task dependency on `:app:assembleDebug` and `:app:jacocoDebugUnitTestReport`.
- `app/build.gradle.kts` owns Android configuration, signing, lint, Kotlin/JVM targets, Hilt/KSP, serialization, Compose, stability validation, detekt, JaCoCo, conditional OWASP configuration, and dependencies.
- `gradle/libs.versions.toml` is the central version catalog.
- `gradle/wrapper/gradle-wrapper.properties` pins and checksums the Gradle distribution.
- `gradle.properties` enables configuration/build cache, limits workers, sets Android/R8 behavior, and centralizes forced build-tool dependency versions.

AGP 9 built-in Kotlin is the project baseline. The app does not apply `org.jetbrains.kotlin.android`, `kotlin-android`, or kapt. Kotlin serialization and Compose compiler plugins remain explicit, and KSP is used for Hilt code generation.

### Current version pins

| Component | Version |
|---|---:|
| Gradle wrapper | 9.6.1 |
| Android Gradle Plugin | 9.3.1 |
| Kotlin | 2.4.10 |
| KSP | 2.3.10 |
| Hilt | 2.60.1 |
| AndroidX Hilt lifecycle/ViewModel Compose | 1.4.0 |
| Compose BOM | 2026.06.01 |
| Compose Stability Analyzer | 0.11.1 |
| Android security lint checks | 1.0.4 |
| Lifecycle | 2.11.0 |
| Coroutines | 1.11.0 |
| DataStore | 1.2.1 |
| AndroidX Core KTX | 1.19.0 |
| Core SplashScreen | 1.2.0 |
| Activity Compose | 1.13.0 |
| kotlinx.serialization | 1.11.0 |
| Play Services Nearby | 19.3.0 |
| JaCoCo | 0.8.15 |
| ktlint Gradle plugin | 14.2.0 |
| detekt | 2.0.0-alpha.5 |
| detekt Compose rules | 0.6.3 |
| OWASP Dependency-Check | 12.2.2 |
| SonarQube Gradle plugin | 7.3.1.8318 |
| JUnit | 4.13.2 |
| MockK | 1.14.11 |
| AndroidX Test JUnit / Runner / Espresso | 1.3.0 / 1.7.0 / 3.7.0 |

Version comments for Billing, Glance, CameraX, ML Kit, and Sentry in `gradle/libs.versions.toml` are dormant comments, not active dependencies.

### Current official platform cross-checks

These links are external compatibility and redesign references, not substitutes for the checked-in source:

- [AGP 9.3 release notes](https://developer.android.com/build/releases/agp-9-3-0-release-notes) list API 37 as the maximum supported API level and JDK 17 as the required JDK; the project uses AGP 9.3.1, compile/target SDK 37, and JVM 17.
- [AGP 9 built-in Kotlin migration guidance](https://developer.android.com/build/releases/agp-9-0-0-release-notes#built-in-kotlin) matches the project’s intentional omission of `org.jetbrains.kotlin.android`.
- [Android architecture recommendations](https://developer.android.com/topic/architecture/recommendations) support the project’s unidirectional state, ViewModel, `StateFlow`, and lifecycle-aware collection direction.
- [Compose accessibility guidance](https://developer.android.com/develop/ui/compose/accessibility) is the review baseline for semantics, custom gesture alternatives, touch targets, and future board accessibility work.
- [Nearby Android setup guidance](https://developers.google.com/nearby/connections/android/get-started) is the current permission-declaration baseline used to identify the SDK 31 coarse-location review item below.
- [Nearby connection management guidance](https://developers.google.com/nearby/connections/android/manage-connections) is the review baseline for mutual acceptance and authentication-token confirmation.
- [Nearby Connections overview](https://developers.google.com/nearby/connections/overview) documents SDK-level usage analytics collection, including performance metrics and device information, which must be considered separately from app-owned analytics code.
- [Android local-network permission guidance](https://developer.android.com/privacy-and-security/local-network-permission) is the review baseline for the SDK 37 `ACCESS_LOCAL_NETWORK` declaration and runtime request.

### Direct runtime dependency responsibilities

- Compose UI/graphics/foundation/Material 3: rendering and interaction.
- Lifecycle runtime/Compose/ViewModel Compose: lifecycle-aware collection and ViewModel integration.
- Hilt and AndroidX Hilt ViewModel Compose: injection.
- Coroutines core/Android: flows, serialization, delays, and background opponent work.
- DataStore core: JSON-backed persistent state.
- kotlinx.serialization core/JSON: persistence and Nearby protocol encoding.
- Core KTX and SplashScreen: Android helpers and launch splash.
- Activity Compose: Compose activity integration.
- Play Services Nearby: advertising, discovery, connections, and BYTES payloads.

There is no direct Navigation Compose, Material 2, DataStore Preferences, Room, Coil, DiceBear, Firebase, analytics, ads, or crash-reporting dependency.

### Gradle behavior

`gradle.properties` currently sets:

- `org.gradle.jvmargs=-Xmx2048m -Dfile.encoding=UTF-8`
- configuration cache and build cache enabled
- parallel execution disabled
- maximum two workers
- AndroidX and non-transitive R classes enabled
- official Kotlin style
- R8 full mode enabled

`buildToolSecurityOverrides` forces Jackson Core 2.21.5, Bouncy Castle 1.84 artifacts, jose4j 0.9.6, and JDOM 2.0.6.1 across build configurations. These are build-tool dependency controls, not a declaration that those artifacts ship in the app runtime.

### Build types and signing

Debug has no custom `BuildConfig` fields.

Release is non-debuggable, enables code minification and resource shrinking, and uses the Android Gradle Plugin default optimized ProGuard template (requested through `getDefaultProguardFile`) plus `app/proguard-rules.pro`. The default template is supplied by the Android toolchain, not stored at the repository root. The project rules retain Hilt-related classes.

Release signing requires all four variables:

- `CORNERS_APART_KEYSTORE_PATH`
- `CORNERS_APART_KEYSTORE_PASSWORD`
- `CORNERS_APART_KEY_ALIAS`
- `CORNERS_APART_KEY_PASSWORD`

The task-graph guard fails artifact-producing release tasks when the complete set is unavailable. It deliberately permits release verification tasks that do not produce an artifact. Actual signing values and signed artifact generation are external and unverified here.

### Common direct commands

Use the Windows wrapper in this checkout:

```powershell
.\gradlew.bat assembleDebug
.\gradlew.bat test
.\gradlew.bat :app:detekt
.\gradlew.bat lint
.\gradlew.bat :app:jacocoDebugUnitTestReport
.\gradlew.bat sonar
.\gradlew.bat :app:dependencyCheckAnalyze --no-configuration-cache --console=plain
```

When configuration cache is active, `app/build.gradle.kts` registers a guidance-only `dependencyCheckAnalyze` task instead of applying the incompatible OWASP task. The real scan therefore requires `--no-configuration-cache`.

Project instructions reserve `lc`/`tools/lc.ps1` and `sc`/`tools/sc.ps1` for the user. Do not run those wrappers autonomously.

## Runtime manifest and application entry

### Manifest security and components

`app/src/main/AndroidManifest.xml` configures:

- `android:name=".App"`
- `android:allowBackup="false"`
- `android:fullBackupContent="false"`
- `android:dataExtractionRules="@xml/data_extraction_rules"`
- `android:usesCleartextTraffic="false"`
- `android:supportsRtl="true"`
- launcher icon `@mipmap/ic_launcher`
- label `@string/app_name`
- base theme `@style/Theme.CornersApart`

`app/src/main/res/xml/data_extraction_rules.xml` excludes the root data domain from both cloud backup and device transfer.

`MainActivity` is the exported launcher component. The main manifest also contains `tools:node="remove"` directives for an unwanted Play Services exposure-notification wake-up service and Profile Installer receiver. Those are manifest-merger removal instructions, not app-owned runtime services.

`ManifestSecurityPolicyTest.releaseManifestExportsOnlyMainActivity` checks the packaged release manifest, but that assertion depends on `processReleaseManifestForPackage` output and was not executed in this audit.

The debug manifest repeats backup/cleartext restrictions and adds only non-exported `ComposeTestActivity`.

### Declared Nearby permissions

The main manifest declares:

- `ACCESS_WIFI_STATE` and `CHANGE_WIFI_STATE`, maximum SDK 31.
- Legacy `BLUETOOTH` and `BLUETOOTH_ADMIN`, maximum SDK 30.
- `ACCESS_COARSE_LOCATION`, maximum SDK 31.
- `ACCESS_FINE_LOCATION`, SDK 29–31.
- `BLUETOOTH_ADVERTISE`, `BLUETOOTH_CONNECT`, and `BLUETOOTH_SCAN` from SDK 31; scan is marked `neverForLocation`.
- `NEARBY_WIFI_DEVICES` from SDK 32, marked `neverForLocation`.
- `ACCESS_LOCAL_NETWORK` from SDK 37.

No `INTERNET` permission is declared.

`NearbyPermissions.requiredRuntimePermissions()` maps runtime requests as follows:

| Device SDK | Requested at runtime |
|---|---|
| 26–28 | coarse location |
| 29–30 | fine location |
| 31 | coarse + fine location + advertise/connect/scan |
| 32–36 | advertise/connect/scan + nearby Wi-Fi devices |
| 37+ | advertise/connect/scan + nearby Wi-Fi devices + local network |

`GameRoute` requests all missing permissions in one `RequestMultiplePermissions` launch before host/discover actions. It proceeds only if every returned value is granted. The current callback does not surface a denial explanation or settings deep link.

The current Google Nearby “Get started” sample declares coarse location only through SDK 28 and fine location for SDK 29–31. This project additionally declares and requests coarse location on SDK 31. The table above describes the code exactly; the extra SDK 31 coarse request should be revalidated on real Android 12 devices and against the Play Services version in use rather than treated as an assumed requirement.

### Startup flow

```text
Android launcher
  -> Theme.CornersApart.Starting
  -> MainActivity.installSplashScreen()
  -> @HiltAndroidApp App
  -> @AndroidEntryPoint MainActivity
  -> enableEdgeToEdge(dark transparent system bars)
  -> CornersApartTheme
  -> CornersApartLaunch custom overlay
  -> Surface(fillMaxSize)
  -> GameRoute(hiltViewModel<GameViewModel>())
  -> GameScreenContent
```

`CornersApartLaunch` in `app/src/main/java/com/finnvek/cornersapart/ui/components/CornersApartLaunch.kt` composes the app behind a custom overlay, assembles four packaged WebP pieces over a 720 ms timeline, performs a short settle/hold/fade, blocks pointer input, and clears overlay semantics. Visibility is stored with `rememberSaveable`, so it is a composition/activity launch experience rather than a ViewModel-owned state machine.

The platform starting theme uses a static placeholder drawable; the four-piece animation is Compose-owned after activity content starts.

## Architecture and package boundaries

### High-level component flow

```text
Android resources / MainActivity
              |
              v
Compose GameRoute / GameScreenContent
        |             |
        v             v
 MatchReview UI   GameViewModel
        ^          /  |  |  \
        |         v   v  v   v
   review analyzer LocalSession Data repositories NearbyConnectionsCoordinator
        |             |       |                 |
        v             v       v                 v
 GameEngine/MoveEvaluator GameEngine DataStore NearbySession -> ConnectionsClientFacade
        |             |        JSON          |                 |
        v             v                      v                 v
      model         model                GameProtocol   Play Services Nearby
                      ^
                      |
              opponents -> GameEngine
```

### Package responsibilities

| Package/path | Responsibility and important symbols |
|---|---|
| root package | `App`, `MainActivity` |
| `model/` | Serializable domain/save/profile/settings/history state; `GameConstants`, `GameModeConfigs`, `PieceCatalog`, `PieceTransforms`, `BoardSnapshot`, progression calculators |
| `engine/` | Pure rules; `GameEngine`, internal `PlacementValidator`, `CornerCache`, `BonusTileGenerator`, `Scoring`, typed move results |
| `opponents/` | Move generation/evaluation and named Rivals; `ComputerOpponentEngine`, `MoveGenerator`, `MoveEvaluator`, `OpponentDifficulty`, `OpponentRoster` |
| `review/` | Pure transient match reconstruction and deterministic owner 0 coaching; `GameReplayer`, `MatchReviewAnalyzer`, `ReviewScoring` |
| `multiplayer/` | `GameSession`, `LocalSession`, Nearby protocol/session/coordinator, permissions, facade, Play Services adapter |
| `data/` | JSON DataStore setup, repositories, Hilt runtime/persistence modules |
| `runtime/` | Android-facing abstractions shared across layers: `TimeProvider`, `SystemTimeProvider`, `StringProvider` |
| `viewmodel/` | `GameViewModel`, `GameUiState`, transient `MatchReviewUiState`, `GameEffect`, profile display mapping |
| `ui/screens/` | Route, playable/review boards, layouts, match-review and other dialogs, challenge/Rivals/Top 20 surfaces, sound policy/player |
| `ui/components/` | Shared board drawing, candy controls/dialog/status chip, piece drawing, avatars, streak/confetti/launch visuals |
| `ui/theme/` | Fixed color system, spacing/breakpoints/alpha, shapes, typography, player palette |
| `ui/util/` | Catalog-piece to localized resource mapping |

These are package boundaries inside one Gradle module. `PackageDependencyBoundaryTest` scans imports for cycles and explicitly forbids `model -> engine`, `data -> viewmodel`, and `multiplayer -> ui`. It also restricts serialization declarations to `model` or the multiplayer protocol boundary.

`GameRuntimeModule` is intentionally cross-cutting: it lives under `data/` but provides engine/opponent/review/runtime services and the Play Services facade/coordinator. The `data -> review` import is DI wiring only; `review/` does not depend on `data`, ViewModels, UI, Android, or DataStore. A future Gradle module split must either relocate or divide this DI module.

### Dependency and ownership invariants

- `model` must not depend on `engine`.
- `engine` is the single source for placement, scoring, ranking, bonus, pass, turn, and end-game rules.
- UI, sessions, repositories, and opponents must not reimplement engine rules.
- UI/ViewModels do not access DataStore directly.
- Match review is memory-only and available only for the just-finished local state captured by `GameViewModel`; starting/restoring another game or entering Nearby cancels it and invalidates stale analyzer emissions.
- `GameReplayer` uses public engine APIs and requires exact final-state equality. Because `moveHistory` stores placements but not passes, it reconstructs voluntary passes between recorded moves and the final manual passes after the last recorded move; automatically skipped immobile players do not create timeline steps.
- `MatchReviewAnalyzer` evaluates all valid candidates with a per-call `MoveEvaluator`, fixed MASTER/BLOCKER parameters, deterministic tie-breaking, and an injected background dispatcher. It never calls the randomized/private 2-ply opponent action path.
- ViewModels do not call Android resources directly; `StringProvider` supplies the small non-Compose resource boundary.
- Play Services types stay in `PlayServicesConnectionsClientFacade`; coordinator/session/UI use project-owned interfaces and state types.
- Generic `GameSession` does not expose `replaceState`; restore is concrete local/host-only behavior.
- Serializable domain types live under `model/`; `GameMessage`, protocol payload types, and `SessionPlayer` are the multiplayer exception.

## Domain model

### Core constants

`app/src/main/java/com/finnvek/cornersapart/model/GameConstants.kt` is the current shared source for:

| Constant | Value |
|---|---:|
| Standard board | 20 × 20 |
| Compact board | 14 × 14 |
| Standard / compact bonus count | 10 / 6 |
| Placed cell points | 1 |
| Bonus tile points | 3 |
| Completion bonus | 10 |
| Maximum profile history entries | 50 |
| Difficulty levels | 6 |
| Opponent delay | 300 ms minimum + random 0–400 ms |

Player/color labels are `Pink`, `Mango`, `Cyan`, and `Lime`. Timing values for UI notices, animations, Nearby reconnect, and drag previews are owned by their respective UI/coordinator files rather than `GameConstants`.

### Board representation and snapshot semantics

- `BoardView` defines `size`, `cellAt`, bounds checks, row/column to flat index mapping, and read helpers.
- `BoardSnapshot` stores a flat `List<Int>` of exactly `size * size`; `EMPTY` is `-1`.
- `MutableBoard` owns the engine mutation buffer and converts back to `BoardSnapshot`.
- Board cells contain player-slot indexes, not owner indexes or color indexes.
- `GameState.hasValidIndexDomains()` verifies contiguous player indexes, a valid current player, owner ranges, non-negative color indexes, board cell player indexes, bonus claim indexes, and move-history player indexes.
- `StateSnapshots.kt` produces unmodifiable copies for board/player/game/history/profile/save publication boundaries.

The new progression collections in `Profile` (`challengeStars`, `achievements`, `dailyBestScores`, `rivalWins`, and `rivalLosses`) are not explicitly copied in `Profile.toSnapshotCopy()`; only history is copied there. The properties are typed as read-only Kotlin collections, but a future immutability review should decide whether the snapshot helper should defensively copy every collection.

### Pieces and transforms

`PieceCatalog` is the only piece-geometry registry. It contains:

- one monomino
- one domino
- two trominoes
- five tetrominoes
- twelve pentominoes
- 21 stable IDs and 89 total occupied cells

`PieceTransforms` supports clockwise/counterclockwise rotation, horizontal flip, normalization to `(0,0)`, sorted offsets, cached unique orientations, and lookup by orientation index. Tests guard catalog count, total cells, unique normalized orientations, and a maximum of eight orientations.

UI names are mapped in `app/src/main/java/com/finnvek/cornersapart/ui/util/PieceNameResources.kt`; IDs and localized display labels are intentionally separate.

### Serializable game roots

`GameState` contains:

- `BoardSnapshot`
- ordered players and current player index
- turn number
- `Ruleset` and `GameMode`
- random seed
- bonus tiles and optional generated layout ID
- accepted move history
- game-over flag

`Player` contains player/color/owner indexes, start corner, used piece IDs, score breakdown, pass state, active-scoring flag, and computer-control flag.

`Move` is `(playerIndex, pieceId, anchorRow, anchorCol, orientationIndex)`.

`BonusTile` tracks position plus optional claiming player and turn. `ScoreBreakdown` and `ScoreDelta` split placed-cell, bonus-tile, and completion points.

`Ruleset` currently contains only `STANDARD`.

## Modes and game configuration

`GameModeConfigs` is the mode-default authority. The default mode is `FOUR_PLAYER`.

| Mode | Board | Bonuses | Slots and starts | Owners | Computers | Notes |
|---|---:|---:|---|---|---|---|
| `SOLO` | 20 | 10 | 0 bottom-right; 1 top-left; 2 top-right; 3 bottom-left | 0,1,2,3 | slots 1–3 | local automatic opponent loop |
| `TWO_COLOR_DUEL` | 20 | 10 | standard four corners: TL, TR, BR, BL | 0,1,0,1 | none | four color turns, two ranked owners |
| `COMPACT_DUEL` | 14 | 6 | slot 0 TL; slot 1 BR | 0,1 | none by base config | `requiresPlayTesting=true`; Rivals converts slot 1 to computer |
| `THREE_PLAYER` | 20 | 10 | TL, TR, BR | 0,1,2 | none | three active scoring slots |
| `FOUR_PLAYER` | 20 | 10 | TL, TR, BR, BL | 0,1,2,3 | none | default |

`GameConfig` can override board size, random seed, explicit bonus tiles, and requested bonus count. `GameEngine.newGame()` resolves the mode config, creates players, generates bonuses when explicit tiles are absent, and publishes current player 0 at turn 0.

## Engine rules and data flow

### Placement validation

`GameEngine.applyMove()` delegates to internal `PlacementValidator.validate()` in this order:

1. Reject game-over state.
2. When turn enforcement is active, reject a non-current player.
3. Resolve the player; reject invalid player, already-passed player, or reused piece.
4. Resolve piece and orientation.
5. Calculate target cells with `model.targetCells(anchor + offsets)`.
6. Reject out-of-bounds or occupied target cells.
7. For a first move, require the player’s start corner.
8. Reject orthogonal edge contact with the same player.
9. After the first move, require at least one diagonal contact with the same player.
10. Collect distinct, unclaimed bonus tiles covered by the target.

Opponent pieces may touch by edge or corner. Only contact with the moving player’s own cells is restricted.

Typed rejection values are:

`GAME_OVER`, `NOT_PLAYERS_TURN`, `INVALID_PLAYER`, `PLAYER_HAS_PASSED`, `UNKNOWN_PIECE`, `UNKNOWN_ORIENTATION`, `PIECE_ALREADY_USED`, `OUT_OF_BOUNDS`, `CELL_OCCUPIED`, `START_CORNER_NOT_COVERED`, `SAME_PLAYER_EDGE_TOUCH`, and `NO_DIAGONAL_TOUCH`.

`previewPlacement()` uses the same validator and calculates a score preview only for a valid placement.

### Accepted move

An accepted move:

1. Writes the player slot index to every target board cell.
2. Adds the piece ID to `usedPieceIds`.
3. Applies placed-cell, bonus, and possible completion score.
4. Claims covered bonus tiles with the current turn number.
5. Increments the turn and appends the move.
6. Evaluates end-game state.
7. Advances to the next active, non-passed player with a valid move.
8. Publishes a defensive snapshot.

Completion bonus is awarded when the accepted piece makes the used-piece count equal to all 21 catalog pieces.

### Pass, turn advancement, and game over

`GameEngine.pass()` rejects game-over or wrong-turn attempts, marks the player passed, increments the turn, recalculates game over, and advances.

The game ends when every active-scoring player is either already passed or has no valid move. The engine can therefore end a game after an accepted move even if players did not manually pass, because it checks remaining legal moves.

A passed player is not reactivated by turn advancement. The accepted-move path sets the moving player’s `passed` flag false, but a passed player cannot legally move.

### Valid move enumeration

For each unused piece and unique orientation, `CornerCache` derives target corner positions:

- first move: assigned start corner
- later moves: empty diagonal neighbors of owned cells that do not have an owned orthogonal neighbor

Candidate anchors align each orientation cell with each target corner; `PlacementValidator` then performs full validation with turn enforcement disabled. This is shared by engine legal-move checks and the opponent generator.

### Bonus layouts

`BonusTileGenerator` owns one 20×20 standard template and one 14×14 compact template. `SeedMixer` deterministically selects the template and one of four transforms: identity, 180° rotation, vertical mirror, or horizontal mirror. Generated IDs include the base template and transform index.

Explicit `GameConfig.bonusTiles` bypasses generation and leaves `bonusLayoutId` null.

### Scoring and ranking

`Scoring.scoreMove()` awards:

- one point per placed piece cell
- three points per newly claimed bonus tile
- ten points when the full piece set is completed

`Scoring.rankPlayers()` filters active-scoring players, groups them by `ownerIndex`, combines score breakdowns, and sorts owners by:

1. total score descending
2. placed-cell score descending
3. claimed bonus tiles descending
4. remaining piece count ascending
5. owner index ascending

This owner aggregation is essential for Two-color duel: color slots 0/2 belong to owner 0, and 1/3 to owner 1. A multi-slot owner is displayed as `Player N` at the engine ranking boundary and renamed/colored for UI later.

## Computer opponents and Rivals

### Difficulty model

`OpponentDifficulty` has six tiers:

| Persisted level | Enum | Temperature | Candidate cap | Key behavior |
|---:|---|---:|---:|---|
| 1 | `BEGINNER` | 3.0 | 10 | weak bonus awareness, no blocking |
| 2 | `EASY` | 2.0 | 25 | light awareness |
| 3 | `MEDIUM` | 1.0 | 80 | default |
| 4 | `HARD` | 0.5 | 200 | stronger large-piece/bonus/blocking weights |
| 5 | `EXPERT` | 0.2 | 500 | near-greedy selection |
| 6 | `MASTER` | 0.1 | 500 | 12-candidate two-ply lookahead |

`OpponentDifficultyMapper` clamps persisted values to 1–6. `GameSettings.DEFAULT_DIFFICULTY` is 3.

### Move generation and evaluation

- `MoveGenerator` asks `GameEngine.getValidMoves()`, samples evenly within each piece, alternates large/small piece groups, and caps by difficulty.
- `MoveEvaluator` scores placed cells, bonus claims, spread from start, center access, denial of opponent attachment cells, new mobility, and late-game small-piece conservation.
- `ComputerOpponentEngine` evaluates on an injected dispatcher, uses style/difficulty weights, optional Master lookahead, and seeded temperature-weighted selection.
- The seed mixes game seed, turn, player, difficulty, and style, making selection deterministic for equivalent state and inputs.
- The final choice is preview-validated; it falls back to another evaluated legal move or pass.

Styles are `EXPANSIONIST`, `OPPORTUNIST`, and `BLOCKER`. Default Solo style cycles by player index.

### Rivals roster

`OpponentRoster` defines 12 ordered characters:

`Jelly`, `Pip`, `Sprout`, `Coco`, `Dash`, `Fig`, `Blaze`, `Luna`, `Onyx`, `Nova`, `Vex`, and `Sol`.

The roster covers all six difficulties in non-decreasing tiers, assigns a style and palette index to each character, unlocks the first character unconditionally, and unlocks each later character after at least one win over the previous character.

`GameViewModel.startRivalMatch()`:

1. Resolves the character and rejects locked/unknown IDs.
2. Leaves Nearby and resets transient local action state.
3. Creates a `COMPACT_DUEL` game with a current-time seed.
4. Uses `LocalSessionFactory.createRivalMatch()` to convert slot 1 into the named computer opponent with the character’s style/difficulty and a display color that avoids the local/profile color mapping.
5. Records the active rival ID for game-over persistence/UI.

Rival win/loss counts are profile-local. A win is determined by ranked owner 0 being first. The first win can expose the next character’s name in `RivalMatchResult`.

## Session layer

### Shared contract

`GameSession` exposes:

- `sessionType`
- current `gameMode`
- `StateFlow<List<SessionPlayer>>`
- `StateFlow<GameState>`
- `StateFlow<ConnectionState>`
- suspending `sendMove` and `sendPass`
- `startNewGame`

`NearbyGameSession` adds lobby state and one-shot session events. `replaceState` is intentionally absent from both public interfaces.

### LocalSession

`LocalSession` owns a single publication containing game state, projected session players, and `CONNECTED` state. A `Mutex` serializes moves and passes.

Local accepted actions:

- run through `GameEngine`
- continue automatic computer turns while the current player is computer-controlled
- delay each computer action by a random 300–700 ms
- publish only after the human action and full automatic opponent sequence complete

If a generated computer move is unexpectedly rejected, the computer passes. If no legal move exists, the opponent engine explicitly returns pass.

`replacementVersion` and the mutation mutex prevent a stale in-flight action from publishing after `replaceState` or `startNewGame`. `startNewGame` generates a fresh random seed distinct from the current seed and forces bonus regeneration.

`LocalSessionFactory.create()` maps persisted difficulty. `createRivalMatch()` supplies the character override and mutates only the freshly created slot-1 identity/control data through validated state replacement.

## Nearby architecture and state machines

### Transport boundary

`ConnectionsClientFacade` defines project-owned advertising, discovery, request/accept/reject, BYTES send, stop operations, callbacks, failures, and status codes.

`PlayServicesConnectionsClientFacade` is the only production file that directly uses `ConnectionsClient`, `Payload`, Play Services callbacks, and `Strategy.P2P_STAR`. It:

- maps operation failures, including `ApiException.statusCode`
- forwards authentication digits as a string
- accepts BYTES payloads
- closes and rejects non-BYTES payloads
- reports transfer failure

The app does not implement an app-level heartbeat. Liveness originates from Play Services connection lifecycle callbacks.

### Protocol

`GameProtocol` uses kotlinx.serialization JSON with:

- discriminator `type`
- defaults encoded
- unknown keys rejected

Message variants:

- `PlaceMove`
- `MoveAccepted` with full authoritative state and score delta
- `MoveRejected` with typed reason
- `Pass`
- `FullSync`
- `PlayerJoined`
- `PlayerLeft`
- `GameConfig`

Persistence JSON is permissive toward unknown keys; Nearby protocol JSON is intentionally strict. There is no explicit protocol version, migration field, message sequence number, replay counter, app-level signature, or payload-size guard in `GameMessage.kt`.

### HostGameCoordinator

The host coordinator is the authority for state mutation:

- applies client moves through `GameEngine`
- broadcasts `MoveAccepted` with the full state
- returns `MoveRejected` only to the originating endpoint
- applies pass and broadcasts `FullSync`
- broadcasts `PlayerJoined` and sends the joining endpoint `FullSync`
- accepts host-only validated state replacement

`HostGameCoordinator` itself does not establish endpoint authorization. `NearbyConnectionsCoordinator` authorizes before delivery.

### NearbySession roles

Host:

- owns `HostGameCoordinator`
- routes local host moves/passes directly through it
- can start a new game and replace validated state
- broadcasts outputs through `NearbyTransport`

Client:

- sends `PlaceMove`/`Pass` to `MessageTarget.Host`
- does not mutate optimistically
- applies `FullSync` and `MoveAccepted` only as authoritative state
- emits `GameSessionEvent.MoveRejected` for a host rejection
- ignores state replacement calls

Both roles publish game state, projected players, lobby state, connection state, and events. Invalid authoritative state moves the client session to `FAILED` and emits an action-failed event.

`SessionPlayer.isLocal` is role-wide in the current projection: every host-side projected player is marked local and every client-side projected player non-local. It is not a per-owner “this device controls this player” signal.

### Coordinator host lifecycle

`startHosting(config)`:

1. Creates a host session.
2. Clears endpoints/timeouts and increments callback generation.
3. Sets role host and publishes a connected coordinator state.
4. Stops previous advertising/discovery/endpoints.
5. Starts advertising with service ID/application ID.

Incoming connection initiation produces one `NearbyPendingConnection` with endpoint name and authentication digits. The UI must accept or reject it.

For a host acceptance:

- owner 0 is reserved for the host
- the endpoint receives the first distinct, non-computer owner not already assigned
- capacity exhaustion rejects the connection without failing the host session
- Two-color duel assigns both color slots owned by remote owner 1 to the same endpoint
- accepted connection triggers an authoritative full sync

Client-originated `PlayerJoined`, `PlayerLeft`, sync, config, accepted, and rejected messages are unauthorized. Move/pass requests are accepted only when the endpoint owns the move’s player owner. Unauthorized move/pass receives `NOT_PLAYERS_TURN`.

### Coordinator client lifecycle

`startDiscovery()`:

1. resets endpoint/session state and callback generation
2. sets role client with no active session
3. stops previous Nearby activity
4. starts discovery

Discovered endpoints are projected to `NearbyUiState`. Selecting one stops discovery, resets endpoint state again, stores the intended host ID, advances callback generation, and requests a connection.

The client session is created only from the first valid `FullSync` sent by the selected, connected host. Full sync from another endpoint or invalid player/board index domains is rejected.

### Disconnect and reconnect

- Explicit coordinator `disconnect()` stops all Nearby activity, cancels reconnect jobs, clears endpoints/session, increments callback generation, and publishes `DISCONNECTED`.
- A client losing its selected host clears its active session and becomes `DISCONNECTED`; there is no client reconnection path in the current coordinator.
- A host losing a mapped remote endpoint marks every slot for that owner reconnecting, broadcasts internal `PlayerLeft` effects to the session, and starts a 60-second timeout.
- Reaccepting an endpoint for that owner cancels the timeout, sends full sync, and applies `PlayerJoined` for reconnecting slots.
- Timeout removes reconnect markers, sets session/coordinator state `FAILED`, and reports `Player reconnect timed out`.
- Callback generation prevents stale discovery, payload, operation, or disconnect callbacks from reopening superseded state.
- A callback mutex serializes asynchronous payload/disconnect processing.

The disconnected owner mapping is removed immediately. A later accepted endpoint can claim the first free owner; endpoint identity is not a durable owner reservation.

### Nearby UI surface

`GameUiState.nearbyState` exposes coordinator-level status, discovered endpoints, one pending authentication decision, and an error string. The current screen does not expose `NearbyLobbyState`, connected-player/owner mappings, reconnecting player indexes, or an explicit “disconnect/leave” button. Switching to a local game or another local feature calls coordinator disconnect indirectly.

## Persistence and serialization

### DataStore files

`app/src/main/java/com/finnvek/cornersapart/data/CornersApartDataStores.kt` creates:

| File | Root type | Repository |
|---|---|---|
| “saved-game.json” | `SavedGameData` | `GameRepository` |
| “profiles.json” | `ProfilesData` | `ProfileRepository` |
| “settings.json” | `GameSettings` | `SettingsRepository` |

These quoted values are runtime DataStore file names, not checked-in repository paths. Their app-private paths are managed by DataStore and are not hardcoded beyond the names.

`CornersApartJson` encodes defaults and ignores unknown keys. `JsonDataStoreSerializer` converts serialization/argument errors to `CorruptionException`. No custom corruption handler is configured at the declaration sites.

The app does not apply its own encryption to these JSON values. OS/app sandboxing is the current storage boundary.

### Saved game

`SavedGameData` stores nullable `GameState`, epoch save time, and a `GameSettings` snapshot.

`GameRepository.saveGame()` rejects invalid index domains and snapshots the state. `clearSavedGame()` writes an empty root.

`GameViewModel`:

- auto-saves only local sessions
- saves after a complete accepted local turn, including automatic computer replies
- saves after an accepted local pass
- clears the save when the local game ends
- does not persist Nearby game state or Nearby history
- validates stored index domains and clears invalid saves on collection/resume

Resume replaces the local session with the saved state and restores saved settings. The resume summary currently displays `settings.preferredDifficulty` from the ViewModel’s current settings, not explicitly `savedGameData.settings.preferredDifficulty`, even though actual resume uses the saved settings snapshot.

### Settings

`GameSettings` persists:

- difficulty, default 3 and normalized to 1–6
- sound enabled
- haptics enabled
- preferred base mode, default Four players

Changing preferred difficulty/mode updates persistence and UI but does not recreate the current game. New local games use the then-current settings.

There is no persisted reduced-motion or preferred-ruleset setting.

### Profiles and history

`Profile` persists:

- ID and name
- color index
- avatar style and seed metadata
- active flag
- up to 50 history entries
- challenge stars
- achievement IDs
- up to 60 daily best-score date entries
- best daily streak
- Rivals wins and losses

`ProfileRepository` keeps one active profile when profiles exist, refuses to delete the final profile, activates a remaining profile after deletion, never lowers a challenge star result or daily best, deduplicates achievements, increments Rivals counters, and truncates history/daily data to their configured limits.

The ViewModel creates `local-default` asynchronously when no profiles exist. New profile IDs combine current epoch milliseconds and a lowercase alphanumeric name fragment. Name input is trimmed with a localized fallback, but no explicit length limit is applied in source.

`LocalAvatarStyle` values are `INITIALS`, `GEOMETRIC`, `MOSAIC`, and `RINGS`. The current UI selects/persists the style, but there is no avatar generator or profile-avatar renderer in production source: the former `LocalAvatarGenerator` is not present. `avatarSeed` is retained as metadata.

### Profile display mapping

For local sessions, active profile owner 0 receives the profile name and selected visual color. `ProfileDisplayMapper` swaps color 0 and the selected profile color bijectively so all four visual colors remain unique. Other engine-named players are relabeled when needed.

Nearby state does not apply local profile naming/color remapping in the same way because `ProfileDisplayMapper` only transforms local sessions.

### History/statistics/Top 20

Every completed local game produces a `HistoryEntry` for owner 0 with:

- date from `TimeProvider.todayIsoDate()`
- rank and aggregated owner score
- score breakdown, claimed bonuses, and placed-piece count
- difficulty from current global `settings.preferredDifficulty`
- ruleset/mode
- elapsed seconds measured since ViewModel game start/resume
- full ranked score list

The stored difficulty therefore describes the global preference even when a challenge level or Rival character supplies a different actual opponent difficulty.

`HistoryStatsCalculator` provides total games, wins, win rate, average/best score, average rank/bonus count, completion count, favorite difficulty, last-20 score trend, and per-difficulty statistics. The current Stats tab renders only total games, wins, average score, best score, average rank, and average bonus tiles.

`HallOfFameCalculator` merges all profiles, optionally filters by mode, sorts score descending then date ascending, and limits to 20. A new equal score ranks after existing equal scores. The UI caches all-mode and per-mode lists by the current profile-list object identity.

## Progression features

### Challenge levels

`ChallengeLevels` defines 20 fixed Solo levels:

- difficulty scales from 1 to 6 across level numbers
- seed is `91_000 + level * 7`
- two-star threshold is `55 + level`
- three-star threshold is `70 + level`
- a win earns at least one star
- the first level is open; each next level requires any star on the previous level
- stored stars only improve

The UI prevents clicks on locked levels. `GameViewModel.startChallengeLevel()` itself validates only that the level exists; it does not independently enforce the unlock rule.

### Daily challenge

`startDailyChallenge()` creates Solo with `todayIsoDate().hashCode().toLong()` as seed and the globally preferred difficulty. The date comes from the device’s system time zone through `SystemTimeProvider`.

Daily bests retain 60 newest ISO-date keys. Current streak counts consecutive days ending today or yesterday; best streak never decreases.

The game-over “Play again” callback does not branch on `isDailyChallenge`; when a daily game ends, it currently starts a normal Solo base-mode game instead of replaying the date-seeded daily challenge.

### Achievements

`AchievementEvaluator` can award:

- first win
- at least three bonus tiles in one game
- all 21 pieces placed
- a win with recorded difficulty at least 5
- three consecutive wins
- any three-star challenge
- at least ten cleared challenge levels

New IDs are filtered against the profile’s stored IDs, persisted, shown in game-over UI, and listed locked/unlocked in the Stats tab.

Because achievement evaluation consumes `HistoryEntry.difficulty`, challenge/Rival games currently inherit the global settings difficulty caveat described above.

### New-best and all-time result state

- `isNewBestScore` is true only when prior history is non-empty and the new score is strictly greater than the previous maximum. A first-ever result is not labeled “new best.”
- `allTimeRank` is calculated before inserting the result and only returned if it fits the Top 20.
- These values are transient ViewModel state for the most recently recorded local game.

## ViewModel architecture

### Construction and collected sources

`GameViewModel` is `@HiltViewModel` and injects:

- `LocalSessionFactory`
- `GameRepository`
- `ProfileRepository`
- `SettingsRepository`
- `StringProvider`
- `TimeProvider`
- `NearbyConnectionsCoordinator`
- `GameEngine` for placement previews

At initialization it collects settings, saved game, profiles, coordinator UI state, and coordinator current session. When a Nearby session becomes active, it separately collects authoritative game state and session events.

The active session is always:

```text
nearbySession ?: localSession
```

### Public state

`StateFlow<GameUiState>` contains:

- board/mode/bonuses/player projections/current turn
- selected piece/orientation/cells and full piece panel
- game-over and ranked score state
- sound/haptic/difficulty/preferred mode
- duration, history, active profile, profiles
- saved-game/resume summary
- session type and coordinator Nearby UI
- challenge, daily, achievement, Rival, streak, all-time-rank, and Top 20 data

`currentPlayer` indexes `players[currentPlayerIndex]`; valid engine state is therefore a prerequisite.

`SharedFlow<GameEffect>` emits move rejection, action failure, accepted-move score/bonus, and game-over effects.

### Actions

Base/local:

- `startGame`
- resume/discard saved game
- select/rotate/flip piece
- legality preview
- place/pass
- sound/haptic/difficulty/preferred mode changes

Profiles:

- activate, add, update, delete

Nearby:

- host, discover, connect, accept, reject

Progression:

- start challenge level
- start daily challenge
- start Rival match

There is no public UI-bound ViewModel action that only disconnects and leaves Nearby while preserving the local game surface.

### Local action cancellation and persistence

Local gameplay actions run under a replaceable `SupervisorJob` attached to `viewModelScope`. Starting/restoring/replacing local play cancels the old action job so delayed computer turns cannot save over the new session.

Nearby actions do not use that local job. Client move/pass methods report successful send before host acceptance; later host rejection arrives as a session event.

On a client, a later `MoveAccepted` authoritative update refreshes UI through the state collector but does not itself create `GameEffect.MoveAccepted`. The immediate client send path usually sees unchanged state and therefore emits no positive score effect. Sound/haptic/score-notice parity between host/local and client acceptance is a concrete redesign/review target.

## Compose UI

### Root hierarchy

`GameRoute`:

- lifecycle-collects `GameUiState`
- owns permission launcher
- collects one-shot effects
- maps effects to accessibility announcements, status/score notices, raw sound events, and haptics
- builds grouped action data classes
- calls `GameScreenContent`

`GameScreenContent` owns dialog visibility and renders:

```text
BoxWithConstraints
  -> candyBackground + safeDrawingPadding
  -> vertically scrollable CompactGameLayout or ExpandedGameLayout
  -> DragGhostOverlay
  -> RivalIntroOverlay
  -> conditional dialogs (resume, settings, profiles, help, history, challenges, Rivals, game over)
```

The breakpoint is width-only: compact below 840 dp, expanded at 840 dp or more.

### Compact layout

Order:

1. Header, utility actions, Challenges/Rivals/Nearby actions, score cards.
2. Square full-width board.
3. accessibility live node.
4. turn/status/score notice.
5. rotate-left, rotate-right, flip, and pass controls.
6. selected-piece preview.
7. progress indicator and horizontally scrollable piece cards.

### Expanded layout

The single vertically scrollable row divides into:

- left weight 1.0: header/actions/score, accessibility/status, controls, selected preview
- right weight 1.2: board and piece panel

There is no height breakpoint, landscape-specific policy, or independent column scrolling.

### Board rendering and interaction

`GameBoard` is a square Canvas inside a rounded panel. It draws:

- empty rounded cells
- pulsing unclaimed bonus diamonds/glows
- unoccupied starting-corner markers
- occupied glossy/beveled candy cells
- placement previews and alignment bands

Input supports:

- press/drag/release directly on the board
- drag from a piece card to the board
- a floating ghost outside the board
- a board-owned preview when the finger is over the board

`liftedBoardAnchor()` places the preview two cells above the finger, centers it horizontally, and clamps the full piece within board bounds. Legality comes from `GameViewModel.isPlacementLegal()` and engine preview validation. Valid previews use player colors; invalid previews use the centralized red palette.

The board detects newly occupied cells by comparing snapshots and applies a spring “pop.” Bonus markers pulse continuously.

### Header and actions

- Header shows app name, four-color accent bar, and a mode chip that opens the base-mode picker.
- Utility row provides History & stats, Profiles, Settings, and Help icon buttons.
- Challenges, Rivals, and Nearby are three full candy buttons.
- Nearby expands on request or automatically when state requires attention.
- Score cards are arranged two per row, animate numeric scores, border the active player, and dim passed players.

### Piece panel

- All 21 pieces appear in one horizontal row.
- Used pieces are dimmed and disabled.
- Selection resets orientation to zero.
- Selected cards scale and receive a player-color border.
- Progress shows used/total count and an animated meter.
- Selected-piece preview is separate from the panel.
- Piece content descriptions use localized catalog names.

### Dialogs and overlays

- `ResumeGameDialog`: raw epoch save value, mode, leader, claimed bonuses, difficulty, continue/new game.
- `GameSettingsDialog`: sound/haptics switches, 1–6 difficulty chips, all five preferred modes.
- `ProfilesDialog`: profile selection, name, color, avatar-style metadata, save/new/delete with confirmation.
- `GameHelpDialog`: goal, starting corner, corner/edge contact, scoring, bonus, passing/end, controls, Nearby.
- `HistoryStatsDialog`: History, Stats/Achievements, and Top 20 tabs.
- `ChallengeDialog`: daily action/streak and 20 sequential star chips.
- `RivalsDialog`: 12 cards with avatar, tier pips, record, lock/defeated/next state.
- `RivalMatchIntro`: skippable two-second VS overlay.
- `GameOverDialog`: confetti, winner/ranking/breakdowns, duration, challenge/daily/Rival results, new best, achievements, all-time rank, stats, and replay.

`CandyDialog` animates scale/alpha over 180 ms and makes its complete column vertically scrollable.

### Sound and haptics

`GameSoundPolicy` maps:

- accepted placement -> `app/src/main/res/raw/snd_place.wav`
- bonus claim -> `app/src/main/res/raw/snd_bonus.wav`
- game over -> `app/src/main/res/raw/snd_game_over.wav`
- rejection/failure -> `app/src/main/res/raw/snd_reject.wav`

`GameSoundPlayer` uses `SoundPool`, two streams, game/sonification audio attributes, and slight random pitch variation only for placement. The player is remembered at route scope and released on disposal.

The implementation does not wait for an explicit SoundPool load-complete callback before allowing playback; very early event behavior is a review target.

Input haptics use `TextHandleMove` for piece/transform interaction and `LongPress` for pass. Effect haptics use stronger feedback for bonus/rejection/failure/game over. Sound and haptics are independently persisted.

### Accessibility

Implemented:

- candy buttons/chips have minimum 48 dp targets
- icon buttons have explicit descriptions
- piece cards expose localized used/available descriptions
- board exposes “Game board”
- effect announcements use polite live regions
- Nearby panel uses a polite live region
- challenge/Rival/Top 20 entries build descriptions
- drag overlay clears irrelevant semantics

Current limitations:

- the Canvas board is one semantic node; individual cells, coordinates, occupancy, bonuses, and direct non-pointer placement actions are not exposed as separate accessibility nodes
- drag/touch is the only board placement mechanism
- the launch overlay intentionally clears semantics while visible
- several decorative/user-visible glyphs (`★`, `☆`, flame, lock, crown, VS treatment) are code constants rather than localized resource content
- there is no reduced-motion preference, while launch, dialog, board, bonus, score, progress, ghost, Rival, and confetti animations are active

## Theme, visual tokens, and resources

### Theme direction

`CornersApartTheme` always applies one dark Material 3 scheme. It does not use system dark/light switching, dynamic color, or a light palette.

The visual language is high-saturation candy:

- deep indigo vertical background
- dark violet panels/dialogs
- pink, mango, cyan, and lime player families with dark/highlight variants
- gold bonus accents
- 3D gradient/bevel buttons
- Nunito rounded typography

### Token ownership

`app/src/main/java/com/finnvek/cornersapart/ui/theme/Tokens.kt` centralizes:

- player and surface colors
- text, button, medal, streak, bonus, and preview colors
- screen/section/gap/touch/piece/board/dialog/Rivals/Top 20 spacing
- 840 dp expanded breakpoint
- opacity values

`PlayerPalette.kt` maps color indexes modulo four to base/dark/highlight triples and owns the invalid preview palette.

`Shapes.kt` defines Material rounded shapes from 8 to 28 dp. `Type.kt` packages Nunito Semibold/Bold/ExtraBold/Black and defines display/headline/title/body/label styles plus a shared shadow.

Some drawing-specific fractions, animation durations, sizes, and offsets remain local constants in their component files. The project’s “centralize design tokens” rule should be applied when those values become shared or need cross-screen tuning.

### Packaged resources

- `app/src/main/res/values/strings.xml`: 188 string entries and 8 plural entries in the base locale.
- No translated `values-xx` directory exists; UI prose is English-first.
- Four Nunito TTF files and `app/src/main/resources/META-INF/LICENSE-NUNITO.txt`.
- Four custom launch WebP layers in `drawable-nodpi`.
- Four raw WAV game effects.
- Local vector controls and launcher artwork.
- Adaptive launcher icon with color foreground and monochrome layer; minimum SDK 26 means only the v26 adaptive resource is required by the current platform floor.
- System splash colors/themes plus placeholder animated-icon drawable.

Most UI prose is resource-backed, but player labels, Rival names, Rival IDs, piece IDs, and some decorative glyphs are code-owned constants.

## Security and privacy boundaries

### Local data

- Profiles, settings, saves, history, challenge/Rivals progression, and daily results remain in app-private DataStore files.
- Backup, full backup, and device transfer are disabled.
- Data is not app-encrypted at rest.
- Deleting a profile permanently removes that profile and history from the DataStore root, except the final profile cannot be deleted.

### Network/peer data

- No `INTERNET` permission or developer-controlled API client is present.
- No app-owned analytics, ads, tracking, or crash-reporting SDK is present in the dependency graph.
- Nearby peers exchange player/session data, full board/game state, scores, used pieces, move history, and connection protocol messages through Google Play services Nearby.
- Google’s current Nearby Connections documentation states that the Google Play services Nearby SDK collects usage analytics: discovery/connection performance metrics plus device model, country, build version, and application package name. Users can control this through Google usage-and-diagnostics settings. This collection is outside the app’s own source and cannot be inferred away from the absence of an `INTERNET` permission in the app manifest.
- Authentication digits are displayed for user confirmation.
- Endpoint IDs/tokens are not persisted by the app.
- Host authorization maps endpoints to owner indexes before accepting move/pass requests.
- The protocol has no app-owned cryptographic signature/version/replay layer; transport security behavior is delegated to Google Play services.

`PRIVACY-POLICY.md` accurately states the app-owned local-storage and peer-exchange model, but its unconditional claims that no third-party analytics or data collection occurs need release review against Google’s documented Nearby SDK usage analytics. Store-facing privacy/data-safety disclosures remain a release/manual responsibility.

### Manifest and static guards

- cleartext disabled
- only launcher intended exported
- no broad FileProvider
- raw transport bypasses prohibited by Semgrep/custom DeepSec matchers
- sensitive Android logging patterns checked
- release identity test rejects external game names and user-facing AI wording
- build dependency test rejects remote avatar/network-image dependencies

No Android log call was found in production source during this audit.

## Tests

### Declared test inventory

The checked-in source contains 279 JVM `@Test` methods and 14 instrumented `@Test` methods.

The full direct verification command listed at the top of this document returned `BUILD SUCCESSFUL` on 2026-07-28. Its fresh XML result contains 279 passing JVM tests with no failures, errors, or skips. The same command passed main/test ktlint, detekt, Android lint, debug assembly, and Android-test compilation. Targeted `GameScreenTest` and `MatchReviewDialogTest` executions passed on a Pixel 9 Pro API 36 emulator; this is not evidence for every connected test or a physical device.

### Model and engine

Tests cover:

- board shape/value/snapshot behavior and serialization rejection
- shared constants and mode configuration
- piece catalog/transforms/target cells
- deterministic seed mixing
- start-corner, diagonal, same-edge, opponent-contact, invalid-player/turn/game-over precedence
- pass/game-over transitions
- scoring, duplicate bonuses, completion, ranking, owner aggregation
- bonus generation
- engine purity and package dependency boundaries
- history statistics, daily streaks, and Hall of Fame sorting/ranking

### Opponents

Tests cover:

- generator legality and empty-move behavior
- evaluator bonus preference
- deterministic action selection
- legal action across all difficulties and pass fallback
- difficulty parameters/mapping/clamping
- 12-character roster identity, ordering, color range, unlocks, next challenger, and first-win progression
- Rival session identity/style/difficulty/compact board

### Sessions and Nearby

Tests cover:

- local publication coherence, concurrency, fresh restart, restore, passes, Solo/computer loops, cancellation-sensitive replacement
- protocol round trips and strict decoding
- host accepted/rejected move/pass/full-sync behavior
- facade type boundary and non-BYTES payload rejection
- permission SDK bands and manifest/dependency/UI terms
- coordinator hosting/discovery replacement, accept/reject/capacity, ownership, Two-color mapping, payload ordering, authorization, invalid sync, status codes, disconnect/reconnect/timeout, and stale callback generations
- NearbySession host/client requests, events, reconnect terminal state, host reset/replacement, invalid sync, client replacement denial, and mutex serialization

### Data/ViewModel

Tests cover baseline repository save/clear/settings/profile/history behavior, serializer defaults/strict required fields, ViewModel base-mode play, settings, saves, profiles, Nearby delegation, cancellation boundaries, game-over history, Two-color owner aggregation, localized fallbacks, local-only review availability, review navigation/close, repository-free analysis, and stale-emission cancellation on game replacement.

### Match review

Dedicated JVM tests cover exact engine replay, voluntary mid-game and finishing passes, automatic no-move skipping, identity/owner/bonus preservation, typed replay failures, scoring thresholds and accuracy edge cases, voluntary/forced pass assessment, Two-color owner 0 slots, incremental progress, and deterministic repeated analysis. The review models are deliberately not serializable.

Direct dedicated tests were not found for every newly added `ProfileRepository` progression mutator (`recordChallengeStars`, `recordRivalResult`, `addAchievements`, `recordDailyBest`), `AchievementEvaluator`, `ChallengeLevels`, daily/replay ViewModel flow, or finished Rival persistence/result flow. Some related calculators/roster/session pieces are tested independently, but these end-to-end gaps are important review targets.

### Compose instrumented tests

`GameScreenTest` currently checks:

- board and accessible transform/pass controls
- local Nearby-status hiding
- History/Stats tabs
- sound/haptics settings and absence of reduced-motion UI
- help rule sections
- Nearby endpoint/pending/error/status actions
- game-over score breakdown
- local-only Review game entry and immediate Game Over dialog dismissal

`MatchReviewDialogTest` checks analyzing/progress state, completed accuracy/classification state, boundary navigation semantics/callbacks, best-move mode and board description, and closeable failure UI. The targeted review and GameScreen classes were executed on the Pixel 9 Pro API 36 emulator during the implementation pass.

It does not directly test the new Challenge dialog, Rivals dialog/intro, Top 20 tab, profile editor, drag/drop coordinates, breakpoint layout, splash animation, animations at font scale, or per-cell accessibility.

### Build/release policy tests

Source-driven tests guard:

- identity/version/label and external-name/AI restrictions
- manifest backup/cleartext/export policy
- launcher and system/custom splash resources
- Nunito license packaging
- repository policy
- AGP/Kotlin/Hilt/KSP/JaCoCo/OWASP/signing compatibility
- dependency verification metadata/signatures/checksums
- Compose BOM/Material 3/no-unused-seam policy
- lint SDK/fatal policy
- centralized colors and tracked strict stability baselines

Some build-tool tests launch nested Gradle commands and depend on generated release manifest output; their success must be established by an actual test run.

## Static analysis, security tooling, and CI

### Android lint

`app/build.gradle.kts`:

- aborts on errors
- checks release builds
- keeps warnings non-fatal
- marks `OldTargetApi` fatal
- enables API, permission, resource, localization, thread, RTL, content-description, and leak checks
- disables dependency/version freshness checks
- adds `com.android.security.lint:lint`

`app/lint.xml` limits an `ObsoleteSdkInt` exception to the v26 launcher resource.

### ktlint and detekt

Root/app ktlint ignore generated/build output; app uses Android mode.

Detekt:

- uses `config/detekt/detekt.yml`
- builds on defaults
- runs in parallel
- references `app/detekt-baseline.xml` (the file is not present in the current file inventory)
- enables Compose rules
- allows 60-line methods with `@Composable` ignored
- allows a 600-line class but production `GameViewModel` and `GameScreen.kt` use explicit suppressions for current size/function counts

The large ViewModel and screen are therefore consciously allowed in source but remain high-risk redesign surfaces.

### Compose Stability Analyzer

The app tracks:

- `app/stability/app-debug.stability`
- `app/stability/app-release.stability`

Validation is enabled, fails on change, rejects missing baselines, and writes to `app/stability`. Baselines are configuration evidence, not a fresh stability pass.

### JaCoCo and Sonar

`jacocoDebugUnitTestReport`:

- depends on `testDebugUnitTest`
- emits XML/HTML under `app/build/reports/jacoco/jacocoDebugUnitTestReport`
- includes Java, standard Kotlin, and AGP built-in Kotlin class paths
- excludes Android generated classes, tests, previews, composable singleton output, and `di`

Sonar configuration:

- project `Insaner1980_Corners_Apart_Android`
- organization `insaner1980`
- host `https://sonarcloud.io`
- sources `app/src/main/java`
- tests JVM + instrumented directories
- coverage excludes `App.kt`, `MainActivity.kt`, all `ui/screens`, and `PlayServicesConnectionsClientFacade`

The root task builds debug and coverage before analysis. `tools/sonar.ps1` is the only non-delegating repository wrapper; its current working-tree version requires the explicit `-AllowExternalUpload` switch before either the Gradle Sonar upload or direct `sonar.exe` operations, also requires `SONAR_TOKEN` or a Gradle token property for the full Gradle scan, writes `reports/sonar.txt`, and optionally exports issues with `sonar.exe`.

### Dependency and security scanners

OWASP Dependency-Check:

- scans debug/release runtime classpaths
- writes HTML/JSON to `reports`
- defaults CVSS failure threshold to 7
- supports env-controlled cache/update/NVD values
- disables OSS Index
- suppresses a verified false CPE match for `compose-stability-runtime-android`

Semgrep rules check backup/cleartext, exported background components, broad FileProvider paths, raw Nearby transport bypasses, and sensitive logging.

MobSF config excludes build/test/report/tool data and filters to warning/error.

DeepSec:

- private local Node tool under `.deepsec`
- `deepsec` 2.2.9, TypeScript 7.0.2
- project `corners_apart_android`
- priority manifest/multiplayer/data/model paths
- custom Android export, sharing, transport, boundary, and log matchers

OSV configuration contains explicit build-tool-only package overrides in `gradle/osv-scanner.toml` and one time-limited DeepSec transitive ignore in `.deepsec/osv-scanner.toml`. These do not describe Android APK runtime dependencies.

Gradle dependency verification is configured in `gradle/verification-metadata.xml`; policy tests require metadata/signature verification, SHA-256 per artifact, no trusted-artifact bypass, and no wildcard trusted-key scope.

### GitHub Actions

`build.yml`, push/PR to `main`:

1. pinned checkout
2. Temurin JDK 17
3. pinned Gradle setup
4. debug assemble
5. unit tests
6. detekt
7. Android lint

All Gradle steps disable configuration cache.

`sonar.yml`, push/PR to `main`:

- full-depth checkout
- JDK 17 and Gradle setup
- mandatory `SONAR_TOKEN` check; the current working-tree workflow exits with code 2 when the secret is absent
- `assembleDebug jacocoDebugUnitTestReport sonar`

`codeql.yml`, push/PR plus Monday 06:00:

- initializes `java-kotlin` in manual-build mode
- installs JDK 17 and Gradle support
- builds `:app:assembleDebug` without configuration cache
- publishes analysis under category `/language:java-kotlin`

Dependabot weekly ecosystems:

- npm in `/.deepsec`
- Gradle at root
- GitHub Actions at root

### Local wrapper map

All except Sonar delegate to `C:\Dev\Android-check\tools\InvokeProjectCheck.ps1`:

| Script | Delegated command |
|---|---|
| `tools/ac.ps1` | `android-check` |
| `tools/cr.ps1` | `compose-rules` |
| `tools/cs.ps1` | `compose-stability` |
| `tools/db.ps1` | `dependabot-check` |
| `tools/dc.ps1` | `dependency-check` |
| `tools/ds.ps1` | `deep-sec` |
| `tools/ga.ps1` | `google-android-security` |
| `tools/lc.ps1` | `lint-check` |
| `tools/ms.ps1` | `mobsf-scan` |
| `tools/os.ps1` | `osv-scan` |
| `tools/pc.ps1` | `pmd-check` |
| `tools/ql.ps1` | `codeql-check` |
| `tools/sc.ps1` | `security-check` |
| `tools/ss.ps1` | `secret-scan` |

`reports/` is ignored and must not be committed. Wrapper output is not evidence of current status unless the relevant wrapper was actually run; direct analyzer/Gradle output is the stronger authority for a fresh audit.

## Non-negotiable implementation invariants

- Keep namespace/application/service identity `com.finnvek.cornersapart` unless an intentional product migration updates every authority and release guard.
- Keep one source of truth for each rule/config/token concept.
- Keep `model -> engine` forbidden and the engine pure.
- Use `GameModeConfigs` for mode starts/owners/computer slots.
- Use `PieceCatalog` and `PieceTransforms` for all piece geometry/orientations.
- Use `model.targetCells` for anchor-plus-offset placement coordinates.
- Use `Scoring.rankPlayers` for final/history ranking, especially Two-color owner aggregation.
- Keep DataStore behind repositories.
- Keep Play Services types behind `ConnectionsClientFacade`.
- Keep host validation authoritative and endpoint ownership checked before host delivery.
- Validate restored/remote game-state index domains.
- Do not persist Nearby games/history as local authoritative results without a deliberate product/security design.
- Do not persist match-review timelines, assessments, progress, or selection; do not expose review for saved/history/Nearby games without a new product and protocol design.
- Keep theme-equivalent colors and shared visual values centralized.
- Keep user-facing prose in resources; if code-owned names/glyphs remain, treat them as intentional exceptions and review localization/accessibility impact.
- Preserve Finnish user communication/commit/comment convention while keeping current app UI English until proper locale resources are added.
- Do not add Room, raw transports, remote avatars, external board-game identity, or AI marketing to v1.

## Known limitations and exact review targets

### Architecture and correctness

- `GameViewModel.kt` is approximately 1,000 lines and `GameScreen.kt` approximately 1,400 lines. Their explicit detekt suppressions are a signal to examine ownership before adding more features.
- Challenge and Rival opponent difficulty is not written into history; history uses global settings. This also feeds difficulty-based achievements/statistics.
- Daily game-over replay routes to normal Solo instead of the daily seed.
- Locked challenge enforcement exists in UI, not in `startChallengeLevel()`.
- New profile progression collections are not all defensively copied by `Profile.toSnapshotCopy()`.
- The first completed game cannot be labeled “new best” because prior history must be non-empty.
- Resume summary difficulty is sourced from current settings rather than the saved settings root.
- Profile name length/content beyond trim/fallback is not constrained.

### Nearby

- No explicit leave/disconnect control is wired to the screen.
- SDK 31 requests both coarse and fine location although the current official Nearby sample lists only fine location for SDK 29–31; verify the actual Play Services/device requirement before release.
- Lobby players, owner assignments, and reconnecting indexes are not exposed in `GameUiState`.
- `SessionPlayer.isLocal` is role-wide, not true controller ownership.
- Client accepted moves do not produce the same positive `GameEffect` path as local/host accepted moves.
- Client host loss is terminal `DISCONNECTED`; only host-side remote-owner timeout/rejoin exists.
- Owner reservation is not endpoint-identity durable.
- One pending authentication decision is represented at a time.
- No app protocol version/migration, replay/sequence guard, size guard, or app-level integrity/authentication layer.
- Physical P2P_STAR capacity, discovery, permission, and reconnect behavior remains unverified.

### UI and redesign

- Fixed dark theme only; no light/dynamic color.
- Width-only 840 dp breakpoint; no compact-height/fold/posture policy.
- Entire screen scrolls as one container; expanded columns do not scroll independently.
- A full 20×20 board remains square/full-width in compact mode, so physical cell size can become very small.
- Canvas board is not cell-addressable to accessibility services.
- No non-pointer board placement flow.
- No reduced-motion setting despite several continuous/entrance animations.
- Profile avatar styles are metadata-only and have no visual generator.
- Several glyphs/names are code-owned rather than resource-owned.
- Resume displays raw epoch milliseconds instead of a formatted local date/time.
- Sound playback does not wait for load completion.
- New Challenge, Rivals, Top 20, launch, breakpoint, drag, and large-font accessibility paths need more direct Compose/device tests.

### Release/manual

- Compact Duel play testing.
- Two-device and multi-client Nearby stress/background/permission tests.
- Emulator/device screenshots at phone/tablet widths, landscape, large font, display scaling, RTL, and TalkBack.
- Release signing/minification/resource shrinking and final manifest inspection.
- Fresh lint/detekt/stability/security/Sonar/coverage evidence.
- Privacy policy/store data-safety alignment for the final artifact and Play Services behavior.

## UI redesign planning map

For a future redesign, preserve behavioral ownership and separate visual restructuring from rule/session changes:

| Redesign area | Current owner | Must remain stable |
|---|---|---|
| App shell/launch | `MainActivity.kt`, `CornersApartLaunch.kt` | Hilt entry, system splash, input block until overlay ends |
| Screen state wiring | `GameRoute`, action data classes | lifecycle collection, permission gate, effects |
| Layout | `GameScreenContent`, compact/expanded functions | dialog ownership, board/session actions |
| Board | `GameBoard.kt`, `BoardDragController` | engine-backed legality, lifted/clamped anchor, slot-to-color mapping |
| Game header/actions | `Header`, `UtilityActions`, `NearbyActions` | all current entry points and permission gating |
| Pieces | `PiecePanel`, `PieceCard`, `SelectedPiecePreview` | catalog IDs, used/selected state, transform actions |
| Score/status | `PlayerScoreBar`, `StatusLine` | owner-aware names/colors, passed/current state |
| Progression | Challenge/Rivals/History/Top 20 dialogs | profile-local persistence and result semantics |
| Visual system | `ui/theme/*`, candy components | centralized tokens and 48 dp controls |
| Accessibility | route announcements, semantics, resources | rejection reason specificity and live feedback |

Before changing navigation or screen ownership, explicitly decide:

1. Whether the single surface becomes a real navigation graph.
2. Whether game state survives route changes only in ViewModel or through save/restore.
3. Where Nearby lobby/leave/reconnect controls live.
4. Whether Challenges, Rivals, Profiles, and Top 20 become screens rather than modal dialogs.
5. How board placement works for TalkBack/keyboard/switch access.
6. How reduced motion affects launch, pulse, pop, confetti, dialog, score, progress, and Rival animations.
7. Whether profile avatar metadata becomes a real local renderer or is removed.
8. Whether daily/challenge/Rival result semantics are corrected before visual redesign so UI does not fossilize current inconsistencies.

Any redesign should first add focused tests for the behavior being moved, then keep `GameEngine`, repositories, sessions, and protocol ownership unchanged unless the change is explicitly architectural.
