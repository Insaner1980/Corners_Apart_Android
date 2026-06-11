package com.finnvek.cornersapart.model

class MutableBoard(
    val size: Int,
    private val cells: IntArray = IntArray(size * size) { BoardSnapshot.EMPTY },
) {
    init {
        require(size > 0) { "Board size must be positive." }
        require(cells.size == size * size) { "Board cells must match board size." }
    }

    constructor(snapshot: BoardSnapshot) : this(
        size = snapshot.size,
        cells = snapshot.cells.toIntArray(),
    )

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
