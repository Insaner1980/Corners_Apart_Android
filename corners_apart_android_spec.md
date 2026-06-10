# Corners Apart — Android Technical Specification

> **Working title:** Corners Apart

## 1. Overview

**Type:** Native Android polyomino strategy game for 1–4 players. Supports solo play against local computer opponents, local pass-and-play multiplayer, Bluetooth multiplayer, and Wi-Fi Direct multiplayer.

**Language/Locale:** English primary. All UI text, error messages, help text, accessibility labels, and user-facing rule explanations must be written in English first.

**Platform:** Android, min SDK 26 / Android 8.0, Kotlin, Jetpack Compose, Material 3 with a heavily customized game visual style.

Corners Apart is an original Android polyomino strategy game. Players place geometric pieces on a square grid, starting from their assigned corners. A player’s own pieces may connect only through diagonal corner contact and may not touch each other by edge. Players gain positive points for cells they place, bonus tiles they claim, and completion bonuses.

This project starts from a clean Android implementation. Do not treat it as a port of an existing HTML prototype. Do not rely on any external prototype file. The rules, architecture, UI, data model, computer-opponent logic, multiplayer, persistence, and accessibility behavior must be defined by this document.

### 1.1 Product Identity Guardrails

Corners Apart must be presented as its own game.

Do not implement, name, describe, market, or visually present this app as a clone, port, remake, or digital version of any existing commercial board game.

Do not use official board game names, logos, product artwork, product photos, box art, official rule-sheet wording, official marketing language, or official licensed digital-game presentation.

Do not use the word “Blokus” in the app UI, help text, Play Store listing, package metadata, screenshots, marketing copy, or user-facing rules.

The game may use generic rule language such as “place pieces so your own pieces touch only at corners,” but all explanatory text must be original to Corners Apart.

### 1.2 Corners Apart Design Identity

Corners Apart should feel like a modern digital strategy game, not a physical board game imitation.

Key identity decisions:

- Game name: Corners Apart
- Core mechanic: corner-only same-player connection
- Start structure: each player always starts from their assigned board corner in the standard four-player game
- Scoring: positive scoring, higher score wins
- Bonus system: visible bonus tiles are placed on the board at game start and give extra points when covered
- Visual style: clean, modern, playful, vivid, glossy, and geometric
- Player colors: indigo, amber, coral, teal
- Piece appearance: opaque glossy lacquered pieces with sharp square corners
- Board appearance: separated cells with visible gaps on a light-gray board surface and a dark graphite frame

### 1.3 Core Differences From a Traditional Corner-Placement Polyomino Game

Corners Apart intentionally uses familiar broad ingredients from the polyomino strategy genre, but its product experience and scoring system are its own.

| Area | Corners Apart direction |
|---|---|
| Scoring | Positive score starts at 0. Higher is better. |
| Bonus tiles | 10 visible bonus tiles are generated at game start. Covering one gives extra points. |
| Running score | Scores update upward during play after every placement. |
| Game-over detail | Final results show placed-cell points, bonus-tile points, completion bonus, and total score. |
| Visual identity | Opaque glossy pieces, separated board cells, custom palette, dark graphite frame. |
| Digital features | Solo computer opponents, pass-and-play, Bluetooth, Wi-Fi Direct, profiles, history, statistics, save/resume. |
| Terminology | Uses Corners Apart terminology, not external board game branding. |

## 2. Architecture — Multiplayer-First Design

The architecture must be designed for multiplayer from the start. The game engine is completely decoupled from the UI and transport layers.

### 2.1 Core Principle

Every action in the game is a message: `Player X places Piece Y at Position Z with Orientation W`, `Player X passes`, or `Host sends synchronized game state`.

The game engine processes moves identically regardless of their source: local touch input, local computer-opponent computation, pass-and-play input, Bluetooth, or Wi-Fi Direct.

The host is authoritative in multiplayer sessions. Clients send move requests. The host validates moves, applies accepted moves, updates score, updates bonus-tile ownership, advances turn state, and broadcasts the resulting state.

### 2.2 High-Level Architecture

```text
┌─────────────────────────────────────────────────────┐
│                      UI Layer                        │
│   GameScreen, Board, PiecePanel, Dialogs, etc.      │
│   Jetpack Compose                                   │
└─────────────────┬───────────────────────────────────┘
                  │ observes StateFlow
┌─────────────────┴───────────────────────────────────┐
│                   GameViewModel                      │
│   Coordinates UI state, delegates to engine/session  │
└────────┬────────────────────────────┬───────────────┘
         │                            │
┌────────┴────────┐    ┌──────────────┴──────────────┐
│   GameEngine    │    │       GameSession           │
│   Pure logic    │    │   Interface                 │
│   No Android    │    │                              │
│   dependencies  │    │   ┌─ LocalSession           │
│                 │    │   │  Solo + pass-and-play   │
│   - Board       │    │   ├─ BluetoothSession       │
│   - Placement   │    │   └─ WifiDirectSession      │
│   - Bonus tiles │    │                              │
│   - Scoring     │    │   Handles:                   │
│   - Turn logic  │    │   - connection lifecycle     │
│   - Validation  │    │   - move transmission        │
│                 │    │   - state synchronization    │
│                 │    │   - reconnection             │
└────────┬────────┘    └──────────────────────────────┘
         │
┌────────┴────────┐
│ ComputerOpponentEngine │
│   Deterministic/local   │
│   opponent logic        │
│   Runs on Dispatchers   │
│   .Default              │
└─────────────────────────┘
```

### 2.3 Project Structure

