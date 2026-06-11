package com.finnvek.cornersapart.opponents

import com.finnvek.cornersapart.model.Move

data class MoveCandidate(
    val move: Move,
    val placedCellCount: Int,
    val claimedBonusTileCount: Int,
)

data class MoveEvaluation(
    val placedCellScore: Double,
    val bonusScore: Double,
    val spreadScore: Double,
    val centerScore: Double,
    val blockingScore: Double,
) {
    val total: Double
        get() = placedCellScore + bonusScore + spreadScore + centerScore + blockingScore
}
