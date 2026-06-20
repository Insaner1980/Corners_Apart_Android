# Corners Apart Android

Corners Apart is an Android polyomino strategy game built with Kotlin, Jetpack Compose, Hilt, DataStore, and kotlinx.serialization.

## Build

- `.\gradlew.bat assembleDebug` - debug build
- `.\gradlew.bat test` - unit tests
- `.\gradlew.bat :app:detekt` - static analysis
- `.\gradlew.bat lint` - Android lint

## Project Identity

- Root project: `CornersApart`
- Namespace and application ID: `com.finnvek.cornersapart`
- App name: `Corners Apart`
- Release signing environment prefix: `CORNERS_APART`

## Architecture Baseline

- Compose UI with centralized Corners Apart theme tokens.
- Hilt for dependency injection.
- DataStore plus kotlinx.serialization JSON for v1 persistence.
- Room is intentionally not part of v1.
- `data/` wraps JSON DataStore access behind `GameRepository`, `ProfileRepository`, and `SettingsRepository`; UI and ViewModels should depend on repositories, not raw DataStore.
- Nearby Connections uses Google Play services `play-services-nearby`; raw Bluetooth or Wi-Fi Direct fallback is not part of v1.
- AGP 9 built-in Kotlin is used; do not apply `org.jetbrains.kotlin.android` in the app module.
- Game engine code is pure Kotlin and independent from Android UI.
- Serializable save/network models live in `model/`; board state uses a flat immutable `BoardSnapshot`.
- `model/SavedGameData.kt`, `model/ProfilesData`, and `model/GameSettings.kt` are the persisted JSON roots for saved games, profiles/history, and preferences; saved games include a settings snapshot for resume.
- `model/LocalAvatarGenerator.kt` generates deterministic local-only avatar descriptors for initials, geometric, mosaic, and rings styles.
- `model/HistoryStatsCalculator.kt` owns higher-is-better history and stats aggregation.
- `model/GameModeConfig.kt` is the single source of truth for mode defaults: board size, bonus count, color slots, start corners, computer slots, and color owner mapping.
- `engine/` owns placement validation, corner-candidate generation, bonus tile layout generation, scoring, ranking, turn advancement, and game-over checks.
- Two-Color Duel keeps turn order as color slots 0-3 while `Player.ownerIndex` maps colors 0/2 to Player 1 and 1/3 to Player 2; rankings aggregate by owner.
- `opponents/` owns local computer turns through `MoveGenerator`, `MoveEvaluator`, and `ComputerOpponentEngine`; decisions use seeded randomness from game state and always return a legal move or pass.
- `multiplayer/LocalSessionFactory` creates local sessions with the persisted difficulty mapping; `LocalSession` supports state replacement for saved-game restore.
- `multiplayer/GameProtocol` is the JSON message boundary for Nearby sessions.
- `multiplayer/HostGameCoordinator` owns host-authoritative Nearby validation and broadcasts accepted moves with full authoritative state.
- `multiplayer/NearbySession` sends client move/pass requests to the host, applies host sync messages, and exposes `NearbyLobbyState` for reconnect tracking.
- `multiplayer/NearbyConnectionsCoordinator` wraps Google Play services Nearby through `ConnectionsClientFacade` for advertising, discovery, auth-token confirmation, BYTES payloads, and endpoint sends.
- `viewmodel/GameViewModel` exposes repository-backed `StateFlow<GameUiState>` and delegates rules through `LocalSessionFactory`, repositories, and the Nearby coordinator.
- `ui/screens/GameScreen.kt` is the first playable Compose surface: Canvas board, mode chips, permission-gated Nearby create/find actions, resume/profile/settings/help/history dialogs, player score bar, rotate/flip/pass controls, selected-piece preview, and piece strip.
- UI, sessions, persistence, and computer opponents must call `GameEngine` instead of duplicating rule logic.

## Project Structure

```
app/src/main/java/com/finnvek/cornersapart/
├── model/              # Serializable game, mode config, piece, profile, history, stats, settings, and save-state models
├── engine/             # Pure rules, move validation, scoring, ranking, and bonus layouts
├── opponents/          # Move generation, move evaluation, and seeded local opponents
├── multiplayer/        # Local and Nearby sessions, protocol, host validation, Play Services adapter, lobby state
├── ui/                 # Compose UI, theme, screens, components, dialogs, sheets
├── viewmodel/          # StateFlow-based game UI state and ViewModels
└── data/               # JSON DataStore serializers, Hilt bindings, and repositories
```

## Release Signing

Set environment variables before release build:

```bash
export CORNERS_APART_KEYSTORE_PATH=/path/to/keystore.jks
export CORNERS_APART_KEYSTORE_PASSWORD=password
export CORNERS_APART_KEY_ALIAS=alias
export CORNERS_APART_KEY_PASSWORD=password
```

## License

MIT
