# Corners Apart UI Reference

Last verified from the live checkout on 2026-06-20.

This document describes the current UI implementation for `Corners Apart`, the data it consumes, the visual system it uses, and the constraints future UI work must preserve. The source code remains the final source of truth after any future changes.

## Current UI Scope

Corners Apart is a single-surface Android game UI built with Jetpack Compose and Material 3. There is no active navigation graph yet. The app enters through `MainActivity`, applies `CornersApartTheme`, and renders `GameRoute`.

Current UI surface:

- `MainActivity` creates the Compose root, enables edge-to-edge drawing, wraps content in `CornersApartTheme`, and shows `GameRoute`.
- `GameRoute` owns ViewModel collection, one-shot effects, sound, haptics, accessibility announcements, and Nearby runtime permission requests.
- `GameScreenContent` owns the playable screen structure and local dialog visibility for Settings, Profiles, Help, History & stats, Resume, and Game over.
- `GameBoard` owns the board canvas, board tap mapping, bonus tile markers, start markers, and occupied-cell rendering.
- `PlayerScoreBar` owns player score cards and current-player/pass visual state.
- `GamePolishDialogs` owns resume, settings, profiles, help, and game-over dialogs.
- `HistoryStatsDialog` owns the history/statistics tabbed dialog.
- `PieceShape` owns reusable piece drawing for previews and piece cards.

No Navigation Compose dependency is currently declared, and current UI does not define `NavHost` routes. Treat this as a single-screen app until a navigation graph is explicitly added.

## External UI Guidance Checked

The implementation aligns with the current Android Compose guidance that state should be explicit, declarative UI should be driven by observable state, and unidirectional data flow should pass state down and events up. Material 3 is the current Compose design system used by the app. Accessibility work should continue to use Compose semantics and built-in accessible Material components.

References checked on 2026-06-20:

- Android Developers: State and Jetpack Compose (`https://developer.android.com/develop/ui/compose/state`)
- Android Developers: Compose UI Architecture (`https://developer.android.com/develop/ui/compose/architecture`)
- Android Developers: Material Design 3 in Compose (`https://developer.android.com/develop/ui/compose/designsystems/material3`)
- Android Developers: Accessibility in Jetpack Compose (`https://developer.android.com/develop/ui/compose/accessibility`)

## Source Files

Main UI files:

- `app/src/main/java/com/finnvek/cornersapart/MainActivity.kt`
- `app/src/main/java/com/finnvek/cornersapart/ui/screens/GameScreen.kt`
- `app/src/main/java/com/finnvek/cornersapart/ui/screens/GameBoard.kt`
- `app/src/main/java/com/finnvek/cornersapart/ui/screens/PlayerScoreBar.kt`
- `app/src/main/java/com/finnvek/cornersapart/ui/screens/GamePolishDialogs.kt`
- `app/src/main/java/com/finnvek/cornersapart/ui/screens/HistoryStatsDialog.kt`
- `app/src/main/java/com/finnvek/cornersapart/ui/screens/GamePolishPolicies.kt`
- `app/src/main/java/com/finnvek/cornersapart/ui/screens/GameSoundPlayer.kt`
- `app/src/main/java/com/finnvek/cornersapart/ui/components/PieceShape.kt`

Theme and resources:

- `app/src/main/java/com/finnvek/cornersapart/ui/theme/Tokens.kt`
- `app/src/main/java/com/finnvek/cornersapart/ui/theme/Theme.kt`
- `app/src/main/java/com/finnvek/cornersapart/ui/theme/PlayerPalette.kt`
- `app/src/main/java/com/finnvek/cornersapart/ui/theme/Type.kt`
- `app/src/main/java/com/finnvek/cornersapart/ui/theme/Shapes.kt`
- `app/src/main/res/values/strings.xml`
- `app/src/main/res/font/quicksand.ttf`

UI state and effects:

- `app/src/main/java/com/finnvek/cornersapart/viewmodel/GameViewModel.kt`
- `app/src/main/java/com/finnvek/cornersapart/viewmodel/GameUiState.kt`
- `app/src/main/java/com/finnvek/cornersapart/viewmodel/GameEffect.kt`
- `app/src/main/java/com/finnvek/cornersapart/multiplayer/NearbyUiState.kt`

UI tests:

