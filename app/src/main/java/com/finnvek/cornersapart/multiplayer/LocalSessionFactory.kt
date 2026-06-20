package com.finnvek.cornersapart.multiplayer

import com.finnvek.cornersapart.engine.GameEngine
import com.finnvek.cornersapart.model.GameConfig
import com.finnvek.cornersapart.opponents.ComputerOpponentEngine
import com.finnvek.cornersapart.opponents.OpponentDifficultyMapper
import javax.inject.Inject

class LocalSessionFactory
    @Inject
    constructor(
        private val engine: GameEngine,
        private val opponentEngine: ComputerOpponentEngine,
    ) {
        fun create(
            initialConfig: GameConfig,
            persistedDifficulty: Int,
        ): LocalSession =
            LocalSession(
                engine = engine,
                opponentEngine = opponentEngine,
                opponentDifficulty = OpponentDifficultyMapper.fromPersistedLevel(persistedDifficulty),
                initialConfig = initialConfig,
            )
    }
