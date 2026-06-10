# Polyo Strategy Game — Android Technical Specification

> **Working title:** Corners Apart

## 1. Overview

**Type:** Native Android polyomino strategy board game for 1–4 players. Supports solo play against AI, local pass-and-play multiplayer, Bluetooth multiplayer, and Wi-Fi Direct multiplayer.

**Language/Locale:** English primary. All UI text, error messages, accessibility labels.

**Platform:** Android (min SDK 26 / Android 8.0), Jetpack Compose, Material 3 (heavily customized).

**Source reference:** Based on an existing single-file HTML implementation (`blokus-specs.md`). Core game rules and piece definitions are identical. This spec defines the Android architecture, multiplayer system, visual design, and AI improvements. The html-file is not essential for the Android app. This is a new project.

### Key Differences from Official Blokus

This game must be visually and experientially distinct from the official Blokus board game and its licensed digital versions:

| Aspect | Official Blokus | This Game |
|--------|----------------|-----------|
| Player colors | Blue, yellow, red, green | Indigo, amber, coral, teal |
| Piece appearance | Translucent plastic | Opaque glossy "lacquered" finish |
| Board surface | Continuous white grid with thin lines | Separated cells with visible gaps on gray background |
| Board frame | None (flat grid) | Dark graphite frame |
| Piece corners | Slightly rounded plastic edges | Sharp square corners (no rounding) |
| Name | "Blokus" (Mattel trademark) | My app's name "Corners Apart" |
| Game modes | Physical board game | Digital with AI, multiplayer, profiles |

---

## 2. Architecture — Multiplayer-First Design

The architecture must be designed for multiplayer from the start. The game engine is completely decoupled from the UI and transport layers.

### 2.1 Core Principle

Every move in the game is a message: `Player X places Piece Y at Position Z with Orientation W`. The game engine processes these messages identically regardless of their source (local touch input, AI computation, Bluetooth, Wi-Fi Direct).

### 2.2 High-Level Architecture

```
┌─────────────────────────────────────────────────────┐
│                      UI Layer                        │
│   GameScreen, Board, PiecePanel, Dialogs, etc.      │
│   (Jetpack Compose)                                  │
└─────────────────┬───────────────────────────────────┘
                  │ observes StateFlow
┌─────────────────┴───────────────────────────────────┐
│                   GameViewModel                      │
│   Coordinates UI state, delegates to engine/session  │
└────────┬────────────────────────────┬───────────────┘
         │                            │
┌────────┴────────┐    ┌──────────────┴──────────────┐
│   GameEngine    │    │       GameSession           │
│   Pure logic    │    │   (interface)               │
│   No Android    │    │                              │
│   dependencies  │    │   ┌─ LocalSession           │
│                 │    │   │  (solo + pass-and-play)  │
│   - Board       │    │   ├─ BluetoothSession       │
│   - Placement   │    │   └─ WifiDirectSession      │
│   - Scoring     │    │                              │
│   - Turn logic  │    │   Handles:                   │
│   - Validation  │    │   - Connection lifecycle     │
│                 │    │   - Move transmission        │
│                 │    │   - State synchronization    │
│                 │    │   - Reconnection             │
└────────┬────────┘    └──────────────────────────────┘
         │
┌────────┴────────┐
│    AiEngine     │
│   Runs on       │
│   Dispatchers   │
│   .Default      │
└─────────────────┘
```

### 2.3 Project Structure