- `app/src/androidTest/java/com/finnvek/cornersapart/ui/screens/GameScreenTest.kt`
- `app/src/test/java/com/finnvek/cornersapart/ui/screens/GamePolishPolicyTest.kt`
- `app/src/test/java/com/finnvek/cornersapart/ui/theme/CornersApartTokensTest.kt`

## UI Architecture

The UI follows unidirectional data flow:

- `GameViewModel` exposes immutable `StateFlow<GameUiState>`.
- `GameRoute` collects `uiState` with `collectAsStateWithLifecycle()`.
- `GameRoute` collects one-shot `SharedFlow<GameEffect>` events in `LaunchedEffect`.
- `GameScreenContent` receives state plus action objects and renders from those inputs.
- UI events call ViewModel methods through small action containers.
- Rules, persistence, settings, profiles, Nearby state, scoring, and game history remain outside composables.

Action containers in `GameScreen.kt`:

- `GameScreenActions`: mode selection, Nearby create/find, history/stats, saved-game resume/discard.
- `GamePieceActions`: select piece, rotate counterclockwise, rotate clockwise, flip, pass, place at board cell.
- `GameSettingsActions`: sound, haptics, reduced motion, preferred difficulty, preferred mode.
- `GameProfileActions`: set active profile, add profile, update profile.
- `GameDialogState`: accessibility announcement and History & stats dialog state.

Composables must remain presentation/input mapping only. They must not duplicate placement validation, scoring, ranking, bonus tile rules, turn advancement, DataStore access, or Nearby transport behavior.

## Entry Flow

1. Android launches `MainActivity`.
2. `MainActivity.onCreate` calls `enableEdgeToEdge()`.
3. `setContent` applies `CornersApartTheme`.
4. Root `Surface(fillMaxSize())` hosts `GameRoute`.
5. `GameRoute` obtains `GameViewModel` through Hilt.
6. `GameRoute` collects `GameUiState`.
7. `GameRoute` renders `GameScreenContent`.
8. `GameScreenContent` selects compact or expanded layout from the current width.

## Layout System

The playable screen uses `BoxWithConstraints` and `GameLayoutPolicy`.

Breakpoints:

- Compact: width below `840.dp`.
- Expanded: width at least `840.dp`.

Global page behavior:

- The root fills the screen.
- The root background is `CornersApartColors.AppBackground`.
- `safeDrawingPadding()` is applied for edge-to-edge safety.
- A vertical scroll state wraps the screen content.
- Screen padding is `CornersApartSpacing.ScreenPadding` (`16.dp`).

Compact layout order:

1. Header and top actions.
2. Game board.
3. Accessibility announcement node, when present.
4. Status line.
5. Control bar.
6. Selected piece preview.
7. Piece panel.

Expanded layout:

- A two-column `Row` with `SectionGap`.
- Left column weight `1f`: header/actions, accessibility node, status, controls, selected piece preview.
- Right column weight `1.2f`: board and piece panel.

Do not add a landing page. The first screen must remain the playable game surface.

## Header And Top Actions

The header displays:

- App name: `Corners Apart`.
- Mode chips, horizontally scrollable:
  - `Four players`
  - `Solo`
  - `Two-color duel`
  - `Compact duel`
  - `Three players`

Mode chips are Material 3 `FilterChip`s. Selecting a mode calls `GameViewModel.startGame(mode)`, which starts a local session, stores the preferred mode, clears saved game data, resets selection, and refreshes UI state.

Nearby action panel:

- Surface title: `Nearby game`.
- Buttons:
  - `Create nearby game`
  - `Find nearby game`

Nearby runtime permission behavior:

- `GameRoute` checks `NearbyPermissions.requiredRuntimePermissions()`.
- If all permissions are granted, it runs the selected Nearby action immediately.
- If permissions are missing, it launches `RequestMultiplePermissions`.
- After all requested permissions are granted, the pending host/discovery action resumes.

Current Nearby visibility limitation:

- `GameUiState.nearbyState` includes connection state, discovered endpoints, pending connection, and error message.
- Current visible UI only renders create/find buttons.
- The current screen does not yet render discovered endpoint rows, pending authentication token confirmation, connection status text, or Nearby errors.

Utility action row:

- Horizontally scrollable Material 3 buttons.
- Buttons and icons:
  - `History & stats` with History icon.
  - `Profiles` with Person icon.
  - `Settings` with Settings icon.
  - `Help` with Help icon.
