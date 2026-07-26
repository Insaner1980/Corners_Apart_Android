package com.finnvek.cornersapart.multiplayer

import com.finnvek.cornersapart.engine.GameEngine
import com.finnvek.cornersapart.model.GameMode
import com.finnvek.cornersapart.model.GameModeConfigs
import com.finnvek.cornersapart.opponents.ComputerOpponentEngine
import com.finnvek.cornersapart.opponents.OpponentRoster
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalSessionFactoryRivalTest {
    private val engine = GameEngine()
    private val factory =
        LocalSessionFactory(
            engine = engine,
            opponentEngine = ComputerOpponentEngine(gameEngine = engine),
        )

    private fun rivalSession(rivalColorIndex: Int = 2): LocalSession =
        factory.createRivalMatch(
            initialConfig =
                GameModeConfigs.defaultGameConfig(
                    mode = GameMode.COMPACT_DUEL,
                    randomSeed = 42L,
                ),
            character = OpponentRoster.all.first(),
            rivalColorIndex = rivalColorIndex,
        )

    @Test
    fun rivalSlotBecomesComputerControlledWithCharacterIdentity() {
        val rival = OpponentRoster.all.first()
        val players = rivalSession(rivalColorIndex = 3).gameState.value.players

        val humanSlot = players[0]
        val rivalSlot = players[LocalSessionFactory.RIVAL_SLOT_INDEX]
        assertFalse(humanSlot.isComputerControlled)
        assertTrue(rivalSlot.isComputerControlled)
        assertEquals(rival.name, rivalSlot.name)
        assertEquals(3, rivalSlot.colorIndex)
    }

    @Test
    fun rivalSessionUsesCharacterStyleAndDifficulty() {
        val rival = OpponentRoster.all.first()
        val session = rivalSession()

        assertEquals(rival.style, session.opponentStyleOverride)
        assertEquals(rival.difficulty, session.opponentDifficulty)
    }

    @Test
    fun rivalMatchStaysOnCompactDuelBoard() {
        val state = rivalSession().gameState.value
        assertEquals(GameMode.COMPACT_DUEL, state.gameMode)
        assertEquals(2, state.players.size)
    }
}
