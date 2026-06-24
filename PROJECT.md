# Corners Apart Android Project Reference

Last verified from the live checkout on 2026-06-20.

This document is the current-state project map for `C:\Dev\Corners_Apart_Android`. It is written for precise future code review questions, implementation planning, and agent handoff. Treat the source code as the final source of truth after any future code changes.

## Executive Summary

Corners Apart is a native Android polyomino strategy game written in Kotlin with Jetpack Compose, Material 3, Hilt, DataStore, kotlinx.serialization, and Google Play services Nearby as the v1 multiplayer transport direction.

The app package, namespace, and application id are `com.finnvek.cornersapart`. The user-facing app name is `Corners Apart`. The root Gradle project is `CornersApart`, and the only included module is `:app`.

The current implementation has a working pure Kotlin game engine, local sessions, a playable Compose game screen, rule-based computer opponents for Solo mode, multiple mode configurations, JSON DataStore repository scaffolding, a Nearby session/protocol abstraction, Android runtime permission handling for Nearby, polish dialogs, history/stat calculation models, unit tests, instrumented Compose tests, CI workflows, and several local static-analysis/security wrappers.

Current non-release state: `GameViewModel` is repository-backed for settings, saved games, active profiles, history, and Nearby UI state. Local sessions are created through `LocalSessionFactory`, saved games include a settings snapshot, resume/profile/settings/history/game-over flows are wired into Compose, and Nearby has a concrete Google Play services adapter behind `NearbyConnectionsCoordinator` and `ConnectionsClientFacade`. The current playable board path in `GameViewModel` still uses `LocalSession`; Nearby session/protocol/coordinator logic exists below the UI, while Compose currently exposes create/find actions but not a complete endpoint-selection/auth/gameplay flow. Release-only items such as privacy placeholders, Compact Duel physical play-test coverage, two-device Nearby stress testing, full Nearby gameplay UI, and Play Store data-safety remain outside this non-release implementation pass.

## Repository Identity

- Root path: `C:\Dev\Corners_Apart_Android`
- Root project name: `CornersApart`
- Gradle module graph: root plus `:app`
- Namespace: `com.finnvek.cornersapart`
- Application id: `com.finnvek.cornersapart`
- App label: `Corners Apart`
- Version code: `1`
- Version name: `1.0.0`
- Release signing environment prefix: `CORNERS_APART`
- Primary language: Kotlin
- UI framework: Jetpack Compose with Material 3
- Minimum SDK: `26`
- Compile SDK: `36`
- Target SDK: `36`
- Java/Kotlin JVM target: 17
- License: MIT, copyright 2026 Finnvek

## Source-Of-Truth Order

Use this order when answering implementation or review questions:

1. Live source code under `app/src/main/java/com/finnvek/cornersapart/`.
2. Tests under `app/src/test/java/` and `app/src/androidTest/java/`.
3. Build, manifest, resource, and tool config files.
4. `AGENTS.md`, `memory/MEMORY.md`, and this `PROJECT.md` for architecture guardrails and handoff context.
5. Historical specs such as `corners_apart_android_spec_reviewed.md`; these explain intended v1 behavior but can be stale compared with code.

Do not treat `blokus_standard_rules.md`, prototype docs, or external commercial game material as implementation sources. The app must remain Corners Apart with original wording, presentation, and product identity.

## Implementation Status

Implemented now:

- Android app identity, manifest, launcher activity, Hilt application class, and Compose entry point.
- Pure Kotlin model layer for game state, players, board snapshots, pieces, moves, settings, profiles, history, score breakdowns, and mode configuration.
- Pure Kotlin engine for new game creation, placement validation, move application, passing, valid move enumeration, previews, scoring, ranking, turn advancement, and game-over detection.
- Piece catalog with 21 pieces and 89 total cells.
- Bonus tile templates and deterministic seed-based transforms for standard and compact boards.
- Five game modes: Solo, Two-Color Duel, Compact Duel, Three-Player, and Four-Player.
- Rule-based local computer opponents with 3 styles and 5 difficulty levels.
- `LocalSession` for local play and Solo computer turns.
- Nearby protocol/session abstractions and concrete adapter: `GameMessage`, `GameProtocol`, `HostGameCoordinator`, `NearbySession`, `NearbyTransport`, `NearbyConnectionsCoordinator`, `ConnectionsClientFacade`, `PlayServicesConnectionsClientFacade`, lobby reconnect state, endpoint-owner authorization, and host-authoritative validation. This layer is implemented and unit-tested, but it is not yet the active `GameViewModel` board-session owner.
- Nearby runtime permission policy and manifest permission declarations for current target SDK bands.
- JSON DataStore repository layer for saved game, profiles, and settings.
- Compose game UI with board, score bar, mode chips, piece strip, selected-piece preview, rotate/flip/pass controls, resume/profile/settings/help/history dialogs, game-over dialog, haptics, local sound cues, and accessibility announcements.
- Repository-backed `GameViewModel` flow for settings, save/resume, active profile history, ranked game-over state, and Nearby state.
- Settings dialog for difficulty, preferred mode, sound, haptics, and reduced motion.
- Profile dialog for local profile selection, creation, and editing with local avatar styles.
- Saved-game resume dialog backed by `SavedGameData` settings snapshots.
- Game-over and history entries use `Scoring.rankPlayers` so Two-Color Duel owner aggregation is preserved.
- Centralized theme tokens for colors, spacing, shapes, typography, alpha, and animation durations.
- Unit and instrumented tests across model, engine, opponents, multiplayer, data, ViewModel, UI policy, UI screen smoke, theme, and release identity.
- GitHub Actions build/test/lint workflow, SonarCloud workflow, CodeQL workflow for Actions, Dependabot config, Gradle dependency verification metadata, ktlint, detekt config, Compose Stability Analyzer dumps, Android security lint checks, Semgrep config, DeepSec config, MobSF config, OSV config, Dependency-Check config, and Android-check wrapper scripts.

Remaining or release-only:

- `navigation-compose` is a dependency, but there is currently a single `MainActivity`/`GameRoute` surface rather than a navigation graph.
- `MotionPolicy` and `GameSoundPolicy` are wired for reduced-motion durations and local event sound policy, but additional visual animation polish can still be expanded.
- Physical two-device Nearby stress testing remains a manual/release verification item.
- Compact Duel still needs manual play-test coverage before release claims.
- `PRIVACY-POLICY.md` is present but still contains placeholders (`[App Name]`, `[your email]`, `[date]`).

## Build System

Gradle files:

- `settings.gradle.kts`: configures plugin repositories, dependency repositories, root project name, and includes `:app`.
- `build.gradle.kts`: root plugin aliases, ktlint root config, Sonar root config, and root `sonar` task dependency wiring.
- `app/build.gradle.kts`: Android app module config, build types, signing gate, lint, Kotlin compiler options, Hilt, ktlint, detekt, Dependency-Check, JaCoCo, and dependencies.
- `gradle/libs.versions.toml`: central dependency and plugin version catalog.
- `gradle/wrapper/gradle-wrapper.properties`: Gradle wrapper distribution.
- `gradle.properties`: Gradle, Android, and Kotlin build properties.

Current toolchain versions from `gradle/libs.versions.toml` and wrapper config:

| Item | Version |
|---|---:|
| Gradle wrapper | 9.4.1 |
| Android Gradle Plugin | 9.2.0 |
| Kotlin | 2.4.0 |
| KSP | 2.3.9 |
| Hilt | 2.59.2 |
| AndroidX Hilt Navigation Compose | 1.3.0 |
| Compose BOM | 2026.05.01 |
| Compose Stability Analyzer | 0.10.0 |
| Navigation Compose | 2.9.8 |
| Lifecycle | 2.10.0 |
| Coroutines | 1.11.0 |
| DataStore | 1.2.1 |
| Core KTX | 1.18.0 |
| Activity Compose | 1.13.0 |
| kotlinx.serialization | 1.11.0 |
| Play Services Nearby | 19.3.0 |
| JaCoCo | 0.8.15 |
| ktlint Gradle plugin | 14.2.0 |
| detekt | 1.23.8 |
| detekt Compose rules | 0.4.27 |
| Android Security Lints | 1.0.4 |
| OWASP Dependency-Check | 12.2.2 |
| SonarQube Gradle plugin | 7.3.1.8318 |
| JUnit | 4.13.2 |
| MockK | 1.13.16 |
| AndroidX Test JUnit | 1.3.0 |
| AndroidX Test Runner | 1.7.0 |

AGP 9 built-in Kotlin is used for the app module. Do not add `org.jetbrains.kotlin.android` to `app/build.gradle.kts` unless the project intentionally reverses that architecture decision. The app module applies the Android application plugin, Compose compiler plugin, Kotlin serialization plugin, Compose Stability Analyzer plugin, KSP, Hilt, ktlint, detekt, Dependency-Check, and JaCoCo. Android security lint checks are added through the `lintChecks(libs.android.security.lints)` dependency.

Gradle repository policy:

- Plugin resolution uses `google`, `mavenCentral`, and `gradlePluginPortal`.
- Dependency resolution uses `google` and `mavenCentral`.
- `repositoriesMode` is `FAIL_ON_PROJECT_REPOS`.

Gradle properties:

- Configuration cache is enabled.
- Build cache is enabled.
- Parallel Gradle execution is disabled.
- Worker max is `2`.
- AndroidX is enabled.
- Kotlin code style is official.
- Android non-transitive R classes are enabled.
- R8 full mode is enabled.
- Kotlin compiler execution strategy is in-process.

## Build Types And Signing

Debug:

- No debug-specific build config fields are currently defined.

Release:

- `isDebuggable = false`
- `isMinifyEnabled = true`
- `isShrinkResources = true`
- Uses default optimized Android ProGuard file plus `app/proguard-rules.pro`.
- Uses release signing config only when all required env vars are present.

Release signing env vars:

- `CORNERS_APART_KEYSTORE_PATH`
- `CORNERS_APART_KEYSTORE_PASSWORD`
- `CORNERS_APART_KEY_ALIAS`
- `CORNERS_APART_KEY_PASSWORD`

The Gradle task graph rejects release artifact tasks such as assemble, bundle, package, or publish release variants if these signing env vars are missing.

## Main Commands

Project commands from `AGENTS.md` and build files:

- `.\gradlew.bat assembleDebug`: debug build.
- `.\gradlew.bat test`: unit tests.
- `.\gradlew.bat :app:detekt`: detekt static analysis.
- `.\gradlew.bat lint`: Android lint.
- `.\gradlew.bat :app:jacocoDebugUnitTestReport`: JaCoCo XML and HTML unit-test coverage report for debug.
- `.\gradlew.bat sonar`: Gradle Sonar analysis task; root task depends on `:app:assembleDebug` and `:app:jacocoDebugUnitTestReport`.

User-owned wrappers:

- `lint-check` / `lc`: ktlint, detekt, Android lint, reports under `reports/`.
- `security-check` / `sc`: Semgrep and OWASP Dependency-Check, reports under `reports/`.

Do not run `lc` or `sc` yourself. The user runs them when wanted. If asked to read lint results, read `reports/ktlint.txt`, `reports/detekt.txt`, and `reports/lint.txt`. If asked to read security results, read `reports/security-code.txt` and `reports/security-deps.txt`.

## Android Manifest And Runtime Boundaries

Manifest path: `app/src/main/AndroidManifest.xml`.

Declared permissions:

- `ACCESS_WIFI_STATE`, max SDK 31.
- `CHANGE_WIFI_STATE`, max SDK 31.
- `BLUETOOTH`, max SDK 30.
- `BLUETOOTH_ADMIN`, max SDK 30.
- `ACCESS_COARSE_LOCATION`.
- `ACCESS_FINE_LOCATION`, min SDK 29 and max SDK 31.
- `BLUETOOTH_ADVERTISE`, min SDK 31.
- `BLUETOOTH_CONNECT`, min SDK 31.
- `BLUETOOTH_SCAN`, min SDK 31, `neverForLocation`.
- `NEARBY_WIFI_DEVICES`, min SDK 32, `neverForLocation`.

Application flags:

- `android:name=".App"`
- `android:allowBackup="false"`
- `android:dataExtractionRules="@xml/data_extraction_rules"`
- `android:fullBackupContent="false"`
- `android:usesCleartextTraffic="false"`
- `android:icon="@mipmap/ic_launcher"`
- `android:label="@string/app_name"`
- `android:supportsRtl="true"`
- Theme: platform Material Light NoActionBar.

Components:

- `MainActivity` is exported and is the launcher activity.
- No services, receivers, providers, or other exported components are currently declared.

Backup and device-transfer policy:

- `app/src/main/res/xml/data_extraction_rules.xml` excludes root path `.` from cloud backup.
- The same file excludes root path `.` from device transfer.

## Application Entry

`App.kt`:

- Declares `class App : Application()`.
- Annotated with `@HiltAndroidApp`.

`MainActivity.kt`:

- Extends `ComponentActivity`.
- Annotated with `@AndroidEntryPoint`.
- Calls `enableEdgeToEdge()`.
- Sets Compose content to `CornersApartTheme`, `Surface(fillMaxSize())`, and `GameRoute`.
- Nearby runtime permission launcher lives in `GameRoute`, not in `MainActivity`.

## Package Structure

Current source package root: `app/src/main/java/com/finnvek/cornersapart/`.

Main package areas:

- `model/`: serializable domain data, save/profile/settings/history models, game constants, game modes, piece catalog, board state, score models, and piece transforms.
- `engine/`: pure rules, validation, scoring, ranking, bonus layout generation, candidate corner logic, and move result types.
- `opponents/`: local rule-based computer opponent move generation, evaluation, difficulty parameters, styles, and action selection.
- `multiplayer/`: local and Nearby session boundaries, protocol messages, host coordinator, lobby/reconnect state, runtime permission policy, and transport abstraction.
- `data/`: JSON DataStore serializers, state-store wrapper, repository classes, and Hilt persistence/runtime modules.
- `viewmodel/`: `GameViewModel`, `GameUiState`, UI player/piece models, and one-shot effects.
- `ui/screens/`: game route, game screen content, board rendering, score bar, dialogs, history/stats, layout/motion policies.
- `ui/components/`: piece drawing helpers.
- `ui/theme/`: colors, spacing, animation tokens, alpha tokens, typography, shapes, player palette, and Material theme.

Current source counts:

- Main Kotlin source files: 81
- Unit-test Kotlin source files: 38
- Instrumented-test Kotlin source files: 1

Package dependency note:

- These package areas are descriptive boundaries inside the single `:app` Gradle module, not separately enforced Gradle modules.
- Current code has a deliberate `model -> engine` reference from `ScoreBreakdown.plus(ScoreDelta)` and `engine -> model` references from scoring/rules code. Resolve that cycle first if `model` and `engine` are ever split into separate modules.
- `GameRuntimeModule` currently lives in `data/` and provides `TimeProvider`/`SystemTimeProvider` from `viewmodel/`, so a future module split must either move the clock abstraction or document that runtime DI depends on the UI-state package.

## Domain Constants

`GameConstants` centralizes shared v1 constants:

