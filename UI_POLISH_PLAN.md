# Corners Apart — Cheerful Polish Plan (Phase 2)

## Context

The candy-bevel restyle (see `UI_RESTYLE_PLAN.md`) is implemented. The user reviewed the result on device
(screenshots `kuvakaappauksia/Screenshot_20260719-*.png`) and identified the remaining gaps:

1. **Piece color problem.** Indigo pieces barely stand out from the indigo board/background. Additionally the
   current palette (indigo/amber/coral/teal ≈ blue/orange/red/teal) is too close to official Blokus colors
   (blue, yellow, red, green) — a hard requirement is that the piece colors must NOT read as the Blokus set.
   Pieces should be cheerful, mutually distinct, and pop against the dark indigo background.
2. **No sounds.** `GameSoundPlayer` uses `ToneGenerator` beeps (ugly, quiet, often inaudible). Real, pleasant
   sound effects are wanted.
3. **No animations.** Nothing moves — placement, score changes, dialogs and turn changes are all static.
   Note: the repo guardrail `BuildDependencyHygieneTest` currently forbids all `androidx.compose.animation.*`
   imports (it treats the module as an unused dependency seam). Adding animations requires updating that guardrail.
4. **Cramped dialogs.** Settings: the Difficulty (5 chips) and Preferred mode (5 chips) rows overflow and need
   horizontal scrolling. Profiles: the Color and Avatar chip rows also overflow. Selectors labelled "Color 1…4"
   are text chips instead of showing the actual colors. These should be reorganized so nothing scrolls sideways.
5. **Overall**: the game should look genuinely cheerful and presentable ("edustavan näköinen").

## Current code facts (verified)

- Player colors and names: `GameConstants.PLAYER_NAMES = PLAYER_COLORS = ["Indigo","Amber","Coral","Teal"]`;
  color families in `ui/theme/Tokens.kt` (`Player*` base/dark/highlight/ghost), mapped by
  `CornersApartPlayerPalette.colorsFor(index)`. Player names appear in score bar, status line, game-over ranking,
  history, and in `GameScreenTest` ("Teal wins") + `CornersApartTokensTest` (exact hex asserts).
- Sound: `ui/screens/GameSoundPlayer.kt` (`ToneGenerator`, 3 tones), events routed by `GameSoundPolicy.eventFor`
  (`GameSoundEvent.PLACEMENT/BONUS_CLAIM/GAME_OVER`), gated by the Sound setting.
- Animations guardrail: `app/src/test/java/com/finnvek/cornersapart/release/BuildDependencyHygieneTest.kt`
  defines `composeAnimationSeam` inside `unusedRuntimeDependencySeams` (live enforcement) and
  `allForbiddenUnusedDependencySeams` (recognizer self-test). Compose-foundation already brings the animation
  module transitively; only the guardrail blocks imports.
- Dialogs: `GamePolishDialogs.kt` — `DifficultySelector`/`ModeSelector`/`ProfileColorSelector`/
  `ProfileAvatarStyleSelector` all render `CandyChip`s inside `Row + horizontalScroll` (`SelectorSection`).
- Board: `GameBoard.kt` Canvas draw passes; start markers are thin stroke rings (hard to see);
  drag ghost overlay + preview implemented in `GameScreen.kt` (`BoardDragController`, `DragGhostOverlay`).
- Guardrails: all colors must live in `Tokens.kt`; no `AlertDialog`/`FilterChip` in production UI.

---

## Phase 1 — New cheerful piece palette

**Goal:** pieces that pop on the dark indigo background, mutually distinct, NOT the Blokus set (blue/yellow/red/green).

**Palette (all in `Tokens.kt`; names change everywhere):**

| Player | Base | Highlight | Dark | Rationale |
|---|---|---|---|---|
| **Pink** (was Indigo) | `#F0509E` | `#FF8AC2` | `#B62E72` | hot pink — maximum pop on indigo, nothing like Blokus |
| **Mango** (was Amber) | `#FFA726` | `#FFC46B` | `#C67908` | warm orange, clearly not Blokus yellow |
| **Cyan** (was Coral) | `#29C8E0` | `#7BE3F2` | `#17849B` | bright turquoise, clearly not Blokus blue |
| **Lime** (was Teal) | `#9BD934` | `#C4EE7D` | `#6FA51F` | yellow-green, distinct from Blokus mid-green |

Ghost variants: same hex at `0x4D` alpha, as today.