```text
app/src/main/java/com/[package]/
├── model/
│   ├── Board.kt                — Board state, configurable size: 20×20 or 14×14
│   ├── BonusTile.kt            — Bonus tile position and claimed state
│   ├── BonusTileLayout.kt      — Generated or predefined bonus-tile layouts
│   ├── Piece.kt                — 21 piece definitions, PieceDef data class
│   ├── PieceTransforms.kt      — Rotate, flip, normalize, getAllOrientations, cached
│   ├── Player.kt               — Player state: usedPieces, score, scoreBreakdown, passed
│   ├── ScoreBreakdown.kt       — placedCellPoints, bonusTilePoints, completionBonus
│   ├── GameState.kt            — Full serializable game state
│   ├── GameMode.kt             — SOLO, TWO_COLOR_DUEL, COMPACT_DUEL, THREE_PLAYER, FOUR_PLAYER
│   ├── Ruleset.kt              — CLASSIC, TACTICAL, EXPERT if variants are added later
│   ├── Move.kt                 — playerIndex, pieceIndex, anchorRow, anchorCol, orientation
│   ├── Profile.kt              — Player profile
│   ├── HistoryEntry.kt         — Game history record
│   └── Difficulty.kt           — Difficulty level enum + parameters
│
├── engine/
│   ├── GameEngine.kt           — Pure game logic, no Android dependencies
│   │                             placement validation, scoring, bonus tiles,
│   │                             turn management, game end detection
│   ├── PlacementValidator.kt   — isValidPlacement(), corner-position cache
│   ├── BonusTileGenerator.kt   — fair bonus tile generation
│   ├── Scoring.kt              — positive score calculation, rankings, tie-breakers
│   └── GameRules.kt            — ruleset-specific constants and rule switches
│
├── opponents/
│   ├── ComputerOpponentEngine.kt   — Move generation, evaluation, selection
│   ├── OpponentStyle.kt            — 3 predefined play styles
│   ├── OpponentDifficulty.kt       — 5-level difficulty parameters
│   ├── OpponentState.kt            — Per-opponent state: momentum, frustration
│   ├── MoveEvaluator.kt            — Multi-criteria move scoring
│   ├── MoveGenerator.kt            — Valid move enumeration with pruning
│   ├── BonusTileAwareness.kt       — Bonus-tile opportunity and denial evaluation
│   └── Territory.kt                — BFS territory estimation
│
├── multiplayer/
│   ├── GameSession.kt          — Interface for all session types
│   ├── LocalSession.kt         — Solo with computer opponents + pass-and-play
│   ├── BluetoothSession.kt     — Bluetooth Classic connection
│   ├── WifiDirectSession.kt    — Wi-Fi Direct P2P connection
│   ├── SessionDiscovery.kt     — Find nearby players, Bluetooth + Wi-Fi
│   ├── GameProtocol.kt         — Message format, serialization
│   ├── SyncManager.kt          — State synchronization, conflict resolution
│   └── ReconnectionHandler.kt  — Handle disconnects and rejoin logic
│
├── ui/
│   ├── theme/
│   │   ├── Theme.kt            — Custom game theme
│   │   ├── Color.kt            — Full color palette
│   │   ├── Type.kt             — Typography, Quicksand
│   │   └── Shape.kt            — Shape definitions
│   ├── screens/
│   │   ├── MainMenuScreen.kt   — Game mode selection, profiles
│   │   ├── GameScreen.kt       — Main gameplay screen
│   │   ├── LobbyScreen.kt      — Multiplayer lobby
│   │   └── ConnectionScreen.kt — Bluetooth/Wi-Fi discovery + pairing
│   ├── components/
│   │   ├── GameBoard.kt        — Canvas-based board rendering
│   │   ├── BonusTileMarker.kt  — Bonus tile visual marker + claimed animation
│   │   ├── PiecePanel.kt       — Scrollable piece selection grid
│   │   ├── PieceCard.kt        — Individual piece card
│   │   ├── PiecePreview.kt     — Selected piece preview
│   │   ├── PlayerInfoBar.kt    — Running scores and status
│   │   ├── ControlBar.kt       — Rotate, flip, pass buttons
│   │   ├── GameTimer.kt        — Elapsed time display
│   │   ├── TurnStatus.kt       — Status message bar
│   │   └── GhostPreview.kt     — Ghost overlay on board
│   ├── dialogs/
│   │   ├── GameOverDialog.kt
│   │   ├── ScoreBreakdownDialog.kt
│   │   ├── ConfirmDialog.kt
│   │   ├── HelpDialog.kt
│   │   ├── ProfileDialog.kt
│   │   └── HistoryStatsDialog.kt
│   ├── sheets/
│   │   ├── SettingsSheet.kt    — Difficulty, history, profiles, help
│   │   └── MultiplayerSheet.kt — Game mode, connection options
│   └── util/
│       ├── SoundManager.kt     — SoundPool-based audio
│       └── HapticManager.kt    — Vibration feedback
│
├── viewmodel/
│   ├── GameViewModel.kt        — Main game state management
│   ├── LobbyViewModel.kt       — Multiplayer lobby state
│   └── ProfileViewModel.kt     — Profile management
│
└── data/
    ├── GameRepository.kt       — DataStore persistence, save/load game
    ├── ProfileRepository.kt    — Profile CRUD
    └── Serializers.kt          — kotlinx.serialization
```

### 2.4 GameSession Interface

```kotlin
interface GameSession {
    val sessionType: SessionType
    val gameMode: GameMode
    val players: StateFlow<List<SessionPlayer>>
    val incomingMessages: SharedFlow<GameMessage>
    val connectionState: StateFlow<ConnectionState>

    suspend fun sendMove(move: Move): Result<Unit>
    suspend fun sendPass(playerIndex: Int): Result<Unit>
    suspend fun sendGameState(state: GameState): Result<Unit>
    suspend fun disconnect()
}

data class SessionPlayer(
    val index: Int,
    val name: String,
    val isLocal: Boolean,
    val isComputerControlled: Boolean,
    val colorIndex: Int
)

enum class ConnectionState {
    DISCONNECTED,
    DISCOVERING,
    CONNECTING,
    CONNECTED,
    RECONNECTING,
    FAILED
}
```

