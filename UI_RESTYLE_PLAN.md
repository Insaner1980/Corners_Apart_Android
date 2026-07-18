# Corners Apart — "Candy Bevel" Visual Restyle Plan

## Context

Corners Apart screenshots were compared against reference games (Block Blast, Woodoku Blast, Color Block Puzzle). The goal is to adopt the same visual style while keeping the existing four player colors (Indigo `#4338CA`, Amber `#E88C0A`, Coral `#E8513D`, Teal `#0D9488`) and the dark identity. The gap analysis (from the screenshots in `kuvakaappauksia/`):

1. Reference tiles are chunky 3D-beveled blocks that fill the whole cell (light top edge, dark bottom slab, glossy face); ours are flat thin rects floating on a white-line grid.
2. Reference backgrounds are saturated colored gradients with the board as a rounded darker panel of the same hue; ours is near-neutral black with a white grid and dark frame.
3. Reference typography is rounded ExtraBold white with soft shadows; ours is thin Quicksand in muted gray.
4. Reference chrome is candy-styled (3D buttons, tinted rounded dialogs, glossy switches) and minimal (score + board + tray only); ours is stock Material3 with heavy header chrome.

Decisions made: font = **Nunito**; header = **light de-clutter** (everything stays reachable in the game view, but collapsed: single mode chip opening a picker, Nearby behind one button, utility actions as icon buttons).

## Current code facts (verified)

- `ui/theme/Tokens.kt` — `CornersApartColors` (player families base/dark/highlight/ghost + surfaces + text), `CornersApartSpacing`, `CornersApartAlpha`. Most game visuals read these tokens directly, bypassing `MaterialTheme`.
- `ui/theme/PlayerPalette.kt` — `PlayerPieceColors(base, dark, highlight, ghost)` + `colorsFor(index)`.
- `ui/theme/Type.kt` — single `res/font/quicksand.ttf` for all styles; 6 styles, displayLarge 28sp.
- `ui/theme/Shapes.kt` — RoundedCornerShape 4→16dp.
- `ui/theme/Theme.kt` — Light/Dark M3 schemes from tokens.
- `ui/components/PieceShape.kt` — `PieceShape` composable (Canvas) → `drawPieceCells` → `drawGlossyCell` (5 stacked square-corner `drawRect`s). Used by tray cards, selected-piece preview, **and** `GameBoard.drawOccupiedCells`.
- `ui/screens/GameBoard.kt` — single Canvas. Outer Box: `.background(BoardFrame)` + `.padding(BoardFrameWidth)` = frame. Draw passes: `drawEmptyCells` (white rects; gaps show frame color = the "grid lines"), `drawBonusTiles` (diamond), `drawStartMarkers` (circle), `drawOccupiedCells`, `drawPlacementPreview` (ghost rect). Tap mapping via `Offset.toBoardCell` uses the Canvas size (inside the padding).
- `ui/screens/GameScreen.kt` (~910 lines) — `GameRoute` → `GameScreenContent` → `CompactGameLayout` / `ExpandedGameLayout` (breakpoint 840dp in `GamePolishPolicies`). Compact order: `GameHeaderActions` (Header title + mode `FilterChip`s, `NearbyActions` card, `UtilityActions` buttons, `PlayerScoreBar`) → `GameBoard` → `StatusLine` → `ControlBar` (3 `IconButton`s + Pass `Button`) → `SelectedPiecePreview` → `PiecePanel` (`PieceCard` tray).
- `ui/screens/PlayerScoreBar.kt` — `PlayerScoreCard` Surface; active = ghost fill + border.
- `ui/screens/GamePolishDialogs.kt` — stock `AlertDialog`s (Settings with `Switch` + `FilterChip` selectors, Profiles with `OutlinedTextField`, Help, GameOver, Resume). `HistoryStatsDialog.kt` uses `PrimaryTabRow`.
- All buttons are stock M3 `Button`/`IconButton`/`FilterChip`; no custom button component exists.
- `res/font/quicksand.ttf` is the only font; `colors.xml` holds only launcher colors; all strings externalized.
- Conventions: no hardcoded colors/dimensions outside theme tokens; Finnish commit messages; detekt + ktlint enforced; `GameScreenTest` androidTest exists.