- Minimum height for these buttons is `TouchTargetMin` (`48.dp`).

## Board

`GameBoard` is the central play target.

Structure:

- Outer `Box`.
- `fillMaxWidth()`.
- `aspectRatio(1f)`.
- Background `CornersApartColors.BoardFrame`.
- Padding `CornersApartSpacing.BoardFrameWidth` (`4.dp`).
- Inner `Canvas` fills the available square.

Board sizing:

- Board size comes from `state.board.size`.
- Standard game modes use a 20 x 20 board.
- Compact Duel uses a 14 x 14 board.
- Each cell is computed from the canvas minimum dimension minus gaps.
- Cell gap is `CornersApartSpacing.BoardCellGap` (`2.dp`).

Draw order:

1. Empty cells.
2. Unclaimed bonus tiles.
3. Start markers on empty start-corner cells.
4. Occupied cells.

Empty cells:

- Drawn as rectangles.
- Color: `CornersApartColors.BoardCellSurface`.

Bonus tiles:

- Only unclaimed bonus tiles are drawn.
- Shape: diamond path centered in the cell.
- Color: `CornersApartColors.BonusAccent`.
- Radius fraction: `0.24f` of cell size.

Start markers:

- Drawn only while the start cell is empty.
- Shape: circle.
- Color: owning player's base color with `CornersApartAlpha.StartMarker` (`0.55f`).
- Radius fraction: `0.18f` of cell size.

Occupied cells:

- Drawn with `drawGlossyCell`.
- Player color is resolved through `CornersApartPlayerPalette.colorsFor(playerIndex)`.
- The board cell value is the player index.

Tap behavior:

- `detectTapGestures` converts pointer offset to row/column.
- Row and column are clamped to the board bounds.
- The tap calls `onPlaceCell(row, col)`.
- `GameViewModel.placeSelectedAt(row, col)` builds a `Move` with current player, selected piece id, anchor row/col, and selected orientation.

Accessibility:

- Board canvas semantics content description is `Game board`.

## Piece Rendering

`PieceShape` renders piece previews and piece cards.

Inputs:

- `cells: List<CellOffset>`
- `colorIndex: Int`
- optional `alpha`

Behavior:

- Normalizes cells through `PieceTransforms.normalize`.
- Resolves player color through `CornersApartPlayerPalette`.
- Draws all cells centered inside the available canvas.
- Cell size is the minimum of available width per column and height per row.

`drawGlossyCell` visual layers:

1. Black drop shadow with `CornersApartAlpha.PieceDropShadow` (`0.12f`).
2. Base player color.
3. Top highlight with `CornersApartAlpha.PieceHighlight` (`0.35f`).
4. Bottom dark band with `CornersApartAlpha.PieceShadow` (`0.50f`).
5. Inner inset overlay with `CornersApartAlpha.PieceInnerInset` (`0.08f`).

Glossy-cell internal fractions:

- Shadow offset: `0.05f` of cell size.
- Inset: `0.08f` of cell size.
- Highlight height: `0.25f` of cell size.
- Bottom shadow starts at `0.85f` of cell size.
- Bottom shadow height: `0.15f` of cell size.

## Score Bar

`PlayerScoreBar` displays players in rows of two cards.

Player card behavior:

- Minimum height: `ScoreCardMinHeight` (`48.dp`).
- Shape: `MaterialTheme.shapes.small`.
- Current player card:
  - Surface color is the player's ghost color.
  - Border uses the player's base color.
  - Border width is `ActivePlayerBorderWidth` (`2.dp`).
- Passed player card:
  - Alpha is `CornersApartAlpha.PassedPlayer` (`0.40f`).
  - Shows `passed`.
- Score label format: player name plus total score.

The score bar does not show detailed score categories; those are in the game-over dialog.

## Status And Controls

Status line:

- Material 3 `Surface`.
- Shows current turn as `%playerName turn`.
- Shows `Game over` when `state.isGameOver` is true.

Control bar:

- Horizontal row.
- Controls:
  - Rotate counterclockwise.
  - Rotate clockwise.
  - Flip selected piece.
  - Pass turn.
- Rotate/flip controls are `IconButton`s with semantic content descriptions.
- Pass is a weighted `Button` with icon and text.
- Minimum touch target is `48.dp`.