### 2.5 Game Protocol

Messages between devices are serialized with kotlinx.serialization as JSON.

```kotlin
@Serializable
sealed class GameMessage {
    @Serializable data class PlaceMove(val move: Move) : GameMessage()
    @Serializable data class MoveAccepted(val state: GameState) : GameMessage()
    @Serializable data class MoveRejected(val reason: String) : GameMessage()
    @Serializable data class Pass(val playerIndex: Int) : GameMessage()
    @Serializable data class FullSync(val state: GameState) : GameMessage()
    @Serializable data class PlayerJoined(val player: SessionPlayer) : GameMessage()
    @Serializable data class PlayerLeft(val playerIndex: Int) : GameMessage()
    @Serializable data class GameConfig(
        val mode: GameMode,
        val ruleset: Ruleset,
        val difficulty: Int,
        val boardSize: Int,
        val bonusTiles: List<BonusTile>
    ) : GameMessage()
    @Serializable object Ping : GameMessage()
    @Serializable object Pong : GameMessage()
}
```

The host model is authoritative. One device runs the `GameEngine`, validates all moves, calculates scoring, updates bonus-tile ownership, and broadcasts accepted state updates.

If a player disconnects mid-game, the game pauses for up to 60 seconds. If they reconnect, the host sends a full sync. If they do not reconnect, the host may assign their remaining turns to a computer-controlled opponent at difficulty 3 or allow players to save and exit.

## 3. Game Modes

Mode names should avoid terminology that directly echoes existing commercial variants. Use Corners Apart-specific names where possible.

### 3.1 Solo vs Computer Opponents, 20×20

- 1 human player + 3 computer-controlled opponents
- Human always starts at bottom-right corner `[19,19]` by default
- Computer opponent corners are assigned among the remaining three corners
- 5 difficulty levels
- Turn order proceeds around the four corner positions
- Bonus tiles are generated at game start and visible to all players

### 3.2 Two-Color Duel, 20×20

- 2 human players
- Each player controls 2 colors, two sets of 21 pieces each
- Player 1 controls opposite corners, colors 0 and 2
- Player 2 controls opposite corners, colors 1 and 3
- Turn order cycles through colors: 0, 1, 2, 3
- Available via pass-and-play, Bluetooth, and Wi-Fi Direct

### 3.3 Compact Duel, 14×14

- 2 players, 1 color per player
- Smaller 14×14 board
- 21 pieces per player
- Start positions are corner-based but adjusted for the compact board:
  - Player 1 starts at `[0,0]`
  - Player 2 starts at `[13,13]`
- Faster games, better suited for short mobile sessions
- Bonus tile count should be lower than the standard board, recommended 4–6 bonus tiles
- Available via pass-and-play, Bluetooth, and Wi-Fi Direct

### 3.4 Three-Player, 20×20

- 3 human or computer-controlled players, each with their own color
- The fourth color can be disabled or controlled as a rotating shared color, depending on implementation choice
- If a shared color is used, its score is ignored in final ranking
- Bonus tiles are claimable by active scoring players only
- Available via pass-and-play or networked play

### 3.5 Four-Player, 20×20

- 4 players, each with one color and one corner
- This is the primary standard Corners Apart mode
- Available via pass-and-play, Bluetooth, and Wi-Fi Direct

### 3.6 Mixed Human + Computer-Controlled Modes

In any supported mode, empty slots may be filled with computer-controlled players at a host-selected difficulty level. In multiplayer, computer-controlled moves are calculated on the host device and broadcast as regular accepted moves.

## 4. Core Game Logic

This section defines the Corners Apart ruleset from scratch. Do not depend on any external prototype or external rule sheet.

### 4.1 Board

- Standard board size: 20×20
- Compact board size: 14×14
- Cell values:
  - `-1`: empty
  - `0..3`: occupied by player index
- Bonus tiles are stored separately from occupied cell values
- A bonus tile can be visible, unclaimed, claimed, or hidden under a placed piece depending on UI rendering needs

```kotlin
data class Board(
    val size: Int,
    val cells: Array<IntArray>
)

data class BonusTile(
    val row: Int,
    val col: Int,
    val claimedByPlayerIndex: Int? = null,
    val claimedOnTurn: Int? = null
)
```

### 4.2 Piece Definitions

Each player has 21 pieces. The total cell count per player is 89.

The set is made from polyomino-style shapes of size 1–5 cells. The exact shapes must be defined in `Piece.kt`. The code should treat piece definitions as Corners Apart game data, not as user-facing references to any external game.

```kotlin
data class PieceDef(
    val id: String,
    val displayName: String,
    val cells: List<Pair<Int, Int>>
)
```

Implementation requirements:

- `PIECE_COUNT = 21`
- `TOTAL_PIECE_CELLS = 89`
- Each player owns one independent copy of the full piece set
- A piece can be used once per player color
- Used pieces become unavailable and visually dimmed

### 4.3 Piece Transformations

The game must support rotation and flipping.

- `rotateCW(cells)`: `[r,c] → [c,-r]`
- `rotateCCW(cells)`: `[r,c] → [-c,r]`
- `flipH(cells)`: `[r,c] → [r,-c]`
- `normalize(cells)`: shift to origin and lexicographically sort
- `getAllOrientations(cells)`: return all unique orientations, maximum 8, cached by piece id

### 4.4 Placement Rules

A placement is valid only if all of the following are true:

