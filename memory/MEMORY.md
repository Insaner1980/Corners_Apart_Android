# Corners Apart Android Memory

## Current State

- Project root: `C:\Dev\Corners_Apart_Android`
- App identity: `Corners Apart`
- Root project name: `CornersApart`
- Namespace and application ID: `com.finnvek.cornersapart`
- Authoritative implementation sources: live source code and tests, documented in `PROJECT.md`
- `corners_apart_android_spec_reviewed.md` is historical reference material and may be stale
- Starter conversion plan: `Corners Apart Android - Starterista v1-projektiksi.md`

## Architecture

- v1 persistence uses DataStore plus kotlinx.serialization JSON.
- Room is intentionally not part of v1.
- Nearby Connections uses Google Play services `play-services-nearby`; raw Bluetooth or Wi-Fi Direct fallback is not part of v1.
- AGP 9 built-in Kotlin is used; do not apply `org.jetbrains.kotlin.android` in the app module.
- Hilt remains the dependency injection boundary.
- `data/` owns JSON DataStore persistence through `GameRepository`, `ProfileRepository`, and `SettingsRepository`; UI and ViewModels should use repository APIs instead of raw DataStore.
- Compose UI must use centralized Corners Apart theme tokens from `com.finnvek.cornersapart.ui.theme`.
- Shared game constants are centralized in `com.finnvek.cornersapart.model.GameConstants`.
- Serializable game/save models live under `com.finnvek.cornersapart.model`; `BoardSnapshot` uses a flat immutable cell list.
- `BoardView` owns shared read-only board access helpers (`contains`, `index`, `get`) for `BoardSnapshot` and `MutableBoard`.
- Persisted JSON roots are `SavedGameData`, `ProfilesData`, and `GameSettings`; saved games include a `GameSettings` snapshot for resume.
- Profiles support local-only v1 avatars through `LocalAvatarStyle` and `LocalAvatarGenerator`; remote avatar services are out of scope.
- `HistoryStatsCalculator` owns higher-is-better history and statistics aggregation.
- `GameModeConfig` / `GameModeConfigs` is the single source of truth for mode defaults: board size, bonus count, color slots, start corners, computer slots, and color owner mapping.
- `PieceCatalog` is the single source of truth for the 21 stable piece IDs and their geometry (89 total cells); localized names are mapped at the UI boundary.
- `StringProvider` keeps localized Android resource lookup behind a runtime interface so ViewModels remain Android-resource agnostic.
- Pure game rules live under `com.finnvek.cornersapart.engine`.
- `GameEngine` owns new-game creation, move application, pass handling, valid move lookup, and placement previews.
- `PlacementValidator`, `CornerCache`, `BonusTileGenerator`, and `Scoring` own rule validation, corner candidates, bonus layouts, and higher-is-better ranking.
- Two-Color Duel keeps turn order as color slots 0-3 while `Player.ownerIndex` maps colors 0/2 to Player 1 and 1/3 to Player 2; rankings aggregate by owner.
- `opponents/` owns local computer turns through `MoveGenerator`, `MoveEvaluator`, and `ComputerOpponentEngine`.
- Opponent decisions use seeded randomness from `GameState.randomSeed`, support 3 styles and 5 difficulty levels, and always return a legal move or pass.
- `OpponentDifficultyMapper` maps persisted difficulty `1..5` to `OpponentDifficulty` and clamps invalid values.
- `LocalSessionFactory` creates `LocalSession` instances from a `GameConfig` and persisted difficulty; ViewModels should not call `LocalSession()` directly.
- Retention: `DailyStreakCalculator` (streak from `dailyBestScores` dates; `bestDailyStreak` on `Profile`) and `HallOfFameCalculator` (device-local Top 20 across profiles + `allTimeRank`); UI in `StreakBadge` and `HallOfFameTab` (third tab of `HistoryStatsDialog`).
- Rivals: `OpponentRoster` (in `opponents/`) defines 12 named characters (style + difficulty + color); matches run via `LocalSessionFactory.createRivalMatch` on the Compact Duel board with slot 1 computer-controlled; per-profile `rivalWins`/`rivalLosses` persist on `Profile`; UI is `RivalsDialog`, `RivalAvatar`, and the `RivalMatchIntro` VS overlay.
- `LocalSession` is the current playable session boundary for local Solo, Two-Color Duel, Compact Duel, Three-Player, and Four-Player configurations, serializes local move/pass mutations, and supports `replaceState` for restore.
- `GameProtocol` is the strict kotlinx.serialization JSON message boundary for Nearby sessions: place move, accepted/rejected move with typed `MoveRejectionReason`, pass, full sync, player join/left, and config. `NearbyConnectionsCoordinator` uses service id `com.finnvek.cornersapart` and stops previous Nearby activity before changing host/client role; strict decode failures publish `ConnectionState.FAILED`. Connection liveness relies on Play Services `ConnectionLifecycleCallback.onDisconnected`, not app-level heartbeat messages.
- `HostGameCoordinator` owns host-authoritative Nearby validation and broadcasts accepted moves with full authoritative state.
- `NearbySession` sends client move/pass requests to the host, applies host sync messages, exposes `NearbyLobbyState` for reconnect tracking plus `GameSessionEvent` one-shot failures, and only allows host-side `replaceState`.
- `NearbyConnectionsCoordinator` owns Nearby session orchestration, auth-token pending state, BYTES payload decoding, operation/status-code failure reporting, and endpoint sends through `ConnectionsClientFacade`; concrete Google Play services types and the `P2P_STAR` strategy are contained in `PlayServicesConnectionsClientFacade`.
- `TimeProvider` / `SystemTimeProvider` live under `com.finnvek.cornersapart.runtime` so Hilt runtime wiring and ViewModels can share time services without a `data` -> `viewmodel` package dependency.
- `GameViewModel` is Hilt-injected with `LocalSessionFactory`, `GameRepository`, `ProfileRepository`, `SettingsRepository`, runtime `TimeProvider`, and `NearbyConnectionsCoordinator`; it exposes repository-backed `StateFlow<GameUiState>` and `SharedFlow<GameEffect>`, renders the active Nearby session state/effects when present, and keeps saved-game/history persistence scoped to local gameplay.
- `GameScreen` is the Compose entry screen and provides the Canvas board, mode chips, permission-gated Nearby create/find actions, resume/profile/settings/help/history dialogs, player score bar, rotate/flip/pass controls, selected-piece preview, and piece strip.
- `ProfileRepository.appendHistory` trims to the latest `GameConstants.MAX_HISTORY_ENTRIES`; game-over history uses `Scoring.rankPlayers`.
- UI, session, persistence, and future computer-opponent code should call the engine/model APIs instead of duplicating rule logic.

## Verification Notes

- Do not run `lc` or `sc`; the user runs those wrappers.
- Use `.\gradlew.bat test` and `.\gradlew.bat assembleDebug` for milestone verification unless the user asks for narrower checks.
