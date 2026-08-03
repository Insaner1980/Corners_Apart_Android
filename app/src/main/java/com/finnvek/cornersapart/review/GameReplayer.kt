package com.finnvek.cornersapart.review

import com.finnvek.cornersapart.engine.GameEngine
import com.finnvek.cornersapart.engine.MoveResult
import com.finnvek.cornersapart.model.BoardSnapshot
import com.finnvek.cornersapart.model.GameState
import com.finnvek.cornersapart.model.ScoreBreakdown
import com.finnvek.cornersapart.model.hasValidIndexDomains

class GameReplayer(
    private val gameEngine: GameEngine,
) {
    fun replay(finalState: GameState): GameReplayResult {
        if (!finalState.isGameOver || !finalState.hasValidIndexDomains()) {
            return GameReplayResult.Failed(MatchReviewFailure.InvalidFinalState())
        }

        val timeline = mutableListOf<ReviewTimelineStep>()
        val historyResult = replayHistory(finalState, initialStateFrom(finalState), timeline)
        if (historyResult is ReplayPhaseResult.Failed) {
            return GameReplayResult.Failed(historyResult.failure)
        }
        val finalPassResult =
            replayFinalPasses(
                finalState = finalState,
                initialState = (historyResult as ReplayPhaseResult.Success).state,
                timeline = timeline,
            )
        if (finalPassResult is ReplayPhaseResult.Failed) {
            return GameReplayResult.Failed(finalPassResult.failure)
        }
        val reconstructedState = (finalPassResult as ReplayPhaseResult.Success).state
        if (reconstructedState != finalState) {
            return GameReplayResult.Failed(MatchReviewFailure.FinalStateMismatch())
        }
        return GameReplayResult.Success(timeline = timeline.toList(), finalState = reconstructedState)
    }

    private fun replayHistory(
        finalState: GameState,
        initialState: GameState,
        timeline: MutableList<ReviewTimelineStep>,
    ): ReplayPhaseResult {
        var state = initialState
        finalState.moveHistory.forEachIndexed { historyIndex, move ->
            var passCount = 0
            while (state.currentPlayerIndex != move.playerIndex && passCount < state.players.size) {
                if (state.isGameOver) {
                    return alignmentFailure(state, historyIndex, move.playerIndex)
                }
                val playerIndex = state.currentPlayerIndex
                val hadValidMoves = gameEngine.hasValidMove(state, playerIndex)
                val stateBefore = state
                state = gameEngine.pass(state, playerIndex)
                timeline +=
                    ReviewTimelineStep(
                        stateBefore = stateBefore,
                        action = ReviewAction.Pass(playerIndex, hadValidMoves),
                        stateAfter = state,
                    )
                passCount += 1
            }

            if (state.isGameOver || state.currentPlayerIndex != move.playerIndex) {
                return alignmentFailure(state, historyIndex, move.playerIndex)
            }

            val stateBefore = state
            when (val result = gameEngine.applyMove(state, move)) {
                is MoveResult.Accepted -> {
                    state = result.state
                    timeline +=
                        ReviewTimelineStep(
                            stateBefore = stateBefore,
                            action = ReviewAction.Placement(move),
                            stateAfter = state,
                        )
                }

                is MoveResult.Rejected -> {
                    return ReplayPhaseResult.Failed(
                        MatchReviewFailure.HistoryMoveRejected(
                            sourceHistoryIndex = historyIndex,
                            rejectionReason = result.reason,
                        ),
                    )
                }
            }
        }
        return ReplayPhaseResult.Success(state)
    }

    private fun replayFinalPasses(
        finalState: GameState,
        initialState: GameState,
        timeline: MutableList<ReviewTimelineStep>,
    ): ReplayPhaseResult {
        var state = initialState
        var finalPassCount = 0
        while (!state.isGameOver && finalPassCount < state.players.size) {
            val playerIndex = state.currentPlayerIndex
            if (!finalState.players[playerIndex].passed) {
                return ReplayPhaseResult.Failed(
                    MatchReviewFailure.FinalPassMismatch(playerIndex = playerIndex),
                )
            }
            val stateBefore = state
            val hadValidMoves = gameEngine.hasValidMove(state, playerIndex)
            state = gameEngine.pass(state, playerIndex)
            timeline +=
                ReviewTimelineStep(
                    stateBefore = stateBefore,
                    action = ReviewAction.Pass(playerIndex, hadValidMoves),
                    stateAfter = state,
                )
            finalPassCount += 1
        }

        if (!state.isGameOver) {
            return ReplayPhaseResult.Failed(
                MatchReviewFailure.FinalPassMismatch(playerIndex = state.currentPlayerIndex),
            )
        }
        return ReplayPhaseResult.Success(state)
    }

    private fun initialStateFrom(finalState: GameState): GameState =
        finalState.copy(
            board = BoardSnapshot.empty(finalState.board.size),
            players =
                finalState.players.map { player ->
                    player.copy(
                        usedPieceIds = emptySet(),
                        scoreBreakdown = ScoreBreakdown(),
                        passed = false,
                    )
                },
            currentPlayerIndex = 0,
            turnNumber = 0,
            bonusTiles =
                finalState.bonusTiles.map { tile ->
                    tile.copy(claimedByPlayerIndex = null, claimedOnTurn = null)
                },
            moveHistory = emptyList(),
            isGameOver = false,
        )

    private fun alignmentFailure(
        state: GameState,
        historyIndex: Int,
        expectedPlayerIndex: Int,
    ): ReplayPhaseResult.Failed =
        ReplayPhaseResult.Failed(
            MatchReviewFailure.TurnAlignmentMismatch(
                sourceHistoryIndex = historyIndex,
                expectedPlayerIndex = expectedPlayerIndex,
                actualPlayerIndex = state.currentPlayerIndex,
            ),
        )

    private sealed interface ReplayPhaseResult {
        data class Success(
            val state: GameState,
        ) : ReplayPhaseResult

        data class Failed(
            val failure: MatchReviewFailure,
        ) : ReplayPhaseResult
    }
}
