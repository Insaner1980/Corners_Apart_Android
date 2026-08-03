package com.finnvek.cornersapart.review

import com.finnvek.cornersapart.engine.GameEngine
import com.finnvek.cornersapart.engine.MoveRejectionReason
import com.finnvek.cornersapart.engine.MoveResult
import com.finnvek.cornersapart.model.BonusTile
import com.finnvek.cornersapart.model.CellPosition
import com.finnvek.cornersapart.model.GameConfig
import com.finnvek.cornersapart.model.GameMode
import com.finnvek.cornersapart.model.GameState
import com.finnvek.cornersapart.model.Move
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GameReplayerTest {
    private val engine = GameEngine()
    private val replayer = GameReplayer(engine)

    @Test
    fun voluntaryPassBetweenRecordedMovesIsReconstructed() {
        var state = newGame()
        state = engine.applyMove(state, firstMove(state)).acceptedState()
        val passedPlayerIndex = state.currentPlayerIndex
        assertTrue(engine.hasValidMove(state, passedPlayerIndex))
        state = engine.pass(state, passedPlayerIndex)
        state = engine.applyMove(state, firstMove(state)).acceptedState()
        val finalState = finishByPassing(state)

        val replay = replayer.replay(finalState).success()

        assertEquals(finalState, replay.finalState)
        assertTrue(
            replay.timeline.any { step ->
                step.action ==
                    ReviewAction.Pass(
                        playerIndex = passedPlayerIndex,
                        hadValidMoves = true,
                    )
            },
        )
    }

    @Test
    fun finishingPassesAreReconstructedWhenMoveHistoryIsEmpty() {
        val finalState = finishByPassing(newGame())

        val replay = replayer.replay(finalState).success()

        assertEquals(finalState, replay.finalState)
        assertEquals(emptyList<Move>(), finalState.moveHistory)
        assertEquals(finalState.players.size, replay.timeline.size)
        assertTrue(replay.timeline.all { step -> step.action is ReviewAction.Pass })
    }

    @Test
    fun automaticallySkippedPlayerDoesNotCreatePassStep() {
        val initialState =
            newGame().copy(
                players =
                    newGame().players.map { player ->
                        if (player.index == 1) {
                            player.copy(startCorner = CellPosition(row = -1, col = -1))
                        } else {
                            player
                        }
                    },
            )
        assertFalse(initialState.players[1].passed)
        assertFalse(engine.hasValidMove(initialState, playerIndex = 1))
        var state = engine.applyMove(initialState, firstMove(initialState)).acceptedState()
        assertEquals(2, state.currentPlayerIndex)
        state = engine.applyMove(state, firstMove(state)).acceptedState()
        val finalState = finishByPassing(state)

        val replay = replayer.replay(finalState).success()

        assertEquals(finalState, replay.finalState)
        assertFalse(
            replay.timeline.any { step ->
                (step.action as? ReviewAction.Pass)?.playerIndex == 1
            },
        )
    }

    @Test
    fun replayPreservesPlayerIdentityOwnersAndExplicitBonusTiles() {
        val bonuses = listOf(BonusTile(row = 0, col = 0), BonusTile(row = 5, col = 5))
        var state =
            engine
                .newGame(
                    GameConfig(
                        mode = GameMode.TWO_COLOR_DUEL,
                        randomSeed = 44L,
                        bonusTiles = bonuses,
                    ),
                ).let { created ->
                    created.copy(
                        players =
                            created.players.map { player ->
                                if (player.index == 1) {
                                    player.copy(name = "Rival", isComputerControlled = true)
                                } else {
                                    player
                                }
                            },
                    )
                }
        state = engine.applyMove(state, firstMove(state)).acceptedState()
        val finalState = finishByPassing(state)

        val replay = replayer.replay(finalState).success()
        val replayInitialState = replay.timeline.first().stateBefore

        assertEquals(finalState, replay.finalState)
        assertEquals("Rival", replayInitialState.players[1].name)
        assertTrue(replayInitialState.players[1].isComputerControlled)
        assertEquals(listOf(0, 1, 0, 1), replayInitialState.players.map { it.ownerIndex })
        assertEquals(bonuses, replayInitialState.bonusTiles)
    }

    @Test
    fun invalidPlayerIndexReturnsInvalidFinalStateFailure() {
        val finalState =
            finishByPassing(newGame()).let { ended ->
                ended.copy(
                    players =
                        ended.players.mapIndexed { index, player ->
                            if (index == 0) player.copy(index = 99) else player
                        },
                )
            }

        val failure = replayer.replay(finalState).failure()

        assertTrue(failure is MatchReviewFailure.InvalidFinalState)
        assertEquals(null, failure.sourceHistoryIndex)
        assertEquals(null, failure.rejectionReason)
    }

    @Test
    fun impossibleTurnOrderReturnsTurnAlignmentFailure() {
        val initial =
            newGame().let { created ->
                created.copy(
                    players =
                        created.players.map { player ->
                            player.copy(isActiveScoring = player.index == 0)
                        },
                )
            }
        val impossibleHistoryMove =
            engine
                .getValidMoves(newGame(), playerIndex = 2)
                .first()
        val finalState =
            finishByPassing(initial).copy(
                moveHistory = listOf(impossibleHistoryMove),
            )

        val failure = replayer.replay(finalState).failure()

        assertTrue(failure is MatchReviewFailure.TurnAlignmentMismatch)
        assertEquals(0, failure.sourceHistoryIndex)
    }

    @Test
    fun rejectedHistoryMoveReturnsTypedRejectionReason() {
        val invalidMove =
            Move(
                playerIndex = 0,
                pieceId = "missing-piece",
                anchorRow = 0,
                anchorCol = 0,
                orientationIndex = 0,
            )
        val finalState =
            finishByPassing(newGame()).copy(
                moveHistory = listOf(invalidMove),
            )

        val failure = replayer.replay(finalState).failure()

        assertTrue(failure is MatchReviewFailure.HistoryMoveRejected)
        assertEquals(0, failure.sourceHistoryIndex)
        assertEquals(MoveRejectionReason.UNKNOWN_PIECE, failure.rejectionReason)
    }

    private fun newGame(): GameState =
        engine.newGame(
            GameConfig(
                mode = GameMode.FOUR_PLAYER,
                randomSeed = 41L,
                bonusTiles = emptyList(),
            ),
        )

    private fun firstMove(state: GameState): Move = engine.getValidMoves(state, state.currentPlayerIndex).first()

    private fun finishByPassing(initialState: GameState): GameState {
        var state = initialState
        repeat(state.players.size) {
            if (!state.isGameOver) {
                state = engine.pass(state, state.currentPlayerIndex)
            }
        }
        check(state.isGameOver)
        return state
    }

    private fun MoveResult.acceptedState(): GameState = (this as MoveResult.Accepted).state

    private fun GameReplayResult.success(): GameReplayResult.Success = this as GameReplayResult.Success

    private fun GameReplayResult.failure(): MatchReviewFailure = (this as GameReplayResult.Failed).failure
}
