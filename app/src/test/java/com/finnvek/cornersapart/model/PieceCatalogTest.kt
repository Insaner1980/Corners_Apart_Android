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

    @Test
    @Suppress("UNCHECKED_CAST")
    fun returnedOrientationsCannotMutateCachedOrientations() {
        val piece = PieceCatalog.require(PieceCatalog.THREE_BEND_ID)
        val expectedOrientations = PieceTransforms.getAllOrientations(piece).map { orientation -> orientation.toList() }
        val returnedOrientations = PieceTransforms.getAllOrientations(piece)

        runCatching {
            (returnedOrientations.first() as MutableList<CellOffset>).clear()
        }
        runCatching {
            (returnedOrientations as MutableList<List<CellOffset>>).clear()
        }

        assertEquals(expectedOrientations, PieceTransforms.getAllOrientations(piece))
    }
}
