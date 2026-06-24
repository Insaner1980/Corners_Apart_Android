package com.finnvek.cornersapart.model

import kotlinx.serialization.Serializable

@Serializable
data class BoardSnapshot(
    override val size: Int,
    val cells: List<Int>,
) : BoardView {
    init {
        require(size > 0) { "Board size must be positive." }
        require(cells.size == size * size) { "Board cells must match board size." }
    }

    override fun cellAt(index: Int): Int = cells[index]

    companion object {
        const val EMPTY = -1

        fun empty(size: Int): BoardSnapshot =
            BoardSnapshot(
                size = size,
                cells = List(size * size) { EMPTY }.toSnapshotList(),
            )
    }
}