## Design foundation — new tokens

All in `ui/theme/Tokens.kt`. Additive first; old tokens removed only in Phase 7.

**Background family (indigo-tinted dark, saturated):**

| Token | Hex | Role |
|---|---|---|
| `BackgroundGradientTop` | `#312B63` | top of vertical screen gradient |
| `BackgroundGradientBottom` | `#1D1940` | bottom of gradient |
| `BoardPanel` | `#241F4E` | rounded board panel |
| `BoardCellEmpty` | `#1B173D` | empty cell squares |
| `PanelSurface` | `#2E2960` | score cards, tray panel, status pill |
| `PanelSurfaceRaised` | `#3A3475` | selected/active card face |
| `DialogSurface` | `#2A2458` | CandyDialog background |
| `DialogSurfaceEdge` | `#171335` | dialog bottom bevel |

**Text on dark:** `TextOnDarkPrimary #FFFFFF`, `TextOnDarkSecondary #BCB6EA`, `TextOnDarkMuted #7E78B4`, `TextShadow #000000` (used at alpha `TextShadow = 0.35f`).

**Accents / buttons:**

| Token | Hex | Role |
|---|---|---|
| `BonusAccentBright` | `#FFC53D` | bonus diamond face |
| `ButtonPrimaryFace` / `ButtonPrimaryBevel` | `#5B4FE8` / `#3A31A8` | main CTA |
| `ButtonPositiveFace` / `ButtonPositiveBevel` | `#22B573` / `#15804F` | confirm/play |
| `ButtonWarnFace` / `ButtonWarnBevel` | `#E8513D` / `#A83224` | pass/destructive (Coral family) |
| `ButtonNeutralFace` / `ButtonNeutralBevel` | `#454078` / `#2B2760` | secondary/utility |

**Player colors:** base hexes untouched. Brighten `*Dark` values slightly so bottom bevels stay readable on the dark board: `PlayerCoralDark → #B02A20`, `PlayerTealDark → #0A6B62`, `PlayerAmberDark → #B56E08`; `PlayerIndigoDark` stays.

**New spacing:** `BoardPanelRadius = 20.dp`, `BoardPanelPadding = 8.dp`, `CandyButtonRadius = 18.dp`, `CandyButtonBevel = 4.dp`, `CandyButtonHeight = 52.dp`, `DialogRadius = 24.dp`; `ScoreCardMinHeight 48dp → 40dp`.

**New alphas:** `CellTopBevel = 0.55f`, `CellBottomBevel = 0.85f`, `CellGloss = 0.18f`, `TextShadow = 0.35f`, `BonusGlow = 0.30f`, `EmptyCellInnerShadow = 0.25f`.

---

## Phase 1 — Typography + tokens (buildable, visuals mostly unchanged)

**Files:** `res/font/` (new), `ui/theme/Type.kt`, `ui/theme/Tokens.kt`, `ui/theme/Shapes.kt`

1. Add bundled static Nunito TTFs (OFL license; static weights, not the variable font — Compose `Font(resId, weight)` with static files is simplest and API-safe): `res/font/nunito_semibold.ttf` (600), `nunito_bold.ttf` (700), `nunito_extrabold.ttf` (800), `nunito_black.ttf` (900). Download from Google Fonts at implementation time (verify current download URL then).
2. Rewrite `Type.kt`: `NunitoFontFamily` with those weights; styles:
   - `displayLarge` 40sp Black (big score numeral), `displayMedium` 30sp ExtraBold (title)
   - `headlineMedium` 22sp ExtraBold, `titleMedium` 18sp Bold
   - `bodyLarge` 16sp SemiBold, `labelLarge` 15sp Bold (buttons), `bodySmall` 13sp SemiBold, `labelSmall` 11sp **Bold** (ExtraBold smears at 11sp)
   - Shared helper: `fun TextStyle.withCandyShadow(): TextStyle = copy(shadow = Shadow(TextShadow @ alpha, Offset(0f, 2f), blurRadius = 4f))` — applied at headline/score call sites, not baked into every style.