Input haptics:

- Piece select: `TextHandleMove`.
- Rotate counterclockwise: `TextHandleMove`.
- Rotate clockwise: `TextHandleMove`.
- Flip: `TextHandleMove`.
- Pass: `LongPress`.
- Haptics only run when `state.hapticsEnabled` is true.

## Selected Piece Preview

The selected piece preview is a full-width `Surface`.

Content:

- Label: `Selected piece`.
- Square preview size: `PiecePreviewSize` (`84.dp`).
- Preview border color: current player's base color.
- Preview border width: `ActivePlayerBorderWidth` (`2.dp`).
- Preview shape: `MaterialTheme.shapes.small`.
- Preview uses `state.selectedCells`, which already reflects selected orientation.

## Piece Panel

The piece panel shows all pieces available to the current player.

Structure:

- Title: `Pieces`.
- Horizontally scrollable row.
- One fixed-size card per piece.
- Piece card size: `PieceCardSize` (`64.dp`).

Piece card behavior:

- Used pieces:
  - Alpha is `CornersApartAlpha.UsedPiece` (`0.35f`).
  - Click is disabled.
  - Content description says `Piece {name}, used`.
- Available pieces:
  - Click calls `onSelectPiece(piece.id)`.
  - Content description says `Piece {name}`.
- Selected piece:
  - Border uses current player's base color.
  - Border width is `ActivePlayerBorderWidth`.

Current panel order follows `PieceCatalog.all`.

## Dialogs

All current secondary flows are Material 3 `AlertDialog`s.

### Resume Game Dialog

Shown when:

- `state.hasSavedGame` is true.
- `state.resumeSummary` is not null.
- The user has not yet chosen continue/new game for the stored save.

Content:

- Title: `Continue saved game?`
- Saved timestamp as epoch milliseconds.
- Mode.
- Leader name and score.
- Claimed bonus tile count.
- Difficulty.

Actions:

- `Continue`: calls resume saved game.
- `New game`: clears saved game and starts the preferred mode.
- Outside dismiss is disabled.

### Settings Dialog

Content:

- Difficulty selector.
- Preferred mode selector.
- Sound switch.
- Haptics switch.
- Reduced motion switch.

Difficulty:

- Levels `1..GameConstants.DIFFICULTY_LEVELS`.
- Currently five levels.
- Rendered as horizontal `FilterChip`s.

Preferred mode:

- Uses `GameMode.entries`.
- Rendered as horizontal `FilterChip`s.

Switch rows:

- Label on the left, Material `Switch` on the right.
- Row minimum height is `TouchTargetMin`.

Close action:

- `Close` text button.

### Profiles Dialog

Content:

- Existing profiles as `FilterChip`s.
- Active profile label format: `{name} active`.
- Name field.
- Color selector.
- Avatar selector.

Profile selection:

- Selecting a profile chip updates the local draft fields.
- Selecting a profile also calls `onSetActiveProfile(profile.id)`.

Draft fields:

- Name uses `OutlinedTextField`.
- Color selector iterates `GameConstants.PLAYER_COLORS.indices`.
- Avatar selector iterates `LocalAvatarStyle.entries`.

Actions:

- `Save profile` updates the selected profile when a selected id exists.
- `Save profile` creates a profile when no selected id exists.
- `Add profile` always calls add profile with the current draft.

Current profile UI limitation:

- It does not draw avatar previews in the dialog.
- It labels colors as `Color 1`, `Color 2`, etc., rather than using visual color swatches.

### Help Dialog

Sections:

- Goal.
- Start in your corner.
- Corner contact, no edge contact.
- Scoring.
- Bonus tiles.
- Passing and game end.
- Controls.
- Nearby games.

The help text is original Corners Apart wording from `strings.xml`. Do not add external board-game names, logos, official wording, or marketing language.

### Game Over Dialog

Shown when `state.isGameOver` is true.

Content:

- Title: `Game over`.
- Winner, when a ranked score exists.
- Duration in pluralized seconds.
- Score category labels.
- Ranked player rows.

Player score row:

- Rank.
- Player name.
- Total score as pluralized points.
- Breakdown:
  - Placed cell points.
  - Bonus tile points.
  - Completion bonus.
  - Claimed bonus tiles.

