package com.finnvek.cornersapart.engine

import com.finnvek.cornersapart.model.ScoreBreakdown

object ScoreFixtures {
    fun breakdown(
        totalCells: Int,
        bonusPoints: Int,
    ): ScoreBreakdown =
        ScoreBreakdown(
            placedCellPoints = totalCells,
            bonusTilePoints = bonusPoints,
        )
}