3. Add all new tokens above to `Tokens.kt` (additive only).
4. `Shapes.kt`: bump to `extraSmall 8dp, small 12dp, medium 16dp, large 20dp, extraLarge 28dp`.
5. `quicksand.ttf` is deleted in Phase 7, not here.

**Verify:** `./gradlew assembleDebug :app:detekt` — app runs with new font, old colors.

## Phase 2 — Theme + screen background

**Files:** `ui/theme/Theme.kt`, new `ui/theme/Background.kt`, `ui/screens/GameScreen.kt` (root + text-color sweep), `ui/screens/PlayerScoreBar.kt` (text-color sweep)

1. New `Modifier.candyBackground()` in `Background.kt`: `background(Brush.verticalGradient(BackgroundGradientTop → BackgroundGradientBottom))`.
2. `Theme.kt`: unify to a single dark-candy scheme (both `darkTheme` branches map to it): `background`/`surface` from the panel family, `onSurface = TextOnDarkPrimary`, `onSurfaceVariant = TextOnDarkSecondary`, `primary = ButtonPrimaryFace`, `outline = TextOnDarkMuted`, `surfaceVariant = PanelSurfaceRaised`. This instantly darkens all remaining stock M3 components before their candy replacements land.
3. Apply `candyBackground()` on the root of `GameScreenContent`; sweep direct reads of `TextPrimary/TextSecondary/TextMuted/CardSurface` in `GameScreen.kt`/`PlayerScoreBar.kt` to the `TextOnDark*`/`PanelSurface` tokens.
4. Check `MainActivity` edge-to-edge/status-bar icon contrast for the now-always-dark background.

**Verify:** build + launch; whole app dark indigo, readable text; `GameScreenTest` passes (semantics unchanged).

## Phase 3 — Candy cell renderer (tiles everywhere: board, tray, preview)

**Files:** `ui/components/PieceShape.kt` (+ its 3 call sites in the same commit)

Rewrite `drawGlossyCell` → `drawCandyCell(topLeft, cellSize, colors: PlayerPieceColors, alpha)`. All layers `drawRoundRect` with `CornerRadius(cellSize * 0.18f)`:

1. **Bottom bevel slab (full cell):** solid `colors.dark`.
2. **Face (raised):** vertical gradient `highlight → base` over the top `FACE_HEIGHT = 0.86f` of the cell — dark slab peeks out the bottom 14%.
3. **Gloss stripe:** `Color.White @ CellGloss`, inset near the top (`x+10%, y+7%`, size `80% × 16%`, corner `8%`).
4. Delete drop shadow, inner inset, old band constants. New constants: `CELL_CORNER_FRACTION = 0.18f`, `CELL_FACE_HEIGHT_FRACTION = 0.86f`, gloss fractions.

Multi-cell pieces stay per-cell beveled (exactly the Block Blast look; no connectivity logic needed). Inside `drawPieceCells`, shrink each cell ~3% centered so adjacent cells of one piece read as chunky units.

`drawPieceCells` signature changes from `(base, dark, highlight)` to `(colors: PlayerPieceColors, alpha)` — update `PieceShape` composable and `GameBoard.drawOccupiedCells` in the same commit.

**Verify:** build; tray, preview, and board tiles all beveled/rounded; play several moves.

## Phase 4 — GameBoard panel

**Files:** `ui/screens/GameBoard.kt`

1. **Remove frame:** replace `.background(BoardFrame).padding(BoardFrameWidth)` with `.clip(RoundedCornerShape(BoardPanelRadius)).background(BoardPanel).padding(BoardPanelPadding)`. Tap mapping stays correct because `toBoardCell` uses the Canvas size inside the padding; keep padding uniform and `aspectRatio(1f)` on the outer Box.
2. **Empty cells:** `drawRoundRect(BoardCellEmpty, cornerRadius = CornerRadius(cellSize * 0.12f))` per cell. Gaps show `BoardPanel` — no white grid, no dark frame. Optional polish: 1px inner top shadow (`PieceShadowOverlay @ EmptyCellInnerShadow`) for a recessed look; skip if it costs noticeable frame time.
3. **Bonus tiles:** radial glow first (`drawCircle(BonusAccentBright @ BonusGlow, radius = cellSize * 0.42f)`), diamond fill `BonusAccentBright`, small darker inset diamond (`BonusAccent`) for depth.
4. **Start markers:** switch disc → ring (`style = Stroke(width = cellSize * 0.08f)`) so it reads as a target on dark cells.
5. **Placement preview:** ghost as `drawRoundRect` with tile corner radius + `Stroke` outline in `colors.highlight @ 0.6f` (old ghost alphas were tuned for a white board).

