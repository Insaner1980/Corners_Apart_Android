package com.finnvek.cornersapart.model

import kotlinx.serialization.Serializable

@Serializable
data class BonusTile(
    val row: Int,
    val col: Int,
    val claimedByPlayerIndex: Int? = null,
    val claimedOnTurn: Int? = null,
) {
    val position: CellPosition
        get() = CellPosition(row = row, col = col)
}
