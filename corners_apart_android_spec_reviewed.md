# Corners Apart - Android Technical Specification

> Working title: Corners Apart

## 1. Overview

**Type:** Native Android polyomino strategy game for 1 to 4 players.

**Primary platform:** Android, Kotlin, Jetpack Compose, Material 3 with a custom game visual style.

**SDK targets:**

- `minSdk = 26`, Android 8.0
- `compileSdk = 36`, Android 16 SDK
- `targetSdk = 36`, Android 16

The target SDK should be checked again immediately before implementation and release. If a dependency blocks `targetSdk = 36`, the project may temporarily target the highest Play Store compliant level, but the intended target is Android 16.

**Language/locale:** English primary. All UI text, error messages, help text, accessibility labels, and user-facing rule explanations must be written in English first.

Corners Apart is an original Android polyomino strategy game. Players place geometric pieces on a square grid, starting from their assigned board corners. A player’s own pieces may connect only through diagonal corner contact and may not touch each other by edge. Players gain positive points for cells they place, bonus tiles they claim, and completion bonuses.

This project starts from a clean Android implementation. Do not treat it as a port of an existing HTML prototype. Do not rely on any external prototype file. The rules, architecture, UI, data model, opponent behavior, local multiplayer, persistence, and accessibility behavior must be defined by this document.

### 1.1 Product Identity Guardrails

Corners Apart must be presented as its own game.

Do not implement, name, describe, market, or visually present this app as a clone, port, remake, or digital version of any existing commercial board game.

Do not use official board game names, logos, product artwork, product photos, box art, official rule-sheet wording, official marketing language, or official licensed digital-game presentation.

Do not use the word “Blokus” in the app UI, help text, Play Store listing, package metadata, screenshots, marketing copy, or user-facing rules.

The game may use generic rule language such as “place pieces so your own pieces touch only at corners,” but all explanatory text must be original to Corners Apart.

### 1.2 No Runtime AI Feature

Corners Apart does not include runtime artificial intelligence, machine learning models, LLMs, cloud AI, downloadable AI components, API keys, or online inference.

Computer-controlled opponents are normal local Kotlin game logic. Their play styles, difficulty values, heuristics, and strategy weights may be designed with the help of development tools before release, but the installed app only runs self-contained rule-based code.

Do not market the app as having AI. Do not use the word “AI” in the app UI, Play Store text, onboarding, settings, help text, screenshots, achievements, or user-facing rules.

Use user-facing wording such as:

- computer opponent
- opponent difficulty
- solo game
- practice game
- local opponent

### 1.3 Corners Apart Design Identity

Corners Apart should feel like a modern digital strategy game, not a physical board game imitation.

Key identity decisions:

- Game name: Corners Apart
- Core mechanic: corner-only same-player connection
- Start structure: players always start from assigned board corners in standard play
- Scoring: positive scoring, higher score wins
- Bonus system: visible bonus tiles are placed on the board at game start and give extra points when covered
- Visual style: clean, modern, playful, vivid, glossy, and geometric
- Player colors: indigo, amber, coral, teal
- Piece appearance: opaque glossy lacquered pieces with sharp square corners
- Board appearance: separated cells with visible gaps on a light-gray board surface and a dark graphite frame
- Avatars: local generated avatars only, no online avatar service by default

### 1.4 Core Differences From a Traditional Corner-Placement Polyomino Game

Corners Apart intentionally uses familiar broad ingredients from the polyomino strategy genre, but its product experience and scoring system are its own.

| Area | Corners Apart direction |
|---|---|
| Scoring | Positive score starts at 0. Higher is better. |
| Bonus tiles | Visible bonus tiles are generated at game start. Covering one gives extra points. |
| Running score | Scores update upward during play after every placement. |
| Game-over detail | Final results show placed-cell points, bonus-tile points, completion bonus, and total score. |
| Visual identity | Opaque glossy pieces, separated board cells, custom palette, dark graphite frame. |
| Digital features | Solo computer opponents, pass-and-play, nearby local multiplayer, profiles, history, statistics, save/resume. |
| Terminology | Uses Corners Apart terminology, not external board game branding. |

## 2. Architecture - Multiplayer-First Design

The architecture must be designed for multiplayer from the start. The game engine is completely decoupled from the UI and transport layers.

### 2.1 Core Principle

Every action in the game is a message: `Player X places Piece Y at Position Z with Orientation W`, `Player X passes`, or `Host sends synchronized game state`.

The game engine processes moves identically regardless of their source: local touch input, local computer-opponent computation, pass-and-play input, or nearby local multiplayer.

The host is authoritative in multiplayer sessions. Clients send move requests. The host validates moves, applies accepted moves, updates score, updates bonus-tile ownership, advances turn state, and broadcasts the resulting state.

### 2.2 Recommended Multiplayer Transport

Use Google Nearby Connections as the primary local multiplayer transport.

Nearby Connections supports offline peer-to-peer discovery, connection, and data exchange. It abstracts Bluetooth, BLE, and Wi-Fi under one API, which is preferable to building separate raw Bluetooth Classic and Wi-Fi Direct implementations for the first release.

