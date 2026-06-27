package com.finnvek.cornersapart.multiplayer

import com.finnvek.cornersapart.engine.GameEngine
import com.finnvek.cornersapart.engine.MoveRejectionReason
import com.finnvek.cornersapart.model.GameConfig
import com.finnvek.cornersapart.model.GameMode
import com.finnvek.cornersapart.model.Move
import com.finnvek.cornersapart.model.PieceCatalog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GameProtocolTest {
    private val move =
        Move(
            playerIndex = 0,
            pieceId = PieceCatalog.SINGLE_CELL_ID,
            anchorRow = 0,
            anchorCol = 0,
            orientationIndex = 0,
        )

    @Test
    fun gameMessagesRoundTripThroughJson() {
        val state =
            GameEngine().newGame(
                GameConfig(
                    mode = GameMode.FOUR_PLAYER,
                    randomSeed = 31L,
                    bonusTiles = emptyList(),
                ),
            )
        val messages =
            listOf(
                GameMessage.GameConfig(config = GameConfig(mode = GameMode.FOUR_PLAYER, randomSeed = 31L)),
                GameMessage.PlaceMove(move),
                GameMessage.MoveAccepted(move = move, state = state),
                GameMessage.MoveRejected(move = move, reason = MoveRejectionReason.START_CORNER_NOT_COVERED),
                GameMessage.Pass(playerIndex = 0),
                GameMessage.FullSync(state),
                GameMessage.PlayerJoined(
                    SessionPlayer(
                        index = 0,
                        name = "Indigo",
                        isLocal = false,
                        isComputerControlled = false,
                        colorIndex = 0,
                        ownerIndex = 0,
                        usedPieceCount = 0,
                    ),
                ),
                GameMessage.PlayerLeft(playerIndex = 0),
                GameMessage.Ping,
                GameMessage.Pong,
            )

        messages.forEach { message ->
            val encoded = GameProtocol.encode(message)
            val decoded = GameProtocol.decode(encoded)

            assertEquals(message, decoded)
            assertTrue(encoded.contains("\"type\""))
        }
    }

    @Test
    fun equivalentFullSyncMessagesEncodeIdentically() {
        val config = GameConfig(mode = GameMode.FOUR_PLAYER, randomSeed = 31L, bonusTiles = emptyList())
        val first = GameMessage.FullSync(GameEngine().newGame(config))
        val second = GameMessage.FullSync(GameEngine().newGame(config))

        assertEquals(GameProtocol.encode(first), GameProtocol.encode(second))
    }

    @Test
    fun decodeRejectsUnknownMessageType() {
        val result = runCatching { GameProtocol.decode("""{"type":"unknown"}""") }

        assertTrue(result.isFailure)
    }
}
