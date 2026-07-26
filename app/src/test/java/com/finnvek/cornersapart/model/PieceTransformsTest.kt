package com.finnvek.cornersapart.model

import org.junit.Assert.assertEquals
import org.junit.Test

class PieceTransformsTest {
    @Test
    fun clockwiseThenCounterclockwiseIsIdentityAfterNormalization() {
        val cells =
            listOf(
                CellOffset(row = 0, col = 0),
                CellOffset(row = 1, col = 0),
                CellOffset(row = 1, col = 1),
                CellOffset(row = 2, col = 1),
            )

        val transformed = PieceTransforms.rotateCCW(PieceTransforms.rotateCW(cells))

        assertEquals(PieceTransforms.normalize(cells), PieceTransforms.normalize(transformed))
    }

    @Test
    fun flippingTwiceIsIdentity() {
        val cells =
            listOf(
                CellOffset(row = -1, col = 2),
                CellOffset(row = 0, col = 2),
                CellOffset(row = 0, col = 3),
            )

        assertEquals(cells, PieceTransforms.flipH(PieceTransforms.flipH(cells)))
    }
}