- Standard board size: 20.
- Compact board size: 14.
- Player count: 4.
- Piece count: 21.
- Total piece cells: 89.
- Standard bonus tile count: 10.
- Compact bonus tile count: 6.
- Bonus tile points: 3.
- Placed cell points: 1.
- Completion bonus points: 10.
- Max history entries: 50.
- Difficulty levels: 5.
- Board interaction lock: 160 ms.
- Invalid feedback cooldown: 180 ms.
- Opponent turn delay min/range: 300 ms plus 400 ms range.
- Turn advance delay: 400 ms.
- Human auto-pass delay: 1500 ms.
- Save notification duration: 2000 ms.
- Reconnect timeout: 60000 ms.
- Background timeout: 300000 ms.
- Max avatar dimension: 160.
- Max avatar file size: 5 MiB.
- Player names and color labels: Indigo, Amber, Coral, Teal.

The standard constants also include historical corner lists, but current game-mode corner ownership is generated by `GameModeConfigs`.

## Game Modes

`GameMode` values:

- `SOLO`
- `TWO_COLOR_DUEL`
- `COMPACT_DUEL`
- `THREE_PLAYER`
- `FOUR_PLAYER`

`Ruleset` currently has only `STANDARD`.

`GameModeConfigs` is the single source of truth for mode defaults:

| Mode | Board | Bonuses | Slots | Owners | Computer slots | Starts | Notes |
|---|---:|---:|---:|---|---|---|---|
| `SOLO` | 20 | 10 | 4 | 0, 1, 2, 3 | 1, 2, 3 | slot 0 bottom-right; slot 1 top-left; slot 2 top-right; slot 3 bottom-left | Human is slot 0. Solo auto-runs computer turns in `LocalSession`. |
| `TWO_COLOR_DUEL` | 20 | 10 | 4 | 0, 1, 0, 1 | none | standard four corners | Turn order remains color slots 0-3; rankings aggregate by owner in engine scoring. |
| `COMPACT_DUEL` | 14 | 6 | 2 | 0, 1 | none | top-left and bottom-right | `requiresPlayTesting = true`. |
| `THREE_PLAYER` | 20 | 10 | 3 | 0, 1, 2 | none | top-left, top-right, bottom-right | Uses the first three standard slots. |
| `FOUR_PLAYER` | 20 | 10 | 4 | 0, 1, 2, 3 | none | top-left, top-right, bottom-right, bottom-left | Default mode. |

Standard slot names and colors follow index order:

- 0: Indigo.
- 1: Amber.
- 2: Coral.
- 3: Teal.

## Board Model

`BoardSnapshot` is the immutable serializable board shape:

- `size: Int`.
- `cells: List<Int>`.
- `cells.size` must equal `size * size`.
- `size` must be positive.
- Empty cells use `BoardSnapshot.EMPTY`, value `-1`.
- Occupied cells store player index values.
- Indexing is flat row-major: `row * size + col`.

`MutableBoard` is an engine-side mutable buffer:

- Holds `IntArray`.
- Can be created from `BoardSnapshot`.
- Implements content equality and content hash code.
- Converts back to `BoardSnapshot`.

`CellPosition` is a serializable row/column coordinate.

`CellOffset` is a serializable row/column shape offset.

## Piece Catalog

`PieceCatalog` is the single source of truth for pieces. It contains 21 pieces and 89 total cells.

Piece ids and display names:

| Id | Name | Cells |
|---|---|---:|
| `one-dot` | One Dot | 1 |
| `two-bar` | Two Bar | 2 |
| `three-bar` | Three Bar | 3 |
| `three-corner` | Three Corner | 3 |
| `four-bar` | Four Bar | 4 |
| `four-block` | Four Block | 4 |
| `four-tee` | Four Tee | 4 |
| `four-corner` | Four Corner | 4 |
| `four-step` | Four Step | 4 |
| `five-bar` | Five Bar | 5 |
| `five-block-tail` | Five Block Tail | 5 |
| `five-tee` | Five Tee | 5 |
| `five-cross` | Five Cross | 5 |
| `five-long-corner` | Five Long Corner | 5 |
| `five-shift` | Five Shift | 5 |
| `five-stair` | Five Stair | 5 |
| `five-cup` | Five Cup | 5 |
| `five-wide-corner` | Five Wide Corner | 5 |
| `five-hook` | Five Hook | 5 |
| `five-zag` | Five Zag | 5 |
| `five-offset` | Five Offset | 5 |

Important ids:

- `SINGLE_CELL_ID = "one-dot"`
- `TWO_LINE_ID = "two-bar"`
- `THREE_BEND_ID = "three-corner"`

`PieceTransforms` owns piece transforms:

- Max orientation count: 8.
- `rotateCW`.
- `rotateCCW`.
- `flipH`.
- `normalize`.
- `getAllOrientations`.
- `getOrientation`.
- Orientation lists are cached by piece id in a `ConcurrentHashMap`.
- Orientations are built from four rotations of the original cells plus four rotations of a horizontal flip, then normalized and deduplicated.

## Game State Model

`GameState` is serializable and contains:

- `board`.
- `players`.
- `currentPlayerIndex`.
- `turnNumber`.
- `ruleset`.
- `gameMode`.
- `randomSeed`.
- `bonusTiles`.
- `bonusLayoutId`.
- `moveHistory`.
- `isGameOver`.

`Player` is serializable and contains:

- `index`.
- `name`.
- `colorIndex`.
- `startCorner`.
- `usedPieceIds`.
- `scoreBreakdown`.
- `passed`.
- `isActiveScoring`.
- `isComputerControlled`.
- `ownerIndex`.

`Move` is serializable and contains:

- `playerIndex`.
- `pieceId`.
- `anchorRow`.
- `anchorCol`.
- `orientationIndex`.

`ScoreBreakdown` is serializable:

- `placedCellPoints`.
- `bonusTilePoints`.
- `completionBonus`.
- `total` computed as the sum of those fields.
- `plus(ScoreDelta)` returns an updated breakdown.

`BonusTile` is serializable:

- `row`.
- `col`.
- `claimedByPlayerIndex`.
- `claimedOnTurn`.
- `position` computed as `CellPosition(row, col)`.

## Core Rules

`GameEngine` is the main pure rule entrypoint:

- `newGame(config)`.
- `applyMove(state, move)`.
- `pass(state, playerIndex)`.
- `getValidMoves(state, playerIndex)`.
- `hasValidMove(state, playerIndex)`.
- `previewPlacement(state, move)`.

Placement validation is owned by `PlacementValidator`:

- Rejects moves after game over.
- Rejects non-current player moves when turn enforcement is enabled.
- Rejects invalid player indexes.
- Rejects already-passed players.
- Rejects reused pieces.
- Rejects unknown piece ids.
- Rejects unknown orientation indexes.
- Rejects out-of-bounds placements.
- Rejects placements over occupied cells.
- First placement for each player must cover that player's start corner.
- Same-player edge contact is always rejected.
- After the first placement, same-player diagonal contact is required.
- Opponent edge or corner contact is allowed.
- Claimable bonus tiles are any unclaimed bonus tiles covered by the target cells.

`MoveRejectionReason` values:

- `GAME_OVER`
- `NOT_PLAYERS_TURN`
- `INVALID_PLAYER`
- `PLAYER_HAS_PASSED`
- `UNKNOWN_PIECE`
- `UNKNOWN_ORIENTATION`
- `PIECE_ALREADY_USED`
- `OUT_OF_BOUNDS`
- `CELL_OCCUPIED`
- `START_CORNER_NOT_COVERED`
- `SAME_PLAYER_EDGE_TOUCH`
- `NO_DIAGONAL_TOUCH`

Turn and game-end behavior:

- Accepted moves update the board, player's used piece set, score breakdown, bonus tile ownership, turn number, and move history.
- Accepted moves clear the moving player's `passed` flag.
- Pass is allowed only for the current player.
- Pass marks the current player as passed and increments `turnNumber`.
- Game ends when all active scoring players have passed or have no valid move.
- `nextPlayerIndex` skips inactive, passed, and no-valid-move players.
- If the game is over, current player does not advance.