Actions:

- `Play again`: starts a new game using the current mode.
- `Stats`: opens History & stats.
- Outside dismiss is disabled.

### History & Stats Dialog

Structure:

- Material 3 `PrimaryTabRow`.
- Two tabs:
  - `History`
  - `Stats`

History tab:

- Empty state: `No finished games yet`.
- Otherwise shows the last 20 history entries from the provided history list.
- Row format: date, rank, points, difficulty, duration.

Stats tab:

- Total games.
- Wins.
- Average score.
- Best score.
- Average rank.
- Average bonus tiles.
- Decimal values are formatted with one decimal place.

Stats are calculated by `HistoryStatsCalculator.calculate(history)` unless a test supplies explicit stats.

## Game Modes In UI

The UI exposes all current game modes through header chips and settings preferred-mode chips.

Mode details:

| Mode | Header label | Board | Players shown | Bonus tiles | UI notes |
|---|---|---:|---:|---:|---|
| `FOUR_PLAYER` | Four players | 20 x 20 | 4 | 10 | Default preferred mode. |
| `SOLO` | Solo | 20 x 20 | 4 | 10 | Player 0 is human; players 1-3 are computer-controlled in session state. |
| `TWO_COLOR_DUEL` | Two-color duel | 20 x 20 | 4 color slots | 10 | UI keeps four color cards; scoring/ranking aggregates by owner outside UI. |
| `COMPACT_DUEL` | Compact duel | 14 x 14 | 2 | 6 | Config has `requiresPlayTesting = true`. |
| `THREE_PLAYER` | Three players | 20 x 20 | 3 | 10 | Uses first three standard corners. |

The UI does not branch into separate screens per mode. Board size, player cards, start markers, piece ownership, turn state, and score data come from `GameUiState`.

## UI State Contract

`GameUiState` fields consumed by UI:

- `gameMode`: selected mode chip and play-again mode.
- `board`: board size and occupied cells.
- `bonusTiles`: unclaimed marker drawing and resume/game-over metadata source.
- `players`: score bar, start markers, current player color, current player name, pass state.
- `currentPlayerIndex`: `currentPlayer` lookup.
- `selectedPieceId`: selected piece identity.
- `selectedOrientationIndex`: selected orientation identity.
- `selectedCells`: selected-piece preview shape.
- `pieces`: piece panel list with selected/used state.
- `isGameOver`: status line and game-over dialog.
- `soundEnabled`: one-shot sound policy.
- `hapticsEnabled`: input and effect haptics.
- `reducedMotionEnabled`: stored in state and settings; `MotionPolicy` supports zero durations when enabled.
- `gameDurationSeconds`: game-over duration.
- `preferredDifficulty`: settings difficulty chips and the currently collected settings value.
- `preferredMode`: settings preferred-mode chips.
- `history`: History & stats dialog.
- `activeProfileName`: present in state; not visibly rendered by current UI.
- `hasSavedGame`: resume dialog gate.
- `resumeSummary`: resume dialog content.
- `rankedScores`: game-over dialog.
- `nearbyState`: present in state; not visibly rendered beyond create/find actions.
- `profiles`: Profiles dialog.

`PlayerUiState` fields consumed or available:

- Visible now: `name`, `colorIndex`, `totalScore`, `startRow`, `startCol`, `hasPassed`, `isCurrentTurn`.
- Used by dialogs or future-ready state: owner index, score breakdown fields, claimed bonuses, pieces placed/remaining, computer flag.

`PiecePanelItem` fields:

- `piece`: id, display name, and cells.
- `isSelected`: card border.
- `isUsed`: disabled click, alpha, and content description.

`ResumeGameSummary` fields:

- Saved epoch milliseconds.
- Game mode.
- Leading player name.
- Leading score.
- Claimed bonus tile count.
- Difficulty.

## ViewModel Event Contract

UI calls into `GameViewModel` through these behaviors:

- Start game: `startGame(mode)`.
- Resume saved game: `resumeSavedGame()`.
- Discard saved game and start new game: `discardSavedGameAndStartNewGame()`.
- Sound setting: `setSoundEnabled(enabled)`.
- Haptics setting: `setHapticsEnabled(enabled)`.
- Reduced motion setting: `setReducedMotionEnabled(enabled)`.
- Difficulty setting: `setPreferredDifficulty(level)`.
- Preferred mode setting: `setPreferredMode(mode)`.
- Nearby host: `startNearbyHosting()`.
- Nearby discovery: `startNearbyDiscovery()`.
- Profile activate: `setActiveProfile(profileId)`.
- Profile add: `addProfile(name, colorIndex, avatarStyle)`.
- Profile update: `updateProfile(profileId, name, colorIndex, avatarStyle)`.
- Piece select: `selectPiece(pieceId)`.
- Rotate clockwise: `rotateSelectedClockwise()`.
- Rotate counterclockwise: `rotateSelectedCounterClockwise()`.
- Flip: `flipSelected()`.
- Place: `placeSelectedAt(row, col)`.
- Pass: `passCurrentPlayer()`.

One-shot `GameEffect`s:

- `MoveAccepted(playerName, scoreDelta, bonusTileClaimed)`.
- `MoveRejected(reason)`.
- `GameOver`.

Effect mapping:

- Move accepted without bonus:
  - Accessibility: `{playerName} gained {points}`.
  - Haptic: `TextHandleMove`.
  - Sound: placement tone, if sound is enabled.
- Move accepted with bonus:
  - Accessibility: score gained plus bonus tile claimed message.
  - Haptic: `LongPress`.
  - Sound: bonus claim tone, if sound is enabled.
- Move rejected:
  - Accessibility: `Move rejected`.
  - Haptic: `LongPress`.
  - Sound: none.
- Game over:
  - Accessibility: `Game over`.
  - Haptic: `LongPress`.
  - Sound: game-over tone, if sound is enabled.

## Visual Design System

The app uses a custom Corners Apart token layer over Material 3. Do not hardcode colors, spacing, shapes, typography, alpha values, or animation durations when a token exists.

### Color Tokens

Player colors:

| Player color | Base | Dark | Highlight | Ghost |
|---|---|---|---|---|
| Indigo | `0xFF4338CA` | `0xFF312E81` | `0xFF6366F1` | `0x4D4338CA` |
| Amber | `0xFFE88C0A` | `0xFFA16207` | `0xFFF5B040` | `0x4DE88C0A` |
| Coral | `0xFFE8513D` | `0xFF991B1B` | `0xFFF08070` | `0x4DE8513D` |
| Teal | `0xFF0D9488` | `0xFF134E4A` | `0xFF2DD4BF` | `0x4D0D9488` |

Surface and text colors:

| Token | Value | Use |
|---|---|---|
| `AppBackground` | `0xFFE4E4E8` | Screen background. |
| `BoardCellGap` | `0xFFDCDCE0` | Material surface variant and board gap role. |
| `BoardCellSurface` | `0xFFFAFAFA` | Empty board cells. |
| `BoardFrame` | `0xFF2C2C30` | Board frame and dark theme background. |
| `CardSurface` | `0xFFFFFFFF` | Light theme surface. |
| `BonusAccent` | `0xFFD8A928` | Bonus tile markers and tertiary color. |
| `TextPrimary` | `0xFF1A1A1E` | Primary text. |
| `TextSecondary` | `0xFF4A4A52` | Secondary text. |
| `TextMuted` | `0xFF8A8A92` | Outline/muted text. |
| `OnPlayerColor` | `0xFFFFFFFF` | Text/icon on player-colored surfaces. |

Material color schemes:

- Light scheme uses Indigo primary, Teal secondary, BonusAccent tertiary, AppBackground background, CardSurface surface.
- Dark scheme exists but `CornersApartTheme` defaults `darkTheme = false`; current `MainActivity` does not pass dynamic dark mode.
- Dynamic color is not currently wired.

### Spacing Tokens

| Token | Value |
|---|---:|
| `ScreenPadding` | `16.dp` |
| `SectionGap` | `12.dp` |
| `CompactGap` | `8.dp` |
| `TinyGap` | `4.dp` |
| `BoardCellGap` | `2.dp` |
| `BoardFrameWidth` | `4.dp` |
| `PieceInnerInset` | `2.dp` |
| `PieceShadowOffset` | `1.dp` |
| `PieceShadowBlur` | `2.dp` |
| `TouchTargetMin` | `48.dp` |
| `PieceCardSize` | `64.dp` |
| `PiecePreviewSize` | `84.dp` |
| `ScoreCardMinHeight` | `48.dp` |
| `ActivePlayerBorderWidth` | `2.dp` |

