package com.finnvek.cornersapart.ui.util

import com.finnvek.cornersapart.R
import com.finnvek.cornersapart.model.PieceCatalog
import org.junit.Assert.assertEquals
import org.junit.Test

class PieceNameResourcesTest {
    @Test
    fun everyCatalogPieceHasItsOwnLocalizedNameResource() {
        val expectedResources =
            mapOf(
                PieceCatalog.SINGLE_CELL_ID to R.string.piece_name_one_dot,
                PieceCatalog.TWO_LINE_ID to R.string.piece_name_two_bar,
                PieceCatalog.THREE_LINE_ID to R.string.piece_name_three_bar,
                PieceCatalog.THREE_BEND_ID to R.string.piece_name_three_corner,
                PieceCatalog.FOUR_LINE_ID to R.string.piece_name_four_bar,
                PieceCatalog.FOUR_BLOCK_ID to R.string.piece_name_four_block,
                PieceCatalog.FOUR_TEE_ID to R.string.piece_name_four_tee,
                PieceCatalog.FOUR_CORNER_ID to R.string.piece_name_four_corner,
                PieceCatalog.FOUR_STEP_ID to R.string.piece_name_four_step,
                PieceCatalog.FIVE_LINE_ID to R.string.piece_name_five_bar,
                PieceCatalog.FIVE_BLOCK_TAIL_ID to R.string.piece_name_five_block_tail,
                PieceCatalog.FIVE_TEE_ID to R.string.piece_name_five_tee,
                PieceCatalog.FIVE_CROSS_ID to R.string.piece_name_five_cross,
                PieceCatalog.FIVE_LONG_CORNER_ID to R.string.piece_name_five_long_corner,
                PieceCatalog.FIVE_SHIFT_ID to R.string.piece_name_five_shift,
                PieceCatalog.FIVE_STAIR_ID to R.string.piece_name_five_stair,
                PieceCatalog.FIVE_CUP_ID to R.string.piece_name_five_cup,
                PieceCatalog.FIVE_WIDE_CORNER_ID to R.string.piece_name_five_wide_corner,
                PieceCatalog.FIVE_HOOK_ID to R.string.piece_name_five_hook,
                PieceCatalog.FIVE_ZAG_ID to R.string.piece_name_five_zag,
                PieceCatalog.FIVE_OFFSET_ID to R.string.piece_name_five_offset,
            )
        val actualResources =
            PieceCatalog.all.associate { piece ->
                piece.id to piece.displayNameResource()
            }

        assertEquals(expectedResources, actualResources)
    }
}
