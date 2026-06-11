package com.finnvek.cornersapart.model

import kotlinx.serialization.Serializable

@Serializable
data class Player(
    val index: Int,
    val name: String,
    val colorIndex: Int,
    val startCorner: CellPosition,
    val usedPieceIds: Set<String> = emptySet(),
    val scoreBreakdown: ScoreBreakdown = ScoreBreakdown(),
    val passed: Boolean = false,
    val isActiveScoring: Boolean = true,
    val isComputerControlled: Boolean = false,
    val ownerIndex: Int = index,
) {
    val hasPlacedAnyPiece: Boolean
        get() = usedPieceIds.isNotEmpty()
}
