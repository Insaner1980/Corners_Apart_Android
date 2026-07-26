package com.finnvek.cornersapart.model

import org.junit.Assert.assertEquals
import org.junit.Test

class PlacementCellsTest {
    @Test
    fun targetCellsUseAnchorPlusEveryOrientationOffset() {
        val offsets =
            listOf(
                CellOffset(row = 0, col = 1),
                CellOffset(row = 2, col = 0),
                CellOffset(row = 2, col = 1),
            )

        val targets = targetCells(anchorRow = 4, anchorCol = 7, offsets = offsets)

        assertEquals(
            listOf(
                CellPosition(row = 4, col = 8),
                CellPosition(row = 6, col = 7),
                CellPosition(row = 6, col = 8),
            ),
            targets,
        )
    }
}