```
app/src/main/java/com/[package]/
├── model/
│   ├── Board.kt                — Board state (configurable size: 20×20 or 14×14)
│   ├── Piece.kt                — 21 piece definitions, PieceDef data class
│   ├── PieceTransforms.kt      — Rotate, flip, normalize, getAllOrientations (cached)
│   ├── Player.kt               — Player state (usedPieces, score, passed)
│   ├── GameState.kt            — Full game state, serializable
│   ├── GameMode.kt             — Enum: SOLO, TWO_PLAYER_STANDARD, TWO_PLAYER_DUO, THREE_PLAYER, FOUR_PLAYER
│   ├── Move.kt                 — Data class: playerIndex, pieceIndex, cells, orientation
│   ├── Profile.kt              — Player profile
│   ├── HistoryEntry.kt         — Game history record
│   └── Difficulty.kt           — Difficulty level enum + parameters
│
├── engine/
│   ├── GameEngine.kt           — Pure game logic, no Android dependencies
│   │                             Placement validation, turn management, scoring,
│   │                             game end detection. Configurable board size.
│   ├── PlacementValidator.kt   — isValidPlacement(), corner cache
│   └── Scoring.kt              — Score calculation, rankings, tie-breaking
│
├── ai/
│   ├── AiEngine.kt             — Move generation, evaluation, selection
│   ├── AiPersonality.kt        — 3 personality archetypes
│   ├── AiDifficulty.kt         — 5-level difficulty parameters
│   ├── AiState.kt              — Per-AI mutable state (momentum, frustration)
│   ├── MoveEvaluator.kt        — Multi-criteria move scoring
│   ├── MoveGenerator.kt        — Valid move enumeration with pruning
│   └── Territory.kt            — BFS territory estimation
│
├── multiplayer/
│   ├── GameSession.kt          — Interface for all session types
│   ├── LocalSession.kt         — Solo (with AI) + pass-and-play
│   ├── BluetoothSession.kt     — Bluetooth Classic connection
│   ├── WifiDirectSession.kt    — Wi-Fi Direct (P2P) connection
│   ├── SessionDiscovery.kt     — Find nearby players (BT + WiFi)
│   ├── GameProtocol.kt         — Message format, serialization
│   ├── SyncManager.kt          — State synchronization, conflict resolution
│   └── ReconnectionHandler.kt  — Handle disconnects, rejoin logic
│
├── ui/
│   ├── theme/
│   │   ├── Theme.kt            — Custom game theme (not standard M3)
│   │   ├── Color.kt            — Full color palette
│   │   ├── Type.kt             — Typography (Quicksand)
│   │   └── Shape.kt            — Shape definitions
│   ├── screens/
│   │   ├── MainMenuScreen.kt   — Game mode selection, profiles
│   │   ├── GameScreen.kt       — Main gameplay screen
│   │   ├── LobbyScreen.kt      — Multiplayer lobby (waiting for players)
│   │   └── ConnectionScreen.kt — Bluetooth/WiFi discovery + pairing
│   ├── components/
│   │   ├── GameBoard.kt        — Canvas-based board rendering
│   │   ├── PiecePanel.kt       — Scrollable piece selection grid
│   │   ├── PieceCard.kt        — Individual piece card
│   │   ├── PiecePreview.kt     — Selected piece preview
│   │   ├── PlayerInfoBar.kt    — Player scores & status (2×2 grid)
│   │   ├── ControlBar.kt       — Rotate, flip, pass buttons
│   │   ├── GameTimer.kt        — Elapsed time display
│   │   ├── TurnStatus.kt       — Status message bar
│   │   └── GhostPreview.kt     — Ghost overlay on board
│   ├── dialogs/
│   │   ├── GameOverDialog.kt
│   │   ├── ConfirmDialog.kt
│   │   ├── HelpDialog.kt
│   │   ├── ProfileDialog.kt
│   │   └── HistoryStatsDialog.kt
│   ├── sheets/
│   │   ├── SettingsSheet.kt    — Bottom sheet: difficulty, history, profiles, help
│   │   └── MultiplayerSheet.kt — Bottom sheet: game mode, connection options
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
    ├── GameRepository.kt       — DataStore persistence (save/load game)
    ├── ProfileRepository.kt    — Profile CRUD
    └── Serializers.kt          — kotlinx.serialization
```

### 2.4 GameSession Interface

```kotlin
interface GameSession {
    val sessionType: SessionType  // LOCAL, BLUETOOTH, WIFI_DIRECT
    val gameMode: GameMode
    val players: StateFlow<List<SessionPlayer>>  // who's connected
    val incomingMoves: SharedFlow<Move>          // moves from remote players
    val connectionState: StateFlow<ConnectionState>

    suspend fun sendMove(move: Move): Result<Unit>
    suspend fun sendPass(playerIndex: Int): Result<Unit>
    suspend fun sendGameState(state: GameState): Result<Unit>  // full sync
    suspend fun disconnect()
}

data class SessionPlayer(
    val index: Int,          // 0-3
    val name: String,
    val isLocal: Boolean,    // true = this device controls this player
    val isAi: Boolean,
    val colorIndex: Int
)

enum class ConnectionState {
    DISCONNECTED, DISCOVERING, CONNECTING, CONNECTED, RECONNECTING, FAILED
}
```

### 2.5 Game Protocol

Messages between devices are serialized with kotlinx.serialization as JSON:

```kotlin
@Serializable
sealed class GameMessage {
    @Serializable data class PlaceMove(val move: Move) : GameMessage()
    @Serializable data class Pass(val playerIndex: Int) : GameMessage()
    @Serializable data class FullSync(val state: GameState) : GameMessage()
    @Serializable data class PlayerJoined(val player: SessionPlayer) : GameMessage()
    @Serializable data class PlayerLeft(val playerIndex: Int) : GameMessage()
    @Serializable data class GameConfig(val mode: GameMode, val difficulty: Int) : GameMessage()
    @Serializable object Ping : GameMessage()
    @Serializable object Pong : GameMessage()
}
```

**Host model:** One device is the host (runs GameEngine, validates all moves). Other devices send move requests; host validates and broadcasts accepted moves to all. This prevents cheating and ensures consistency.

**Reconnection:** If a player disconnects mid-game, the game pauses for up to 60 seconds. If they reconnect, host sends a FullSync. If they don't reconnect, their remaining turns are played by AI at difficulty 3, or the game can be saved.

---

## 3. Game Modes

### 3.1 Solo vs AI (20×20)

- 1 human player + 3 AI opponents
- Human always starts at bottom-right corner `[19,19]`
- AI corners shuffled each game
- 5 difficulty levels
- Turn order: clockwise from human

