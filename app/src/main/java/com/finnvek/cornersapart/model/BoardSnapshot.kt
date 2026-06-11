package com.finnvek.cornersapart.model

import kotlinx.serialization.Serializable

@Serializable
data class BoardSnapshot(
    val size: Int,
    val cells: List<Int>,
) {
    init {
        require(size > 0) { "Board size must be positive." }
        require(cells.size == size * size) { "Board cells must match board size." }
    }

    fun contains(
        row: Int,
        col: Int,
    ): Boolean = row in 0 until size && col in 0 until size

    fun contains(position: CellPosition): Boolean = contains(row = position.row, col = position.col)

    fun index(
        row: Int,
        col: Int,
    ): Int {
        require(contains(row, col)) { "Cell is outside board bounds." }
        return row * size + col
    }

    fun get(
        row: Int,
        col: Int,
    ): Int = cells[index(row, col)]

    fun get(position: CellPosition): Int = get(row = position.row, col = position.col)

    companion object {
        const val EMPTY = -1

        fun empty(size: Int): BoardSnapshot =
            BoardSnapshot(
                size = size,
                cells = List(size * size) { EMPTY },
            )
    }
}
