package com.finnvek.cornersapart.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class GameEnginePassTest {
    private val engine = GameEngine()

    @Test
    fun passRejectsGameOverStateWithoutMutation() {
        val endedState =
            EngineTestFixtures
                .standardState(engine)
                .copy(
                    currentPlayerIndex = 0,
                    turnNumber = 12,
                    isGameOver = true,
                )

        val error =
            assertThrows(IllegalArgumentException::class.java) {
                engine.pass(endedState, playerIndex = 0)
            }

        assertEquals(MoveRejectionReason.GAME_OVER.name, error.message)
    }
}
