package com.finnvek.cornersapart.model

import kotlinx.serialization.Serializable

@Serializable
data class CellPosition(
    val row: Int,
    val col: Int,
) {
    fun translated(offset: CellOffset): CellPosition =
        CellPosition(
            row = row + offset.row,
            col = col + offset.col,
        )
}

@Serializable
data class CellOffset(
    val row: Int,
    val col: Int,
)
