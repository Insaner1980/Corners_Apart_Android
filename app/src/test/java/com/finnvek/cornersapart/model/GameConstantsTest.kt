package com.finnvek.cornersapart.model

import org.junit.Assert.assertEquals
import org.junit.Test

class GameConstantsTest {
    @Test
    fun constantsMatchReviewedSpecification() {
        assertEquals(20, GameConstants.STANDARD_BOARD_SIZE)
        assertEquals(14, GameConstants.COMPACT_BOARD_SIZE)
        assertEquals(3, GameConstants.BONUS_TILE_POINTS)
        assertEquals(10, GameConstants.COMPLETION_BONUS_POINTS)
    }

    @Test
    fun playerNamesFollowCornersApartPaletteOrder() {
        assertEquals(listOf("Pink", "Mango", "Cyan", "Lime"), GameConstants.PLAYER_NAMES)
    }
}