**Steps:**
1. `Tokens.kt`: replace the four `Player*` families with `PlayerPink*`, `PlayerMango*`, `PlayerCyan*`, `PlayerLime*`.
2. `PlayerPalette.kt`: update mapping (index 0→Pink, 1→Mango, 2→Cyan, 3→Lime).
3. `GameConstants.kt`: `PLAYER_NAMES`/`PLAYER_COLORS` → `["Pink","Mango","Cyan","Lime"]`.
4. Sweep name references: grep `Indigo|Amber|Coral|Teal` across `app/src` — update `Theme.kt` scheme colors that
   reused player tokens (`primary` etc. already use Button* tokens; verify), launcher colors stay.
5. Update tests: `CornersApartTokensTest` hex asserts, `GameScreenTest` ("Teal wins" → new ranking winner name),
   any engine/model tests using player names (grep).
6. Check bonus diamond (`BonusAccentBright` gold) vs Mango pieces — the glow + diamond shape differentiates;
   if too close on device, darken bonus inner diamond.

**Verify:** build + device screenshot; each color pops on the board; unit tests green.

## Phase 2 — Dialog layout redesign (no sideways scrolling)

**Files:** `GamePolishDialogs.kt`, `ui/components/CandyControls.kt` (+ small additions)

1. Replace `SelectorSection`'s `Row + horizontalScroll` with `FlowRow`
   (`androidx.compose.foundation.layout.FlowRow` — allowed, foundation) so chips wrap to new lines. Applies to:
   Difficulty, Preferred mode, Avatar, Profile list.
2. **Settings dialog:**
   - Difficulty: `FlowRow` of 5 compact `CandyChip`s ("1".."5" with a "Difficulty" title — shorter labels
     `settings_difficulty_level` can stay if they fit two rows).
   - Preferred mode: vertical stack of full-width `CandyChip`s (same pattern as the mode-picker dialog) — clearest,
     no wrapping ambiguity.
   - Sound/Haptics switch rows unchanged.
3. **Profiles dialog:**
   - New `ColorSwatchSelector`: a single row of four 44dp round swatches drawn with the actual player colors
     (mini candy bevel via `drawCandyCell` or a simple circle + ring when selected + checkmark). Replaces the
     "Color 1…4" text chips — all four fit without scrolling.
   - Avatar: `FlowRow` of the four style chips.
   - Profile list: `FlowRow`.
   - Order: profile chips → name field → Color (swatches) → Avatar → buttons.
4. Add string resources if labels change; keep all existing strings referenced by tests intact.

**Verify:** device: open Settings and Profiles — nothing scrolls horizontally, everything visible at once;
`GameScreenTest` settings/profile assertions pass.

## Phase 3 — Real sound effects

**Files:** new `app/src/main/res/raw/*.ogg` (or `.wav`), `GameSoundPlayer.kt`, `GamePolishPolicies.kt`

1. Generate pleasant short samples with a synthesis script (Python, offline, no external assets — marimba/pluck
   style sine+harmonics with soft envelope, 44.1 kHz 16-bit):
   - `snd_place.wav` (short pluck, ~120 ms)
   - `snd_bonus.wav` (two-note rising chime)
   - `snd_reject.wav` (soft low thud)
   - `snd_game_over.wav` (three-note fanfare)
2. Rewrite `GameSoundPlayer` on **SoundPool** (`AudioAttributes` USAGE_GAME / CONTENT_TYPE_SONIFICATION,
   maxStreams 2), preloading raw resources; `release()` in `DisposableEffect` as today.
3. Extend `GameSoundEvent` with `REJECT`; map `GameEffect.MoveRejected/ActionFailed → REJECT` in
   `GameSoundPolicy.eventFor` (currently returns null for those). Keep the Sound toggle gating.
4. Note: device mute switch still silences everything (expected); tones now play on the game stream at sample
   volume.

**Verify:** device test with volume up: place / reject / bonus / game over each audible and pleasant;
`GameSoundPolicy` unit tests updated.

## Phase 4 — Animations

**Guardrail first:** in `BuildDependencyHygieneTest`, remove `composeAnimationSeam` from
`unusedRuntimeDependencySeams` (keep the seam object + `allForbiddenUnusedDependencySeams` so the recognizer
self-test still passes). Compose animation is transitively available; no build.gradle change needed
(BOM policy untouched).

Then add, smallest-risk first:

1. **Placement pop:** in `GameBoard`, remember the previous `BoardSnapshot`; diff → newly occupied cells animate
   scale 0.6→1.0 (spring, ~250 ms) via a single `Animatable` restarted on board change; `drawCandyCell` gets an
   optional `scale` param (scale around cell center).