## Scoring And Ranking

`Scoring.scoreMove`:

- 1 point per placed cell.
- 3 points per claimed bonus tile.
- 10 completion bonus points if the move completes the full piece set.

`Scoring.rankPlayers`:

- Filters to active-scoring players.
- Groups players by `ownerIndex`.
- Combines score breakdowns within each owner group.
- Counts claimed bonus tiles by owner.
- Uses owner name when multiple color slots belong to one owner (`Player {ownerIndex + 1}`).
- Sort order:
  1. Higher total score.
  2. Higher placed-cell points.
  3. Higher claimed bonus tile count.
  4. Fewer remaining pieces.
  5. Lower owner index.

`GameOverDialog` and history entries consume this canonical ranking through ViewModel-prepared `rankedScores`, so Two-Color Duel owner aggregation is preserved in end-of-game presentation.

## Bonus Tile Generation

`BonusTileGenerator` owns deterministic bonus layout selection and transforms.

Constants and templates:

- `MIN_BONUS_DISTANCE = 2` is declared.
- Standard board template id: `standard-cross-01`.
- Standard template board size: 20.
- Standard template positions: `(3,5)`, `(5,14)`, `(5,5)`, `(8,8)`, `(8,11)`, `(11,8)`, `(11,11)`, `(14,5)`, `(14,14)`, `(16,14)`.
- Compact board template id: `compact-balance-01`.
- Compact template board size: 14.
- Compact positions: `(3,3)`, `(3,10)`, `(6,5)`, `(7,8)`, `(10,3)`, `(10,10)`.

Generation:

- Template list is chosen by board size.
- Seed-derived index chooses a template.
- Seed-derived transform chooses one of four transforms.
- Transforms are identity, rotate 180, mirror vertical, or mirror horizontal.
- Returned layout id is `${template.id}:$transform`.
- Returned positions are truncated to requested count.

## Opponent System

`opponents/` owns local computer turns.

`OpponentStyle` values:

- `EXPANSIONIST`
- `OPPORTUNIST`
- `BLOCKER`

`OpponentDifficulty` values and parameters:

| Difficulty | Temperature | Candidate cap | Time budget | Large-piece bias | Bonus awareness | Blocking awareness |
|---|---:|---:|---:|---:|---:|---:|
| `BEGINNER` | 3.0 | 10 | 250 ms | -0.40 | 0.2 | 0.0 |
| `EASY` | 2.0 | 25 | 400 ms | -0.15 | 0.6 | 0.3 |
| `MEDIUM` | 1.0 | 80 | 700 ms | 0.00 | 1.0 | 0.8 |
| `HARD` | 0.5 | 200 | 1200 ms | 0.25 | 1.4 | 1.3 |
| `EXPERT` | 0.2 | 500 | 1800 ms | 0.45 | 1.8 | 1.7 |

`MoveGenerator`:

- Calls `GameEngine.getValidMoves`.
- Sorts moves by larger piece size first, then piece id, orientation index, anchor row, anchor col.
- Applies the difficulty candidate soft cap.
- Converts moves to `MoveCandidate` with placed cell count and claimed bonus tile count.

`MoveEvaluator`:

- Scores placed cells.
- Scores bonus claims.
- Scores spread away from the player's start corner.
- Scores center pressure.
- Scores blocking proximity to opponent start corners.
- Applies style-specific and difficulty-specific weights.

`ComputerOpponentEngine`:

- Runs selection on an injected dispatcher, default `Dispatchers.Default`.
- Uses a monotonic time deadline based on difficulty.
- Returns `OpponentAction.Pass` if no candidates exist.
- Evaluates candidates until deadline.
- Sorts by evaluation total descending.
- Uses seeded temperature-weighted selection unless difficulty temperature is effectively deterministic.
- Seed input includes `GameState.randomSeed`, turn number, player index, difficulty, and style.
- Revalidates the chosen move before returning it.
- Falls back to the first valid evaluated move, then pass.
- Default style by player index: index mod 3 of 1 is Expansionist, 2 is Opportunist, otherwise Blocker.

`LocalSession` auto-runs computer turns only when `gameMode == SOLO`, the game is not over, and the current player is computer-controlled.

## Sessions And Multiplayer

`GameSession` interface:

- `sessionType`.
- `gameMode`.
- `players: StateFlow<List<SessionPlayer>>`.
- `gameState: StateFlow<GameState>`.
- `connectionState: StateFlow<ConnectionState>`.
- `sendMove(move)`.
- `sendPass(playerIndex)`.
- `startNewGame(config)`.
- `replaceState(state)`.

`LocalSession`:

- Defaults to a Four-Player game.
- Publishes `GameState`, `SessionPlayer` list, and `ConnectionState.CONNECTED`.
- Applies moves through `GameEngine.applyMove`.
- Converts engine rejections to `IllegalArgumentException(reason.name)`.
- Applies pass through `GameEngine.pass`.
- Runs Solo computer actions in a loop until the next current player is human or the game ends.

`SessionPlayer`:

- `index`.
- `name`.
- `isLocal`.
- `isComputerControlled`.
- `colorIndex`.
- `ownerIndex`.
- `usedPieceCount`.

`ConnectionState` values:

- `DISCONNECTED`
- `CONNECTED`
- `RECONNECTING`
- `FAILED`

`SessionType` values:

- `LOCAL`
- `NEARBY`

## Nearby Protocol

`GameProtocol` uses kotlinx.serialization JSON:

- Class discriminator: `type`.
- `encodeDefaults = true`.
- `ignoreUnknownKeys = false`.

`GameMessage` types:

- `PlaceMove(move)`, serial name `placeMove`.
- `MoveAccepted(move, state, scoreDelta)`, serial name `moveAccepted`.
- `MoveRejected(move, reason)`, serial name `moveRejected`.
- `Pass(playerIndex)`, serial name `pass`.
- `FullSync(state)`, serial name `fullSync`.
- `PlayerJoined(player)`, serial name `playerJoined`.
- `PlayerLeft(playerIndex)`, serial name `playerLeft`.
- `GameConfig(config)`, serial name `gameConfig`.
- `Ping`, serial name `ping`.
- `Pong`, serial name `pong`.

`HostGameCoordinator`:

- Owns an authoritative `GameState`.
- Accepts `PlaceMove` by applying `GameEngine.applyMove`.
- Broadcasts accepted moves as `MoveAccepted` with full authoritative state and score delta.
- Sends rejected moves back to the endpoint only.
- Accepts pass by applying `GameEngine.pass`.
- Broadcasts pass results as `FullSync`.
- On invalid pass, sends `MoveRejected` with a placeholder empty move and reason.
- Broadcasts `PlayerJoined` and sends a `FullSync` to the new endpoint.
- Broadcasts `PlayerLeft`.
- Responds to `Ping` with endpoint-targeted `Pong`.
- Ignores client-side sync/config/accepted/rejected/pong messages at the host handler layer.

`NearbySession`:

- Has roles `HOST` and `CLIENT`.
- Uses abstract `NearbyTransport`.
- Host calls `HostGameCoordinator` locally and sends resulting messages through transport.
- Client sends `PlaceMove` and `Pass` requests to `MessageTarget.Host`.
- Client applies `FullSync` and `MoveAccepted` messages by publishing the authoritative state.
- Lobby messages update connected/reconnecting player sets.
- `connectionState` is `RECONNECTING` when any player index is marked reconnecting, otherwise `CONNECTED`.
- Host-only `startNewGame` creates a new engine state and coordinator.

`NearbyTransport`:

- Functional interface with `send(target, message)`.
- Concrete Google Play services sends are handled by `NearbyConnectionsCoordinator` through `ConnectionsClientFacade`.

`NearbyConnectionsCoordinator`:

- Owns service id `com.finnvek.cornersapart`.
- Uses `Strategy.P2P_STAR` for advertising and discovery.
- Wraps concrete Play Services APIs behind `ConnectionsClientFacade`.
- Stores pending endpoint name and authentication digits before accept/reject.
- Tracks connected endpoints, approved endpoints, host endpoint id, session role, and endpoint-to-owner mappings.
- Assigns each accepted remote endpoint to the next non-computer owner index that is not the local owner.
- Accepts only BYTES payloads through the facade and decodes them with `GameProtocol`.
- Routes decoded messages into the current `NearbySession`.
- Host-side inbound authorization requires a connected endpoint; `PlaceMove`, `Pass`, and `PlayerJoined` are restricted to the endpoint's mapped owner.
- Unauthorized `PlaceMove` payloads receive `MoveRejected(reason = NOT_PLAYERS_TURN)`.
- Client-side inbound sync is accepted only from the selected connected host endpoint.
- Sends `MessageTarget.Broadcast`, `MessageTarget.Host`, and endpoint-targeted messages as encoded BYTES payloads.
- Host disconnect handling removes the endpoint mapping and marks the departed owner's player slots reconnecting through `PlayerLeft`.
- Marks decode and payload failures as `ConnectionState.FAILED`.

`PlayServicesConnectionsClientFacade`:

- Uses `AdvertisingOptions.Builder().setStrategy(...)` and `DiscoveryOptions.Builder().setStrategy(...)`.
- Translates Play Services connection, discovery, and payload callbacks into testable project callbacks.
- Uses `Payload.fromBytes(...)` for outgoing messages.

## Nearby Permissions

`NearbyPermissions.requiredRuntimePermissions(sdkInt)`:

- SDK <= 28: `ACCESS_COARSE_LOCATION`.
- SDK 29-30: `ACCESS_FINE_LOCATION`.
- SDK 31: `ACCESS_COARSE_LOCATION`, `ACCESS_FINE_LOCATION`, `BLUETOOTH_ADVERTISE`, `BLUETOOTH_CONNECT`, `BLUETOOTH_SCAN`.
- SDK >= 32: `BLUETOOTH_ADVERTISE`, `BLUETOOTH_CONNECT`, `BLUETOOTH_SCAN`, `NEARBY_WIFI_DEVICES`.
- SDK >= 37 additionally adds string permission `android.permission.ACCESS_LOCAL_NETWORK`.

`NearbyPermissions.hasRequiredPermissions` requires every runtime permission returned for the SDK band to map to `true`.

Review note: target/compile SDK are currently 36, and the manifest does not declare `ACCESS_LOCAL_NETWORK`; the code has a future SDK 37 branch.

## Persistence

Persistence is intentionally DataStore plus kotlinx.serialization JSON for v1. Room is intentionally not used.

`CornersApartJson`:

- `encodeDefaults = true`.
- `ignoreUnknownKeys = true`.

DataStore files:

- `saved-game.json`: serialized `SavedGameData`.
- `profiles.json`: serialized `ProfilesData`.
- `settings.json`: serialized `GameSettings`.

Repository factory extensions:

- `Context.gameRepository()`.
- `Context.profileRepository()`.
- `Context.settingsRepository()`.

Hilt module:

- `PersistenceModule`.
- Installed in `SingletonComponent`.
- Provides singleton `GameRepository`, `ProfileRepository`, and `SettingsRepository`.

Runtime Hilt module:

- `GameRuntimeModule`.
- Installed in `SingletonComponent`.
- Provides singleton `GameEngine`.
- Provides singleton `ComputerOpponentEngine` using the shared `GameEngine`.
- Provides singleton `TimeProvider` as `SystemTimeProvider`.
- Provides singleton `ConnectionsClientFacade` as `PlayServicesConnectionsClientFacade`.
- Provides singleton `NearbyConnectionsCoordinator` with the app name as the local endpoint name.

`JsonDataStoreSerializer<T>`:

- Implements `androidx.datastore.core.Serializer<T>`.
- Uses a supplied `KSerializer<T>`.
- Converts kotlinx serialization or illegal argument parse failures to `CorruptionException`.
- Writes encoded JSON bytes.

`JsonStateStore<T>`:

- Abstracts a `Flow<T>` and suspend `update`.

`DataStoreJsonStateStore<T>`:

- Wraps a `DataStore<T>`.
- Exposes `dataStore.data`.
- Calls `dataStore.updateData`.

`GameRepository`:

- Exposes `savedGameData`.
- Exposes `savedGame`.
- `saveGame(state, settings, savedAtEpochMillis)` writes `SavedGameData`.
- `clearSavedGame()` resets to default `SavedGameData()`.

`ProfileRepository`:

- Exposes `profiles`.
- Exposes `activeProfile`.
- `upsertProfile(profile)` inserts or replaces a profile and ensures at least one active profile.
- `setActiveProfile(profileId)` sets exactly one profile active when ids match.
- `appendHistory(profileId, entry)` appends a history entry to the selected profile.
- `appendHistory` keeps only the latest `GameConstants.MAX_HISTORY_ENTRIES` entries.

`SettingsRepository`:

- Exposes `settings`.
- `updateSettings(transform)` updates `GameSettings`.

`GameSettings`:

- `preferredDifficulty`, default 3.
- `soundEnabled`, default true.
- `hapticsEnabled`, default true.
- `reducedMotionEnabled`, default false.
- `preferredMode`, default `FOUR_PLAYER`.
- `preferredRuleset`, default `STANDARD`.

## Profiles, Avatars, And History

`Profile`:

- Serializable.
- Fields: `id`, `name`, `colorIndex`, `avatarStyle`, `avatarSeed`, `customAvatarPath`, `active`, `history`.
- `preferredColorIndex` currently returns `colorIndex`.

`LocalAvatarStyle`:

- `INITIALS`
- `GEOMETRIC`
- `MOSAIC`
- `RINGS`

`LocalAvatarGenerator`:

- Generates local-only avatar descriptors.
- Uses profile `avatarSeed`, falling back to profile `id`.
- Initials are first letters from up to two name parts, uppercase, or `CA` when blank.
- Produces four opaque ARGB palette entries from deterministic hash mixing.
- `GeneratedAvatar.localOnly` defaults to `true`.

`HistoryEntry`:

- `date`.
- `rank`.
- `totalScore`.
- `scoreBreakdown`.
- `claimedBonusTiles`.
- `piecesPlaced`.
- `difficulty`.
- `ruleset`.
- `gameMode`.
- `timeSeconds`.
- `scores`.

`HistoryStatsCalculator`:

- Returns default empty stats for no entries.
- Higher score is better.
- Calculates total games, win count, win rate, average score, best score, average rank, average claimed bonus tiles, completion bonus count, favorite difficulty, score trend, and per-difficulty stats.
- Score trend uses the last 20 entries.
- Favorite difficulty is chosen by highest count, then higher difficulty on ties.

## ViewModel State

`GameViewModel`:

- Annotated with `@HiltViewModel`.
- Injected with `LocalSessionFactory`, `GameRepository`, `ProfileRepository`, `SettingsRepository`, `TimeProvider`, and `NearbyConnectionsCoordinator`.
- Exposes `uiState: StateFlow<GameUiState>`.
- Exposes `effects: SharedFlow<GameEffect>`.
- Defaults selected piece to `one-dot`.
- Defaults orientation index to 0.
- Collects settings from `SettingsRepository` and clamps persisted difficulty through `OpponentDifficultyMapper`.
- Collects saved-game data from `GameRepository`.
- Collects profiles from `ProfileRepository` and creates active default profile `local-default` when the store is empty.
- Collects Nearby UI state from `NearbyConnectionsCoordinator`.
- Does not collect `NearbyConnectionsCoordinator.currentSession`; board input and persisted game state are still produced from the private `LocalSession`.
- Tracks game start time through `TimeProvider`.
- Starts new local games through `LocalSessionFactory`.
- Resets selected piece/orientation on new game.
- Persists `preferredMode` when a new game starts.
- Saves unfinished accepted turns through `GameRepository.saveGame(state, settings, now)`.
- Restores saved games through `LocalSession.replaceState`.
- Records game-over history once through `ProfileRepository.appendHistory`.
- Clears saved game after game over.
- Exposes profile creation, profile update, active-profile selection, and Nearby host/discovery/connect/accept/reject/disconnect methods.
- Selects only pieces available to the current player.
- Rotate clockwise increments orientation index modulo orientation count.
- Rotate counterclockwise subtracts one modulo orientation count.
- Flip normalizes horizontal flip and selects matching orientation when present.
- Places selected piece by sending `Move` to the session.
- Emits `GameEffect.MoveRejected` from session rejection reason names.
- Emits `GameEffect.MoveAccepted` when score delta is positive.
- Emits `GameEffect.GameOver` when an accepted move or pass ends the game.
- Passes current player through `session.sendPass`; pass is ignored after game over.
- Normalizes selection when the selected piece has been used by the current player.

Current Nearby UI limitation:

- `GameScreenActions` exposes only create/find Nearby actions to Compose.
- `GameUiState.nearbyState` contains discovered endpoints, pending connection auth data, errors, and connection state, but `GameScreenContent` does not yet render endpoint selection, auth-token confirmation, accept/reject, disconnect, or an active `NearbySession` game board.

`GameUiState`:

- Contains game mode, board, bonus tiles, players, current player index, selected piece id, selected orientation index, selected cells, piece panel items, game over flag, sound/haptics/reduced motion flags, game duration seconds, preferred difficulty/mode, active profile history/name, saved-game resume summary, ranked scores, Nearby state, and profiles.
- `currentPlayer` returns `players[currentPlayerIndex]`; current configs currently keep player indexes aligned with list indexes.

`PlayerUiState`:

- Includes index, name, color index, owner index, start row/col, score breakdown fields, claimed bonus count, placed/remaining pieces, passed/current/computer flags.

`GameEffect`:

- `MoveRejected(reason)`.
- `MoveAccepted(playerName, scoreDelta, bonusTileClaimed)`.
- `GameOver`.

## Compose UI

`GameRoute`:

- Uses `hiltViewModel<GameViewModel>()`.
- Collects state with `collectAsStateWithLifecycle()`.
- Collects effects in `LaunchedEffect`.
- Performs haptic feedback when enabled.
- Plays local event sounds through `GameSoundPolicy` and `GameSoundPlayer` when sound is enabled.
- Holds local dialog state for history/stats and accessibility announcement.
- Owns the Nearby runtime permission launcher and continues host/discover only after required permissions are granted.
- Passes active-profile history to `HistoryStatsDialog`.

`GameScreenContent`:

- Owns local `showSettings`, `showProfiles`, and `showHelp`.
- Shows `ResumeGameDialog` when a saved game is available and no continue/new-game decision has been made.
- Shows `HistoryStatsDialog` when requested.
- Shows `GameSettingsDialog` for difficulty, preferred mode, sound, haptics, and reduced motion.
- Shows `ProfilesDialog` for local profile selection, creation, and editing.
- Shows `GameHelpDialog`.
- Shows `GameOverDialog` when `state.isGameOver`.
- Uses compact or expanded layout based on `GameLayoutPolicy.modeForWidthDp`.

Layout policy:

- Compact below 840 dp width.
- Expanded at 840 dp or wider.

Primary visible controls:

- Mode chips: Four players, Solo, Two-color duel, Compact duel, Three players.
- Nearby actions: Create nearby game, Find nearby game.
- Utility actions: History & stats, Profiles, Settings, Help.
- Score cards by player.
- Canvas board.
- Status line showing current turn or game over.
- Rotate counterclockwise.
- Rotate clockwise.
- Flip selected piece.
- Pass turn.
- Selected piece preview.
- Horizontal piece panel.

Accessibility and haptics:

- Game board has content description `Game board`.
- Icon buttons have semantic content descriptions.
- Accessibility live announcement node is polite.
- Move accepted uses text-handle haptic unless a bonus tile was claimed.
- Bonus claim and rejection use long-press haptic.
- Game over uses long-press haptic.

`GameBoard`:

- Square Canvas in a dark board frame.
- Draws empty board cells.
- Draws unclaimed bonus tiles as diamond markers.
- Draws start markers on empty start corners.
- Draws occupied cells with glossy piece rendering.
- Converts tap offsets to row/col by board size and calls `onPlaceCell(row, col)`.

`PlayerScoreBar`:

- Chunks player cards into rows of two.
- Highlights current player with ghost color and border.
- Dims passed players.

`GameSettingsDialog`:

- Difficulty selector 1-5.
- Preferred mode selector.
- Sound switch.
- Haptics switch.
- Reduced motion switch.

`ProfilesDialog`:

- Lists local profiles and marks the active profile.
- Can switch the active profile.
- Can add or update local profile name, color index, and avatar style.

`ResumeGameDialog`:

- Shows saved time, mode, leader, claimed bonus count, and difficulty from the saved-game snapshot.
- Continue restores saved state.
- New game clears saved data and starts the preferred mode.

`GameHelpDialog`:

- Goal.
- Start in your corner.
- Corner contact, no edge contact.
- Scoring.
- Bonus tiles.
- Passing and game end.
- Controls.
- Nearby games.

`GameOverDialog`:

- Shows winner, duration, score categories, per-player breakdown, play-again button, and stats button.
- Does not dismiss by outside request.
- Ranking rows are supplied as `List<PlayerScore>` from `Scoring.rankPlayers`.

`HistoryStatsDialog`:

- Uses Material 3 `PrimaryTabRow`.
- Has History and Stats tabs.
- History tab shows empty message or last 20 history rows.
- Stats tab shows total games, wins, average score, best score, average rank, and average bonus tiles.

## Theme And Visual Tokens

Theme files live under `app/src/main/java/com/finnvek/cornersapart/ui/theme/`.

`CornersApartColors`:

- Player Indigo: `0xFF4338CA`.
- Player Indigo dark: `0xFF312E81`.
- Player Indigo highlight: `0xFF6366F1`.
- Player Indigo ghost: `0x4D4338CA`.
- Player Amber: `0xFFE88C0A`.
- Player Amber dark: `0xFFA16207`.
- Player Amber highlight: `0xFFF5B040`.
- Player Amber ghost: `0x4DE88C0A`.
- Player Coral: `0xFFE8513D`.
- Player Coral dark: `0xFF991B1B`.
- Player Coral highlight: `0xFFF08070`.
- Player Coral ghost: `0x4DE8513D`.
- Player Teal: `0xFF0D9488`.
- Player Teal dark: `0xFF134E4A`.
- Player Teal highlight: `0xFF2DD4BF`.
- Player Teal ghost: `0x4D0D9488`.
- App background: `0xFFE4E4E8`.
- Board cell gap: `0xFFDCDCE0`.
- Board cell surface: `0xFFFAFAFA`.
- Board frame: `0xFF2C2C30`.
- Card surface: `0xFFFFFFFF`.
- Bonus accent: `0xFFD8A928`.
- Text primary: `0xFF1A1A1E`.
- Text secondary: `0xFF4A4A52`.
- Text muted: `0xFF8A8A92`.
- On-player color: `0xFFFFFFFF`.

`CornersApartSpacing`:

- Screen padding: 16 dp.
- Section gap: 12 dp.
- Compact gap: 8 dp.
- Tiny gap: 4 dp.
- Board cell gap: 2 dp.
- Board frame width: 4 dp.
- Piece inner inset: 2 dp.
- Piece shadow offset: 1 dp.
- Piece shadow blur: 2 dp.
- Touch target min: 48 dp.
- Piece card size: 64 dp.
- Piece preview size: 84 dp.
- Score card min height: 48 dp.
- Active player border width: 2 dp.