### 3.2 Two-Player Standard (20×20)

- Each player controls 2 colors (2 sets of 21 pieces each)
- Player 1: colors 0 and 2 (opposite corners)
- Player 2: colors 1 and 3 (opposite corners)
- Turn order alternates colors: 0, 1, 2, 3 (so players alternate turns)
- Available via: pass-and-play, Bluetooth, Wi-Fi Direct

### 3.3 Two-Player Duo (14×14)

- Smaller 14×14 board (196 cells)
- 1 color per player, 21 pieces each
- Start positions: NOT corners, but offset 5 cells diagonally toward center
  - Player 1: `[4,4]` (top-left region)
  - Player 2: `[9,9]` (bottom-right region)
- Faster games, better suited for mobile screens
- Available via: pass-and-play, Bluetooth, Wi-Fi Direct

### 3.4 Three-Player (20×20)

- 3 human/AI players, each with own color
- 4th color is "shared" — players take turns controlling it in rotation
- Shared color turn order: Player 1, Player 2, Player 3, repeat
- Shared color's score is ignored in final ranking
- Available via: pass-and-play (3 humans), or networked (any mix)

### 3.5 Four-Player (20×20)

- 4 human players, each with own color, standard rules
- Available via: pass-and-play, Bluetooth, Wi-Fi Direct
- Pure multiplayer — no AI involved

### 3.6 Mixed Modes

In any multiplayer mode, empty slots can be filled with AI at a chosen difficulty level. For example, 2 humans + 2 AI in a four-player game.

---

## 4. Game Logic

Port from HTML version (`blokus-specs.md` sections 4 and 5). All rules identical except where noted.

### 4.1 Board

- Configurable size: `boardSize = 20` (standard) or `boardSize = 14` (duo)
- Cell values: `-1` (empty) or player index `0..3`
- `data class Board(val size: Int, val cells: Array<IntArray>)`

### 4.2 Piece Definitions

21 pieces per player (all free polyominoes of size 1–5). See `blokus-specs.md` section 4 for full definitions. `PIECE_COUNT = 21`, `TOTAL_CELLS = 89`.

```kotlin
data class PieceDef(val name: String, val cells: List<Pair<Int, Int>>)
```

### 4.3 Piece Transformations

- `rotateCW(cells)`: `[r,c] → [c,-r]`
- `rotateCCW(cells)`: `[r,c] → [-c,r]`
- `flipH(cells)`: `[r,c] → [r,-c]`
- `normalize(cells)`: Shift to origin, lexicographic sort
- `getAllOrientations(cells)`: All unique orientations (max 8), cached in HashMap

### 4.4 Placement Rules

1. All cells within board bounds (`0..boardSize-1`)
2. All cells land on empty cells
3. No orthogonal adjacency with same player's existing pieces
4. First piece: must cover player's assigned start position
5. Subsequent pieces: must touch at least one diagonal neighbor of same player

### 4.5 Start Positions

**Standard 20×20 (solo and 3-4 player):**
- Four corners: `[0,0]`, `[0,19]`, `[19,0]`, `[19,19]`
- Solo: human always at `[19,19]`, AI corners shuffled among remaining three
- Multiplayer: assigned based on join order or host configuration

**Duo 14×14:**
- Player 1: `[4,4]`
- Player 2: `[9,9]`

**Enforcement:** Players can ONLY place their first piece at their assigned start position. The UI should clearly indicate start positions and prevent placement elsewhere.

### 4.6 Scoring

- Start: `TOTAL_CELLS = 89` per player
- Each placed cell: score decreases by 1
- Lower score = better (fewer remaining unplaced cells)
- Rankings: sorted ascending by score (lowest = winner)
- Ties broken by player index

### 4.7 Game End

- All active players have passed consecutively (no valid moves remain)
- Player passes when: voluntarily (human, with confirmation), or automatically when no valid moves exist
- After passing: `player.passed = true`, skipped in future turns

### 4.8 Corner Position Cache

- `cornerCache[playerIdx]`: Cached set of valid diagonal positions per player
- Invalidated on every piece placement
- First piece: positions near start corner
- Subsequent: scan all diagonal-adjacent empty cells not edge-adjacent to same color

---

## 5. AI System (the app doesn't use AI (for instance API's) when the user installs and uses the app. Everything in this document which mentions AI means just means that AI has done all the different "AI"- personalities and strategies so it feels like an AI is playing against you.

### 5.1 Architecture

Three AI personality archetypes that can fill any opponent slot. Each personality is modulated by the global difficulty level (1–5).

AI computation runs on `Dispatchers.Default` with artificial delay (300–700ms) for natural feel.

### 5.2 Personalities

#### Expansionist
- Maximizes territory and corners
- High spread bonus (1.4), high corner bonus (1.4)
- Low center focus (0.3)
- On easy difficulty: edge-seeking

#### Aggressive
- Center control, blocks the leader
- Highest center bonus (1.5), highest blocking bonus (1.6)
- Actively destroys opponent corners
- Specifically targets the leading opponent

