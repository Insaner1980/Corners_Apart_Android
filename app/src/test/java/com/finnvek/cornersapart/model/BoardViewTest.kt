package com.finnvek.cornersapart.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class BoardViewTest {
    @Test
    fun containsAcceptsOnlyCellsWithinBoardBounds() {
        val board = TestBoardView(size = 3)

        assertTrue(board.contains(row = 2, col = 2))
        assertTrue(board.contains(CellPosition(row = 0, col = 1)))
        assertFalse(board.contains(row = -1, col = 2))
        assertFalse(board.contains(CellPosition(row = 3, col = 0)))
    }

    @Test
    fun indexMapsCellToFlatOffsetAndRejectsOutOfBoundsCells() {
        val board = TestBoardView(size = 3)

        assertEquals(5, board.index(row = 1, col = 2))

        val error =
            assertThrows(IllegalArgumentException::class.java) {
                board.index(row = 3, col = 0)
            }
        assertEquals("Cell is outside board bounds.", error.message)
    }

    private class TestBoardView(
        override val size: Int,
    ) : BoardView {
        override fun cellAt(index: Int): Int = index
    }
}