`CornersApartAnimationTokens`:

- Piece placement: 400 ms.
- Invalid attempt shake: 400 ms.
- Bonus tile claimed: 350 ms.
- Active player pulse: 1500 ms.
- Opponent thinking dot: 900 ms.
- Dialog enter: 300 ms.
- Piece card intro: 300 ms.
- Piece card intro stagger: 20 ms.
- Score increase: 250 ms.

`CornersApartAlpha`:

- Passed player: 0.40.
- Used piece: 0.35.
- Piece highlight: 0.35.
- Piece shadow: 0.50.
- Piece inner inset: 0.08.
- Piece drop shadow: 0.12.
- Start marker: 0.55.

Typography:

- Uses bundled `quicksand.ttf`.
- Defines Material typography entries for displayLarge, headlineMedium, bodyLarge, labelLarge, bodySmall, and labelSmall.

Shapes:

- extraSmall 4 dp.
- small 6 dp.
- medium 8 dp.
- large 12 dp.
- extraLarge 16 dp.

## Resources And Strings

Resources:

- `app/src/main/res/values/strings.xml`: English UI strings, plurals, accessibility strings, and dialog text.
- `app/src/main/res/values/colors.xml`: launcher icon background and foreground colors.
- `app/src/main/res/font/quicksand.ttf`: bundled font.
- `app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml`: adaptive launcher icon.
- `app/src/main/res/drawable/ic_launcher_monochrome.xml`: monochrome launcher resource.
- `app/src/main/res/xml/data_extraction_rules.xml`: backup/transfer exclusion policy.

All user-facing UI strings should remain in `strings.xml`. Do not hardcode user-facing text in composables.

Current localization state:

- Only the base `values/strings.xml` file is present.
- UI text is English-first.
- No additional locale directories are present.

## Tests

Unit tests cover:

- Board snapshot, `BoardView`, and mutable board value behavior.
- Game constants.
- Game mode defaults, owners, computer slots, and compact play-testing flag.
- Piece catalog count, total cell count, orientation normalization, uniqueness, and cap.
- Game state JSON serialization round trip.
- Local avatar generation and initials.
- History stats calculation.
- Seed mixing for deterministic non-negative indexes and unit interval selection.
- Placement rules: start corner, diagonal contact, same-player edge rejection, opponent edge allowance.
- Scoring: bonus tiles, completion bonus, ranking, tie-breakers, Two-Color Duel owner aggregation.
- Bonus tile generation.
- Local session move/publish behavior, `LocalSessionFactory`, saved-state replacement, and Solo computer turns.
- Nearby protocol JSON messages and unknown type rejection.
- Nearby permission SDK bands.
- Nearby configuration dependency/manifest/UI terms.
- Host coordinator accepted/rejected moves and full sync.
- Nearby session host/client/reconnect behavior.
- Nearby Connections coordinator facade calls, discovery state, accept/reject, BYTES decode routing, endpoint-owner authorization, host-only sync acceptance, reconnect mapping, and disconnect state.
- Move generator legality.
- Move evaluator bonus preference.
- Computer opponent deterministic seed behavior, legality across difficulties, pass fallback.
- Opponent difficulty mapper persisted `1..5` mapping and clamping.
- Data repositories, saved-game settings snapshots, profile history cap, JSON serializer, and JVM-safe runtime providers.
- GameViewModel initial state, placing, rejection effects, clockwise/counterclockwise rotation, flip, Solo flow, supported modes, repository-backed settings, preferred-mode persistence, saved-game resume/discard, Nearby delegation, profile actions, saved-game persistence, game-over history including Two-Color Duel owner aggregation, and polish settings toggles.
- Game layout policy, motion policy, and sound policy.
- `SystemTimeProvider` epoch millis and ISO date formatting.
- Theme token values.
- Release identity guardrails.

Instrumented Compose tests cover:

- Game screen board and accessible controls.
- History/stats dialog tabs.
- Settings dialog toggles.
- Help dialog rule sections.
- Game-over score breakdown.

Test support:

- `MainDispatcherRule` swaps the main dispatcher for coroutine ViewModel tests.
- `InMemoryJsonStateStore` supports repository tests without Android DataStore.
- Engine fixtures and score fixtures live under `app/src/test/java/com/finnvek/cornersapart/engine/`.

## Static Analysis, Security, And CI

Android lint:

- `abortOnError = true`.
- `warningsAsErrors = false`.
- `checkReleaseBuilds = true`.
- Adds `com.android.security.lint:lint` through `lintChecks`.
- Enables checks including permissions, hardcoded text, missing translation, unused resources, wrong thread, content descriptions, RTL hardcoded, static field leaks, and others.
- Disables `GradleDependency` and `AndroidGradlePluginVersion`.

ktlint:

- Root and app module use `org.jlleitschuh.gradle.ktlint`.
- App module sets `android = true`.
- Ignores generated and build directories.

detekt:

- Config: `config/detekt/detekt.yml`.
- `buildUponDefaultConfig = true`.
- Parallel enabled.
- App baseline path: `app/detekt-baseline.xml`.
- Complexity rules are active with Compose-aware exclusions for annotated composables.
- Long method threshold is 60, ignoring `@Composable`.
- TooManyFunctions thresholds: files 30, classes 25, interfaces 15, objects 11, enums 11.
- Compose detekt rules are active through `io.nlopez.compose.rules`.

Compose Stability Analyzer:

- App module applies `com.github.skydoves.compose.stability.analyzer`.
- Tracked stability dumps are `app/stability/app-debug.stability` and `app/stability/app-release.stability`.
- The dump files are generated by `./gradlew :app:stabilityDump`.

Dependency-Check:

- Formats: HTML and JSON.
- Output directory: root `reports`.
- Suppressions file: `config/dependency-check/suppressions.xml`.
- Current suppression marks the `compose-stability-runtime-android` GitHub Enterprise CPE match as a false positive for Skydoves Compose Stability Analyzer runtime.
- Data directory defaults to `.gradle/dependency-check-data` but can be overridden with `DEPENDENCY_CHECK_DATA_DIRECTORY`.
- Auto-update defaults true and can be controlled with `DEPENDENCY_CHECK_AUTO_UPDATE`.
- Fail CVSS defaults to 7 and can be overridden with `DEPENDENCY_CHECK_FAIL_BUILD_ON_CVSS`.
- Scans `debugRuntimeClasspath` and `releaseRuntimeClasspath`.
- Test groups are skipped.
- OSS Index analyzer is disabled.
- NVD API key, delay, retry count, and valid hours can be configured by env vars.

JaCoCo:

- Tool version 0.8.15.
- Report task: `jacocoDebugUnitTestReport`.
- Depends on `testDebugUnitTest`.
- Produces XML and HTML under `app/build/reports/jacoco/jacocoDebugUnitTestReport/`.
- Includes Java classes, Kotlin debug classes, and AGP built-in Kotlin class paths.
- Excludes generated Android classes, tests, previews, composable singleton artifacts, and `di`.

Sonar:

- Project key: `Insaner1980_Corners_Apart_Android`.
- Organization: `insaner1980`.
- Host: `https://sonarcloud.io`.
- Sources: `app/src/main/java`.
- Tests: `app/src/test/java`, `app/src/androidTest/java`.
- Coverage XML path: `app/build/reports/jacoco/jacocoDebugUnitTestReport/jacocoDebugUnitTestReport.xml`.
- Coverage excludes app entrypoint files, UI screens, and `PlayServicesConnectionsClientFacade`.
- Root Gradle `sonar` task depends on `:app:assembleDebug` and `:app:jacocoDebugUnitTestReport`.
- `tools/sonar.ps1` writes `reports/sonar.txt`, optionally exports issues to `reports/sonar-issues.json`, and requires `SONAR_TOKEN` or `systemProp.sonar.token` for Gradle scanner authentication.

