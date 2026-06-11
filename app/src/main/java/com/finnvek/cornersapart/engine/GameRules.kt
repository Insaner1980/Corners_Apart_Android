package com.finnvek.cornersapart.engine

import com.finnvek.cornersapart.model.CellPosition
import com.finnvek.cornersapart.model.GameMode
import com.finnvek.cornersapart.model.GameModeConfigs

object GameRules {
    fun playerCountFor(mode: GameMode): Int = GameModeConfigs.forMode(mode).playerSlots.size

    fun bonusTileCountFor(mode: GameMode): Int = GameModeConfigs.forMode(mode).bonusTileCount

    fun cornersFor(
        mode: GameMode,
        boardSize: Int,
    ): List<CellPosition> = GameModeConfigs.forMode(mode, boardSize).playerSlots.map { slot -> slot.startCorner }
}