2. **Score bump:** `PlayerScoreCard` — `animateIntAsState` on the score plus a short scale pulse on change.
3. **Turn change:** `StatusLine` text via `AnimatedContent` (fade/slide-up).
4. **Rejection notice:** `AnimatedVisibility` (fade+slide) instead of plain if; plus a brief horizontal shake of
   the status card (offset keyframes).
5. **Nearby expand:** switch the `if (expanded)` back to `AnimatedVisibility` (expand/collapse).
6. **Dialog entry:** `CandyDialog` content scale 0.9→1 + fade on entry (`Animatable` in `LaunchedEffect`).
7. **Drag ghost:** scale 1.0→1.1 while dragging (animateFloatAsState on `dragCells != null`).
8. **Selected piece card:** animate the 1.05 scale.
9. **Game-over confetti:** Canvas particle burst behind the winner title in `GameOverDialog`
   (~60 particles in player colors, gravity + rotation, 1.5 s, runs once) — the "cheerful signature" moment.

**Verify:** device: play moves, reject a move, open dialogs, finish a game (confetti); no jank on the 20×20 board;
detekt + tests green.

## Phase 5 — Cheer pass (small visual tweaks)

1. Brighten the background gradient slightly (`BackgroundGradientTop` toward `#3A3378`) so the scene reads
   "playful evening" rather than "night".
2. Start-corner markers: filled glowing dot (player base at higher alpha + soft glow circle) instead of the thin
   ring — visible invitation for the first move.
3. Bonus diamonds: gentle idle pulse (alpha 0.85↔1.0, slow) — ties into Phase 4 infra.
4. Board panel: add a 1dp lighter inner border (`PanelSurfaceRaised`) for definition.
5. Header title: tiny colored accent — render "Corners Apart" with the four player colors on the initial letters
   or a 4-color underline bar (single `Canvas`/`Brush.horizontalGradient` under the title).

**Verify:** side-by-side screenshots vs `kuvakaappauksia/` references; user review.

## Phase 6 — Cleanup + full verification

1. Grep guards: no `Color(0x` outside `Tokens.kt`; no `AlertDialog(`/`FilterChip(`; no leftover old player token
   names.
2. `./gradlew :app:detekt lint test assembleDebug` (3 pre-existing multiplayer detekt findings remain out of scope).
3. Device smoke: solo + compact duel, drag + tap placement, all dialogs, sounds, game over with confetti.
4. Update `CLAUDE.md` (palette names, SoundPool player, animation guardrail change) and `MEMORY.md` if needed.
5. Finnish commit per phase.

## Phase ordering rationale

Palette first (1) because every later visual (confetti colors, swatches) depends on it. Dialog layout (2) is
independent and safe. Sounds (3) touch no UI. Animations (4) need the guardrail edit and build on final colors.
Cheer pass (5) is polish on top. Each phase leaves the app buildable and testable.

## Risks

- Renaming players touches history data display (`HistoryEntry` stores names?) — verify whether saved
  games/history persist player names; if yes, old entries show old names (acceptable) — check `SavedGameData`
  compatibility so resume still works after rename (names come from constants at new-game time; resumed games
  keep stored names).
- Confetti + 400-cell board redraws: keep particle count modest, measure frame time.
- Guardrail edit must not break the recognizer self-test (`unusedDependencyGuardRecognizesForbiddenCoordinatesAndImports`).
- `GameScreenTest` winner-name assertions must be updated in the same commit as the rename.

## Critical files

- `app/src/main/java/com/finnvek/cornersapart/ui/theme/Tokens.kt`, `PlayerPalette.kt`
- `app/src/main/java/com/finnvek/cornersapart/model/GameConstants.kt`
- `app/src/main/java/com/finnvek/cornersapart/ui/screens/GamePolishDialogs.kt`, `GameSoundPlayer.kt`,
  `GamePolishPolicies.kt`, `GameBoard.kt`, `GameScreen.kt`, `PlayerScoreBar.kt`
- `app/src/main/java/com/finnvek/cornersapart/ui/components/CandyControls.kt`, `CandyDialog.kt`, `PieceShape.kt`
- `app/src/test/java/com/finnvek/cornersapart/release/BuildDependencyHygieneTest.kt`
- `app/src/test/java/com/finnvek/cornersapart/ui/theme/CornersApartTokensTest.kt`
- `app/src/androidTest/java/com/finnvek/cornersapart/ui/screens/GameScreenTest.kt`
- new `app/src/main/res/raw/snd_*.wav`