**Verify:** build; run `GameScreenTest` (board contentDescription untouched); manual: tap accuracy at board edges, bonus glow, ghost visibility on dark.

## Phase 5 — Reusable candy chrome components

**New files:** `ui/components/CandyButton.kt`, `ui/components/CandyDialog.kt`, `ui/components/CandyControls.kt`

1. **`CandyButton(text, onClick, style: CandyButtonStyle = Primary, modifier, enabled)`** — outer rounded rect (`CandyButtonRadius`) in bevel color; inner face offset up by `CandyButtonBevel` with vertical gradient (`lerp(face, White, 0.25f) → face`); white `labelLarge` text with `withCandyShadow()`. Pressed: face translates down 2dp via `interactionSource` + `graphicsLayer`; disabled: 0.4 alpha. Keep `Role.Button` semantics + ≥48dp touch target. `CandyButtonStyle` enum: `Primary, Positive, Warn, Neutral`.
2. **`CandyIconButton(icon, contentDescription, onClick, style = Neutral)`** — 44dp square, same bevel structure. Replaces `GameIconButton`.
3. **`CandyDialog(title, onDismiss, content, buttons)`** — `Dialog {}` wrapper (not `AlertDialog`): `DialogRadius` rounded box, `DialogSurface` background, bottom bevel strip `DialogSurfaceEdge`, centered white `headlineMedium` title, content slot, `CandyButton` row.
4. **`CandySwitch(checked, onCheckedChange)`** — stock M3 `Switch` with custom `SwitchColors` (track `PanelSurfaceRaised`/`ButtonPositiveFace`, thumb white), scaled 1.15× via `graphicsLayer` — keeps a11y for free.
5. **`CandyChip(selected, onClick, label)`** — pill (50% corner); selected = `ButtonPrimaryFace` face + bevel, unselected = `PanelSurface`; keep `selectable` semantics.

**Call-site replacements in `GameScreen.kt` (this phase):**
- `NearbyActions` buttons + endpoint-list buttons → `CandyButton(Neutral/Positive)`
- `UtilityActions` buttons → `CandyIconButton`
- Pass button → `CandyButton(Warn)`
- `ModeChip` → `CandyChip`
- `ControlBar` icon buttons → `CandyIconButton`

**Verify:** build; `GameScreenTest` (texts/contentDescriptions preserved); TalkBack spot-check one CandyButton.

## Phase 6 — GameScreen de-cluttering + score bar + dialogs (light de-clutter)

**Files:** `ui/screens/GameScreen.kt`, `ui/screens/PlayerScoreBar.kt`, `ui/screens/GamePolishDialogs.kt`, `ui/screens/HistoryStatsDialog.kt`

1. **Header collapse:** one compact row ≤56dp — small title left; the mode `FilterChip` row becomes a single current-mode `CandyChip` that opens a mode-picker (`CandyDialog` or dropdown of `CandyChip`s); utility actions (History/Profiles/Settings/Help) as `CandyIconButton`s right.
2. **Nearby collapse:** single nearby `CandyIconButton` expanding the existing `NearbyActions` card via `AnimatedVisibility`; auto-expanded whenever `connectionState != Idle` or a pending connection exists, so multiplayer flows are never hidden. All strings/semantics kept.
3. **`PlayerScoreCard` → compact candy pill (40dp):** `PanelSurface` bevel card, mini beveled color square (reuse `drawCandyCell` in a 16dp Canvas), score in `titleMedium` white; active = `PanelSurfaceRaised` face + player-color bevel edge; passed = existing 0.4 alpha.
4. **StatusLine + SelectedPiecePreview:** `PanelSurface` rounded pills; preview keeps `PiecePreviewSize`.
5. **PiecePanel/PieceCard:** card background `PanelSurface`; selected = `PanelSurfaceRaised` + 1.05 scale.
6. **ExpandedGameLayout:** same component swaps; breakpoint unchanged.
7. **Dialogs:** each `AlertDialog` → `CandyDialog`: Resume, Settings (`Switch` → `CandySwitch`, selectors → `CandyChip`), Profiles (keep `OutlinedTextField`, recolor via `OutlinedTextFieldDefaults.colors`), Help, GameOver (winner in `displayLarge` + shadow, breakdown on `PanelSurface` rows, big Positive play-again button). `HistoryStatsDialog`: wrap in `CandyDialog`, keep `PrimaryTabRow` (already dark from Phase 2 theme).