User-facing wording should be “Nearby game,” “Create nearby game,” and “Find nearby game.” Do not expose low-level transport names unless needed for troubleshooting.

Raw Bluetooth Classic and raw Wi-Fi Direct implementations are optional future fallback transports. Do not implement them in the first release unless Nearby Connections fails a required test case.

### 2.3 High-Level Architecture

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
│   Pure Kotlin   │    │   Interface                 │
│   No Android UI │    │                              │
│   dependencies  │    │   ┌─ LocalSession           │
│                 │    │   │  Solo + pass-and-play   │
│   - Board       │    │   └─ NearbySession          │
│   - Placement   │    │      Local peer-to-peer     │
│   - Bonus tiles │    │                              │
│   - Scoring     │    │   Handles:                   │
│   - Turn logic  │    │   - connection lifecycle     │
│   - Validation  │    │   - move transmission        │
│                 │    │   - state synchronization    │
│                 │    │   - reconnection             │
└────────┬────────┘    └──────────────────────────────┘
         │
┌────────┴────────────────┐
│ ComputerOpponentEngine  │
│   Local self-contained  │
│   opponent logic        │
│   Runs on Dispatchers   │
│   .Default              │
└─────────────────────────┘
```

### 2.4 Project Structure

```text
app/src/main/java/com/[package]/
├── model/
│   ├── BoardSnapshot.kt        - Serializable immutable board state, flat cell list
│   ├── MutableBoard.kt         - Engine-only mutable board buffer with content equality helpers
│   ├── BonusTile.kt            - Bonus tile position and claimed state
│   ├── BonusTileLayout.kt      - Predefined or generated bonus-tile layouts
│   ├── Piece.kt                - 21 piece definitions, PieceDef data class
│   ├── PieceTransforms.kt      - Rotate, flip, normalize, getAllOrientations, cached
│   ├── Player.kt               - Player state: usedPieces, scoreBreakdown, passed
│   ├── ScoreBreakdown.kt       - placedCellPoints, bonusTilePoints, completionBonus
│   ├── GameState.kt            - Full serializable game state
│   ├── GameMode.kt             - SOLO, TWO_COLOR_DUEL, COMPACT_DUEL, THREE_PLAYER, FOUR_PLAYER
│   ├── Ruleset.kt              - STANDARD, FUTURE_VARIANT placeholders if needed
│   ├── Move.kt                 - playerIndex, pieceIndex, anchorRow, anchorCol, orientation
│   ├── Profile.kt              - Player profile
│   ├── HistoryEntry.kt         - Game history record
│   └── Difficulty.kt           - Difficulty level enum + parameters
│
├── engine/
│   ├── GameEngine.kt           - Pure game logic, no Android UI dependencies
│   ├── PlacementValidator.kt   - isValidPlacement(), corner-position cache
│   ├── BonusTileGenerator.kt   - fair bonus tile generation
│   ├── Scoring.kt              - positive score calculation, rankings, tie-breakers
│   └── GameRules.kt            - ruleset-specific constants and rule switches
│
├── opponents/
│   ├── ComputerOpponentEngine.kt   - Move generation, evaluation, selection
│   ├── OpponentStyle.kt            - Predefined play styles
│   ├── OpponentDifficulty.kt       - 5-level difficulty parameters
│   ├── OpponentState.kt            - Per-opponent state: momentum, frustration, RNG seed
│   ├── MoveEvaluator.kt            - Multi-criteria move scoring
│   ├── MoveGenerator.kt            - Valid move enumeration with pruning
│   ├── BonusTileAwareness.kt       - Bonus-tile opportunity and denial evaluation
│   └── Territory.kt                - BFS territory estimation
│
├── multiplayer/
│   ├── GameSession.kt          - Interface for all session types
│   ├── LocalSession.kt         - Solo with computer opponents + pass-and-play
│   ├── NearbySession.kt        - Google Nearby Connections session
│   ├── NearbyDiscovery.kt      - Advertising, discovery, endpoint selection
│   ├── GameProtocol.kt         - Message format, serialization
│   ├── SyncManager.kt          - State synchronization, conflict resolution
│   └── ReconnectionHandler.kt  - Handle disconnects and rejoin logic
│
├── ui/
│   ├── theme/
│   ├── screens/
│   ├── components/
│   ├── dialogs/
│   ├── sheets/
│   └── util/
│
├── viewmodel/
│   ├── GameViewModel.kt
│   ├── LobbyViewModel.kt
│   └── ProfileViewModel.kt
│
└── data/
    ├── GameRepository.kt       - DataStore persistence, save/load game
    ├── ProfileRepository.kt    - Profile CRUD
    └── Serializers.kt          - kotlinx.serialization helpers