#### Defensive
- Stays near own territory, protects own corners
- Low spread bonus (0.6) — compact play style
- High edge bonus (1.2) — favors own quadrant
- Low center focus (0.1)

### 5.3 Difficulty Levels (5 Levels)

The current 3-level system from the HTML version has balance issues (level 1 sometimes too hard, level 3 sometimes too easy). The 5-level system must be carefully designed with smooth progression.

**Key design principles for difficulty scaling:**
- Easy levels should feel RELIABLY easy, not randomly easy. Reduce AI quality through consistent strategic mistakes, not just randomness.
- Hard levels should be RELIABLY challenging. Increase search depth and evaluation sophistication, not just reduce randomness.
- Each level should feel noticeably different from adjacent levels.

| Parameter | 1 (Beginner) | 2 (Easy) | 3 (Medium) | 4 (Hard) | 5 (Expert) |
|-----------|-------------|----------|------------|----------|------------|
| `temperature` | 3.0 | 2.0 | 1.0 | 0.5 | 0.2 |
| `skipFilterChance` | 0.7 | 0.4 | 0.1 | 0.0 | 0.0 |
| `localFocusRadius` | 3 | 5 | 20 | 20 | 20 |
| `maxCandidates` | 10 | 25 | 80 | 200 | 500 |
| `pieceSizeBias` | Strong small-piece preference | Slight small-piece preference | None | None | Large-piece preference in opening |
| `blockingAwareness` | Ignores opponents | Occasional blocking | Reactive blocking | Proactive blocking | Predictive blocking |
| `territoryEval` | None | None | Basic BFS | Full BFS | BFS + lookahead |
| `openingStrategy` | Random first 3 moves | Weak center push | Center-oriented | Optimized opening | Memorized openings |
| `cornerPreservation` | Ignores own corner count | Slight preference | Balanced | Strong preference | Optimized |

**Level 1 (Beginner):** AI makes obvious mistakes — plays small pieces early, doesn't block, stays near edges, doesn't plan ahead. A new player should win easily.

**Level 2 (Easy):** AI plays somewhat logically but misses opportunities. Occasionally plays small pieces when large ones are available. Weak at blocking.

**Level 3 (Medium):** AI plays competently. Uses large pieces first, blocks when obvious, pushes toward center. A decent player can win but needs to think.

**Level 4 (Hard):** AI plays well. Proactively blocks the leading player, manages territory, preserves corners. Only experienced players win consistently.

**Level 5 (Expert):** AI plays near-optimally. Evaluates many candidates, uses territory estimation, predicts opponent moves, has optimized opening sequences. Very difficult to beat.

**Important implementation note:** The difficulty parameters above are starting points. They MUST be play-tested and tuned iteratively. The key metric is: "Does each level feel appropriately challenging for its target audience?" Claude Code should implement the parameter framework first, then the values can be adjusted through testing.

### 5.4 Decision Pipeline

1. **Context gathering:** Calculate opponent corners, identify leader (for aggressive), estimate territory
2. **Frustration system:** (easy levels only) If blocked, quality degrades for several turns
3. **Move generation:** Iterate available pieces × orientations × corner positions
   - Local focus on easy levels (only near last placement)
   - Candidate limit per difficulty
   - Fall back to exhaustive search if limited set is empty
4. **Move evaluation:** Multi-criteria scoring:
   - Piece size × weight
   - New corners created (quality weighted by distance from center)
   - Own corners destroyed (penalty)
   - Opponent corners destroyed (blocking bonus, amplified for aggressive vs leader)
   - Center distance
   - Edge/quadrant affinity (easy levels)
   - Territory estimation (medium+ levels)
   - Spread/compactness preference
   - Decision momentum (alignment with previous direction)
5. **Anti-pattern filter:**
   - Don't play small pieces when large ones are available (except endgame)
   - Reject zero-corner moves when alternatives exist
   - Skip filter with probability `skipFilterChance`
6. **Softmax selection:** Probabilistic choice from candidates within score window, modulated by `temperature`
7. **Fallback:** If selected move fails validation, fall back to `findFirstValidMove()`

### 5.5 Territory Estimation

BFS flood fill from corner positions through empty cells reachable without edge-adjacency violations. Returns count of reachable cells. Used in medium+ difficulty levels.

### 5.6 AI in Multiplayer

- In solo mode: 3 AI opponents, difficulty selected by player
- In multiplayer: empty slots filled with AI at host-selected difficulty
- AI plays only on the host device (moves are broadcast like any other)

---

## 6. Visual Design

### 6.1 Style Direction

**Modern, clean, playful.** Light gray background, white board surface, vivid opaque glossy pieces. NOT wood/beige/skeuomorphic. The game should look like a polished modern app, not a physical board game imitation.

The visual identity is built around:
- Sharp geometry (no rounded corners on pieces)
- High-contrast player colors against white/gray
- Glossy opaque piece effect ("lacquered candy" look)
- Clean typography (Quicksand)

### 6.2 Color Palette

#### Player Colors