1. Every cell of the piece is within board bounds.
2. Every cell of the piece lands on an empty board cell.
3. The piece has not already been used by that player color.
4. The player’s first piece covers their assigned starting corner cell.
5. The player’s later pieces touch at least one of that player’s existing pieces diagonally.
6. The player’s pieces may not touch that same player’s existing pieces orthogonally by edge.
7. A player’s pieces may touch opponent pieces by edge or corner.
8. A piece may cover an unclaimed bonus tile. This claims the bonus tile for that player and awards bonus points.
9. A piece may cover an already claimed bonus tile only if that tile is already occupied by that same placement event. In normal board state, a claimed bonus tile is occupied and cannot be covered again because the board cell is no longer empty.

### 4.5 Start Positions

Corners Apart standard play always starts from the board corners.

**Standard 20×20 board:**

- Player 0: `[0,0]`
- Player 1: `[0,19]`
- Player 2: `[19,19]`
- Player 3: `[19,0]`

**Solo default:**

- Human: `[19,19]`
- Computer-controlled players: assigned to the remaining corners

**Compact 14×14 duel:**

- Player 0: `[0,0]`
- Player 1: `[13,13]`

The UI must clearly indicate each player’s starting corner before and during the first round. The first placement must cover that player’s assigned starting corner exactly.

### 4.6 Positive Scoring

Corners Apart uses positive scoring.

Players start at 0 points. Higher score is better.

Score increases during the game after each accepted placement.

Scoring rules:

- Each placed cell gives +1 point.
- Each unclaimed bonus tile covered by a placed cell gives +3 additional points.
- A cell placed on a bonus tile is therefore worth 4 points total: +1 placed-cell point and +3 bonus-tile points.
- A player who places all 21 pieces gains a +10 completion bonus.
- Completion bonus is awarded once, immediately when the player places their final unused piece.
- End-game ranking is sorted by total score in descending order.

Recommended tie-breakers:

1. Higher total score wins.
2. If tied, the player with more placed cells ranks higher.
3. If still tied, the player with more claimed bonus tiles ranks higher.
4. If still tied, the player with fewer unused pieces ranks higher.
5. If still tied, the tied players share the rank or player index may be used as a deterministic internal fallback.

### 4.7 Score Breakdown

The game must store and display a score breakdown.

```kotlin
@Serializable
data class ScoreBreakdown(
    val placedCellPoints: Int = 0,
    val bonusTilePoints: Int = 0,
    val completionBonus: Int = 0
) {
    val total: Int
        get() = placedCellPoints + bonusTilePoints + completionBonus
}
```

`players[].score` must count upward from 0 and should equal `players[].scoreBreakdown.total`.

Score updates on every accepted placement:

```text
placement points = piece cell count
bonus points = number of newly covered bonus tiles × 3
completion bonus = 10 if the placement uses the player's final remaining piece, otherwise 0
```

The game-over dialog must show:

- Total score
- Placed-cell points
- Bonus-tile points
- Completion bonus
- Claimed bonus tile count
- Pieces placed / pieces remaining

History entries must store higher-is-better scoring.

### 4.8 Bonus Tiles

Bonus tiles are visible scoring opportunities placed on the board at game start.

Default standard-board behavior:

- Generate 10 bonus tile positions at game start.
- Bonus tiles are visible to all players from the beginning of the game.
- Bonus tiles are not obstacles. Pieces may be placed on them normally.
- When a player covers a bonus tile, that player immediately claims it and gains +3 bonus points.
- A claimed bonus tile cannot be claimed again.
- Bonus tile positions and claimed state must be stored in `GameState` and save state.
- Multiplayer sessions must synchronize bonus tile positions before the first move.

Placement constraints:

- Bonus tiles must not be placed on starting corner cells.
- Bonus tiles must not be placed on the immediate diagonal neighbor of a starting corner.
- Bonus tiles must not be adjacent to each other by edge or corner. Use a minimum Chebyshev distance of 2 between bonus tiles.
- Bonus tiles should be distributed across the board and must not cluster heavily in one quadrant.
- Bonus tile generation must be fair across starting corners.

Recommended implementation:

- Prefer a small set of predefined symmetrical layout templates for 20×20 and 14×14 boards.
- Randomly select one template at game start.
- Optionally rotate or mirror the template before use.
- Avoid fully unconstrained random generation unless fairness validation is implemented.

Example model:

```kotlin
@Serializable
data class BonusTileLayout(
    val boardSize: Int,
    val positions: List<Pair<Int, Int>>
)
```

### 4.9 Bonus Tile Rendering

Visual requirements:

- Use a distinct marker on the cell, such as a small diamond or star shape.
- Use accent gold, recommended `#D8A928`, with semi-transparent alpha.
- The marker must be visible but not visually louder than player pieces.
- If a piece ghost hovers over a bonus tile, the preview should make it clear the bonus will be claimed.
- When claimed, show brief visual feedback: small flash, pulse, or sparkle-like scale animation, but keep it restrained.
- After the cell is occupied, the bonus marker may disappear under the piece, but the score feedback should confirm the claim.

Accessibility requirements:

- Bonus tiles must not be identified by color alone.
- TalkBack should announce a bonus tile cell as “Bonus tile, unclaimed” or “Bonus tile claimed by [player name]” when relevant.
- The help dialog must explain bonus tiles in plain language.

### 4.10 Game End

A player becomes inactive when either:

- They voluntarily pass, after confirmation, or
- The game determines they have no valid moves.

After a player passes, their turns are skipped.

The game ends when all active scoring players have passed or have no valid moves.

Final ranking is based on highest total score.

### 4.11 Corner Position Cache

The engine should maintain a cache of useful corner positions per player.

- `cornerCache[playerIndex]`: set of empty positions diagonally adjacent to that player’s pieces and not orthogonally adjacent to that player’s pieces
- Invalidate the cache after every accepted placement
- First move uses the assigned starting corner only
- Later move generation starts from cached corner positions

This cache is used by placement validation, ghost previews, computer-opponent move generation, and “no valid moves” detection.

## 5. Computer-Controlled Opponent System