```

### 2.5 Serialization Rule

All models that appear in save state or network messages must be explicitly serializable.

Use `@Serializable` on at least:

- `GameState`
- `BoardSnapshot`
- `Player`
- `ScoreBreakdown`
- `BonusTile`
- `Move`
- `SessionPlayer`
- `GameMessage`
- `GameConfig`
- `HistoryEntry`
- `PlayerScore`
- `Profile`

Do not rely on non-serializable Android classes inside these models. Keep them pure Kotlin.

### 2.6 Board Representation Warning

Do not use `data class Board(val cells: Array<IntArray>)` for persistent state or Compose-observed state.

Kotlin arrays use reference equality in generated data-class equality. Two boards with identical cell contents could compare as different or fail to behave as expected in state comparisons.

Use a flat immutable snapshot for UI and serialization:

```kotlin
@Serializable
data class BoardSnapshot(
    val size: Int,
    val cells: List<Int>
) {
    init {
        require(cells.size == size * size)
    }

    fun index(row: Int, col: Int): Int = row * size + col
    fun get(row: Int, col: Int): Int = cells[index(row, col)]
}
```

Use an engine-only mutable buffer for faster validation and move generation:

```kotlin
class MutableBoard(
    val size: Int,
    private val cells: IntArray = IntArray(size * size) { EMPTY }
) {
    fun index(row: Int, col: Int): Int = row * size + col
    fun get(row: Int, col: Int): Int = cells[index(row, col)]
    fun set(row: Int, col: Int, value: Int) { cells[index(row, col)] = value }

    fun toSnapshot(): BoardSnapshot = BoardSnapshot(size, cells.toList())

    override fun equals(other: Any?): Boolean =
        other is MutableBoard && size == other.size && cells.contentEquals(other.cells)

    override fun hashCode(): Int = 31 * size + cells.contentHashCode()

    companion object {
        const val EMPTY = -1
    }
}
```

## 3. Session Interface and Protocol

### 3.1 GameSession Interface

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

@Serializable
data class SessionPlayer(
    val index: Int,
    val name: String,
    val isLocal: Boolean,
    val isComputerControlled: Boolean,
    val colorIndex: Int
)

enum class SessionType {
    LOCAL,
    NEARBY
}

enum class ConnectionState {
    DISCONNECTED,
    DISCOVERING,
    CONNECTING,
    CONNECTED,
    RECONNECTING,
    FAILED
}
```

### 3.2 Game Protocol

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
        val randomSeed: Long,
        val bonusTiles: List<BonusTile>
    ) : GameMessage()
    @Serializable data object Ping : GameMessage()
    @Serializable data object Pong : GameMessage()
}
```

The host model is authoritative. One device runs the `GameEngine`, validates all moves, calculates scoring, updates bonus-tile ownership, and broadcasts accepted state updates.

If a player disconnects mid-game, the game pauses for up to 60 seconds. If they reconnect, the host sends a full sync. If they do not reconnect, the host may assign their remaining turns to a computer-controlled opponent at difficulty 3 or allow players to save and exit.

## 4. Game Modes

Mode names should avoid terminology that directly echoes existing commercial variants. Use Corners Apart-specific names where possible.

### 4.1 Solo vs Computer Opponents, 20 x 20

- 1 human player + 3 computer-controlled opponents
- Human starts at bottom-right corner `[19,19]` by default
- Computer opponent corners are assigned among the remaining three corners
- 5 opponent difficulty levels
- Turn order proceeds around the four corner positions
- 10 bonus tiles are generated at game start and visible to all players

### 4.2 Two-Color Duel, 20 x 20

- 2 human players
- Each player controls 2 colors, two sets of 21 pieces each
- Player 1 controls opposite corners, colors 0 and 2
- Player 2 controls opposite corners, colors 1 and 3
- Turn order cycles through colors: 0, 1, 2, 3
- Available via pass-and-play and nearby local multiplayer

### 4.3 Compact Duel, 14 x 14

- 2 players, 1 color per player
- Smaller 14 x 14 board
- 21 pieces per player
- Start positions remain corner-based by design:
  - Player 0 starts at `[0,0]`
  - Player 1 starts at `[13,13]`
- Faster games, better suited for short mobile sessions
- Recommended bonus tile count: 4 to 6
- Available via pass-and-play and nearby local multiplayer

Compact Duel must be play-tested. If corner starts on 14 x 14 feel too isolated or too predictable, adjust the bonus tile layout, board size, or piece availability. Do not move the start positions inward unless the game design intentionally changes later.

### 4.4 Three-Player, 20 x 20

- 3 human or computer-controlled players, each with their own color
- The fourth color can be disabled or controlled as a rotating shared color, depending on implementation choice
- If a shared color is used, its score is ignored in final ranking
- Bonus tiles are claimable by active scoring players only
- Available via pass-and-play or nearby local multiplayer

### 4.5 Four-Player, 20 x 20

- 4 players, each with one color and one corner
- This is the primary standard Corners Apart mode
- Available via pass-and-play and nearby local multiplayer

### 4.6 Mixed Human + Computer-Controlled Modes

In any supported mode, empty slots may be filled with computer-controlled players at a host-selected difficulty level. In multiplayer, computer-controlled moves are calculated on the host device and broadcast as regular accepted moves.

## 5. Core Game Logic

This section defines the Corners Apart ruleset from scratch. Do not depend on any external prototype or external rule sheet.

### 5.1 Board

- Standard board size: 20 x 20
- Compact board size: 14 x 14
- Cell values:
  - `-1`: empty
  - `0..3`: occupied by player index
- Bonus tiles are stored separately from occupied cell values
- A bonus tile can be visible, unclaimed, claimed, or hidden under a placed piece depending on UI rendering needs

```kotlin
@Serializable
data class BonusTile(
    val row: Int,
    val col: Int,
    val claimedByPlayerIndex: Int? = null,
    val claimedOnTurn: Int? = null
)
```

### 5.2 Piece Definitions

Each player has 21 pieces. The total cell count per player is 89.

The set is made from polyomino-style shapes of size 1 to 5 cells. The exact shapes must be defined in `Piece.kt`. The code should treat piece definitions as Corners Apart game data, not as user-facing references to any external game.

```kotlin
@Serializable
data class PieceDef(
    val id: String,
    val displayName: String,
    val cells: List<CellOffset>
)