**Verify:** build; `./gradlew test` + `connectedAndroidTest` (inspect `GameScreenTest` finders **before** restructuring the header; keep testTags/strings stable, update finders if structure changed); manual pass through every dialog, nearby expand/collapse, game-over flow.

## Phase 7 — Cleanup + final verification

1. Remove dead tokens after grep confirms zero references: `AppBackground`, `BoardCellSurface`, `BoardFrame`, `BoardCellGap` (color), `CardSurface`, `TextPrimary/Secondary/Muted`, `BoardFrameWidth`, old alphas (`PieceShadow`, `PieceInnerInset`, `PieceDropShadow`, `PieceHighlight` if unused), `quicksand.ttf`, and `LightColorScheme` if Phase 2 unified schemes.
2. Grep guards: no `Color(0x` outside `Tokens.kt`; no `AlertDialog(` outside tests; no `FilterChip(` outside `CandyChip`.
3. Full run: `./gradlew :app:detekt lint test assembleDebug` + device smoke test; confirm status-bar contrast in both system modes (app is now always dark-candy).
4. Update `CLAUDE.md` Current Architecture notes (new `ui/components/Candy*` components, single dark theme).

## Phase ordering rationale

1–2 are purely additive (fonts/tokens/theme) so nothing breaks; 3 changes one file with three known call sites; 4 depends on 3; 5 adds components without removing anything; 6 swaps call sites onto them; 7 deletes. The app builds and runs after every phase. One Finnish commit per phase minimum.

## Risks

- Ghost-preview visibility on dark cells — mitigated with highlight outline (Phase 4.5).
- `GameScreenTest` header assertions — inspect test before Phase 6; keep strings/testTags stable.
- Nunito ExtraBold smearing at 11sp — labelSmall uses Bold.
- Tap mapping — keep `BoardPanelPadding` uniform; `toBoardCell` reads Canvas size inside padding.
- Optional per-cell inner shadows on large boards (20×20 = 400 cells × extra draw ops) — treat as polish, measure first.

## Verification summary (end-to-end)

- Per phase: `./gradlew assembleDebug :app:detekt`; app launched and played manually.
- After Phases 4 and 6: `connectedAndroidTest` (`GameScreenTest`).
- Final: full lint/detekt/test suite + manual smoke of solo game, two-color duel start, all dialogs, nearby advertise/discover buttons (permission prompts appear), game-over flow.

## Critical files

- `app/src/main/java/com/finnvek/cornersapart/ui/theme/Tokens.kt`
- `app/src/main/java/com/finnvek/cornersapart/ui/theme/Type.kt`, `Theme.kt`, `Shapes.kt`, new `Background.kt`
- `app/src/main/java/com/finnvek/cornersapart/ui/components/PieceShape.kt`
- new `ui/components/CandyButton.kt`, `CandyDialog.kt`, `CandyControls.kt`
- `app/src/main/java/com/finnvek/cornersapart/ui/screens/GameBoard.kt`
- `app/src/main/java/com/finnvek/cornersapart/ui/screens/GameScreen.kt`
- `app/src/main/java/com/finnvek/cornersapart/ui/screens/GamePolishDialogs.kt`, `HistoryStatsDialog.kt`, `PlayerScoreBar.kt`
- `app/src/androidTest/java/com/finnvek/cornersapart/ui/screens/GameScreenTest.kt`