Corners Apart does not use runtime artificial intelligence, machine learning models, LLMs, cloud AI, downloadable AI components, API keys, or network access for opponent turns.

The opponent system is local, deterministic game logic with predefined strategies and difficulty parameters. During development, AI tools may help design and tune those strategies, but the installed app itself only runs normal Kotlin code.

Do not market this as an AI feature. Do not use the word “AI” in the app UI, help text, Play Store listing, screenshots, onboarding, settings, or user-facing rules. User-facing text should say “computer opponent,” “opponent difficulty,” or simply “opponent.”

Opponent move calculation runs on `Dispatchers.Default` with an artificial delay of 300–700ms so turns feel natural.

### 5.1 Opponent Play Styles

Three predefined opponent play styles can fill any computer-controlled slot. Each style is modulated by the selected difficulty level.

#### Expansionist

- Maximizes spread, future corners, and available territory
- High spread bonus
- High future-corner bonus
- Medium bonus-tile interest
- On easy difficulty, may overextend and leave inefficient gaps

#### Opportunist

- Prioritizes bonus tiles and high-value placements
- Strongly values reachable bonus tiles
- Tries to claim bonus tiles before opponents can reach them
- On easy difficulty, may chase bonus tiles inefficiently

#### Blocker

- Pushes toward contested areas and blocks leading opponents
- High opponent-corner denial value
- Higher center and interception value
- Tries to prevent opponents from claiming nearby bonus tiles

### 5.2 Difficulty Levels

The opponent difficulty system has 5 levels.

Key design principles:

- Easy levels should be reliably easy, not just randomly weak.
- Hard levels should be reliably challenging through better evaluation, better move search, and stronger endgame awareness.
- Each level should feel noticeably different from adjacent levels.
- Bonus tile awareness should scale by difficulty.

| Parameter | 1 Beginner | 2 Easy | 3 Medium | 4 Hard | 5 Expert |
|---|---:|---:|---:|---:|---:|
| `temperature` | 3.0 | 2.0 | 1.0 | 0.5 | 0.2 |
| `skipFilterChance` | 0.7 | 0.4 | 0.1 | 0.0 | 0.0 |
| `localFocusRadius` | 3 | 5 | 20 | 20 | 20 |
| `maxCandidates` | 10 | 25 | 80 | 200 | 500 |
| `pieceSizeBias` | small-piece preference | slight small-piece preference | none | large early pieces | optimized opening |
| `blockingAwareness` | ignores opponents | occasional | reactive | proactive | predictive |
| `bonusTileAwareness` | mostly ignores | chases obvious bonuses | values reachable bonuses | contests bonuses | predicts bonus races |
| `territoryEval` | none | none | basic BFS | full BFS | BFS + lookahead |
| `cornerPreservation` | ignores | weak | balanced | strong | optimized |

### 5.3 Decision Pipeline

1. Gather context: player corners, opponent corners, claimed/unclaimed bonus tiles, leader, remaining pieces.
2. Generate valid moves: available pieces × orientations × candidate corner positions.
3. Apply difficulty pruning: local focus, candidate limits, anti-pattern skipping.
4. Evaluate moves:
   - placed cell count
   - bonus tiles claimed by this move
   - future corner creation
   - own future corners destroyed
   - opponent corners blocked
   - bonus tiles denied to opponents
   - center pressure
   - territory estimate
   - personality-specific weights
5. Apply anti-pattern filter:
   - avoid small pieces too early on higher difficulty
   - avoid zero-future-corner moves when alternatives exist
   - avoid chasing a bonus tile if the move ruins long-term position, except on easy levels
6. Choose a move using softmax selection modulated by difficulty temperature.
7. If the selected move fails validation, fall back to the best remaining valid move.
8. If no valid moves exist, pass automatically.

### 5.4 Computer-Controlled Players in Multiplayer

In multiplayer sessions, computer-controlled players exist only on the host device. Their moves are broadcast like any other accepted host move.

## 6. Visual Design

### 6.1 Style Direction

Modern, clean, playful. Light gray background, white board cells, vivid opaque glossy pieces, and a dark graphite board frame.

The game should not look like a physical board game imitation. Avoid wood, beige, cardboard, translucent plastic, skeuomorphic packaging references, or official product-like visuals.

The visual identity is built around:

- Sharp geometry
- High-contrast player colors
- Opaque glossy “lacquered candy” pieces
- Separated board cells
- Clean rounded typography
- Clear bonus-tile markers

### 6.2 Color Palette

#### Player Colors

| Index | Name | Base | Dark/Border | Highlight | Ghost 30% alpha |
|---:|---|---|---|---|---|
| 0 | Indigo | `#4338CA` | `#312E81` | `#6366F1` | `#4338CA4D` |
| 1 | Amber | `#E88C0A` | `#A16207` | `#F5B040` | `#E88C0A4D` |
| 2 | Coral | `#E8513D` | `#991B1B` | `#F08070` | `#E8513D4D` |
| 3 | Teal | `#0D9488` | `#134E4A` | `#2DD4BF` | `#0D94884D` |

#### Surface Colors

| Role | Color | Usage |
|---|---|---|
| App background | `#E4E4E8` | Main background |
| Board cell gap | `#DCDCE0` | Gaps between separated cells |
| Board cell surface | `#FAFAFA` | Empty cells |
| Board frame | `#2C2C30` | Dark graphite frame |
| Card surface | `#FFFFFF` | Sheets, dialogs, cards |
| Bonus accent | `#D8A928` | Bonus tile marker |

#### Text Colors

| Role | Color |
|---|---|
| Primary | `#1A1A1E` |
| Secondary | `#4A4A52` |
| Muted | `#8A8A92` |
| On player color | `#FFFFFF` |

### 6.3 Typography

Primary font: Quicksand bundled TTF.

