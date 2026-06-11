package com.finnvek.cornersapart.multiplayer

import com.finnvek.cornersapart.engine.GameEngine
import com.finnvek.cornersapart.engine.ScoreDelta
import com.finnvek.cornersapart.model.GameConfig
import com.finnvek.cornersapart.model.GameMode
import com.finnvek.cornersapart.model.Move
import com.finnvek.cornersapart.model.PieceCatalog
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NearbySessionTest {
    private val engine = GameEngine()

    @Test
    fun hostSessionBroadcastsAcceptedMovesThroughTransport() =
        runTest {
            val transport = RecordingNearbyTransport()
            val session =
                NearbySession.host(
                    engine = engine,
                    transport = transport,
                    initialConfig =
                        GameConfig(
                            mode = GameMode.FOUR_PLAYER,
                            randomSeed = 47L,
                            bonusTiles = emptyList(),
                        ),
                )
            val move =
                Move(
                    playerIndex = 0,
                    pieceId = PieceCatalog.SINGLE_CELL_ID,
                    anchorRow = 0,
                    anchorCol = 0,
                    orientationIndex = 0,
                )

            val result = session.sendMove(move)

            assertTrue(result.isSuccess)
            assertEquals(1, session.gameState.value.currentPlayerIndex)
            assertEquals(
                GameMessage.MoveAccepted(
                    move = move,
                    state = session.gameState.value,
                    scoreDelta = ScoreDelta(placedCellPoints = 1),
                ),
                transport.sentMessages.single(),
            )
        }

    @Test
    fun clientSessionSendsMoveRequestWithoutLocalValidation() =
        runTest {
            val transport = RecordingNearbyTransport()
            val session =
                NearbySession.client(
                    transport = transport,
                    initialState =
                        engine.newGame(
                            GameConfig(
                                mode = GameMode.FOUR_PLAYER,
                                randomSeed = 53L,
                                bonusTiles = emptyList(),
                            ),
                        ),
                )
            val move =
                Move(
                    playerIndex = 0,
                    pieceId = PieceCatalog.SINGLE_CELL_ID,
                    anchorRow = 0,
                    anchorCol = 1,
                    orientationIndex = 0,
                )

            val result = session.sendMove(move)

            assertTrue(result.isSuccess)
            assertEquals(GameMessage.PlaceMove(move), transport.sentMessages.single())
        }

    @Test
    fun clientSessionTracksPlayerReconnectStateFromLobbyMessages() =
        runTest {
            val transport = RecordingNearbyTransport()
            val session =
                NearbySession.client(
                    transport = transport,
                    initialState =
                        engine.newGame(
                            GameConfig(
                                mode = GameMode.FOUR_PLAYER,
                                randomSeed = 59L,
                                bonusTiles = emptyList(),
                            ),
                        ),
                )
            val returningPlayer = session.lobbyState.value.connectedPlayers[2]

            session.applyRemoteMessage("host", GameMessage.PlayerLeft(playerIndex = 2))

            assertEquals(ConnectionState.RECONNECTING, session.connectionState.value)
            assertEquals(setOf(2), session.lobbyState.value.reconnectingPlayerIndexes)

            session.applyRemoteMessage("host", GameMessage.PlayerJoined(returningPlayer))

            assertEquals(ConnectionState.CONNECTED, session.connectionState.value)
            assertTrue(
                session.lobbyState.value.reconnectingPlayerIndexes
                    .isEmpty(),
            )
        }
}

private class RecordingNearbyTransport : NearbyTransport {
    val sentMessages = mutableListOf<GameMessage>()

    override suspend fun send(
        target: MessageTarget,
        message: GameMessage,
    ) {
        sentMessages += message
    }
}
