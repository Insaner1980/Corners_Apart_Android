package com.finnvek.cornersapart.multiplayer

import com.finnvek.cornersapart.engine.GameEngine
import com.finnvek.cornersapart.model.GameConfig
import com.finnvek.cornersapart.opponents.ComputerOpponentEngine
import com.finnvek.cornersapart.opponents.OpponentCharacter
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

        /**
         * Rivals-ottelu: paikka 1 muutetaan konevastustajaksi, joka kantaa
         * hahmon nimen ja väriperheen; tyyli ja vaikeus tulevat hahmolta.
         */
        fun createRivalMatch(
            initialConfig: GameConfig,
            character: OpponentCharacter,
            rivalColorIndex: Int,
        ): LocalSession {
            val session =
                LocalSession(
                    engine = engine,
                    opponentEngine = opponentEngine,
                    opponentDifficulty = character.difficulty,
                    opponentStyleOverride = character.style,
                    initialConfig = initialConfig,
                )
            val state = session.gameState.value
            session.replaceState(
                state.copy(
                    players =
                        state.players.map { player ->
                            if (player.index == RIVAL_SLOT_INDEX) {
                                player.copy(
                                    name = character.name,
                                    colorIndex = rivalColorIndex,
                                    isComputerControlled = true,
                                )
                            } else {
                                player
                            }
                        },
                ),
            )
            return session
        }

        companion object {
            const val RIVAL_SLOT_INDEX = 1
        }
    }
