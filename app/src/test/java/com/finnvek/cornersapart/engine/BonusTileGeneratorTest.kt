package com.finnvek.cornersapart.engine

import com.finnvek.cornersapart.model.CellPosition
import com.finnvek.cornersapart.model.GameConstants
import com.finnvek.cornersapart.model.GameMode
import com.finnvek.cornersapart.model.GameModeConfigs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class BonusTileGeneratorTest {
    @Test
    fun standardLayoutKeepsBonusesAwayFromCornersAndEachOther() {
        val layout =
            BonusTileGenerator.generate(
                mode = GameMode.FOUR_PLAYER,
                boardSize = GameConstants.STANDARD_BOARD_SIZE,
                randomSeed = 42L,
            )

        assertEquals(GameConstants.STANDARD_BONUS_TILE_COUNT, layout.positions.size)
        assertEquals(layout.positions.size, layout.positions.distinct().size)
        layout.positions.forEach { position ->
            assertFalse(position in GameConstants.STANDARD_CORNERS.map { CellPosition(it.first, it.second) })
            assertFalse(
                position in listOf(CellPosition(1, 1), CellPosition(1, 18), CellPosition(18, 18), CellPosition(18, 1)),
            )
        }
        layout.positions.forEachIndexed { index, position ->
            layout.positions.drop(index + 1).forEach { other ->
                assertTrue(chebyshevDistance(position, other) >= BonusTileGenerator.MIN_BONUS_DISTANCE)
            }
        }
    }

    @Test
    fun compactModeDefaultGameGeneratesCompactBonusTiles() {
        val state =
            GameEngine().newGame(
                GameModeConfigs.defaultGameConfig(
                    mode = GameMode.COMPACT_DUEL,
                    randomSeed = 43L,
                ),
            )
        val startCorners =
            GameModeConfigs
                .forMode(GameMode.COMPACT_DUEL)
                .playerSlots
                .map { slot -> slot.startCorner }
                .toSet()

        assertEquals(GameConstants.COMPACT_BOARD_SIZE, state.board.size)
        assertEquals(GameConstants.COMPACT_BONUS_TILE_COUNT, state.bonusTiles.size)
        assertEquals(
            state.bonusTiles.size,
            state.bonusTiles
                .map { tile -> tile.position }
                .distinct()
                .size,
        )
        state.bonusTiles.forEach { tile ->
            assertTrue(tile.row in 0 until GameConstants.COMPACT_BOARD_SIZE)
            assertTrue(tile.col in 0 until GameConstants.COMPACT_BOARD_SIZE)
            assertFalse(tile.position in startCorners)
        }
    }

    private fun chebyshevDistance(
        first: CellPosition,
        second: CellPosition,
    ): Int = maxOf(abs(first.row - second.row), abs(first.col - second.col))
}
