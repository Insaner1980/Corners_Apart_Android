package com.finnvek.cornersapart.engine

import com.finnvek.cornersapart.model.CellPosition

object CellNeighbors {
    fun orthogonal(position: CellPosition): List<CellPosition> =
        listOf(
            CellPosition(position.row - 1, position.col),
            CellPosition(position.row + 1, position.col),
            CellPosition(position.row, position.col - 1),
            CellPosition(position.row, position.col + 1),
        )

    fun diagonal(position: CellPosition): List<CellPosition> =
        listOf(
            CellPosition(position.row - 1, position.col - 1),
            CellPosition(position.row - 1, position.col + 1),
            CellPosition(position.row + 1, position.col - 1),
            CellPosition(position.row + 1, position.col + 1),
        )
}
