package com.finnvek.cornersapart.model

import org.junit.Assert.assertEquals
import org.junit.Test

class GameConstantsTest {
    @Test
    fun constantsMatchReviewedSpecification() {
        assertEquals(20, GameConstants.STANDARD_BOARD_SIZE)
        assertEquals(14, GameConstants.COMPACT_BOARD_SIZE)
        assertEquals(4, GameConstants.PLAYER_COUNT)
        assertEquals(21, GameConstants.PIECE_COUNT)
        assertEquals(89, GameConstants.TOTAL_PIECE_CELLS)
        assertEquals(3, GameConstants.BONUS_TILE_POINTS)
        assertEquals(10, GameConstants.COMPLETION_BONUS_POINTS)
    }

    @Test
    fun playerNamesFollowCornersApartPaletteOrder() {
        assertEquals(listOf("Indigo", "Amber", "Coral", "Teal"), GameConstants.PLAYER_NAMES)
    }
}
