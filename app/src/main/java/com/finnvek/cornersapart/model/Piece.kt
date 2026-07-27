package com.finnvek.cornersapart.model

import kotlinx.serialization.Serializable

@Serializable
data class PieceDef(
    val id: String,
    val cells: List<CellOffset>,
)

object PieceCatalog {
    const val SINGLE_CELL_ID = "one-dot"
    const val TWO_LINE_ID = "two-bar"
    const val THREE_LINE_ID = "three-bar"
    const val THREE_BEND_ID = "three-corner"
    const val FOUR_LINE_ID = "four-bar"
    const val FOUR_BLOCK_ID = "four-block"
    const val FOUR_TEE_ID = "four-tee"
    const val FOUR_CORNER_ID = "four-corner"
    const val FOUR_STEP_ID = "four-step"
    const val FIVE_LINE_ID = "five-bar"
    const val FIVE_BLOCK_TAIL_ID = "five-block-tail"
    const val FIVE_TEE_ID = "five-tee"
    const val FIVE_CROSS_ID = "five-cross"
    const val FIVE_LONG_CORNER_ID = "five-long-corner"
    const val FIVE_SHIFT_ID = "five-shift"
    const val FIVE_STAIR_ID = "five-stair"
    const val FIVE_CUP_ID = "five-cup"
    const val FIVE_WIDE_CORNER_ID = "five-wide-corner"
    const val FIVE_HOOK_ID = "five-hook"
    const val FIVE_ZAG_ID = "five-zag"
    const val FIVE_OFFSET_ID = "five-offset"

    val all: List<PieceDef> =
        listOf(
            piece(SINGLE_CELL_ID, 0 to 0),
            piece(TWO_LINE_ID, 0 to 0, 0 to 1),
            piece(THREE_LINE_ID, 0 to 0, 0 to 1, 0 to 2),
            piece(THREE_BEND_ID, 0 to 0, 1 to 0, 1 to 1),
            piece(FOUR_LINE_ID, 0 to 0, 0 to 1, 0 to 2, 0 to 3),
            piece(FOUR_BLOCK_ID, 0 to 0, 0 to 1, 1 to 0, 1 to 1),
            piece(FOUR_TEE_ID, 0 to 0, 0 to 1, 0 to 2, 1 to 1),
            piece(FOUR_CORNER_ID, 0 to 0, 1 to 0, 2 to 0, 2 to 1),
            piece(FOUR_STEP_ID, 0 to 1, 0 to 2, 1 to 0, 1 to 1),
            piece(FIVE_LINE_ID, 0 to 0, 0 to 1, 0 to 2, 0 to 3, 0 to 4),
            piece(FIVE_BLOCK_TAIL_ID, 0 to 0, 0 to 1, 1 to 0, 1 to 1, 2 to 0),
            piece(FIVE_TEE_ID, 0 to 0, 0 to 1, 0 to 2, 1 to 1, 2 to 1),
            piece(FIVE_CROSS_ID, 0 to 1, 1 to 0, 1 to 1, 1 to 2, 2 to 1),
            piece(FIVE_LONG_CORNER_ID, 0 to 0, 1 to 0, 2 to 0, 3 to 0, 3 to 1),
            piece(FIVE_SHIFT_ID, 0 to 1, 1 to 0, 1 to 1, 1 to 2, 2 to 2),
            piece(FIVE_STAIR_ID, 0 to 0, 1 to 0, 1 to 1, 2 to 1, 2 to 2),
            piece(FIVE_CUP_ID, 0 to 0, 0 to 2, 1 to 0, 1 to 1, 1 to 2),
            piece(FIVE_WIDE_CORNER_ID, 0 to 0, 1 to 0, 2 to 0, 2 to 1, 2 to 2),
            piece(FIVE_HOOK_ID, 0 to 0, 1 to 0, 2 to 0, 3 to 0, 1 to 1),
            piece(FIVE_ZAG_ID, 0 to 0, 0 to 1, 1 to 1, 2 to 1, 2 to 2),
            piece(FIVE_OFFSET_ID, 0 to 2, 1 to 0, 1 to 1, 1 to 2, 2 to 0),
        ).toSnapshotList()

    private val byId: Map<String, PieceDef> =
        all.associateBy { piece -> piece.id }

    fun find(id: String): PieceDef? = byId[id]

    fun require(id: String): PieceDef = requireNotNull(find(id)) { "Unknown piece id: $id" }

    private fun piece(
        id: String,
        vararg cells: Pair<Int, Int>,
    ): PieceDef =
        PieceDef(
            id = id,
            cells = cells.map { cell -> CellOffset(row = cell.first, col = cell.second) }.toSnapshotList(),
        )
}
