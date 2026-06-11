package com.finnvek.cornersapart.engine

import com.finnvek.cornersapart.model.GameConfig
import com.finnvek.cornersapart.model.GameConstants
import com.finnvek.cornersapart.model.GameMode
import com.finnvek.cornersapart.model.GameState

object EngineTestFixtures {
    fun standardState(
        engine: GameEngine,
        randomSeed: Long = 7L,
    ): GameState =
        engine.newGame(
            GameConfig(
                mode = GameMode.FOUR_PLAYER,
                boardSize = GameConstants.STANDARD_BOARD_SIZE,
                randomSeed = randomSeed,
                bonusTiles = emptyList(),
            ),
        )
}