| Role | Size | Weight |
|---|---:|---:|
| Logo/Title | 28sp | Bold 700 |
| Heading | 20sp | SemiBold 600 |
| Body | 16sp | Medium 500 |
| Label | 14sp | Medium 500 |
| Caption | 12sp | Regular 400 |
| Tiny | 10sp | Regular 400 |

### 6.4 Board Rendering

The board is drawn with Compose `Canvas` for performance.

The board is not a continuous grid with thin lines. Each cell is a separate square with visible gaps.

- Cell size: dynamically calculated from available screen width and board size
- Gap width: 2dp between cells
- Empty cell fill: `#FAFAFA`
- Cell corners: sharp, no rounding
- Board frame: 4dp dark graphite frame
- Start indicators: subtle colored markers on empty starting corner cells
- Bonus tile marker: small gold diamond/star rendered inside the cell, below ghost preview and below placed pieces

### 6.5 Piece Rendering

Each occupied cell is drawn with sharp square corners and a multi-layer glossy effect:

1. Base fill: player color, opaque
2. Top highlight: white at 35% alpha, top 25% of cell height
3. Bottom shadow: player dark color at 50% alpha, bottom 15% of cell height
4. Inner inset: black at 8% alpha, inset 2dp from edges
5. Drop shadow: 1dp offset, 2dp blur, black at 12% alpha

### 6.6 Ghost Preview

- Valid placement: player color at 30% alpha
- Invalid placement: red stripe pattern with red border
- Bonus claim preview: cells that would claim a bonus tile should show a subtle gold ring or pulse under the ghost
- The preview follows touch position, with the selected piece centered on the finger when dragging is used

### 6.7 Layout — Portrait Primary

```text
┌──────────────────────────────┐
│  Logo            Timer  ⋮    │
├──────────────────────────────┤
│ Indigo 34  Amber 29          │
│ Coral  31  Teal  27          │  ← Running positive scores
├──────────────────────────────┤
│  ┌────────────────────────┐  │
│  │ Dark graphite frame    │  │
│  │ ┌──────────────────┐   │  │
│  │ │   GAME BOARD      │  │  │
│  │ │ cells + bonuses   │  │  │
│  │ └──────────────────┘   │  │
│  └────────────────────────┘  │
├──────────────────────────────┤
│  Your turn. Bonus +3 ahead.  │
├──────────────────────────────┤
│  ↺  ↻  ⇆  Pass              │
├──────────────────────────────┤
│  Selected piece preview      │
│  Piece grid / strip          │
└──────────────────────────────┘
```

### 6.8 Top Bar

- Logo left
- Timer center-right, clock icon + `m:ss`
- Three-dot menu far right opens bottom sheet

### 6.9 Player Info Bar

- Shows running positive score for each player
- Active player: background tint in player color, animated dot or border
- Passed players: 40% opacity and clear “passed” state
- Optional compact breakdown on tap: placed cells, bonus tiles, completion bonus

### 6.10 Control Buttons

- Rotate CCW
- Rotate CW
- Flip
- Pass

All controls must have at least 48dp touch targets and content descriptions.

### 6.11 Piece Panel

- Scrollable grid or horizontal strip depending on screen size
- Each piece card: minimum 48dp touch target
- Selected piece: scale up and player-color border glow
- Used pieces: low opacity, non-interactive
- Bonus tile opportunities may be hinted in the board preview, not in the piece panel

### 6.12 Landscape / Tablet Layout

Use a three-column layout when width allows:

- Left panel: logo, timer, player scores, status
- Center: board
- Right panel: controls, selected piece preview, piece grid

### 6.13 Animations

| Animation | Duration | Easing |
|---|---:|---|
| Piece placement | 400ms | FastOutSlowIn with overshoot |
| Invalid attempt shake | 400ms | LinearOutSlowIn |
| Bonus tile claimed | 350ms | Small flash/pulse |
| Active player pulse | 1500ms | InfiniteRepeat, EaseInOut |
| Computer opponent thinking dot | 900ms | InfiniteRepeat, EaseInOut |
| Dialog enter | 300ms | FastOutSlowIn |
| Piece card intro | 300ms + 20ms stagger | FastOutSlowIn |
| Score increase | 250ms | Brief number pop or highlight |

Respect system animation scale. Disable or reduce animations when the user has disabled animations.

### 6.14 Haptic Feedback

- Valid placement: confirm haptic
- Bonus tile claimed: confirm haptic, slightly stronger than normal placement but not disruptive
- Invalid placement: reject haptic or short buzz-pause-buzz fallback
- Piece selection: light tick
- Button press: standard click haptic

### 6.15 Sound

- Piece placement: short warm “tok” sound
- Bonus tile claim: subtle brighter chime layered with placement sound
- Sound only for local human actions by default
- Sound can be toggled off in settings

## 7. Multiplayer Details

### 7.1 Connection Flow

#### Bluetooth

1. Host taps “Create game” and becomes discoverable.
2. Guest taps “Find games” and scans nearby devices.
3. Guest selects host.
4. Bluetooth pairing occurs if needed.
5. Connection established.
6. Host sends `GameConfig`, including mode, ruleset, board size, and bonus tile positions.
7. Guest confirms.
8. Game starts.

#### Wi-Fi Direct

1. Host taps “Create game” and creates Wi-Fi Direct group.
2. Guest taps “Find games” and discovers peers.
3. Guest selects host.
4. Wi-Fi Direct connection is created.
5. TCP socket is established on a known port.
6. Host sends the same game config protocol as Bluetooth.

#### Pass-and-Play

1. Select game mode and number of human players.
2. Assign colors and corners.
3. Generate bonus tile layout before the first move.
4. Play on a single device.
5. Optional setting: rotate board orientation on turn change.

### 7.2 Lobby

Before the game starts, the lobby shows:

- Game mode
- Board size
- Ruleset
- Bonus tile setting, enabled by default in standard Corners Apart
- Connected players with colors and names
- Empty slots, fillable by computer-controlled players
- opponent difficulty selector
- Start button, host only

### 7.3 Edge Cases

| Scenario | Behavior |
|---|---|
| Player disconnects | Pause 60s. If they reconnect, full sync. If not, a computer-controlled player can take over or game can be saved. |
| Host disconnects | Game ends or clients save local last-known state. |
| Move conflict | Host validates and rejects invalid moves. |
| Bonus tile mismatch | Host state is authoritative. Clients resync from host. |
| App backgrounded | Auto-save state. Multiplayer maintains connection for limited time, then disconnects. |
| Battery saver | Reduce animation intensity and disable nonessential haptics. |

### 7.4 Permissions Required

- `BLUETOOTH`, `BLUETOOTH_ADMIN`, `BLUETOOTH_CONNECT`, `BLUETOOTH_SCAN`, `BLUETOOTH_ADVERTISE`
- `ACCESS_FINE_LOCATION` for discovery on older Android versions
- `ACCESS_WIFI_STATE`, `CHANGE_WIFI_STATE`, `ACCESS_NETWORK_STATE`
- `NEARBY_WIFI_DEVICES` on Android 13+

Evaluate Google Nearby Connections API as an alternative to raw Bluetooth and Wi-Fi Direct if it significantly reduces complexity while meeting requirements.

## 8. Profiles, History & Statistics

### 8.1 Player Profiles

```kotlin
@Serializable
data class Profile(
    val id: Long,
    val name: String,
    val colorIndex: Int,
    val avatarStyle: String,
    val avatarSeed: String,
    val avatarData: String? = null,
    val active: Boolean,
    val history: List<HistoryEntry>
)
```

- One active profile at a time
- Profile color preference determines preferred player color where possible
- Avatar can use generated local styles or a custom compressed image
- Custom avatar max 160×160px, WebP preferred

### 8.2 History Entry

```kotlin
@Serializable
data class HistoryEntry(
    val date: String,
    val rank: Int,
    val totalScore: Int,
    val scoreBreakdown: ScoreBreakdown,
    val claimedBonusTiles: Int,
    val piecesPlaced: Int,
    val difficulty: Int,
    val ruleset: Ruleset,
    val gameMode: GameMode,
    val timeSeconds: Int,
    val scores: List<PlayerScore>
)

@Serializable
data class PlayerScore(
    val name: String,
    val totalScore: Int,
    val scoreBreakdown: ScoreBreakdown,
    val claimedBonusTiles: Int,
    val colorIndex: Int
)
```

History must treat higher score as better.

### 8.3 History & Stats Dialog

Two tabs: History and Stats.

History shows:

- Date
- Rank
- Total score
- Placed-cell points
- Bonus-tile points
- Completion bonus
- Difficulty
- Game duration
- All player scores

Stats show:

- Total games played
- Win count and win rate
- Average score
- Best score
- Average rank
- Average claimed bonus tiles
- Completion bonus count
- Favorite difficulty
- Score trend over last 20 games
- Stats per difficulty level

### 8.4 Persistence

Use DataStore for simple JSON persistence:

- `saved-game`: serialized `GameState`
- `profiles`: serialized profile list
- `settings`: difficulty, sound, haptics, preferred mode, preferred ruleset

`GameState` must include:

- Board cells
- Players
- Used pieces
- Current turn
- Passed state
- Bonus tile positions and claimed state
- Score breakdowns
- Game mode
- Ruleset
- Timer state
- Move history if needed for undo/replay

## 9. Dialogs & Sheets

### 9.1 Main Bottom Sheet

Opened from the top bar.

Contains:

- Difficulty selector, 1–5
- Game mode
- Multiplayer options
- Profiles
- History and stats
- Help
- Save current game
- Start new game, with confirmation if game is in progress
- Sound and haptic toggles

### 9.2 Game Over Dialog

Shows:

- Rankings, highest score first
- Winner highlighted
- Total scores
- Score breakdown per player
- Claimed bonus tiles
- Completion bonus if earned
- Game duration
- Play again button
- Stats link

### 9.3 Score Breakdown Dialog

Available by tapping a player score during or after the game.

Shows:

- Placed-cell points
- Bonus-tile points
- Completion bonus
- Claimed bonus tiles
- Pieces placed
- Pieces remaining

### 9.4 Confirm Dialog

Generic title + message + confirm/cancel buttons.

Used for:

- Pass
- New game
- Save and quit
- Multiplayer disconnect

### 9.5 Resume Game Dialog

Shown on app launch if a saved game exists.

Shows:

- Save date
- Game mode
- Current score leader
- Current scores
- Claimed bonus tiles
- Difficulty

Buttons:

- Continue
- New game

### 9.6 Help Dialog

Must explain Corners Apart rules in original wording.

Help content should include:

- Goal of the game
- Starting from corners
- How pieces connect by corners
- No same-color edge contact
- Opponent contact allowed
- Positive scoring
- Bonus tiles
- Completion bonus
- Passing
- Game end
- Rotate, flip, select, place controls
- Multiplayer basics

Do not copy external rule-sheet text.

## 10. Accessibility

- All interactive elements must have minimum 48dp touch targets.
- All icon-only buttons need content descriptions.
- Player colors must be supplemented by names, labels, icons, or positions.
- Bonus tiles must not rely on color alone.
- Turn changes should be announced via polite live region.
- Score increases should have accessible announcements, such as “Amber gained 8 points.”
- Bonus claims should announce, “Bonus tile claimed, plus 3.”
- Board cells should expose useful position and state descriptions for TalkBack where feasible.
- Respect system font scale, test up to at least 1.3x.
- Respect reduced motion settings.
- Ensure text contrast meets WCAG AA.

## 11. Constants

