package com.finnvek.cornersapart.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PieceCatalogTest {
    @Test
    fun catalogContainsTwentyOnePiecesAndEightyNineCells() {
        assertEquals(GameConstants.PIECE_COUNT, PieceCatalog.all.size)
        assertEquals(GameConstants.TOTAL_PIECE_CELLS, PieceCatalog.all.sumOf { piece -> piece.cells.size })
        assertEquals(
            PieceCatalog.all.size,
            PieceCatalog.all
                .map { piece -> piece.id }
                .distinct()
                .size,
        )
    }

    @Test
    fun orientationsAreNormalizedUniqueAndCappedAtEight() {
        val orientationsByPiece =
            PieceCatalog.all.associate { piece ->
                piece.id to PieceTransforms.getAllOrientations(piece)
            }

        orientationsByPiece.values.forEach { orientations ->
            assertTrue(orientations.isNotEmpty())
            assertTrue(orientations.size <= PieceTransforms.MAX_ORIENTATIONS)
            assertEquals(orientations.size, orientations.distinct().size)
            orientations.forEach { orientation ->
                assertEquals(orientation, PieceTransforms.normalize(orientation))
            }
        }
        assertTrue(
            orientationsByPiece.values.any { orientations ->
                orientations.size ==
                    PieceTransforms.MAX_ORIENTATIONS
            },
        )
    }
}
