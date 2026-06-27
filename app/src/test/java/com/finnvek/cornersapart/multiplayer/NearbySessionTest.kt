package com.finnvek.cornersapart.multiplayer

import com.finnvek.cornersapart.engine.GameEngine
import com.finnvek.cornersapart.engine.MoveRejectionReason
import com.finnvek.cornersapart.engine.ScoreDelta
import com.finnvek.cornersapart.model.GameConfig
import com.finnvek.cornersapart.model.GameMode
import com.finnvek.cornersapart.model.Move
import com.finnvek.cornersapart.model.PieceCatalog
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
    fun clientSessionEmitsMoveRejectedEventFromHostRejection() =
        runTest {
            val transport = RecordingNearbyTransport()
            val session =
                NearbySession.client(
                    transport = transport,
                    initialState =
                        engine.newGame(
                            GameConfig(
                                mode = GameMode.FOUR_PLAYER,
                                randomSeed = 54L,
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
            val event = async(start = CoroutineStart.UNDISPATCHED) { session.events.first() }

            session.applyRemoteMessage(
                "host",
                GameMessage.MoveRejected(
                    move = move,
                    reason = MoveRejectionReason.START_CORNER_NOT_COVERED,
                ),
            )

            assertEquals(
                GameSessionEvent.MoveRejected(MoveRejectionReason.START_CORNER_NOT_COVERED),
                event.await(),
            )
        }

    @Test
    fun clientSessionSendsPassRequestWithoutLocalValidation() =
        runTest {
            val transport = RecordingNearbyTransport()
            val initialState =
                engine.newGame(
                    GameConfig(
                        mode = GameMode.FOUR_PLAYER,
                        randomSeed = 55L,
                        bonusTiles = emptyList(),
                    ),
                )
            val session =
                NearbySession.client(
                    transport = transport,
                    initialState = initialState,
                )

            val result = session.sendPass(playerIndex = 0)

            assertTrue(result.isSuccess)
            assertEquals(initialState, session.gameState.value)
            assertEquals(GameMessage.Pass(playerIndex = 0), transport.sentMessages.single())
        }

    @Test
    fun hostSessionRejectsPassAfterGameOverWithoutPublishingSync() =
        runTest {
            val transport = RecordingNearbyTransport()
            val session =
                NearbySession.host(
                    engine = engine,
                    transport = transport,
                    initialConfig =
                        GameConfig(
                            mode = GameMode.FOUR_PLAYER,
                            randomSeed = 57L,
                            bonusTiles = emptyList(),
                        ),
                )
            val endedState =
                session.gameState.value.copy(
                    currentPlayerIndex = 0,
                    turnNumber = 12,
                    isGameOver = true,
                )
            session.replaceState(endedState)

            val result = session.sendPass(playerIndex = 0)

            assertFalse(result.isSuccess)
            assertEquals(endedState, session.gameState.value)
            val rejection = transport.sentMessages.single() as GameMessage.MoveRejected
            assertEquals(MoveRejectionReason.GAME_OVER, rejection.reason)
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

    @Test
    fun clientSessionRejectsLaterFullSyncWithInvalidIndexDomains() =
        runTest {
            val transport = RecordingNearbyTransport()
            val initialState =
                engine.newGame(
                    GameConfig(
                        mode = GameMode.FOUR_PLAYER,
                        randomSeed = 60L,
                        bonusTiles = emptyList(),
                    ),
                )
            val session =
                NearbySession.client(
                    transport = transport,
                    initialState = initialState,
                )
            val invalidState = initialState.copy(currentPlayerIndex = 99)

            val result = session.applyRemoteMessage("host", GameMessage.FullSync(invalidState))

            assertFalse(result.isSuccess)
            assertEquals(initialState, session.gameState.value)
            assertEquals(ConnectionState.FAILED, session.connectionState.value)
        }

    @Test
    fun clientReplaceStateDoesNotOverwriteHostAuthoritativeState() =
        runTest {
            val transport = RecordingNearbyTransport()
            val initialState =
                engine.newGame(
                    GameConfig(
                        mode = GameMode.FOUR_PLAYER,
                        randomSeed = 61L,
                        bonusTiles = emptyList(),
                    ),
                )
            val replacementState =
                engine.newGame(
                    GameConfig(
                        mode = GameMode.THREE_PLAYER,
                        randomSeed = 67L,
                        bonusTiles = emptyList(),
                    ),
                )
            val session =
                NearbySession.client(
                    transport = transport,
                    initialState = initialState,
                )

            session.replaceState(replacementState)

            assertEquals(initialState, session.gameState.value)
        }

    @Test
    fun hostStartNewGameWaitsForInFlightMoveMutation() =
        runTest {
            val transport = BlockingNearbyTransport()
            val session =
                NearbySession.host(
                    engine = engine,
                    transport = transport,
                    initialConfig =
                        GameConfig(
                            mode = GameMode.FOUR_PLAYER,
                            randomSeed = 71L,
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
            val inFlightMove = async { session.sendMove(move) }
            transport.sendStarted.await()

            val restartCompleted = CompletableDeferred<Unit>()
            val restartThread =
                Thread {
                    runCatching {
                        session.startNewGame(
                            GameConfig(
                                mode = GameMode.THREE_PLAYER,
                                randomSeed = 73L,
                                bonusTiles = emptyList(),
                            ),
                        )
                    }.fold(
                        onSuccess = { restartCompleted.complete(Unit) },
                        onFailure = { error -> restartCompleted.completeExceptionally(error) },
                    )
                }
            restartThread.start()
            restartThread.join(100)

            assertFalse(restartCompleted.isCompleted)

            transport.releaseSend.complete(Unit)
            assertTrue(inFlightMove.await().isSuccess)
            restartCompleted.await()
            restartThread.join(1_000)

            assertEquals(73L, session.gameState.value.randomSeed)
            assertEquals(0, session.gameState.value.turnNumber)
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

private class BlockingNearbyTransport : NearbyTransport {
    val sendStarted = CompletableDeferred<Unit>()
    val releaseSend = CompletableDeferred<Unit>()

    override suspend fun send(
        target: MessageTarget,
        message: GameMessage,
    ) {
        sendStarted.complete(Unit)
        releaseSend.await()
    }
}
