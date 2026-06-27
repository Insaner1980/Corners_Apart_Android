package com.finnvek.cornersapart.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
class BoardSnapshot private constructor(
    override val size: Int,
    @SerialName("cells")
    private val rawCells: List<Int>,
    @Transient
    private val trustedSnapshot: Boolean = true,
) : BoardView {
    constructor(size: Int, cells: List<Int>) : this(size, cells.toSnapshotList(), trustedSnapshot = true)

    @Transient
    private val snapshotCells: List<Int> = rawCells.toSnapshotList()

    val cells: List<Int>
        get() = snapshotCells

    init {
        require(size > 0) { "Board size must be positive." }
        require(cells.size == size * size) { "Board cells must match board size." }
    }

    override fun cellAt(index: Int): Int = cells[index]

    override fun equals(other: Any?): Boolean =
        other is BoardSnapshot &&
            size == other.size &&
            cells == other.cells

    override fun hashCode(): Int = 31 * size + cells.hashCode()

    override fun toString(): String = "BoardSnapshot(size=$size, cells=$cells)"

    companion object {
        const val EMPTY = -1

        fun empty(size: Int): BoardSnapshot =
            BoardSnapshot(
                size = size,
                cells = List(size * size) { EMPTY }.toSnapshotList(),
            )
    }
}
