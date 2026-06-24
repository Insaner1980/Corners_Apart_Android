# Project Instructions

## Build & Test

- `./gradlew assembleDebug` — debug build
- `./gradlew test` — unit tests
- `./gradlew :app:detekt` — static analysis
- `./gradlew lint` — Android lint

## Quality Tools

- Project-local PowerShell wrappers live in `tools/*.ps1` and mostly delegate to `C:\Dev\Android-check\tools\InvokeProjectCheck.ps1`
- `lc` / `tools\lc.ps1` runs ktlint + detekt + Android lint, results in `reports/`
- `sc` / `tools\sc.ps1` runs semgrep + OWASP dependency-check, results in `reports/`
- `tools\sonar.ps1` is the repo-local SonarCloud path and requires `SONAR_TOKEN` for the full Gradle scan
- Don't run these scripts yourself — user runs them via `! lc` / `! sc`
- `reports/` is gitignored, never commit it

## Current Architecture

- Namespace and application ID: `com.finnvek.cornersapart`
- App name: `Corners Apart`
- Persistence: DataStore + kotlinx.serialization JSON for v1; Room is intentionally not used in v1
- Nearby Connections uses Google Play services `play-services-nearby`; do not add raw Bluetooth or Wi-Fi Direct fallback for v1
- AGP 9 built-in Kotlin is used; do not apply `org.jetbrains.kotlin.android` in the app module
- Hilt remains the DI boundary
- `data/` owns JSON DataStore repositories through `GameRepository`, `ProfileRepository`, and `SettingsRepository`; UI and ViewModels must not access DataStore directly
- Compose theme tokens live under `app/src/main/java/com/finnvek/cornersapart/ui/theme/`
- Shared v1 game constants live in `com.finnvek.cornersapart.model.GameConstants`
- Serializable game/save models live under `com.finnvek.cornersapart.model`; board state is `BoardSnapshot(size, flat cells)`
- `BoardView` owns shared read-only board access helpers (`contains`, `index`, `get`) for `BoardSnapshot` and `MutableBoard`
- Saved game persistence uses `SavedGameData(gameState, savedAtEpochMillis, settings)`; profile persistence uses `ProfilesData`; settings use `GameSettings`
- Profiles support only local v1 avatars through `LocalAvatarStyle` and `LocalAvatarGenerator`; do not add remote avatar services
- `ProfileRepository.appendHistory` keeps only `GameConstants.MAX_HISTORY_ENTRIES` most recent entries
- `HistoryStatsCalculator` owns higher-is-better history/stat calculations
- `GameModeConfig` / `GameModeConfigs` is the single source of truth for mode defaults: board size, bonus count, color slots, start corners, computer slots, and color owner mapping
- Pure game rules live under `com.finnvek.cornersapart.engine`; UI/session/opponent code must not duplicate placement, scoring, ranking, bonus, or turn logic
- `PieceCatalog` is the single source of truth for the 21 pieces and 89 total cells
- Two-Color Duel keeps turn order as color slots 0-3 while `Player.ownerIndex` maps colors 0/2 to Player 1 and 1/3 to Player 2; rankings aggregate by owner
- `opponents/` owns local computer turns through `MoveGenerator`, `MoveEvaluator`, and `ComputerOpponentEngine`; decisions use seeded randomness from game state and always return a legal move or pass
- `OpponentDifficultyMapper` is the single source of truth for persisted difficulty `1..5` to `OpponentDifficulty`; invalid values are clamped
- `LocalSessionFactory` creates `LocalSession(initialConfig, opponentDifficulty)`; ViewModels must not instantiate `LocalSession()` directly
- `LocalSession` is the current session boundary for local Solo, Two-Color Duel, Compact Duel, Three-Player, and Four-Player configurations, serializes local move/pass mutations, and supports `replaceState` for saved-game restore
- `GameProtocol` is the kotlinx.serialization JSON message boundary for Nearby sessions; move rejections carry typed `MoveRejectionReason` values
- `HostGameCoordinator` owns host-authoritative Nearby validation and broadcasts accepted moves with full authoritative state
- `NearbySession` sends client move/pass requests to the host, applies host sync messages, exposes `NearbyLobbyState` for reconnect tracking and `GameSessionEvent` one-shot failures, and only allows host-side `replaceState`
- `NearbyConnectionsCoordinator` owns Nearby session orchestration, auth-token confirmation, BYTES payload routing, operation/status-code failure reporting, and endpoint sends through `ConnectionsClientFacade`; concrete Google Play services types and the `P2P_STAR` strategy stay inside `PlayServicesConnectionsClientFacade`; do not bypass these boundaries from UI
- `GameViewModel` is repository-backed: it collects `SettingsRepository`, `GameRepository`, `ProfileRepository`, `NearbyConnectionsCoordinator.nearbyState`, and the active Nearby session state/effects when present; local gameplay persists saved games/history, while Nearby gameplay uses the active `NearbySession` as the playable state source
- `GameScreen` is the playable Compose entry screen and owns only presentation/input mapping, not game rules; Nearby create/find actions request runtime permissions in `GameRoute` before advertising or discovery; History/Stats UI reads active-profile history from `GameUiState`

## Conventions

- Hilt for DI, DataStore for local persistence and preferences
- ViewModels expose StateFlow, screens collect via collectAsStateWithLifecycle()
- All strings in `res/values/strings.xml` for localization
- No hardcoded colors/dimensions — use theme tokens
- Finnish in commit messages and comments


<claude-mem-context>
# Memory Context

# [Corners_Apart_Android] recent context, 2026-06-24 8:29pm GMT+3

No previous sessions found.
</claude-mem-context>