### Typography

Font:

- `Quicksand` from `app/src/main/res/font/quicksand.ttf`.

Configured Material typography:

| Style | Weight | Size |
|---|---|---:|
| `displayLarge` | Bold | `28.sp` |
| `headlineMedium` | SemiBold | `20.sp` |
| `bodyLarge` | Medium | `16.sp` |
| `labelLarge` | Medium | `14.sp` |
| `bodySmall` | Normal | `12.sp` |
| `labelSmall` | Normal | `10.sp` |

### Shapes

| Material shape | Radius |
|---|---:|
| `extraSmall` | `4.dp` |
| `small` | `6.dp` |
| `medium` | `8.dp` |
| `large` | `12.dp` |
| `extraLarge` | `16.dp` |

### Alpha Tokens

| Token | Value | Use |
|---|---:|---|
| `PassedPlayer` | `0.40f` | Dim passed player cards. |
| `UsedPiece` | `0.35f` | Dim used piece cards. |
| `PieceHighlight` | `0.35f` | Glossy top highlight. |
| `PieceShadow` | `0.50f` | Glossy bottom shadow. |
| `PieceInnerInset` | `0.08f` | Inner overlay. |
| `PieceDropShadow` | `0.12f` | Cell drop shadow. |
| `StartMarker` | `0.55f` | Start marker circle. |

### Animation Tokens

Tokens currently defined:

| Token | Duration |
|---|---:|
| `PIECE_PLACEMENT_MS` | `400` |
| `INVALID_ATTEMPT_SHAKE_MS` | `400` |
| `BONUS_TILE_CLAIMED_MS` | `350` |
| `ACTIVE_PLAYER_PULSE_MS` | `1500` |
| `OPPONENT_THINKING_DOT_MS` | `900` |
| `DIALOG_ENTER_MS` | `300` |
| `PIECE_CARD_INTRO_MS` | `300` |
| `PIECE_CARD_INTRO_STAGGER_MS` | `20` |
| `SCORE_INCREASE_MS` | `250` |

Current implementation note:

- `MotionPolicy.durationMillis(defaultMillis, reducedMotionEnabled)` returns `0` when reduced motion is enabled.
- Current visible UI code defines the policy but does not yet apply these animation tokens to visible Compose animations.

## Accessibility

Implemented accessibility details:

- Board content description: `Game board`.
- Rotate counterclockwise button content description: `Rotate counterclockwise`.
- Rotate clockwise button content description: `Rotate clockwise`.
- Flip button content description: `Flip selected piece`.
- Pass button content description: `Pass turn`.
- Piece card content descriptions include piece display name and used state.
- Accessibility announcement node uses `LiveRegionMode.Polite`.
- Nearby panel Surface also declares polite live region semantics.
- Utility buttons use icons as decorative children and text as visible labels.
- Material components provide default semantics for buttons, chips, switches, text fields, tabs, and dialogs.

Current accessibility gaps to consider before release:

- Board cells are drawn on a single Canvas and individual cells are not separately focusable.
- Board placement currently requires tapping the canvas; there is no keyboard/D-pad cell navigation.
- Profile color choices are text chips, not swatches.
- Nearby connection/authentication state is not visibly or semantically exposed in the current screen.

## Haptics And Sound

Haptics:

- Controlled by `state.hapticsEnabled`.
- Uses Compose `LocalHapticFeedback`.
- Input interactions and effects map to `TextHandleMove` or `LongPress`.

Sound:

- Controlled by `state.soundEnabled`.
- `GameSoundPolicy` maps eligible effects to `GameSoundEvent`.
- `GameSoundPlayer` uses Android `ToneGenerator`.
- Placement tone: `TONE_PROP_ACK`.
- Bonus claim tone: `TONE_PROP_BEEP2`.
- Game-over tone: `TONE_PROP_PROMPT`.
- Tone volume: `40`.
- Tone duration: `90 ms`.
- Move rejection intentionally has no sound.

Current sound limitation:

- Sounds are generated platform tones, not original `res/raw` audio assets.

## Text And Localization

Current text policy:

