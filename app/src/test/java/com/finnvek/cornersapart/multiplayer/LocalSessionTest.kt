package com.finnvek.cornersapart.multiplayer

import com.finnvek.cornersapart.engine.GameEngine
import com.finnvek.cornersapart.model.GameConfig
import com.finnvek.cornersapart.model.GameConstants
import com.finnvek.cornersapart.model.GameMode
import com.finnvek.cornersapart.model.Move
import com.finnvek.cornersapart.model.PieceCatalog
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalSessionTest {
    @Test
    fun fourPlayerLocalMoveUpdatesAuthoritativeGameState() =
        runTest {
            val session =
                LocalSession(
                    engine = GameEngine(),
                    initialConfig =
                        GameConfig(
                            mode = GameMode.FOUR_PLAYER,
                            boardSize = GameConstants.STANDARD_BOARD_SIZE,
                            randomSeed = 17L,
                            bonusTiles = emptyList(),
                        ),
                )

            val result =
                session.sendMove(
                    Move(
                        playerIndex = 0,
                        pieceId = PieceCatalog.SINGLE_CELL_ID,
                        anchorRow = 0,
                        anchorCol = 0,
                        orientationIndex = 0,
                    ),
                )

            assertTrue(result.isSuccess)
            assertEquals(
                0,
                session.gameState.value.board
                    .get(row = 0, col = 0),
            )
            assertEquals(1, session.gameState.value.currentPlayerIndex)
            assertEquals(1, session.players.value[0].usedPieceCount)
        }

    @Test
    fun soloGameRunsComputerSlotsBackToHumanPlayer() =
        runTest {
            val session =
                LocalSession(
                    engine = GameEngine(),
                    initialConfig =
                        GameConfig(
                            mode = GameMode.SOLO,
                            boardSize = GameConstants.STANDARD_BOARD_SIZE,
                            randomSeed = 19L,
                            bonusTiles = emptyList(),
                        ),
                )

            val result =
                session.sendMove(
                    Move(
                        playerIndex = 0,
                        pieceId = PieceCatalog.SINGLE_CELL_ID,
                        anchorRow = 19,
                        anchorCol = 19,
                        orientationIndex = 0,
                    ),
                )

            assertTrue(result.isSuccess)
            assertEquals(0, session.gameState.value.currentPlayerIndex)
            assertTrue(
                session.players.value
                    .drop(1)
                    .all { player -> player.isComputerControlled },
            )
            assertTrue(
                session.gameState.value.players
                    .drop(1)
                    .all { player -> player.usedPieceIds.isNotEmpty() },
            )
        }
}