| Index | Name | Base | Dark/Border | Highlight | Ghost (30% alpha) |
|-------|------|------|-------------|-----------|-------------------|
| 0 | Indigo | `#4338CA` | `#312E81` | `#6366F1` | `#4338CA4D` |
| 1 | Amber | `#E88C0A` | `#A16207` | `#F5B040` | `#E88C0A4D` |
| 2 | Coral | `#E8513D` | `#991B1B` | `#F08070` | `#E8513D4D` |
| 3 | Teal | `#0D9488` | `#134E4A` | `#2DD4BF` | `#0D94884D` |

These are deliberately different from official Blokus colors (blue/yellow/red/green).

#### Surface Colors

| Role | Color | Usage |
|------|-------|-------|
| App background | `#E4E4E8` | Gray, main background |
| Board cell gap | `#DCDCE0` | Visible between separated cells |
| Board cell surface | `#FAFAFA` | Individual cell fill (near-white) |
| Board frame | `#2C2C30` | Dark graphite frame around board |
| Card surface | `#FFFFFF` | Bottom sheet, dialogs, cards |
| Card elevated | `#FFFFFF` | Modals with elevation shadow |

#### Text Colors

| Role | Color |
|------|-------|
| Primary | `#1A1A1E` |
| Secondary | `#4A4A52` |
| Muted | `#8A8A92` |
| On player color | `#FFFFFF` |

#### UI Semantic Colors

| Role | Color |
|------|-------|
| Accent/CTA | `#4338CA` (indigo) |
| Danger/Pass | `#E8513D` (coral) |
| Success | `#0D9488` (teal) |
| Warning | `#E88C0A` (amber) |

### 6.3 Typography

**Primary font:** Quicksand (bundled TTF) — rounded, friendly game font.

| Role | Size | Weight |
|------|------|--------|
| Logo/Title | 28sp | Bold (700) |
| Heading | 20sp | SemiBold (600) |
| Body | 16sp | Medium (500) |
| Label | 14sp | Medium (500) |
| Caption | 12sp | Regular (400) |
| Tiny | 10sp | Regular (400) |

### 6.4 Board Rendering

The board is drawn using Compose `Canvas` for performance.

#### Board Structure — Separated Cells

The board is NOT a continuous grid with lines. Instead, each cell is a separate square with visible gaps between them. The gap color is the board background (`#DCDCE0`), making cells appear as individual tiles.

- **Cell size:** dynamically calculated: `(screenWidth - framePadding) / boardSize` minus gap
- **Gap width:** 2dp between cells
- **Cell fill:** `#FAFAFA` (empty) or player color (occupied)
- **Cell corners:** Sharp (no rounding, `rx = 0`)

#### Board Frame

- Dark graphite (`#2C2C30`) frame around the entire board
- Frame width: 4dp
- No rounded corners on frame

#### Start Position Indicators

- Small colored markers at assigned start positions
- Player's own start: filled square (4dp) in player color at 50% opacity
- Other players' starts: same but 25% opacity
- Only visible on empty board cells (hidden once a piece covers them)

#### Ghost Preview

- Valid placement: Player color at 30% alpha
- Invalid placement: Red diagonal stripe pattern with red border
- Follows touch position, piece centered on finger

#### Piece Rendering — Opaque Glossy Effect

Each occupied cell is drawn with sharp corners (`rx = 0`) and a multi-layer glossy effect:

1. **Base fill:** Player color (opaque, fully saturated)
2. **Top highlight:** White at 35% alpha, top 25% of cell height — sharp-edged, no gradient
3. **Bottom shadow:** Player dark color at 50% alpha, bottom 15% of cell height
4. **Inner inset:** Slightly darker fill (black at 8% alpha), inset 2dp from edges
5. **Drop shadow:** 1dp offset, 2dp blur, black at 12% alpha

This creates a "lacquered" or "candy" appearance — opaque and glossy, distinctly different from Blokus's translucent plastic.

#### Placement Animation

- New piece: scale 0.8 → 1.08 → 1.0 over 400ms (overshoot spring)
- Invalid attempt: board shakes horizontally ±4dp over 400ms

### 6.5 Layout — Portrait (Primary)

```
┌──────────────────────────────┐
│  Logo            Timer  ⋮   │  ← Top bar: logo left, timer right, menu icon
├──────────────────────────────┤
│ 🟣 Pelaaja  72  🟠 Amber 75 │  ← Player bar: 2×2 grid
│ 🔴 Coral    74  🟢 Teal  76 │    Active player: highlighted bg + glow dot
├──────────────────────────────┤
│  ┌────────────────────────┐  │
│  │ Dark graphite frame    │  │  ← Board: square, fills width
│  │ ┌──────────────────┐   │  │    20×20 separated cells
│  │ │   GAME BOARD      │  │  │
│  │ │   (Canvas)        │  │  │
│  │ └──────────────────┘   │  │
│  └────────────────────────┘  │
├──────────────────────────────┤
│  Sinun vuorosi!              │  ← Status bar with player-color left border
├──────────────────────────────┤
│  ↺ ↻ ⇆  │  Ohita  │        │  ← Control bar: rotate, flip, pass
├──────────────────────────────┤
│  [Valittu: ▪▪ O4]           │  ← Selected piece preview (when piece chosen)
│  ▪ ▪▪ ▪▪▪ ▪▪▪▪ ▪▪▪▪▪ ...  │  ← Piece grid: scrollable, 5-7 columns
│  ▪▪▪ ▪▪▪▪ ▪▪▪▪▪ ▪▪▪▪▪     │
└──────────────────────────────┘
```