Semgrep:

- Config path: `config/semgrep/corners_apart_android-security.yml`.
- Project rules flag:
  - `android:allowBackup="true"`.
  - `android:usesCleartextTraffic="true"`.
  - Unexpected exported service/receiver/provider/activity-alias.
  - Broad FileProvider paths.
  - Raw Nearby bypass through Bluetooth/Wi-Fi Direct/Wi-Fi Aware APIs.
  - Sensitive Android log calls containing nearby/endpoint/payload/profile/player/save/settings/board/move/token/corners terms.

MobSF:

- Config file `.mobsf`.
- Ignores build/cache/report/test/deepsec-data paths.
- Ignores `android_task_hijacking2`.
- Filters to WARNING and ERROR severity.

DeepSec:

- Config file `.deepsec/deepsec.config.ts`.
- Project id: `corners_apart_android`.
- Priority paths: manifest, multiplayer, data, and model packages.
- Prompt focus: exported components, FileProvider exports, URI grants, Nearby trust boundaries, local profile/save privacy, and raw Bluetooth/Wi-Fi Direct bypasses.
- Custom matchers:
  - `android-exported-component`.
  - `android-security-boundary-surface`.
  - `android-uri-share-without-clipdata`.
  - `fileprovider-broad-path`.
  - `raw-nearby-bypass`.
  - `sensitive-android-log`.
- DeepSec package version in `.deepsec/package.json`: `deepsec` 2.0.12.

OSV:

- `gradle/osv-scanner.toml` ignores Gradle verification metadata as an application dependency lockfile because Gradle dependencies are scanned by OWASP Dependency-Check.
- `.deepsec/osv-scanner.toml` also exists for DeepSec-side scanning.

Dependency verification:

- `gradle/verification-metadata.xml` is present.
- It uses Gradle dependency verification schema 1.3 and trusted keys.

GitHub Actions:

- `.github/workflows/build.yml`:
  - Runs on push and pull request to `main`.
  - Checks out code with a pinned SHA for `actions/checkout`.
  - Sets up Temurin JDK 17 with a pinned SHA for `actions/setup-java`.
  - Uses a pinned SHA for `gradle/actions/setup-gradle`.
  - Runs `./gradlew --no-configuration-cache assembleDebug`.
  - Runs `./gradlew --no-configuration-cache test`.
  - Runs `./gradlew --no-configuration-cache :app:detekt`.
  - Runs `./gradlew --no-configuration-cache lint`.
- `.github/workflows/sonar.yml`:
  - Runs on push and pull request to `main`.
  - Uses fetch depth 0.
  - Checks for `SONAR_TOKEN`.
  - If token exists, runs `./gradlew --no-configuration-cache assembleDebug jacocoDebugUnitTestReport sonar --no-daemon --console=plain`.
  - Prints a Finnish GitHub Actions notice if `SONAR_TOKEN` is missing.
- `.github/workflows/codeql.yml`:
  - Runs on push and pull request to `main`, plus Monday 06:00 cron.
  - Scans GitHub Actions workflows with CodeQL.
  - Comment notes Kotlin 2.4 is not yet in the supported CodeQL Kotlin range, so Android/Kotlin source is not scanned by CodeQL here.
- `.github/dependabot.yml`:
  - Runs weekly update checks for Gradle dependencies.
  - Runs weekly update checks for GitHub Actions.

## Local Tool Wrappers

All wrapper scripts except `tools/sonar.ps1` are thin delegates to `C:\Dev\Android-check\tools\InvokeProjectCheck.ps1`.

Wrapper commands:

- `tools/ac.ps1`: `android-check`.
- `tools/cr.ps1`: `compose-rules`.
- `tools/cs.ps1`: `compose-stability`.
- `tools/db.ps1`: `dependabot-check`.
- `tools/dc.ps1`: `dependency-check`.
- `tools/ds.ps1`: `deep-sec`.
- `tools/ga.ps1`: `google-android-security`.
- `tools/lc.ps1`: `lint-check`.
- `tools/ms.ps1`: `mobsf-scan`.
- `tools/os.ps1`: `osv-scan`.
- `tools/pc.ps1`: `pmd-check`.
- `tools/ql.ps1`: `codeql-check`.
- `tools/sc.ps1`: `security-check`.
- `tools/ss.ps1`: `secret-scan`.
- `tools/sonar.ps1`: custom Sonar helper.

The `reports/` directory is gitignored and must not be committed.

## Product Identity And Security Guardrails

Do:

- Keep the app identity as Corners Apart.
- Keep UI text English unless adding proper localization resources.
- Keep communication with the user, commit messages, and necessary code comments in Finnish.
- Keep theme colors, spacing, shapes, typography, alpha, and animation tokens centralized.
- Keep pure rules under `engine/`.
- Keep serializable game/save/domain models under `model/`; Nearby protocol messages (`GameMessage`, `GameProtocol`) live under `multiplayer/`.
- Keep DataStore access behind repositories in `data/`.
- Keep host-authoritative multiplayer validation in `HostGameCoordinator`.
- Keep Nearby v1 transport on Google Play services Nearby.
- Keep all user-facing strings in resources.

Do not:

- Add Room for v1 persistence.
- Add raw Bluetooth, Wi-Fi Direct, Wi-Fi Aware, direct sockets, or low-level transport fallbacks for v1.
- Add remote avatar services for v1.
- Duplicate placement, scoring, ranking, bonus, piece, or turn logic in UI/session/opponent code.
- Hardcode theme-equivalent colors/dimensions in UI code.
- Add user-facing external board-game names, logos, official wording, or marketing language.
- Add user-facing claims that the app has AI; computer opponents are normal local rule-based code.
- Commit `reports/`, local Gradle/Android-check caches, build outputs, APK/AAB outputs, or local IDE files.

## Known Review Targets

These are current-state items worth asking precise code-review questions about:

- Should `NearbyConnectionsCoordinator` expose richer lobby/player mapping before two-device stress testing?
- Should `GameSoundPlayer` move from generated platform tones to original `res/raw` assets before release polish?
- Should `ProfilesDialog` get a denser edit workflow or stay as a compact local v1 editor?
- Should `NearbyPermissions` SDK 37 local-network branch be paired with a manifest declaration when compile/target SDK move beyond 36?
- Should `MotionPolicy` be applied to more visible board/piece animations before release?
- Should `PRIVACY-POLICY.md` be finalized before release?
- Should `sonar` task dependency on `assembleDebug` stay, or should analysis avoid artifact build coupling if release/signing/Firebase-style gates are added later?

## External Docs Checked While Writing

These were re-checked on 2026-06-20 to avoid relying on stale Android ecosystem assumptions. This section records documentation assumptions only; the project still uses the exact versions listed in `gradle/libs.versions.toml`.

- Android Gradle Plugin release notes for AGP 9.x compatibility and built-in Kotlin behavior: <https://developer.android.com/build/releases/agp-9-2-0-release-notes> and <https://developer.android.com/build/releases/agp-9-0-0-release-notes>.
- Jetpack Compose BOM guidance and BOM mapping: <https://developer.android.com/develop/ui/compose/bom/bom-mapping>.
- Android DataStore guidance, including keeping DataStore operations in the data layer and exposing data through ViewModels: <https://developer.android.com/topic/libraries/architecture/datastore>.
- Google Nearby Connections overview for the peer-to-peer, offline local multiplayer transport model: <https://developers.google.com/nearby/connections/overview>.

For future dependency upgrades, re-check official Android, Google, JetBrains, Gradle, and library release notes before changing version numbers or API usage.
