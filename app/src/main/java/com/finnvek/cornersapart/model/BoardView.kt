package com.finnvek.cornersapart.model

interface BoardView {
    val size: Int

    fun cellAt(index: Int): Int

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
    ): Int = cellAt(index(row, col))

    fun get(position: CellPosition): Int = get(row = position.row, col = position.col)
}