#### Top Bar
- Logo left (game name, Quicksand Bold 22sp)
- Timer center-right: clock icon + `m:ss` format
- Three-dot menu icon far right → opens bottom sheet

#### Player Info Bar
- 2×2 grid in portrait
- Each player: colored dot (12dp, glossy), name, score
- Active player: background tint in player color (12% alpha), dot has animated glow
- Passed players: 40% opacity, strikethrough on name

#### Control Buttons
- Rotate CCW / CW: grouped in shared container
- Flip: separate button
- Pass: coral/danger colored
- All buttons: pill-shaped, 48dp height, subtle elevation
- Disabled state: 30% opacity

#### Piece Panel
- Scrollable grid (LazyVerticalGrid), 5-7 columns based on screen width
- Each card: ~56dp square, piece rendered as mini-grid
- Selected: scale 1.08, colored border glow, elevated
- Used: 15% opacity, non-interactive
- When selected: floating preview card appears above piece grid showing enlarged piece

#### Menu (Three-dot) → Bottom Sheet

Bottom sheet contains:
- **Difficulty:** 5 toggle buttons in a row (1–5), active one filled
- **Game mode / Multiplayer:** Start new multiplayer game, find players
- **Profiles:** Open profile management
- **History/Stats** Open history/stats dialog
- **Help** Open help dialog
- **Save current game**
- **Start new game** (with confirmation if game in progress)

### 6.6 Layout — Landscape / Tablet

```
┌─────────────────────────────────────────────────────┐
│ ┌─left panel──┐ ┌────board─────┐ ┌─right panel───┐ │
│ │ Logo/Timer  │ │              │ │ Controls      │ │
│ │             │ │              │ │ ↺ ↻ ⇆        │ │
│ │ 🟣 Plr  72  │ │  GAME BOARD  │ │ Ohita         │ │
│ │ 🟠 Amb  75  │ │              │ │               │ │
│ │ 🔴 Cor  74  │ │              │ │ ┌─preview──┐  │ │
│ │ 🟢 Tea  76  │ │              │ │ │  PIECE   │  │ │
│ │             │ │              │ │ └──────────┘  │ │
│ │ Status msg  │ └──────────────┘ │ Pieces grid   │ │
│ └─────────────┘                  └───────────────┘ │
└─────────────────────────────────────────────────────┘
```

### 6.7 Animations

| Animation | Duration | Easing |
|-----------|----------|--------|
| Piece placement | 400ms | FastOutSlowIn (overshoot) |
| Invalid shake | 400ms | LinearOutSlowIn |
| Active player pulse | 1500ms | InfiniteRepeat, EaseInOut |
| AI thinking dot | 900ms | InfiniteRepeat, EaseInOut |
| Dialog enter | 300ms | FastOutSlowIn, scale 0.9→1.0 |
| Piece card intro | 300ms + 20ms stagger | FastOutSlowIn |
| Ghost appear | 150ms | FastOutSlowIn |
| Score change | 200ms | Brief color flash |

Respect `Settings.Global.ANIMATOR_DURATION_SCALE` — disable all animations if the user has turned them off.

### 6.8 Haptic Feedback

- Valid placement: `HapticFeedbackType.Confirm` (Android 13+) or `LongPress`
- Invalid placement: `HapticFeedbackType.Reject` or buzz-pause-buzz pattern
- Piece selection: light tick
- Button press: standard click haptic

### 6.9 Sound

- Piece placement: short warm "tok" sound via SoundPool
- Only for local human player actions
- Sound can be toggled on/off in settings

---

## 7. Multiplayer Details

### 7.1 Connection Flow

#### Bluetooth
1. Host taps "Create game" → becomes discoverable
2. Guest taps "Find games" → scans for nearby devices
3. Guest selects host → Bluetooth pairing (if not already paired)
4. Connection established → host sends GameConfig message
5. Guest confirms → game starts

#### Wi-Fi Direct
1. Host taps "Create a game" → creates Wi-Fi Direct group
2. Guest taps "Find games" → discovers Wi-Fi Direct peers
3. Guest selects host → Wi-Fi Direct connection
4. TCP socket established on known port
5. Same message protocol as Bluetooth

#### Pass-and-Play
1. Select game mode and number of players
2. Assign colors
3. Play on single device, turn indicator shows whose turn it is
4. Board rotates/flips orientation on turn change (optional setting)

### 7.2 Lobby

Before the game starts, a lobby screen shows:
- Game mode and board size
- Connected players with their colors and names
- Empty slots (can be filled with AI or left open for more players)
- Difficulty selector for AI slots
- "Start button — only host can start

