package com.finnvek.cornersapart.engine

import com.finnvek.cornersapart.model.BonusTile
import com.finnvek.cornersapart.model.CellPosition
import com.finnvek.cornersapart.model.GameConfig
import com.finnvek.cornersapart.model.GameConstants
import com.finnvek.cornersapart.model.GameMode
import com.finnvek.cornersapart.model.GameModeConfigs
import com.finnvek.cornersapart.model.Move
import com.finnvek.cornersapart.model.PieceCatalog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GameEngineModeTest {
    private val engine = GameEngine()

    @Test
    fun twoColorDuelKeepsFourColorTurnOrderWithTwoOwners() {
        val state =
            engine.newGame(
                GameModeConfigs.defaultGameConfig(
                    mode = GameMode.TWO_COLOR_DUEL,
                    randomSeed = 17L,
                    bonusTiles = emptyList(),
                ),
            )

        assertEquals(GameConstants.STANDARD_BOARD_SIZE, state.board.size)
        assertEquals(listOf(0, 1, 2, 3), state.players.map { player -> player.index })
        assertEquals(listOf(0, 1, 2, 3), state.players.map { player -> player.colorIndex })
        assertEquals(listOf(0, 1, 0, 1), state.players.map { player -> player.ownerIndex })
        assertEquals(standardCorners(), state.players.map { player -> player.startCorner })

        val afterColorZero =
            engine
                .applyMove(
                    state,
                    Move(
                        playerIndex = 0,
                        pieceId = PieceCatalog.SINGLE_CELL_ID,
                        anchorRow = 0,
                        anchorCol = 0,
                        orientationIndex = 0,
                    ),
                ).acceptedState()
        val afterColorOne =
            engine
                .applyMove(
                    afterColorZero,
                    Move(
                        playerIndex = 1,
                        pieceId = PieceCatalog.SINGLE_CELL_ID,
                        anchorRow = 0,
                        anchorCol = 19,
                        orientationIndex = 0,
                    ),
                ).acceptedState()

        assertEquals(1, afterColorZero.currentPlayerIndex)
        assertEquals(2, afterColorOne.currentPlayerIndex)
    }

    @Test
    fun compactDuelUsesFourteenByFourteenBoardAndCornerStarts() {
        val state =
            engine.newGame(
                GameModeConfigs.defaultGameConfig(
                    mode = GameMode.COMPACT_DUEL,
                    randomSeed = 19L,
                    bonusTiles = emptyList(),
                ),
            )

        assertEquals(GameConstants.COMPACT_BOARD_SIZE, state.board.size)
        assertEquals(2, state.players.size)
        assertEquals(
            listOf(CellPosition(0, 0), CellPosition(13, 13)),
            state.players.map { player ->
                player.startCorner
            },
        )

        val afterFirst =
            engine
                .applyMove(
                    state,
                    Move(
                        playerIndex = 0,
                        pieceId = PieceCatalog.SINGLE_CELL_ID,
                        anchorRow = 0,
                        anchorCol = 0,
                        orientationIndex = 0,
                    ),
                ).acceptedState()

        assertEquals(1, afterFirst.currentPlayerIndex)
    }

    @Test
    fun threePlayerModeCreatesThreeActiveScoringColors() {
        val state =
            engine.newGame(
                GameModeConfigs.defaultGameConfig(
                    mode = GameMode.THREE_PLAYER,
                    randomSeed = 23L,
                    bonusTiles = emptyList(),
                ),
            )

        assertEquals(3, state.players.size)
        assertEquals(listOf(0, 1, 2), state.players.map { player -> player.index })
        assertTrue(state.players.all { player -> player.isActiveScoring })
        assertEquals(standardCorners().take(3), state.players.map { player -> player.startCorner })
    }

    @Test
    fun newGameTakesSnapshotOfConfiguredBonusTiles() {
        val configuredBonusTiles = mutableListOf(BonusTile(row = 4, col = 4))

        val state =
            engine.newGame(
                GameConfig(
                    mode = GameMode.FOUR_PLAYER,
                    randomSeed = 29L,
                    bonusTiles = configuredBonusTiles,
                ),
            )
        configuredBonusTiles.clear()

        assertEquals(listOf(BonusTile(row = 4, col = 4)), state.bonusTiles)
    }

    private fun MoveResult.acceptedState(): com.finnvek.cornersapart.model.GameState {
        assertTrue(this is MoveResult.Accepted)
        return (this as MoveResult.Accepted).state
    }

    private fun standardCorners(): List<CellPosition> =
        GameConstants.STANDARD_CORNERS.map { corner ->
            CellPosition(corner.first, corner.second)
        }
}