@Serializable
data class CellOffset(
    val row: Int,
    val col: Int
)
```

Implementation requirements:

- `PIECE_COUNT = 21`
- `TOTAL_PIECE_CELLS = 89`
- Each player owns one independent copy of the full piece set
- A piece can be used once per player color
- Used pieces become unavailable and visually dimmed

### 5.3 Piece Transformations

The game must support rotation and flipping.

- `rotateCW(cells)`: `[r,c]` becomes `[c,-r]`
- `rotateCCW(cells)`: `[r,c]` becomes `[-c,r]`
- `flipH(cells)`: `[r,c]` becomes `[r,-c]`
- `normalize(cells)`: shift to origin and lexicographically sort
- `getAllOrientations(cells)`: return all unique orientations, maximum 8, cached by piece id

### 5.4 Placement Rules

A placement is valid only if all of the following are true:

1. Every cell of the piece is within board bounds.
2. Every cell of the piece lands on an empty board cell.
3. The piece has not already been used by that player color.
4. The player’s first piece covers their assigned starting corner cell.
5. The player’s later pieces touch at least one of that player’s existing pieces diagonally.
6. The player’s pieces may not touch that same player’s existing pieces orthogonally by edge.
7. A player’s pieces may touch opponent pieces by edge or corner.
8. A piece may cover an unclaimed bonus tile. This claims the bonus tile for that player and awards bonus points.
9. Claimed bonus tiles are always on occupied cells, so rule 2 already prevents covering or claiming them again.

Do not add special-case placement logic for already claimed bonus tiles.

### 5.5 Start Positions

Corners Apart standard play always starts from the board corners.

**Standard 20 x 20 board:**

- Player 0: `[0,0]`
- Player 1: `[0,19]`
- Player 2: `[19,19]`
- Player 3: `[19,0]`

**Solo default:**

- Human: `[19,19]`
- Computer-controlled players: assigned to the remaining corners

**Compact 14 x 14 duel:**

- Player 0: `[0,0]`
- Player 1: `[13,13]`

The UI must clearly indicate each player’s starting corner before and during the first round. The first placement must cover that player’s assigned starting corner exactly.

### 5.6 Positive Scoring

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

### 5.7 Score Breakdown

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

`players[].score` must count upward from 0 and should equal `players[].scoreBreakdown.total`. Prefer storing the breakdown and deriving the total from it.

Score updates on every accepted placement:

```text
placement points = piece cell count
bonus points = number of newly covered bonus tiles x 3
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

### 5.8 Bonus Tiles

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

- Prefer a small set of predefined symmetrical layout templates for 20 x 20 and 14 x 14 boards.
- Randomly select one template at game start.
- Optionally rotate or mirror the template before use.
- Store the chosen template id, transform, and random seed in `GameState` for replay/debugging.
- Avoid fully unconstrained random generation unless fairness validation is implemented.

Example model:

```kotlin
@Serializable
data class BonusTileLayout(
    val id: String,
    val boardSize: Int,
    val positions: List<CellPosition>
)

@Serializable
data class CellPosition(
    val row: Int,
    val col: Int
)
```

### 5.9 Bonus Tile Rendering

Visual requirements:

- Use a distinct marker on the cell, such as a small diamond or star shape.
- Use accent gold, recommended `#D8A928`, with semi-transparent alpha.
- The marker must be visible but not visually louder than player pieces.
- If a piece ghost hovers over a bonus tile, the preview should make it clear the bonus will be claimed.
- When claimed, show brief visual feedback: small flash or pulse animation, kept restrained.
- After the cell is occupied, the bonus marker may disappear under the piece, but the score feedback should confirm the claim.

Accessibility requirements:

- Bonus tiles must not be identified by color alone.
- TalkBack should announce a bonus tile cell as “Bonus tile, unclaimed” or “Bonus tile claimed by [player name]” when relevant.
- The help dialog must explain bonus tiles in plain language.

### 5.10 Game End

A player becomes inactive when either:

- They voluntarily pass, after confirmation, or
- The game determines they have no valid moves.

After a player passes, their turns are skipped.

The game ends when all active scoring players have passed or have no valid moves.

Final ranking is based on highest total score.

### 5.11 Corner Position Cache