```kotlin
object GameConstants {
    const val STANDARD_BOARD_SIZE = 20
    const val COMPACT_BOARD_SIZE = 14
    const val PLAYER_COUNT = 4
    const val PIECE_COUNT = 21
    const val TOTAL_PIECE_CELLS = 89

    const val STANDARD_BONUS_TILE_COUNT = 10
    const val COMPACT_BONUS_TILE_COUNT = 6
    const val BONUS_TILE_POINTS = 3
    const val PLACED_CELL_POINTS = 1
    const val COMPLETION_BONUS_POINTS = 10

    const val MAX_HISTORY_ENTRIES = 50
    const val DIFFICULTY_LEVELS = 5

    const val BOARD_INTERACTION_LOCK_MS = 160L
    const val INVALID_FEEDBACK_COOLDOWN_MS = 180L
    const val OPPONENT_TURN_DELAY_MIN_MS = 300L
    const val OPPONENT_TURN_DELAY_RANGE_MS = 400L
    const val TURN_ADVANCE_DELAY_MS = 400L
    const val HUMAN_AUTO_PASS_DELAY_MS = 1500L
    const val SAVE_NOTIFICATION_DURATION_MS = 2000L
    const val RECONNECT_TIMEOUT_MS = 60_000L
    const val BACKGROUND_TIMEOUT_MS = 300_000L

    const val MAX_AVATAR_DIMENSION = 160
    const val MAX_AVATAR_FILE_SIZE = 5 * 1024 * 1024

    val AVATAR_STYLES = listOf(
        "bottts",
        "avataaars",
        "pixel-art",
        "fun-emoji",
        "lorelei",
        "thumbs"
    )

    val STANDARD_CORNERS = listOf(
        0 to 0,
        0 to 19,
        19 to 19,
        19 to 0
    )

    val COMPACT_DUEL_CORNERS = listOf(
        0 to 0,
        13 to 13
    )

    val PLAYER_NAMES = listOf("Indigo", "Amber", "Coral", "Teal")
    val PLAYER_COLORS = listOf("Indigo", "Amber", "Coral", "Teal")
}
```

Important: `TOTAL_PIECE_CELLS` is not a starting score. Players start at 0. `TOTAL_PIECE_CELLS` is used for validation, completion checks, statistics, and potential score breakdown display.

## 12. Dependencies

Check latest stable versions before implementation.

```kotlin
dependencies {
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.activity:activity-compose:1.9.3")

    implementation(platform("androidx.compose:compose-bom:2025.01.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")

    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")

    implementation("androidx.datastore:datastore-preferences:1.1.2")

    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")

    implementation("io.coil-kt:coil-compose:2.7.0")
    implementation("io.coil-kt:coil-svg:2.7.0")

    // Evaluate as an alternative to raw Bluetooth + Wi-Fi Direct:
    // implementation("com.google.android.gms:play-services-nearby:19.3.0")
}
```

## 13. Implementation Phases

All phases must preserve multiplayer-first architecture.

### Phase 1 — Foundation

1. Project setup: theme, colors, typography, custom Compose theme
2. Core models: Board, Piece, Player, GameState, BonusTile, ScoreBreakdown
3. `GameEngine` with placement rules and positive scoring
4. `PieceTransforms` with orientation caching
5. Bonus tile layout generation and serialization
6. Board rendering with separated cells, start indicators, and bonus tile markers
7. `GameSession` interface and `LocalSession`
8. Basic `GameViewModel` connecting engine state to UI

### Phase 2 — Playable Solo Game

9. Piece panel, selection, and preview
10. Touch interaction: ghost preview and tap/drag placement
11. Turn management: human + computer-controlled turns
12. Score updates after every placement
13. Bonus tile claim feedback
14. computer-opponent engine with 5 difficulty levels
15. Game end detection and ranking
16. Game over dialog with score breakdown
17. Control bar: rotate, flip, pass
18. Timer
19. Status messages

### Phase 3 — Multiplayer Infrastructure

20. `GameProtocol` message format
21. Host-authoritative move validation
22. Full state sync including bonus tiles and score breakdowns
23. Bluetooth session
24. Wi-Fi Direct session
25. Lobby screen
26. Reconnection handling
27. Pass-and-play mode

### Phase 4 — Game Modes

28. Solo vs Computer Opponents
29. Two-Color Duel
30. Compact Duel
31. Three-player mode
32. Four-player mode
33. Mixed human + computer-controlled support

### Phase 5 — Profiles & Persistence

34. Profile system
35. Game save/load
36. History recording with positive scoring
37. Statistics calculation
38. History & stats dialog
39. Resume game dialog

### Phase 6 — Polish

40. Animation tuning
41. Sound effects
42. Haptic feedback
43. computer-opponent bonus tile tuning
44. Bottom sheet with all settings
45. Help dialog with original Corners Apart rule text
46. Accessibility audit
47. Edge-to-edge display and system insets
48. Landscape/tablet layout
49. Performance optimization

### Phase 7 — Release Prep

50. App icon and splash screen
51. Play Store listing assets
52. Final rule wording review
53. Final computer-opponent balance testing
54. Multiplayer stress testing
55. Battery and performance profiling
56. Final product name decision
57. Trademark and product-identity review of all user-facing text

## 14. Non-Negotiable Implementation Notes

- Players start at 0 points.
- Higher score is better.
- Do not implement leftover-score ranking where lower score wins.
- Bonus tiles are part of the standard Corners Apart scoring system unless a future ruleset explicitly disables them.
- Bonus tile positions must be synchronized in multiplayer before the first move.
- Bonus tile state must be saved and restored.
- The app must not rely on an HTML prototype.
- Do not copy external rule text.
- Do not use external commercial board game names in user-facing app text.
- Keep the engine pure Kotlin and independent from Android UI code.
- Design for multiplayer from the start.
