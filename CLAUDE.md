# Project Instructions

## Build & Test

- `./gradlew assembleDebug` — debug build
- `./gradlew test` — unit tests
- `./gradlew :app:detekt` — static analysis
- `./gradlew lint` — Android lint

## Quality Tools (global, in ~/bin/)

- `lint-check` (alias `lc`) — runs ktlint + detekt + Android lint, results in `reports/`
- `security-check` (alias `sc`) — runs semgrep + OWASP dependency-check, results in `reports/`
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
- Saved game persistence uses `SavedGameData`; profile persistence uses `ProfilesData`; settings use `GameSettings`
- Profiles support only local v1 avatars through `LocalAvatarStyle` and `LocalAvatarGenerator`; do not add remote avatar services
- `HistoryStatsCalculator` owns higher-is-better history/stat calculations
- `GameModeConfig` / `GameModeConfigs` is the single source of truth for mode defaults: board size, bonus count, color slots, start corners, computer slots, and color owner mapping
- Pure game rules live under `com.finnvek.cornersapart.engine`; UI/session/opponent code must not duplicate placement, scoring, ranking, bonus, or turn logic
- `PieceCatalog` is the single source of truth for the 21 pieces and 89 total cells
- Two-Color Duel keeps turn order as color slots 0-3 while `Player.ownerIndex` maps colors 0/2 to Player 1 and 1/3 to Player 2; rankings aggregate by owner
- `opponents/` owns local computer turns through `MoveGenerator` (stratified per-piece sampling), `MoveEvaluator` (incl. corner-liberty mobility, attachment-point denial, endgame piece conservation), and `ComputerOpponentEngine` (6 difficulty levels; MASTER adds a 2-ply lookahead); decisions use seeded randomness from game state and always return a legal move or pass
- Progression features: `ChallengeLevels` (20 fixed-seed solo levels, 1-3 stars), daily challenge (date-derived seed), `AchievementEvaluator` (7 achievements); all progress persists per-profile (`challengeStars`, `dailyBestScores`, `achievements` fields on `Profile`)
- Retention features: `DailyStreakCalculator` derives the daily-challenge streak from `Profile.dailyBestScores` dates (`bestDailyStreak` persists on `Profile`, updated in `ProfileRepository.recordDailyBest`); `HallOfFameCalculator` builds the device-local Top 20 across all profiles (per-mode + all-modes, tie goes to earlier date) and `allTimeRank` for the game-over line; UI: `StreakBadge` flame pill (ChallengeDialog + daily game over), `HallOfFameTab` podium/list as third tab in `HistoryStatsDialog`
- Rivals ladder: `OpponentRoster` in `opponents/` defines 12 named opponents (fixed `OpponentStyle` + `OpponentDifficulty` + color family); matches are Compact Duel with slot 1 computer-controlled via `LocalSessionFactory.createRivalMatch` (per-player style override on `LocalSession`); progress persists as `rivalWins`/`rivalLosses` on `Profile`; UI: `RivalsDialog` gallery, Canvas-drawn `RivalAvatar` faces, `RivalMatchIntro` VS overlay; taglines in `strings.xml` keyed `rival_tagline_<id>`
- `LocalSession` is the current session boundary for local Solo, Two-Color Duel, Compact Duel, Three-Player, and Four-Player configurations
- `GameProtocol` is the kotlinx.serialization JSON message boundary for Nearby sessions
- `HostGameCoordinator` owns host-authoritative Nearby validation and broadcasts accepted moves with full authoritative state
- `NearbySession` sends client move/pass requests to the host, applies host sync messages, and exposes `NearbyLobbyState` for reconnect tracking
- `GameViewModel` exposes `StateFlow<GameUiState>` and one-shot `SharedFlow<GameEffect>`; Compose screens must collect state with `collectAsStateWithLifecycle()`
- `GameScreen` is the playable Compose entry screen and owns only presentation/input mapping, not game rules; Nearby create/find actions must request runtime permissions before advertising or discovery; History/Stats UI reads prepared history models rather than calculating rules inline
- Visual style is "candy bevel" (see `UI_RESTYLE_PLAN.md` + `UI_POLISH_PLAN.md`): single always-dark indigo theme (`CandyColorScheme` in `Theme.kt`, gradient via `Modifier.candyBackground()`), Nunito font (static TTFs in `res/font/`), 3D tiles via `drawCandyCell` in `ui/components/PieceShape.kt` (used by board, tray, preview, and score swatches)
- Player palette is Pink/Mango/Cyan/Lime (deliberately NOT the official Blokus blue/yellow/red/green); names live in `GameConstants.PLAYER_NAMES` and color families in `Tokens.kt` via `CornersApartPlayerPalette`
- Reusable candy chrome lives in `ui/components/`: `CandyButton`/`CandyIconButton`, `CandyDialog`, `CandySwitch`, `CandyChip`, `ConfettiBurst`; do not use stock M3 `Button`, `AlertDialog`, or `FilterChip` in production UI
- Sounds are SoundPool samples in `res/raw/snd_*.wav` played by `GameSoundPlayer`, mapped from effects by `GameSoundPolicy` (place/bonus/reject/game over)
- Compose animation imports are allowed (guardrail seam removed from live enforcement in `BuildDependencyHygieneTest`); board placement pop + bonus pulse live in `GameBoard.kt`
- Guardrail unit tests enforce: no `Color(0x` outside `Tokens.kt`, no unused dependency seams; run `./gradlew test` after UI changes

## Conventions

- Hilt for DI, DataStore for local persistence and preferences
- ViewModels expose StateFlow, screens collect via collectAsStateWithLifecycle()
- All strings in `res/values/strings.xml` for localization
- No hardcoded colors/dimensions — use theme tokens
- Finnish in commit messages and comments