The engine should maintain a cache of useful corner positions per player.

- `cornerCache[playerIndex]`: set of empty positions diagonally adjacent to that player’s pieces and not orthogonally adjacent to that player’s pieces
- Invalidate the cache after every accepted placement
- First move uses the assigned starting corner only
- Later move generation starts from cached corner positions

This cache is used by placement validation, ghost previews, computer-opponent move generation, and no-valid-moves detection.

## 6. Computer-Controlled Opponent System

Corners Apart’s opponent system is local, self-contained game logic with predefined strategies and difficulty parameters.

It is not runtime AI. It does not use machine learning, LLMs, cloud APIs, online inference, downloadable models, or API keys.

Opponent move calculation runs on `Dispatchers.Default` with an artificial delay of 300 to 700ms so turns feel natural.

### 6.1 Randomness and Replays

The opponent system may use randomness for easier and more varied play, including temperature-based softmax move selection.

Because of that, do not describe the system as deterministic unless every source of randomness is seeded and replayed.

Implementation requirement:

- Store a `randomSeed` in `GameState`.
- Use a seeded Kotlin RNG for opponent decisions and bonus layout selection.
- Store enough move history for debugging and optional replay.
- For exact replay, all opponent randomness must come from the stored seeded RNG.

### 6.2 Opponent Play Styles

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

### 6.3 Difficulty Levels

The opponent difficulty system has 5 levels.

Key design principles:

- Easy levels should be reliably easy, not just randomly weak.
- Hard levels should be reliably challenging through better evaluation, better move search, and stronger endgame awareness.
- Each level should feel noticeably different from adjacent levels.
- Bonus tile awareness should scale by difficulty.
- Difficulty should be limited by a time budget so low-end devices do not stall.

| Parameter | 1 Beginner | 2 Easy | 3 Medium | 4 Hard | 5 Expert |
|---|---:|---:|---:|---:|---:|
| `temperature` | 3.0 | 2.0 | 1.0 | 0.5 | 0.2 |
| `skipFilterChance` | 0.7 | 0.4 | 0.1 | 0.0 | 0.0 |
| `localFocusRadius` | 3 | 5 | 20 | 20 | 20 |
| `candidateSoftCap` | 10 | 25 | 80 | 200 | 500 |
| `timeBudgetMs` | 250 | 400 | 700 | 1200 | 1800 |
| `pieceSizeBias` | small-piece preference | slight small-piece preference | none | large early pieces | optimized opening |
| `blockingAwareness` | ignores opponents | occasional | reactive | proactive | predictive |
| `bonusTileAwareness` | mostly ignores | chases obvious bonuses | values reachable bonuses | contests bonuses | predicts bonus races |
| `territoryEval` | none | none | basic BFS | full BFS | BFS + shallow lookahead |
| `cornerPreservation` | ignores | weak | balanced | strong | optimized |

`candidateSoftCap` is not a promise to evaluate exactly that many moves. Stop early if the time budget is reached. Always return a legal move or pass.

### 6.4 Decision Pipeline

1. Gather context: player corners, opponent corners, claimed/unclaimed bonus tiles, leader, remaining pieces.
2. Generate valid moves: available pieces x orientations x candidate corner positions.
3. Apply difficulty pruning: local focus, candidate soft caps, time budget, anti-pattern skipping.
4. Evaluate moves:
   - placed cell count
   - bonus tiles claimed by this move
   - future corner creation
   - own future corners destroyed
   - opponent corners blocked
   - bonus tiles denied to opponents
   - center pressure
   - territory estimate
   - style-specific weights
5. Apply anti-pattern filter:
   - avoid small pieces too early on higher difficulty
   - avoid zero-future-corner moves when alternatives exist
   - avoid chasing a bonus tile if the move ruins long-term position, except on easy levels
6. Choose a move using softmax selection modulated by difficulty temperature.
7. If the selected move fails validation, fall back to the best remaining valid move.
8. If no valid moves exist, pass automatically.

### 6.5 Computer-Controlled Players in Multiplayer

In multiplayer sessions, computer-controlled players exist only on the host device. Their moves are broadcast like any other accepted host move.

## 7. Visual Design

### 7.1 Style Direction

Modern, clean, playful. Light gray background, white board cells, vivid opaque glossy pieces, and a dark graphite board frame.

The game should not look like a physical board game imitation. Avoid wood, beige, cardboard, translucent plastic, skeuomorphic packaging references, or official product-like visuals.

The visual identity is built around:

- Sharp geometry
- High-contrast player colors
- Opaque glossy lacquered pieces
- Separated board cells
- Clean rounded typography
- Clear bonus-tile markers

### 7.2 Color Palette

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

### 7.3 Typography

Primary font: Quicksand bundled TTF.

| Role | Size | Weight |
|---|---:|---:|
| Logo/Title | 28sp | Bold 700 |
| Heading | 20sp | SemiBold 600 |
| Body | 16sp | Medium 500 |
| Label | 14sp | Medium 500 |
| Caption | 12sp | Regular 400 |
| Tiny | 10sp | Regular 400 |

