# Match Review ("Coach") — Implementation Plan

Status: planned, not started.
Audience: implementing agent (Codex). Read `CLAUDE.md`, `AGENTS.md`, and `PROJECT.md` before starting. When this plan and the source code disagree, the source code wins — verify every referenced symbol before using it.

## Ground rules for the implementing agent

- Do NOT invent problems. If something already works, leave it alone. Do not refactor unrelated code, do not "improve" existing files beyond what this plan requires.
- Prefer the simplest implementation that satisfies the acceptance criteria. If a phase feels like it needs a framework, a new dependency, or a large abstraction, stop — it doesn't.
- No new Gradle dependencies. Everything here is pure Kotlin + existing Compose/Hilt.
- Verify every class/function name referenced below against the actual source before writing code that calls it. Names in this plan were checked on 2026-07-28 but may drift.
- Comments and commit messages in Finnish (project convention). All user-facing strings in `res/values/strings.xml`. No `Color(0x...)` outside `Tokens.kt` (guardrail unit test enforces this). Run `./gradlew test` after UI changes.
- `GameViewModel.kt` (~1000 lines) and `GameScreen.kt` (~1400 lines) already carry detekt suppressions for size. Put new logic in NEW files; add only thin wiring to the existing ones.

## Feature summary

After a finished local game, the player can open a **Match Review**: step through the game move by move on a read-only board, see each of their own moves rated (Great / Good / Inaccuracy / Mistake), see what the strongest engine configuration would have played instead, and get a final accuracy summary. Chess.com "Game Review", but for a polyomino game, fully offline, reusing the existing opponent evaluation engine.

Scope for v1:

- Review is available ONLY for the game that just ended (final `GameState` with full `moveHistory` is in memory in `GameViewModel`). No persistence of replayable games. No review of Nearby games (their state is not persisted by design — see `PROJECT.md`).
- Only the local human player's moves (owner 0 slots) are assessed. Other players' moves are shown as plain replay steps.
- Entry point: a "Review game" button in `GameOverDialog`.

## Why this is cheap: what already exists

- `GameState.moveHistory: List<Move>` records every accepted move in order (`Move` = playerIndex, pieceId, anchorRow, anchorCol, orientationIndex). See `model/Move.kt`, `model/GameState.kt`.
- `GameEngine.applyMove`, `GameEngine.pass`, `GameEngine.getValidMoves(state, playerIndex)` are pure and deterministic (`engine/GameEngine.kt`).
- `MoveEvaluator.evaluate(state, move, style, difficulty)` returns a `MoveEvaluation` scoring a candidate move (`opponents/MoveEvaluator.kt`). Check how `MoveEvaluation` exposes a total (sum of components) — reuse whatever exists, do not duplicate the weighting logic.
- `OpponentDifficulty.MASTER` and `OpponentStyle` exist in `opponents/`.
- Board cell rendering (`drawCandyCell` in `ui/components/PieceShape.kt`), `CandyDialog`, `CandyButton`, `CandyIconButton`, theme tokens — all reusable.

## Critical design constraint: replaying a game

`moveHistory` does NOT record passes. You cannot naively fold `applyMove` over the history:

- `applyMove`/`pass` auto-advance `currentPlayerIndex`, skipping players who have no valid move (without marking them passed).
- A player who passed MANUALLY (while still having valid moves) leaves no history entry, so during replay `currentPlayerIndex` will point at them while the next recorded move belongs to someone else, and `applyMove` would reject with `NOT_PLAYERS_TURN`.

Deterministic reconstruction algorithm (this is the core of the feature — implement exactly this):

