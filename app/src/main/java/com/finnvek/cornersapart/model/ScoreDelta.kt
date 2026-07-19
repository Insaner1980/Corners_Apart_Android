package com.finnvek.cornersapart.model

import kotlinx.serialization.Serializable

@Serializable
data class ScoreDelta(
    val placedCellPoints: Int = 0,
    val bonusTilePoints: Int = 0,
    val completionBonus: Int = 0,
) {
    val total: Int
        get() = placedCellPoints + bonusTilePoints + completionBonus
}
