package com.finnvek.cornersapart.model

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertThrows
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

    @Test
    fun boardSnapshotTakesSnapshotOfMutableCellsInput() {
        val cells = mutableListOf(-1, -1, -1, -1)
        val snapshot = BoardSnapshot(size = 2, cells = cells)

        cells[0] = 3

        assertEquals(BoardSnapshot.EMPTY, snapshot.get(row = 0, col = 0))
        assertThrows(UnsupportedOperationException::class.java) {
            @Suppress("UNCHECKED_CAST")
            (snapshot.cells as MutableList<Int>)[0] = 3
        }
    }

    @Test
    fun jsonDeserializationRejectsInvalidBoardShapes() {
        listOf(
            "{\"size\":0,\"cells\":[]}",
            "{\"size\":2,\"cells\":[-1]}",
            "{\"size\":65536,\"cells\":[]}",
        ).forEach { payload ->
            assertThrows(IllegalArgumentException::class.java) {
                Json.decodeFromString<BoardSnapshot>(payload)
            }
        }
    }
}