1. Build the initial state from the FINAL state (do not call `newGame`, so explicit bonus-tile configs and rival/session player identity survive): copy the final `GameState` with an empty board of the same size, every player reset (`usedPieceIds` empty, `scoreBreakdown` zeroed, `passed = false`), every bonus tile unclaimed (`claimedByPlayerIndex = null`, `claimedOnTurn = null`), `currentPlayerIndex = 0`, `turnNumber = 0`, `moveHistory = emptyList()`, `isGameOver = false`.
2. For each recorded move in order: while `state.currentPlayerIndex != move.playerIndex`, call `engine.pass(state, state.currentPlayerIndex)` (this reproduces the manual pass). Guard with a loop bound of `players.size` iterations; if exceeded, the reconstruction has diverged — abort review with a graceful error (this should be impossible for engine-produced histories, but never loop forever).
3. Apply the move via `engine.applyMove`. If it is rejected, abort review gracefully (again: should not happen; do not "fix" it by bypassing validation).
4. Record each intermediate state into a timeline list.

Verification invariant (MUST be a unit test): replaying `finalState.moveHistory` from the reconstructed initial state reproduces `finalState.board` exactly, and each player's final `scoreBreakdown` matches.

## Move assessment

For each timeline step where the moving player is a reviewed player (local human, owner 0):

