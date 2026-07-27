package com.finnvek.cornersapart.ui.util

import com.finnvek.cornersapart.R
import com.finnvek.cornersapart.model.PieceCatalog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class PieceNameResourcesTest {
    @Test
    fun everyCatalogPieceHasItsOwnLocalizedNameResource() {
        val nameResources = PieceCatalog.all.map { piece -> piece.displayNameResource() }

        nameResources.forEach { resourceId ->
            assertNotEquals(R.string.piece_name_unknown, resourceId)
        }
        assertEquals(PieceCatalog.all.size, nameResources.distinct().size)
    }
}
