package com.finnvek.cornersapart.engine

import com.finnvek.cornersapart.model.GameConfig
import com.finnvek.cornersapart.model.GameMode
import com.finnvek.cornersapart.model.Move
import com.finnvek.cornersapart.model.MutableBoard
import com.finnvek.cornersapart.model.PieceCatalog
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GameEngineGameOverTest {
    private val engine = GameEngine()

    @Test
    fun lastMobilePlayerBlockingOwnRemainingMoveEndsGame() {
        val retainedPieces = setOf(PieceCatalog.SINGLE_CELL_ID, PieceCatalog.TWO_LINE_ID)
        val baseState =
            engine.newGame(
                GameConfig(
                    mode = GameMode.FOUR_PLAYER,
                    boardSize = 3,
                    randomSeed = 71L,
                    bonusTiles = emptyList(),
                ),
            )
        val state =
            baseState.copy(
                board =
                    MutableBoard(size = 3)
                        .apply {
                            set(row = 0, col = 0, value = 0)
                            set(row = 0, col = 2, value = 1)
                        }.toSnapshot(),
                players =
                    baseState.players.map { player ->
                        if (player.index == 0) {
                            player.copy(
                                usedPieceIds =
                                    PieceCatalog.all
                                        .map { piece -> piece.id }
                                        .filterNot { pieceId -> pieceId in retainedPieces }
                                        .toSet(),
                            )
                        } else {
                            player.copy(passed = true)
                        }
                    },
            )

        assertTrue(engine.hasValidMove(state, playerIndex = 0))

        val result =
            engine.applyMove(
                state,
                Move(
                    playerIndex = 0,
                    pieceId = PieceCatalog.TWO_LINE_ID,
                    anchorRow = 1,
                    anchorCol = 1,
                    orientationIndex = 1,
                ),
            ) as MoveResult.Accepted

        assertTrue(result.state.isGameOver)
        assertFalse(engine.hasValidMove(result.state.copy(isGameOver = false), playerIndex = 0))
    }
}
