package com.finnvek.cornersapart.engine

import com.finnvek.cornersapart.model.GameConstants
import com.finnvek.cornersapart.model.GameState
import com.finnvek.cornersapart.model.Move
import com.finnvek.cornersapart.model.MutableBoard
import com.finnvek.cornersapart.model.PieceCatalog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GameEnginePlacementTest {
    private val engine = GameEngine()

    @Test
    fun gameOverTakesPrecedenceOverInvalidPlayer() {
        val state = EngineTestFixtures.standardState(engine).copy(isGameOver = true)

        val rejected =
            engine.applyMove(
                state,
                Move(
                    playerIndex = -1,
                    pieceId = PieceCatalog.SINGLE_CELL_ID,
                    anchorRow = 0,
                    anchorCol = 0,
                    orientationIndex = 0,
                ),
            )

        assertRejected(rejected, MoveRejectionReason.GAME_OVER)
    }

    @Test
    fun wrongTurnTakesPrecedenceOverInvalidPlayer() {
        val state = EngineTestFixtures.standardState(engine)

        val rejected =
            engine.applyMove(
                state,
                Move(
                    playerIndex = -1,
                    pieceId = PieceCatalog.SINGLE_CELL_ID,
                    anchorRow = 0,
                    anchorCol = 0,
                    orientationIndex = 0,
                ),
            )

        assertRejected(rejected, MoveRejectionReason.NOT_PLAYERS_TURN)
    }

    @Test
    fun previewRejectsTheSameWrongTurnMoveAsApplyMove() {
        val state = EngineTestFixtures.standardState(engine)
        val move =
            Move(
                playerIndex = 1,
                pieceId = PieceCatalog.SINGLE_CELL_ID,
                anchorRow = 0,
                anchorCol = GameConstants.STANDARD_BOARD_SIZE - 1,
                orientationIndex = 0,
            )

        val preview = engine.previewPlacement(state, move)
        val applied = engine.applyMove(state, move)

        assertEquals(MoveRejectionReason.NOT_PLAYERS_TURN, preview.rejectionReason)
        assertTrue(!preview.isValid)
        assertRejected(applied, MoveRejectionReason.NOT_PLAYERS_TURN)
    }

    @Test
    fun firstMoveMustCoverAssignedStartingCorner() {
        val state = EngineTestFixtures.standardState(engine)

        val rejected =
            engine.applyMove(
                state,
                Move(
                    playerIndex = 0,
                    pieceId = PieceCatalog.SINGLE_CELL_ID,
                    anchorRow = 0,
                    anchorCol = 1,
                    orientationIndex = 0,
                ),
            )
        val accepted =
            engine.applyMove(
                state,
                Move(
                    playerIndex = 0,
                    pieceId = PieceCatalog.SINGLE_CELL_ID,
                    anchorRow = 0,
                    anchorCol = 0,
                    orientationIndex = 0,
                ),
            )

        assertRejected(rejected, MoveRejectionReason.START_CORNER_NOT_COVERED)
        assertTrue(accepted is MoveResult.Accepted)
    }

    @Test
    fun laterMoveMustTouchOwnPieceDiagonallyAndNotByEdge() {
        val afterFirst =
            engine
                .applyMove(
                    EngineTestFixtures.standardState(engine),
                    Move(
                        playerIndex = 0,
                        pieceId = PieceCatalog.SINGLE_CELL_ID,
                        anchorRow = 0,
                        anchorCol = 0,
                        orientationIndex = 0,
                    ),
                ).acceptedState()
                .copy(currentPlayerIndex = 0)

        val edgeTouch =
            engine.applyMove(
                afterFirst,
                Move(
                    playerIndex = 0,
                    pieceId = PieceCatalog.TWO_LINE_ID,
                    anchorRow = 0,
                    anchorCol = 1,
                    orientationIndex = 0,
                ),
            )
        val diagonalTouch =
            engine.applyMove(
                afterFirst,
                Move(
                    playerIndex = 0,
                    pieceId = PieceCatalog.TWO_LINE_ID,
                    anchorRow = 1,
                    anchorCol = 1,
                    orientationIndex = 0,
                ),
            )

        assertRejected(edgeTouch, MoveRejectionReason.SAME_PLAYER_EDGE_TOUCH)
        assertTrue(diagonalTouch is MoveResult.Accepted)
    }

    @Test
    fun opponentEdgeContactIsAllowed() {
        val board =
            MutableBoard(GameConstants.STANDARD_BOARD_SIZE).apply {
                set(row = 0, col = 0, value = 0)
                set(row = 1, col = 3, value = 1)
            }
        val standardState = EngineTestFixtures.standardState(engine)
        val state =
            standardState.copy(
                board = board.toSnapshot(),
                players =
                    standardState.players.map { player ->
                        when (player.index) {
                            0 -> player.copy(usedPieceIds = setOf(PieceCatalog.SINGLE_CELL_ID))
                            1 -> player.copy(usedPieceIds = setOf(PieceCatalog.SINGLE_CELL_ID))
                            else -> player
                        }
                    },
                currentPlayerIndex = 1,
            )

        val result =
            engine.applyMove(
                state,
                Move(
                    playerIndex = 1,
                    pieceId = PieceCatalog.TWO_LINE_ID,
                    anchorRow = 0,
                    anchorCol = 1,
                    orientationIndex = 0,
                ),
            )

        assertTrue(result is MoveResult.Accepted)
    }

    private fun MoveResult.acceptedState(): GameState = (this as MoveResult.Accepted).state

    private fun assertRejected(
        result: MoveResult,
        expectedReason: MoveRejectionReason,
    ) {
        assertTrue(result is MoveResult.Rejected)
        assertEquals(expectedReason, (result as MoveResult.Rejected).reason)
    }
}