### 7.3 Edge Cases

| Scenario | Behavior |
|----------|----------|
| Player disconnects | Pause 60s, show reconnection timer. If reconnects: full sync. If not: AI takes over at difficulty 3. |
| Host disconnects | Game ends. Players can save local state. |
| Move conflict | Host is authoritative — validates all moves. Invalid moves are rejected with error message. |
| App goes to background | Auto-save game state. If multiplayer: maintain connection for 5 minutes, then disconnect. |
| Phone call during game | Pause indicator shown to other players. Resume on return. |
| Battery saver mode | Reduce animation frame rate, disable haptics. |

### 7.4 Permissions Required

- `BLUETOOTH`, `BLUETOOTH_ADMIN`, `BLUETOOTH_CONNECT`, `BLUETOOTH_SCAN`, `BLUETOOTH_ADVERTISE` (for Bluetooth)
- `ACCESS_FINE_LOCATION` (required for Bluetooth/WiFi Direct discovery on Android)
- `ACCESS_WIFI_STATE`, `CHANGE_WIFI_STATE`, `ACCESS_NETWORK_STATE` (for Wi-Fi Direct)
- `NEARBY_WIFI_DEVICES` (Android 13+)

---

## 8. Profiles, History & Statistics

### 8.1 Player Profiles

```kotlin
@Serializable
data class Profile(
    val id: Long,                    // timestamp-based unique ID
    val name: String,                // max 30 characters
    val colorIndex: Int,             // 0-3, preferred player color
    val avatarStyle: String,         // DiceBear style name
    val avatarSeed: String,          // random seed for avatar
    val avatarData: String? = null,  // optional custom avatar (base64, max 160KB)
    val active: Boolean,             // one profile active at a time
    val history: List<HistoryEntry>  // max 50 entries
)
```

- One active profile at a time
- Profile's `colorIndex` determines which player color the human plays as
- In solo mode: human gets their preferred color, AI gets the other three
- Avatar: DiceBear API styles (bottts, avataaars, pixel-art, fun-emoji, lorelei, thumbs) or custom image from camera/gallery
- Custom avatar: compressed to max 160×160px, WebP format

### 8.2 History Entry

```kotlin
@Serializable
data class HistoryEntry(
    val date: String,          // ISO datetime
    val rank: Int,             // 1-4
    val score: Int,            // 0-89
    val difficulty: Int,       // 1-5
    val gameMode: GameMode,
    val time: Int,             // seconds
    val scores: List<PlayerScore>  // all players
)

@Serializable
data class PlayerScore(
    val name: String,
    val score: Int,
    val colorIndex: Int
)
```

### 8.3 History & Stats Dialog

Accessed from bottom sheet. Two tabs:

**History tab:** Chronological list of past games. Each entry shows: date, rank (with medal for 1st), score, difficulty stars, game duration, all player scores.

**Stats tab:** Aggregate statistics:
- Total games played
- Win count and win rate (%)
- Average score
- Best score
- Average rank
- Favorite difficulty level
- Score trend (last 20 games, mini bar chart)
- Stats per difficulty level

### 8.4 Persistence

Use DataStore (Preferences) for simple key-value storage:
- `saved-game` — serialized GameState JSON
- `profiles` — serialized List<Profile> JSON
- `settings` — difficulty preference, sound on/off, haptics on/off

---

## 9. Dialogs & Sheets

### 9.1 Bottom Sheet (main menu)

Opened via three-dot menu icon in top bar. Contains:
- Difficulty selector (1-5)
- Multiplayer options
- Profiles
- History and stats
- Help
- Save game
- New game

Style: white surface, top-left rounded corners, drag handle at top.

### 9.2 Game Over Dialog
- Rankings 1st–4th with player colors, names, scores
- Winner: gold accent + trophy icon
- Game duration
- "Play again" button
- "Stats" link

### 9.3 Confirm Dialog
- Generic: dynamic title + message
- Confirm/Cancel buttons
- Used for: pass, new game, save & quit

### 9.4 Resume Game Dialog
- Shown on app launch if saved game exists
- Shows: save date, scores, difficulty, game mode
- "Continue" / "New game"

### 9.5 Help Dialog
- Game rules overview
- Touch gesture explanations
- Difficulty level descriptions
- Multiplayer instructions
- Scrollable

### 9.6 Profile Dialog
- Profile list with avatars, names, stats
- Add / edit / delete
- Color selection
- Avatar selection (DiceBear styles or custom image)

---

## 10. Accessibility

- All interactive elements: minimum 48dp touch targets
- Content descriptions on all buttons and icons
- Player colors supplemented with player name/position (not color-only identification)
- Turn changes announced via `LiveRegionMode.Polite`
- Board cells have content descriptions with position info for TalkBack
- Respect system font scale (test up to 1.3x)
- Respect reduced animations setting
- High contrast mode: ensure all text passes WCAG AA

---

## 11. Constants

