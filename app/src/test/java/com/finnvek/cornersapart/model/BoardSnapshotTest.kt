package com.finnvek.cornersapart.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class BoardSnapshotTest {
    @Test
    fun boardSnapshotUsesFlatCellValueEquality() {
        val first = BoardSnapshot(size = 2, cells = listOf(-1, 0, 1, -1))
        val second = BoardSnapshot(size = 2, cells = listOf(-1, 0, 1, -1))
        val different = BoardSnapshot(size = 2, cells = listOf(-1, 0, -1, 1))

        assertEquals(first, second)
        assertEquals(first.hashCode(), second.hashCode())
        assertNotEquals(first, different)
        assertEquals(1, first.get(row = 1, col = 0))
    }

    @Test
    fun mutableBoardUsesContentEqualityAndConvertsToSnapshot() {
        val first =
            MutableBoard(size = 3).apply {
                set(row = 1, col = 1, value = 2)
            }
        val second =
            MutableBoard(size = 3).apply {
                set(row = 1, col = 1, value = 2)
            }

        assertEquals(first, second)
        assertEquals(first.hashCode(), second.hashCode())
        assertEquals(BoardSnapshot(size = 3, cells = listOf(-1, -1, -1, -1, 2, -1, -1, -1, -1)), first.toSnapshot())
    }
}
