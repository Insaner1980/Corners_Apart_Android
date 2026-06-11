# Corners Apart Android Memory

## Current State

- Project root: `C:\Dev\Corners_Apart_Android`
- App identity: `Corners Apart`
- Root project name: `CornersApart`
- Namespace and application ID: `com.finnvek.cornersapart`
- Canonical implementation source: `corners_apart_android_spec_reviewed.md`
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
- Persisted JSON roots are `SavedGameData`, `ProfilesData`, and `GameSettings`.
- Profiles support local-only v1 avatars through `LocalAvatarStyle` and `LocalAvatarGenerator`; remote avatar services are out of scope.
- `HistoryStatsCalculator` owns higher-is-better history and statistics aggregation.
- `GameModeConfig` / `GameModeConfigs` is the single source of truth for mode defaults: board size, bonus count, color slots, start corners, computer slots, and color owner mapping.
- `PieceCatalog` is the single source of truth for the 21 pieces and 89 total cells.
- Pure game rules live under `com.finnvek.cornersapart.engine`.
- `GameEngine` owns new-game creation, move application, pass handling, valid move lookup, and placement previews.
- `PlacementValidator`, `CornerCache`, `BonusTileGenerator`, and `Scoring` own rule validation, corner candidates, bonus layouts, and higher-is-better ranking.
- Two-Color Duel keeps turn order as color slots 0-3 while `Player.ownerIndex` maps colors 0/2 to Player 1 and 1/3 to Player 2; rankings aggregate by owner.
- `opponents/` owns local computer turns through `MoveGenerator`, `MoveEvaluator`, and `ComputerOpponentEngine`.
- Opponent decisions use seeded randomness from `GameState.randomSeed`, support 3 styles and 5 difficulty levels, and always return a legal move or pass.
- `LocalSession` is the current playable session boundary for local Solo, Two-Color Duel, Compact Duel, Three-Player, and Four-Player configurations.
- `GameProtocol` is the kotlinx.serialization JSON message boundary for Nearby sessions: place move, accepted/rejected move, pass, full sync, player join/left, config, ping, and pong.
- `HostGameCoordinator` owns host-authoritative Nearby validation and broadcasts accepted moves with full authoritative state.
- `NearbySession` sends client move/pass requests to the host, applies host sync messages, and exposes `NearbyLobbyState` for reconnect tracking.
- `GameViewModel` exposes `StateFlow<GameUiState>` and `SharedFlow<GameEffect>`; it delegates rule changes through `LocalSession`.
- `GameScreen` is the Compose entry screen and provides the Canvas board, mode chips, Nearby create/find actions, History/Stats dialog entry, player score bar, rotate/flip/pass controls, selected-piece preview, and piece strip.
- UI, session, persistence, and future computer-opponent code should call the engine/model APIs instead of duplicating rule logic.

## Verification Notes

- Do not run `lc` or `sc`; the user runs those wrappers.
- Use `.\gradlew.bat test` and `.\gradlew.bat assembleDebug` for milestone verification unless the user asks for narrower checks.