- User-facing UI text is English-first.
- Current localized resources consist of the base `app/src/main/res/values/strings.xml`.
- No additional locale directories are present.
- UI code uses `stringResource` and `pluralStringResource`.
- A scan of `ui/` found no hardcoded `Text(text = "...")` literals in current UI code.

Future UI text rules:

- Add or change visible text in `strings.xml`.
- Use plurals for count-sensitive labels.
- Do not add external board-game names, copied official rule wording, or third-party product identity.
- Keep the product name as `Corners Apart`.

## Persistence And UI

UI-visible persistence flows:

- Settings are backed by `SettingsRepository`.
- Saved game data is backed by `GameRepository`.
- Profiles and history are backed by `ProfileRepository`.
- `GameViewModel` collects all repositories and converts them into `GameUiState`.

UI must not access DataStore directly. All persistence-derived UI values must come through `GameViewModel` or another ViewModel/state holder if future screens are added.

## Game Rules And UI Boundaries

UI does:

- Display board state.
- Display legal and used piece state supplied by the ViewModel.
- Convert tap position to board row/column.
- Forward commands to ViewModel.
- Display accepted/rejected/game-over feedback effects.

UI does not:

- Validate placement legality.
- Compute scoring.
- Compute rankings.
- Generate bonus tiles.
- Advance turns.
- Decide computer moves.
- Aggregate Two-Color Duel owner scores.
- Read or write save/profile/settings storage.
- Send Nearby payloads directly.

These responsibilities remain in `engine/`, `model/`, `opponents/`, `multiplayer/`, `data/`, and `viewmodel/`.

## Testing Coverage

Instrumented Compose tests currently assert:

- Game screen shows board and accessible controls.
- History & stats dialog shows History and Stats tabs.
- Settings dialog shows Sound, Haptics, and Reduced motion toggles.
- Help dialog shows key rule sections.
- Game-over dialog shows score breakdown labels and Play again.

Unit tests currently assert:

- Layout policy switches to expanded at `840.dp`.
- Motion policy disables durations when reduced motion is enabled.
- Sound policy maps move accepted, bonus claim, game over, rejection, and disabled-sound cases.
- Theme player and surface palette token values match the reviewed specification.

When changing UI:

- Update or add Compose tests for visible controls, dialogs, semantics, and critical text.
- Update token tests when changing official token values.
- Keep tests source-backed; do not assert stale prototype/spec text.

## Current UI Gaps And Release Watchpoints

Current gaps visible from code:

- No active navigation graph; add one only when multiple route surfaces exist.
- Nearby state is collected but not fully rendered.
- Reduced motion setting exists, and `MotionPolicy` exists, but visible Compose animations are not yet implemented.
- Profile dialog edits avatar style but does not preview avatar graphics.
- Profile color selector uses numbered chips instead of swatches.
- Board Canvas has one board-level content description, not per-cell semantics.
- Resume dialog shows raw epoch milliseconds rather than formatted local date/time.
- Dark theme exists in code but is not selected from system theme in `MainActivity`.

Known release/polish watchpoints from project state:

- Physical two-device Nearby stress testing remains manual/release verification.
- Compact Duel needs manual play-test coverage before release claims.
- Privacy policy placeholders remain outside the UI code but are release relevant.

## Future UI Change Checklist

Before adding or changing UI:

1. Check existing composables and tokens first.
2. Keep user-facing text in `strings.xml`.
3. Use `GameUiState` or a ViewModel-owned state holder as the UI input.
4. Pass events upward through action callbacks.
5. Keep rules, scoring, persistence, and Nearby transport out of composables.
6. Use theme tokens for colors, spacing, typography, shapes, alpha, and animation durations.
7. Preserve minimum touch targets for interactive controls.
8. Add content descriptions or semantics for icon-only/custom-canvas interactions.
9. Update tests for new controls, dialogs, or state-dependent rendering.
10. If a change adds/removes modules, moves ownership, changes data flow, or changes architectural responsibility, update `AGENTS.md` and `memory/MEMORY.md` as required by project instructions.

## Verification Notes

This file was written from:

- `PROJECT.md`
- Current source under `app/src/main/java/com/finnvek/cornersapart/`
- Current resources under `app/src/main/res/`
- Current UI/unit tests under `app/src/androidTest/` and `app/src/test/`

The document intentionally describes current behavior, not desired future behavior, except where sections explicitly label gaps or future checklist items.
