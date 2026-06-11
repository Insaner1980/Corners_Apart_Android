package com.finnvek.cornersapart.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GameModeConfigTest {
    @Test
    fun defaultModeConfigsMatchSpecPlayerSlots() {
        assertModeConfig(
            mode = GameMode.SOLO,
            boardSize = GameConstants.STANDARD_BOARD_SIZE,
            bonusTileCount = GameConstants.STANDARD_BONUS_TILE_COUNT,
            corners = listOf(19 to 19, 0 to 0, 0 to 19, 19 to 0),
            ownerIndexes = listOf(0, 1, 2, 3),
            computerControlled = listOf(false, true, true, true),
        )
        assertModeConfig(
            mode = GameMode.TWO_COLOR_DUEL,
            boardSize = GameConstants.STANDARD_BOARD_SIZE,
            bonusTileCount = GameConstants.STANDARD_BONUS_TILE_COUNT,
            corners = GameConstants.STANDARD_CORNERS,
            ownerIndexes = listOf(0, 1, 0, 1),
            computerControlled = listOf(false, false, false, false),
        )
        assertModeConfig(
            mode = GameMode.COMPACT_DUEL,
            boardSize = GameConstants.COMPACT_BOARD_SIZE,
            bonusTileCount = GameConstants.COMPACT_BONUS_TILE_COUNT,
            corners = GameConstants.COMPACT_DUEL_CORNERS,
            ownerIndexes = listOf(0, 1),
            computerControlled = listOf(false, false),
        )
        assertModeConfig(
            mode = GameMode.THREE_PLAYER,
            boardSize = GameConstants.STANDARD_BOARD_SIZE,
            bonusTileCount = GameConstants.STANDARD_BONUS_TILE_COUNT,
            corners = GameConstants.STANDARD_CORNERS.take(3),
            ownerIndexes = listOf(0, 1, 2),
            computerControlled = listOf(false, false, false),
        )
        assertModeConfig(
            mode = GameMode.FOUR_PLAYER,
            boardSize = GameConstants.STANDARD_BOARD_SIZE,
            bonusTileCount = GameConstants.STANDARD_BONUS_TILE_COUNT,
            corners = GameConstants.STANDARD_CORNERS,
            ownerIndexes = listOf(0, 1, 2, 3),
            computerControlled = listOf(false, false, false, false),
        )
    }

    @Test
    fun compactDuelIsExplicitlyMarkedForPlayTesting() {
        assertTrue(GameModeConfigs.forMode(GameMode.COMPACT_DUEL).requiresPlayTesting)
        GameMode.entries
            .filterNot { mode -> mode == GameMode.COMPACT_DUEL }
            .forEach { mode -> assertFalse(GameModeConfigs.forMode(mode).requiresPlayTesting) }
    }

    @Test
    fun defaultGameConfigUsesModeBoardAndBonusDefaults() {
        val compactConfig =
            GameModeConfigs.defaultGameConfig(
                mode = GameMode.COMPACT_DUEL,
                randomSeed = 123L,
                bonusTiles = emptyList(),
            )

        assertEquals(GameMode.COMPACT_DUEL, compactConfig.mode)
        assertEquals(GameConstants.COMPACT_BOARD_SIZE, compactConfig.boardSize)
        assertEquals(GameConstants.COMPACT_BONUS_TILE_COUNT, compactConfig.bonusTileCount)
        assertEquals(123L, compactConfig.randomSeed)
        assertEquals(emptyList<BonusTile>(), compactConfig.bonusTiles)
    }

    private fun assertModeConfig(
        mode: GameMode,
        boardSize: Int,
        bonusTileCount: Int,
        corners: List<Pair<Int, Int>>,
        ownerIndexes: List<Int>,
        computerControlled: List<Boolean>,
    ) {
        val config = GameModeConfigs.forMode(mode)

        assertEquals(boardSize, config.boardSize)
        assertEquals(bonusTileCount, config.bonusTileCount)
        assertEquals(
            corners.map { corner ->
                CellPosition(corner.first, corner.second)
            },
            config.playerSlots.map { slot -> slot.startCorner },
        )
        assertEquals(ownerIndexes, config.playerSlots.map { slot -> slot.ownerIndex })
        assertEquals(computerControlled, config.playerSlots.map { slot -> slot.isComputerControlled })
        assertEquals(config.playerSlots.indices.toList(), config.playerSlots.map { slot -> slot.index })
        assertEquals(config.playerSlots.indices.toList(), config.playerSlots.map { slot -> slot.colorIndex })
    }
}
