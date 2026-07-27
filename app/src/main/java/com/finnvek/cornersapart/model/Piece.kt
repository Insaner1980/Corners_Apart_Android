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
    const val THREE_BEND_ID = "three-corner"

    val all: List<PieceDef> =
        listOf(
            piece(SINGLE_CELL_ID, 0 to 0),
            piece(TWO_LINE_ID, 0 to 0, 0 to 1),
            piece("three-bar", 0 to 0, 0 to 1, 0 to 2),
            piece(THREE_BEND_ID, 0 to 0, 1 to 0, 1 to 1),
            piece("four-bar", 0 to 0, 0 to 1, 0 to 2, 0 to 3),
            piece("four-block", 0 to 0, 0 to 1, 1 to 0, 1 to 1),
            piece("four-tee", 0 to 0, 0 to 1, 0 to 2, 1 to 1),
            piece("four-corner", 0 to 0, 1 to 0, 2 to 0, 2 to 1),
            piece("four-step", 0 to 1, 0 to 2, 1 to 0, 1 to 1),
            piece("five-bar", 0 to 0, 0 to 1, 0 to 2, 0 to 3, 0 to 4),
            piece("five-block-tail", 0 to 0, 0 to 1, 1 to 0, 1 to 1, 2 to 0),
            piece("five-tee", 0 to 0, 0 to 1, 0 to 2, 1 to 1, 2 to 1),
            piece("five-cross", 0 to 1, 1 to 0, 1 to 1, 1 to 2, 2 to 1),
            piece("five-long-corner", 0 to 0, 1 to 0, 2 to 0, 3 to 0, 3 to 1),
            piece("five-shift", 0 to 1, 1 to 0, 1 to 1, 1 to 2, 2 to 2),
            piece("five-stair", 0 to 0, 1 to 0, 1 to 1, 2 to 1, 2 to 2),
            piece("five-cup", 0 to 0, 0 to 2, 1 to 0, 1 to 1, 1 to 2),
            piece("five-wide-corner", 0 to 0, 1 to 0, 2 to 0, 2 to 1, 2 to 2),
            piece("five-hook", 0 to 0, 1 to 0, 2 to 0, 3 to 0, 1 to 1),
            piece("five-zag", 0 to 0, 0 to 1, 1 to 1, 2 to 1, 2 to 2),
            piece("five-offset", 0 to 2, 1 to 0, 1 to 1, 1 to 2, 2 to 0),
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