Do not include or redistribute font files in external documentation or user-visible exported artifacts. Bundle fonts only inside the Android project under the correct license.

### 7.4 Board Rendering

The board is drawn with Compose `Canvas` for performance.

The board is not a continuous grid with thin lines. Each cell is a separate square with visible gaps.

- Cell size: dynamically calculated from available screen width and board size
- Gap width: 2dp between cells
- Empty cell fill: `#FAFAFA`
- Cell corners: sharp, no rounding
- Board frame: 4dp dark graphite frame
- Start indicators: subtle colored markers on empty starting corner cells
- Bonus tile marker: small gold diamond/star rendered inside the cell, below ghost preview and below placed pieces

### 7.5 Piece Rendering

Each occupied cell is drawn with sharp square corners and a multi-layer glossy effect:

1. Base fill: player color, opaque
2. Top highlight: white at 35% alpha, top 25% of cell height
3. Bottom shadow: player dark color at 50% alpha, bottom 15% of cell height
4. Inner inset: black at 8% alpha, inset 2dp from edges
5. Drop shadow: 1dp offset, 2dp blur, black at 12% alpha

### 7.6 Ghost Preview

- Valid placement: player color at 30% alpha
- Invalid placement: red stripe pattern with red border
- Bonus claim preview: cells that would claim a bonus tile should show a subtle gold ring or pulse under the ghost
- The preview follows touch position, with the selected piece centered on the finger when dragging is used

### 7.7 Layout - Portrait Primary

```text
┌──────────────────────────────┐
│  Logo            Timer  ⋮    │
├──────────────────────────────┤
│ Indigo 34  Amber 29          │
│ Coral  31  Teal  27          │  Running positive scores
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

### 7.8 Player Info Bar

- Shows running positive score for each player
- Active player: background tint in player color, animated dot or border
- Passed players: 40% opacity and clear “passed” state
- Optional compact breakdown on tap: placed cells, bonus tiles, completion bonus

### 7.9 Control Buttons

- Rotate CCW
- Rotate CW
- Flip
- Pass

All controls must have at least 48dp touch targets and content descriptions.

### 7.10 Piece Panel

- Scrollable grid or horizontal strip depending on screen size
- Each piece card: minimum 48dp touch target
- Selected piece: scale up and player-color border glow
- Used pieces: low opacity, non-interactive
- Bonus tile opportunities may be hinted in the board preview, not in the piece panel

### 7.11 Landscape / Tablet Layout

Use a three-column layout when width allows:

- Left panel: logo, timer, player scores, status
- Center: board
- Right panel: controls, selected piece preview, piece grid

### 7.12 Animations

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

### 7.13 Haptic Feedback

- Valid placement: confirm haptic
- Bonus tile claimed: confirm haptic, slightly stronger than normal placement but not disruptive
- Invalid placement: reject haptic or short buzz-pause-buzz fallback
- Piece selection: light tick
- Button press: standard click haptic

### 7.14 Sound

- Piece placement: short warm “tok” sound
- Bonus tile claim: subtle brighter chime layered with placement sound
- Sound only for local human actions by default
- Sound can be toggled off in settings

## 8. Local Multiplayer Details

### 8.1 Nearby Connection Flow

1. Host taps “Create nearby game.”
2. Host starts advertising through Nearby Connections.
3. Guest taps “Find nearby game.”
4. Guest starts discovery.
5. Guest selects host endpoint.
6. Both devices show the same short connection code/name for confirmation.
7. Both sides accept the connection.
8. Host sends `GameConfig`, including mode, ruleset, board size, random seed, and bonus tile positions.
9. Guest confirms.
10. Game starts.

### 8.2 Pass-and-Play

1. Select game mode and number of human players.
2. Assign colors and corners.
3. Generate bonus tile layout before the first move.
4. Play on a single device.
5. Optional setting: rotate board orientation on turn change.

### 8.3 Lobby

Before the game starts, the lobby shows:

- Game mode
- Board size
- Ruleset
- Bonus tile setting, enabled by default in standard Corners Apart
- Connected players with colors and names
- Empty slots, fillable by computer-controlled players
- Opponent difficulty selector
- Start button, host only

### 8.4 Edge Cases

| Scenario | Behavior |
|---|---|
| Player disconnects | Pause 60s. If they reconnect, full sync. If not, a computer-controlled player can take over or game can be saved. |
| Host disconnects | Game ends or clients save local last-known state. |
| Move conflict | Host validates and rejects invalid moves. |
| Bonus tile mismatch | Host state is authoritative. Clients resync from host. |
| App backgrounded | Auto-save state. Multiplayer maintains connection for limited time, then disconnects. |
| Battery saver | Reduce animation intensity and disable nonessential haptics. |

### 8.5 Permissions

The exact permission set depends on whether the app uses only Nearby Connections or later adds raw Bluetooth/Wi-Fi Direct fallback.

For Android 12 and higher Bluetooth-related discovery/advertising/connection:

```xml
<uses-permission android:name="android.permission.BLUETOOTH_SCAN"
    android:usesPermissionFlags="neverForLocation" />
