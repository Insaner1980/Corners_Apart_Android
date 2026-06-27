package com.finnvek.cornersapart.model

import com.finnvek.cornersapart.engine.GameEngine
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test

class GameStateSerializationTest {
    @Test
    fun gameStateRoundTripsThroughJson() {
        val state =
            GameEngine().newGame(
                GameConfig(
                    mode = GameMode.FOUR_PLAYER,
                    boardSize = GameConstants.STANDARD_BOARD_SIZE,
                    randomSeed = 99L,
                    bonusTiles = listOf(BonusTile(row = 4, col = 4)),
                ),
            )

        val encoded = Json.encodeToString(state)
        val decoded = Json.decodeFromString<GameState>(encoded)

        assertEquals(state, decoded)
    }

    @Test
    fun equivalentSeededGameStatesEncodeIdentically() {
        val config =
            GameConfig(
                mode = GameMode.FOUR_PLAYER,
                boardSize = GameConstants.STANDARD_BOARD_SIZE,
                randomSeed = 99L,
                bonusTiles = listOf(BonusTile(row = 4, col = 4)),
            )

        val first = Json.encodeToString(GameEngine().newGame(config))
        val second = Json.encodeToString(GameEngine().newGame(config))

        assertEquals(first, second)
    }
}
