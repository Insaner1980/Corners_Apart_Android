package com.finnvek.cornersapart.engine

import com.finnvek.cornersapart.model.GameMode
import com.finnvek.cornersapart.model.GameModeConfigs

object GameRules {
    fun bonusTileCountFor(mode: GameMode): Int = GameModeConfigs.forMode(mode).bonusTileCount
}