<uses-permission android:name="android.permission.BLUETOOTH_ADVERTISE" />
<uses-permission android:name="android.permission.BLUETOOTH_CONNECT" />
```

For Android 11 and lower legacy Bluetooth:

```xml
<uses-permission android:name="android.permission.BLUETOOTH"
    android:maxSdkVersion="30" />
<uses-permission android:name="android.permission.BLUETOOTH_ADMIN"
    android:maxSdkVersion="30" />
```

For Android 13 and higher nearby Wi-Fi APIs, if required by the chosen transport:

```xml
<uses-permission android:name="android.permission.NEARBY_WIFI_DEVICES"
    android:usesPermissionFlags="neverForLocation" />
```

For Android 12L and lower Wi-Fi discovery compatibility, if required:

```xml
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION"
    android:maxSdkVersion="32" />
```

If raw Wi-Fi Direct sockets are implemented later, add:

```xml
<uses-permission android:name="android.permission.ACCESS_WIFI_STATE" />
<uses-permission android:name="android.permission.CHANGE_WIFI_STATE" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
<uses-permission android:name="android.permission.INTERNET" />
```

`INTERNET` for raw Wi-Fi Direct is needed for Java socket communication. It does not mean the app uses a remote server.

The app must not request location permission unless the selected transport and Android version require it. If the app does not derive physical location from nearby device scans, use `neverForLocation` where Android supports it.

### 8.6 Nearby Privacy Note

Nearby Connections works offline for peer-to-peer multiplayer, but Google Play services Nearby SDK may collect usage diagnostics such as performance metrics and device information. Do not add separate app analytics unless deliberately specified later.

The Play Store privacy/data-safety wording must be checked before release based on the actual SDKs and permissions included in the final build.

## 9. Profiles, Avatars, History & Statistics

### 9.1 Avatar Decision

Use local avatars by default.

Do not use DiceBear, remote SVG avatars, web avatar APIs, or online avatar generation in the first release.

Reasons:

- Avoid unnecessary network behavior.
- Avoid external attribution and license obligations.
- Avoid adding Coil/network image loading only for avatars.
- Keep the app’s privacy story simple.

Supported avatar options:

- Initials avatar generated from player name
- Local geometric avatar generated from seed
- Local mosaic avatar generated from seed
- Custom image selected from device using Android Photo Picker or equivalent local image picker

Custom image handling:

- Decode locally.
- Crop to square.
- Resize to max 160 x 160px.
- Store as compressed WebP or PNG in app-private storage.
- Store only a local URI/path or encoded small avatar data in profile state.

### 9.2 Player Profiles

```kotlin
@Serializable
data class Profile(
    val id: Long,
    val name: String,
    val colorIndex: Int,
    val avatarStyle: LocalAvatarStyle,
    val avatarSeed: String,
    val customAvatarPath: String? = null,
    val active: Boolean,
    val history: List<HistoryEntry>
)

