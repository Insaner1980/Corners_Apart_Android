package com.finnvek.cornersapart.model

import com.finnvek.cornersapart.engine.ScoreDelta
import kotlinx.serialization.Serializable

@Serializable
data class ScoreBreakdown(
    val placedCellPoints: Int = 0,
    val bonusTilePoints: Int = 0,
    val completionBonus: Int = 0,
) {
    val total: Int
        get() = placedCellPoints + bonusTilePoints + completionBonus

    fun plus(delta: ScoreDelta): ScoreBreakdown =
        copy(
            placedCellPoints = placedCellPoints + delta.placedCellPoints,
            bonusTilePoints = bonusTilePoints + delta.bonusTilePoints,
            completionBonus = completionBonus + delta.completionBonus,
        )
}
