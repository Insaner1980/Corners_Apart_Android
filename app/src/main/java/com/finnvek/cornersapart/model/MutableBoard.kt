package com.finnvek.cornersapart.model

class MutableBoard(
    override val size: Int,
    private val cells: IntArray = IntArray(size * size) { BoardSnapshot.EMPTY },
) : BoardView {
    init {
        require(size > 0) { "Board size must be positive." }
        require(cells.size == size * size) { "Board cells must match board size." }
    }

    constructor(snapshot: BoardSnapshot) : this(
        size = snapshot.size,
        cells = snapshot.cells.toIntArray(),
    )

    override fun cellAt(index: Int): Int = cells[index]

    fun set(
        row: Int,
        col: Int,
        value: Int,
    ) {
        cells[index(row, col)] = value
    }

    fun set(
        position: CellPosition,
        value: Int,
    ) {
        set(row = position.row, col = position.col, value = value)
    }

    fun toSnapshot(): BoardSnapshot =
        BoardSnapshot(
            size = size,
            cells = cells.toList(),
        )

    override fun equals(other: Any?): Boolean =
        other is MutableBoard &&
            size == other.size &&
            cells.contentEquals(other.cells)

    override fun hashCode(): Int = 31 * size + cells.contentHashCode()
}