1. Take the state BEFORE the move. Compute `engine.getValidMoves(state, playerIndex)`.
2. Evaluate the played move and the candidates with `MoveEvaluator.evaluate(state, move, style, difficulty)` using a fixed reference configuration: `OpponentDifficulty.MASTER` and one fixed style (pick `OpponentStyle.BLOCKER` or whatever the roster uses for its strongest character — check `OpponentRoster`; the point is: ONE fixed reference config so ratings are comparable, not the player's settings).
3. `bestMove` = argmax of evaluation total among valid moves. `gap = bestTotal - playedTotal`.
4. Classification by relative gap (`gap / max(abs(bestTotal), 1.0)`):
   - `GREAT`: played move is the best move (or within 2%).
   - `GOOD`: within 15%.
   - `INACCURACY`: within 40%.
   - `MISTAKE`: worse than that.
   These thresholds are a starting point — put them in named constants in one place so they are tunable. Do not agonize over them; they can be balanced later.
5. Game accuracy = mean over reviewed moves of `playedTotal / bestTotal` clamped to 0..1 (guard division by zero and negative totals — evaluation components can be negative; if `bestTotal <= 0`, score that move as 1.0 when played == best, else 0.5, and move on. Keep it simple and documented).
6. Passes by the reviewed player: if the player had valid moves when they passed, mark the pass step as `MISTAKE` ("passed with moves available"); if not, no assessment.

Performance: worst case ~80+ moves, each requiring `getValidMoves` on a 20×20 board. This is the same work the opponent engine already does per turn, so it is feasible, but:

- Run the whole analysis off the main thread (injected dispatcher, same pattern as `ComputerOpponentEngine` — check how its dispatcher is injected and mirror it).
- Analyze incrementally and publish partial results (progress fraction) so the UI can show a progress bar and the user can start stepping through already-analyzed moves.
- If evaluation cost is a problem in practice, cap candidates via the existing `MoveGenerator` sampling instead of full `getValidMoves` — but only do this if actually needed; measure first with a unit-level timing check, don't guess.

## New code layout

New package `com.finnvek.cornersapart.review` (pure Kotlin, no Android imports):

- `GameReplayer` — reconstruction algorithm above. Depends on `engine` + `model` only.
- `MatchReviewAnalyzer` — assessment logic. Depends on `engine`, `model`, `opponents` (`MoveEvaluator`, `OpponentDifficulty`, `OpponentStyle`).
- `MatchReviewModels.kt` — plain data classes: `ReviewTimelineStep` (state before, move or synthesized pass, state after, moving player index), `MoveAssessment` (classification enum, played/best totals, best move, claimed-bonus info), `MatchReviewResult` (steps, assessments by step index, accuracy, counts per classification).

Before creating the package, read `PackageDependencyBoundaryTest` and check whether new packages must be registered there; keep its rules satisfied (`review` may depend on `engine`/`model`/`opponents`; nothing may gain a dependency ON `review` except `viewmodel`/`ui`). These are NOT serializable models — nothing here is persisted, so no `@Serializable` (the boundary test restricts serialization declarations to `model` and the protocol).

DI: provide `GameReplayer`/`MatchReviewAnalyzer` via the existing runtime Hilt module (`GameRuntimeModule` in `data/` provides engine/opponent services — follow the same pattern).

## ViewModel wiring (thin)

In `GameViewModel` (keep additions minimal; put helpers in a new file if they grow):

- On game over of a LOCAL session, keep the final `GameState` reference (it is already available where game-over history recording happens).
- New action `startMatchReview()`: launches analysis on the background dispatcher, publishes `MatchReviewUiState` (loading progress, timeline, assessments, accuracy, current step index) into `GameUiState` — add ONE new nullable field to `GameUiState`, e.g. `matchReview: MatchReviewUiState?`.
- New actions: `reviewStepForward()`, `reviewStepBack()`, `reviewJumpTo(index)`, `closeMatchReview()`.
- Review state is transient — cleared when a new game starts. Do not touch DataStore, repositories, or persistence at all.

## UI (new files under `ui/screens/`)

`MatchReviewDialog.kt` using `CandyDialog`:

- Read-only board rendering the `stateAfter` of the current step. Reuse `drawCandyCell` for cells; do NOT reuse the interactive `GameBoard` (it drags/places). A small dedicated `ReviewBoard` composable is fine. Highlight the cells of the current step's move with a brighter outline; optionally toggle a ghost overlay of the engine's best move (semi-transparent, using the existing preview-alpha token style).
- Controls: first / prev / next / last (`CandyIconButton`), a step indicator ("Move 12 / 47"), and the assessment pill for reviewed moves (classification label + colored chip; colors via theme tokens — add token entries if needed in `Tokens.kt`, nowhere else).
- Header summary once analysis completes: accuracy percent, counts (e.g. "3 great, 1 mistake"), rendered with existing candy chip components.
- While analysis is running: progress indicator; steps already analyzed are browsable.
- Entry: a `CandyButton` "Review game" in `GameOverDialog`, visible only for finished local games (`matchReview` availability flag from the ViewModel). Game-over dialog stays open behind or closes — simplest: close game-over, open review; "back" returns to nothing (main screen), which is acceptable for v1.
- All labels/plurals in `strings.xml`. Content descriptions for the stepper buttons and the board ("Review board, move N of M, <player> placed <piece>") — follow the existing accessibility announcement patterns in `GameRoute`.

## Tests (JVM unless stated)

1. `GameReplayerTest`: play scripted games through `GameEngine` (including at least one manual pass with valid moves available, and one auto-skipped player), then assert the replay invariant: reconstructed step-by-step states end in exactly the recorded final board + scores; synthesized passes appear at the right steps.
2. `MatchReviewAnalyzerTest`: on a small scripted position, best move gets `GREAT`; a deliberately weak legal move gets `INACCURACY`/`MISTAKE`; pass-with-moves-available gets `MISTAKE`; accuracy is in 0..1; determinism (same input → identical result).
3. `GameViewModel` test: `startMatchReview` publishes a completed review for a finished local game; review state clears on new game; no review offered for Nearby sessions.
4. Guardrails: existing `./gradlew test` suite must stay green (color centralization, package boundaries, engine purity).

## Phases

1. `review/` package: models + `GameReplayer` + tests. (Pure logic, no UI — verifiable alone.)
2. `MatchReviewAnalyzer` + tests.
3. ViewModel wiring + tests.
4. `MatchReviewDialog` + game-over entry point + strings + tokens.
5. Full `./gradlew test`, manual smoke on device/emulator: finish a Solo game, review it, step through, check best-move ghost, check a rival match and a two-color duel (owner-0 aggregation: in Two-Color Duel BOTH color slots with `ownerIndex == 0` are reviewed).

## Explicit non-goals (do not build these)

- Persisting replayable games or reviewing games from history.
- Reviewing Nearby games.
- In-game hints during play.
- Any new settings, difficulty for the reviewer, or configurable thresholds in the UI.
- Animations beyond simple step transitions (respect the existing animation budget; no new confetti).
