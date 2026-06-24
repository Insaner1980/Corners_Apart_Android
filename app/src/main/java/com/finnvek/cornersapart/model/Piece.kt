package com.finnvek.cornersapart.model

import kotlinx.serialization.Serializable

@Serializable
data class PieceDef(
    val id: String,
    val displayName: String,
    val cells: List<CellOffset>,
)

object PieceCatalog {
    const val SINGLE_CELL_ID = "one-dot"
    const val TWO_LINE_ID = "two-bar"
    const val THREE_BEND_ID = "three-corner"

    val all: List<PieceDef> =
        listOf(
            piece(SINGLE_CELL_ID, "One Dot", 0 to 0),
            piece(TWO_LINE_ID, "Two Bar", 0 to 0, 0 to 1),
            piece("three-bar", "Three Bar", 0 to 0, 0 to 1, 0 to 2),
            piece(THREE_BEND_ID, "Three Corner", 0 to 0, 1 to 0, 1 to 1),
            piece("four-bar", "Four Bar", 0 to 0, 0 to 1, 0 to 2, 0 to 3),
            piece("four-block", "Four Block", 0 to 0, 0 to 1, 1 to 0, 1 to 1),
            piece("four-tee", "Four Tee", 0 to 0, 0 to 1, 0 to 2, 1 to 1),
            piece("four-corner", "Four Corner", 0 to 0, 1 to 0, 2 to 0, 2 to 1),
            piece("four-step", "Four Step", 0 to 1, 0 to 2, 1 to 0, 1 to 1),
            piece("five-bar", "Five Bar", 0 to 0, 0 to 1, 0 to 2, 0 to 3, 0 to 4),
            piece("five-block-tail", "Five Block Tail", 0 to 0, 0 to 1, 1 to 0, 1 to 1, 2 to 0),
            piece("five-tee", "Five Tee", 0 to 0, 0 to 1, 0 to 2, 1 to 1, 2 to 1),
            piece("five-cross", "Five Cross", 0 to 1, 1 to 0, 1 to 1, 1 to 2, 2 to 1),
            piece("five-long-corner", "Five Long Corner", 0 to 0, 1 to 0, 2 to 0, 3 to 0, 3 to 1),
            piece("five-shift", "Five Shift", 0 to 1, 1 to 0, 1 to 1, 1 to 2, 2 to 2),
            piece("five-stair", "Five Stair", 0 to 0, 1 to 0, 1 to 1, 2 to 1, 2 to 2),
            piece("five-cup", "Five Cup", 0 to 0, 0 to 2, 1 to 0, 1 to 1, 1 to 2),
            piece("five-wide-corner", "Five Wide Corner", 0 to 0, 1 to 0, 2 to 0, 2 to 1, 2 to 2),
            piece("five-hook", "Five Hook", 0 to 0, 1 to 0, 2 to 0, 3 to 0, 1 to 1),
            piece("five-zag", "Five Zag", 0 to 0, 0 to 1, 1 to 1, 2 to 1, 2 to 2),
            piece("five-offset", "Five Offset", 0 to 2, 1 to 0, 1 to 1, 1 to 2, 2 to 0),
        ).toSnapshotList()

    private val byId: Map<String, PieceDef> =
        all.associateBy { piece -> piece.id }

    fun find(id: String): PieceDef? = byId[id]

    fun require(id: String): PieceDef = requireNotNull(find(id)) { "Unknown piece id: $id" }

    private fun piece(
        id: String,
        displayName: String,
        vararg cells: Pair<Int, Int>,
    ): PieceDef =
        PieceDef(
            id = id,
            displayName = displayName,
            cells = cells.map { cell -> CellOffset(row = cell.first, col = cell.second) }.toSnapshotList(),
        )
}
