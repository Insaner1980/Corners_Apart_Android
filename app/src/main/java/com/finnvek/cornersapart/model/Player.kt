package com.finnvek.cornersapart.model

import kotlinx.serialization.Serializable

@Serializable
data class Player(
    val index: Int,
    val name: String,
    val colorIndex: Int,
    val startCorner: CellPosition,
    val usedPieceIds: Set<String>,
    val scoreBreakdown: ScoreBreakdown,
    val passed: Boolean,
    val isActiveScoring: Boolean,
    val isComputerControlled: Boolean,
    val ownerIndex: Int,
) {
    val hasPlacedAnyPiece: Boolean
        get() = usedPieceIds.isNotEmpty()
}