@Serializable
enum class LocalAvatarStyle {
    INITIALS,
    GEOMETRIC,
    MOSAIC,
    RINGS
}
```

- One active profile at a time
- Profile color preference determines preferred player color where possible
- Avatar generation must be entirely local
- Custom avatar max 160 x 160px

### 9.3 History Entry

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

### 9.4 History & Stats Dialog

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

### 9.5 Persistence

Use DataStore for simple JSON persistence:

- `saved-game`: serialized `GameState`
- `profiles`: serialized profile list
- `settings`: difficulty, sound, haptics, preferred mode, preferred ruleset

`GameState` must include:

- Board cells as `BoardSnapshot`
- Players
- Used pieces
- Current turn
- Passed state
- Bonus tile positions and claimed state
- Score breakdowns
- Game mode
- Ruleset
- Timer state
- Random seed
- Move history if needed for undo/replay/debugging

## 10. Dialogs & Sheets

### 10.1 Main Bottom Sheet

Opened from the top bar.

Contains:

- Difficulty selector, 1 to 5
- Game mode
- Nearby game options
- Profiles
- History and stats
- Help
- Save current game
- Start new game, with confirmation if game is in progress
- Sound and haptic toggles

### 10.2 Game Over Dialog

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

### 10.3 Score Breakdown Dialog

Available by tapping a player score during or after the game.

Shows:

- Placed-cell points
- Bonus-tile points
- Completion bonus
- Claimed bonus tiles
- Pieces placed
- Pieces remaining

### 10.4 Confirm Dialog

Generic title + message + confirm/cancel buttons.

Used for:

- Pass
- New game
- Save and quit
- Multiplayer disconnect

### 10.5 Resume Game Dialog

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

### 10.6 Help Dialog

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
- Nearby multiplayer basics

Do not copy external rule-sheet text.

## 11. Accessibility

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

## 12. Constants

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

    val LOCAL_AVATAR_STYLES = listOf(
        LocalAvatarStyle.INITIALS,
        LocalAvatarStyle.GEOMETRIC,
        LocalAvatarStyle.MOSAIC,
        LocalAvatarStyle.RINGS
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

Important: `TOTAL_PIECE_CELLS` is not a starting score. Players start at 0. `TOTAL_PIECE_CELLS` is used for validation, completion checks, statistics, and score breakdown display.

## 13. Dependencies and Build Configuration

Check latest stable versions before implementation.

### 13.1 Android Configuration

```kotlin
android {
    namespace = "com.example.cornersapart"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.cornersapart"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = "11"
    }
}
```

### 13.2 Dependencies

Use a Gradle version catalog (`libs.versions.toml`). Do not copy old pinned versions from older prototypes. Resolve current stable versions from official AndroidX, Kotlin, Google Maven, and library documentation immediately before implementation.

Recommended plugin setup:

```kotlin
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.serialization)
}
```

Core dependencies using version catalog aliases:

```kotlin
dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)

    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.kotlinx.serialization.json)

    implementation(libs.google.play.services.nearby)
}
```

Do not include Coil by default. The first release uses local avatar generation and local image decoding only.

If a future feature reintroduces remote avatars or remote images, add Coil 3, not Coil 2:

```kotlin
implementation("io.coil-kt.coil3:coil-compose:<latest-stable>")
implementation("io.coil-kt.coil3:coil-network-okhttp:<latest-stable>")
implementation("io.coil-kt.coil3:coil-svg:<latest-stable>")
```

Only add those dependencies if the app intentionally uses network image loading. Adding network image loading also requires a privacy and Play Store data-safety review.

## 14. Implementation Phases

All phases must preserve multiplayer-first architecture.

### Phase 1 - Foundation

1. Project setup: SDK 36 target, theme, colors, typography, custom Compose theme
2. Core models: BoardSnapshot, MutableBoard, Piece, Player, GameState, BonusTile, ScoreBreakdown
3. Serialization setup for all save-state and protocol models
4. `GameEngine` with placement rules and positive scoring
5. `PieceTransforms` with orientation caching
6. Bonus tile layout generation and serialization
7. Board rendering with separated cells, start indicators, and bonus tile markers
8. `GameSession` interface and `LocalSession`
9. Basic `GameViewModel` connecting engine state to UI

### Phase 2 - Playable Solo Game

10. Piece panel, selection, and preview
11. Touch interaction: ghost preview and tap/drag placement
12. Turn management: human + computer-controlled turns
13. Score updates after every placement
14. Bonus tile claim feedback
15. Computer-opponent engine with 5 difficulty levels and time budgets
16. Game end detection and ranking
17. Game over dialog with score breakdown
18. Control bar: rotate, flip, pass
19. Timer
20. Status messages

### Phase 3 - Nearby Multiplayer Infrastructure

21. `GameProtocol` message format
22. Host-authoritative move validation
23. Full state sync including bonus tiles, random seed, and score breakdowns
24. Nearby Connections session
25. Nearby discovery and connection confirmation flow
26. Lobby screen
27. Reconnection handling
28. Pass-and-play mode

### Phase 4 - Game Modes

29. Solo vs Computer Opponents
30. Two-Color Duel
31. Compact Duel, with play-testing for corner starts
32. Three-player mode
33. Four-player mode
34. Mixed human + computer-controlled support

### Phase 5 - Profiles & Persistence

35. Local avatar generation
36. Optional custom local avatar image import
37. Profile system
38. Game save/load
39. History recording with positive scoring
40. Statistics calculation
41. History & stats dialog
42. Resume game dialog

### Phase 6 - Polish

43. Animation tuning
44. Sound effects
45. Haptic feedback
46. Computer-opponent bonus tile tuning
47. Bottom sheet with all settings
48. Help dialog with original Corners Apart rule text
49. Accessibility audit
50. Edge-to-edge display and system insets
51. Landscape/tablet layout
52. Performance optimization

### Phase 7 - Release Prep

53. App icon and splash screen
54. Play Store listing assets
55. Final rule wording review
56. Final computer-opponent balance testing
57. Nearby multiplayer stress testing
58. Battery and performance profiling
59. Privacy/data-safety review based on actual SDKs and permissions
60. Final product name decision
61. Trademark and product-identity review of all user-facing text

## 15. Non-Negotiable Implementation Notes

- Players start at 0 points.
- Higher score is better.
- Do not implement leftover-score ranking where lower score wins.
- Bonus tiles are part of the standard Corners Apart scoring system unless a future ruleset explicitly disables them.
- Bonus tile positions must be synchronized in multiplayer before the first move.
- Bonus tile state must be saved and restored.
- Claimed bonus tiles need no special placement exception because claimed tiles are already occupied.
- The app must not rely on an HTML prototype.
- Do not copy external rule text.
- Do not use external commercial board game names in user-facing app text.
- Do not use the word “AI” anywhere user-facing.
- Keep computer-controlled opponents as local self-contained Kotlin logic.
- Use seeded randomness if replay/debug determinism is needed.
- Keep the engine pure Kotlin and independent from Android UI code.
- Do not use `Array<IntArray>` inside a Kotlin data class for observed or serialized board state.
- Prefer Nearby Connections over separate raw Bluetooth and Wi-Fi Direct implementations in the first release.
- Use local avatars. Do not use DiceBear or remote avatar services in the first release.
- Design for multiplayer from the start.