```kotlin
object GameConstants {
    const val STANDARD_BOARD_SIZE = 20
    const val DUO_BOARD_SIZE = 14
    const val PLAYER_COUNT = 4
    const val PIECE_COUNT = 21
    const val TOTAL_CELLS = 89
    const val MAX_HISTORY_ENTRIES = 50
    const val DIFFICULTY_LEVELS = 5

    const val BOARD_INTERACTION_LOCK_MS = 160L
    const val INVALID_FEEDBACK_COOLDOWN_MS = 180L
    const val AI_TURN_DELAY_MIN_MS = 300L
    const val AI_TURN_DELAY_RANGE_MS = 400L
    const val TURN_ADVANCE_DELAY_MS = 400L
    const val HUMAN_AUTO_PASS_DELAY_MS = 1500L
    const val SAVE_NOTIFICATION_DURATION_MS = 2000L
    const val RECONNECT_TIMEOUT_MS = 60_000L
    const val BACKGROUND_TIMEOUT_MS = 300_000L  // 5 minutes

    const val MAX_AVATAR_DIMENSION = 160
    const val MAX_AVATAR_FILE_SIZE = 5 * 1024 * 1024

    val AVATAR_STYLES = listOf("bottts", "avataaars", "pixel-art", "fun-emoji", "lorelei", "thumbs")

    val STANDARD_CORNERS = listOf(0 to 0, 0 to 19, 19 to 0, 19 to 19)
    val DUO_START_POSITIONS = listOf(4 to 4, 9 to 9)

    val PLAYER_NAMES = listOf("Player", "Amber", "Coral", "Teal")
    val PLAYER_COLORS = listOf("Indigo", "Amber", "Coral", "Teal")
}
```

---

## 12. Dependencies

```kotlin
dependencies {
    // Core Android
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.activity:activity-compose:1.9.3")

    // Compose
    implementation(platform("androidx.compose:compose-bom:2025.01.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")

    // ViewModel
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")

    // DataStore
    implementation("androidx.datastore:datastore-preferences:1.1.2")

    // Serialization
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")

    // Coil (avatar loading from DiceBear)
    implementation("io.coil-kt:coil-compose:2.7.0")
    implementation("io.coil-kt:coil-svg:2.7.0")

    // Nearby Connections (alternative to raw BT/WiFi — evaluate)
    // implementation("com.google.android.gms:play-services-nearby:19.3.0")
}
```

> **Note on Nearby Connections API:** Google's Nearby Connections API abstracts away Bluetooth/Wi-Fi Direct complexity into a single API. It handles discovery, connection, and data transfer. This may be significantly easier to implement than raw Bluetooth + Wi-Fi Direct separately. Evaluate whether it meets the requirements before committing to raw implementation.

> **Note on dependency versions:** Check for latest versions before implementing — these may have been updated since this spec was written.

---

## 13. Implementation Phases

All phases must maintain multiplayer-first architecture. No "add multiplayer later" refactoring.

### Phase 1 — Foundation
1. Project setup: theme, colors, typography, custom Compose theme
2. `GameEngine` with configurable board size + all placement rules
3. `PieceTransforms` with caching
4. Board rendering (Canvas) with separated cells, dark frame, piece glossy effect
5. `GameSession` interface + `LocalSession` implementation
6. Basic GameViewModel connecting engine → UI

### Phase 2 — Playable Solo Game
7. Piece panel, selection, preview
8. Touch interaction: ghost preview, tap to place
9. Turn management: human + AI turns
10. AI engine with 5 difficulty levels (initial tuning)
11. Game end detection, scoring, game over dialog
12. Control bar: rotate, flip, pass
13. Timer
14. Status messages

### Phase 3 — Multiplayer Infrastructure
15. `GameProtocol` message format
16. `BluetoothSession` — discovery, pairing, connection, messaging
17. `WifiDirectSession` — discovery, connection, messaging
18. Lobby screen: create/join game, player slots, AI fill
19. `SyncManager` — state sync, conflict resolution
20. `ReconnectionHandler` — disconnect/reconnect flow
21. Pass-and-play mode in `LocalSession`

### Phase 4 — All Game Modes
22. Two-player standard (20×20, 2 colors each)
23. Two-player duo (14×14)
24. Three-player with shared color
25. Four-player
26. Mixed human + AI in any mode

### Phase 5 — Profiles & Persistence
27. Profile system: create, edit, delete, avatar
28. Game save/load
29. History recording
30. Statistics calculation and display
31. History & stats dialog with two tabs

### Phase 6 — Polish
32. Animation tuning (placement, shake, pulse, intro)
33. Sound effects
34. Haptic feedback
35. AI difficulty tuning (play-test all 5 levels)
36. Bottom sheet with all settings
37. Help dialog
38. Accessibility audit
39. Edge-to-edge display, system bar insets
40. Landscape layout
41. Performance optimization (Canvas redraw efficiency)

### Phase 7 — Release Prep
42. App icon and splash screen
43. Play Store listing assets
44. Final AI balance testing
45. Multiplayer stress testing (disconnect, reconnect, edge cases)
46. Battery and performance profiling
47. Choose final app name
