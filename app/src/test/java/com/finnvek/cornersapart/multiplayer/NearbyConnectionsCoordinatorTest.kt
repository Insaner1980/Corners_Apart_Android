package com.finnvek.cornersapart.multiplayer

import com.finnvek.cornersapart.engine.GameEngine
import com.finnvek.cornersapart.engine.MoveRejectionReason
import com.finnvek.cornersapart.engine.MoveResult
import com.finnvek.cornersapart.model.GameConfig
import com.finnvek.cornersapart.model.GameConstants
import com.finnvek.cornersapart.model.GameMode
import com.finnvek.cornersapart.model.Move
import com.finnvek.cornersapart.model.PieceCatalog
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.coroutines.CoroutineContext

@OptIn(ExperimentalCoroutinesApi::class)
@Suppress("LargeClass")
class NearbyConnectionsCoordinatorTest {
    @Test
    fun startHostingUsesPackageServiceIdAndCreatesSession() =
        runTest {
            val facade = RecordingConnectionsClientFacade()
            val coordinator = createCoordinator(facade)

            coordinator.startHosting(
                GameConfig(mode = GameMode.FOUR_PLAYER, randomSeed = 11L, bonusTiles = emptyList()),
            )

            assertEquals("com.finnvek.cornersapart", NearbyConnectionsCoordinator.SERVICE_ID)
            assertEquals(NearbyConnectionsCoordinator.SERVICE_ID, facade.advertisingServiceId)
            assertNotNull(coordinator.currentSession.value)
        }

    @Test
    fun startDiscoveryListsFoundEndpoints() =
        runTest {
            val facade = RecordingConnectionsClientFacade()
            val coordinator = createCoordinator(facade)

            coordinator.startDiscovery()
            facade.discoveryCallback?.onEndpointFound("endpoint-1", "Tablet")

            assertEquals(
                "endpoint-1",
                coordinator.nearbyState.value.discoveredEndpoints
                    .single()
                    .endpointId,
            )
            assertEquals(
                "Tablet",
                coordinator.nearbyState.value.discoveredEndpoints
                    .single()
                    .endpointName,
            )
            assertEquals(NearbyConnectionsCoordinator.SERVICE_ID, facade.discoveryServiceId)
        }

    @Test
    fun startDiscoveryReplacesHostingWithoutOverlappingNearbyRoles() =
        runTest {
            val facade = RecordingConnectionsClientFacade()
            val coordinator = createCoordinator(facade)
            coordinator.startHosting(
                GameConfig(mode = GameMode.FOUR_PLAYER, randomSeed = 12L, bonusTiles = emptyList()),
            )
            facade.operations.clear()

            coordinator.startDiscovery()

            assertEquals(
                listOf("stopAdvertising", "stopDiscovery", "stopAllEndpoints", "startDiscovery"),
                facade.operations,
            )
            assertNull(coordinator.currentSession.value)
        }

    @Test
    fun startHostingReplacesDiscoveryWithoutOverlappingNearbyRoles() =
        runTest {
            val facade = RecordingConnectionsClientFacade()
            val coordinator = createCoordinator(facade)
            coordinator.startDiscovery()
            facade.operations.clear()

            coordinator.startHosting(
                GameConfig(mode = GameMode.FOUR_PLAYER, randomSeed = 13L, bonusTiles = emptyList()),
            )

            assertEquals(
                listOf("stopAdvertising", "stopDiscovery", "stopAllEndpoints", "startAdvertising"),
                facade.operations,
            )
            assertNotNull(coordinator.currentSession.value)
        }

    @Test
    fun acceptAndRejectPendingConnectionCallFacadeMethods() =
        runTest {
            val (facade, coordinator) =
                createPendingHostConnection(
                    mode = GameMode.FOUR_PLAYER,
                    randomSeed = 13L,
                )
            coordinator.acceptPendingConnection("endpoint-1")
            coordinator.rejectPendingConnection("endpoint-2")

            assertEquals(
                "1234",
                coordinator.nearbyState.value.pendingConnection
                    ?.authenticationToken,
            )
            assertEquals(listOf("endpoint-1"), facade.acceptedEndpoints)
            assertEquals(listOf("endpoint-2"), facade.rejectedEndpoints)
        }

    @Test
    fun rejectedPendingConnectionCannotBeAcceptedLater() =
        runTest {
            val (facade, coordinator) =
                createPendingHostConnection(
                    mode = GameMode.FOUR_PLAYER,
                    randomSeed = 14L,
                )

            coordinator.rejectPendingConnection("endpoint-1")
            coordinator.acceptPendingConnection("endpoint-1")

            assertNull(coordinator.nearbyState.value.pendingConnection)
            assertEquals(listOf("endpoint-1"), facade.rejectedEndpoints)
            assertTrue(facade.acceptedEndpoints.isEmpty())
        }

    @Test
    fun rejectedConnectionResultCannotBeAcceptedAgain() =
        runTest {
            val (facade, coordinator) =
                createPendingHostConnection(
                    mode = GameMode.FOUR_PLAYER,
                    randomSeed = 14L,
                )
            coordinator.acceptPendingConnection("endpoint-1")

            facade.connectionCallback?.onConnectionResult(
                "endpoint-1",
                NearbyConnectionResult.Failed(statusCode = null, message = "Rejected"),
            )
            coordinator.acceptPendingConnection("endpoint-1")

            assertNull(coordinator.nearbyState.value.pendingConnection)
            assertEquals(listOf("endpoint-1"), facade.acceptedEndpoints)
        }

    @Test
    fun disconnectedPendingConnectionCannotBeAcceptedLater() =
        runTest {
            val (facade, coordinator) =
                createPendingHostConnection(
                    mode = GameMode.FOUR_PLAYER,
                    randomSeed = 14L,
                )

            facade.connectionCallback?.onDisconnected("endpoint-1")
            coordinator.acceptPendingConnection("endpoint-1")

            assertNull(coordinator.nearbyState.value.pendingConnection)
            assertTrue(facade.acceptedEndpoints.isEmpty())
        }

    @Test
    fun connectingToDifferentEndpointClearsPreviousPendingConnection() =
        runTest {
            val facade = RecordingConnectionsClientFacade()
            val coordinator = createCoordinator(facade)
            coordinator.startDiscovery()
            coordinator.connectToEndpoint("endpoint-1")
            facade.connectionCallback?.onConnectionInitiated("endpoint-1", "Phone", "1234")

            coordinator.connectToEndpoint("endpoint-2")
            coordinator.acceptPendingConnection("endpoint-1")

            assertNull(coordinator.nearbyState.value.pendingConnection)
            assertTrue(facade.acceptedEndpoints.isEmpty())
        }

    @Test
    fun acceptedHostConnectionSendsAuthoritativeConfigurationInFirstFullSync() =
        runTest {
            val (facade, coordinator) =
                createPendingHostConnection(
                    mode = GameMode.COMPACT_DUEL,
                    randomSeed = 15L,
                )
            coordinator.acceptPendingConnection("endpoint-1")
            facade.connectionCallback?.onConnectionResult("endpoint-1", NearbyConnectionResult.Accepted)
            runCurrent()

            val (endpointId, payload) = facade.sentPayloads.single()
            val sync = GameProtocol.decode(payload.decodeToString()) as GameMessage.FullSync
            assertEquals("endpoint-1", endpointId)
            assertEquals(GameMode.COMPACT_DUEL, sync.state.gameMode)
            assertEquals(GameConstants.COMPACT_BOARD_SIZE, sync.state.board.size)
        }

    @Test
    fun hostRejectsConnectionWhenNoRemoteOwnerIsAvailable() =
        runTest {
            val (facade, coordinator) =
                createPendingHostConnection(
                    mode = GameMode.SOLO,
                    randomSeed = 16L,
                )
            coordinator.acceptPendingConnection("endpoint-1")

            assertTrue(facade.acceptedEndpoints.isEmpty())
            assertEquals(listOf("endpoint-1"), facade.rejectedEndpoints)
            assertNull(coordinator.nearbyState.value.pendingConnection)
        }

    @Test
    fun capacityRejectionResultDoesNotFailHostSession() =
        runTest {
            val (facade, coordinator) =
                createPendingHostConnection(
                    mode = GameMode.SOLO,
                    randomSeed = 16L,
                )
            coordinator.acceptPendingConnection("endpoint-1")

            facade.connectionCallback?.onConnectionResult(
                "endpoint-1",
                NearbyConnectionResult.Failed(statusCode = null, message = "Rejected"),
            )

            assertEquals(ConnectionState.CONNECTED, coordinator.nearbyState.value.connectionState)
            assertNull(coordinator.nearbyState.value.errorMessage)
        }

    @Test
    fun hostReservesOwnerBeforeConnectionResult() =
        runTest {
            val (facade, coordinator) =
                createPendingHostConnection(
                    mode = GameMode.TWO_COLOR_DUEL,
                    randomSeed = 16L,
                )
            coordinator.acceptPendingConnection("endpoint-1")
            facade.connectionCallback?.onConnectionInitiated("endpoint-2", "Tablet", "5678")
            coordinator.acceptPendingConnection("endpoint-2")

            assertEquals(listOf("endpoint-1"), facade.acceptedEndpoints)
            assertEquals(listOf("endpoint-2"), facade.rejectedEndpoints)
        }

    @Test
    fun failedAcceptOperationReleasesReservedOwner() =
        runTest {
            val (facade, coordinator) =
                createPendingHostConnection(
                    mode = GameMode.TWO_COLOR_DUEL,
                    randomSeed = 16L,
                )
            coordinator.acceptPendingConnection("endpoint-1")
            val acceptFailureCallback = checkNotNull(facade.operationFailureCallback)
            facade.connectionCallback?.onConnectionInitiated("endpoint-2", "Tablet", "5678")
            coordinator.acceptPendingConnection("endpoint-2")
            acceptFailureCallback.onOperationFailure(
                NearbyOperation.ACCEPT_CONNECTION,
                NearbyOperationFailure(statusCode = null, message = "Accept failed"),
            )
            facade.connectionCallback?.onConnectionInitiated("endpoint-3", "Laptop", "9012")
            coordinator.acceptPendingConnection("endpoint-3")

            assertEquals(listOf("endpoint-1", "endpoint-3"), facade.acceptedEndpoints)
            assertEquals(listOf("endpoint-2"), facade.rejectedEndpoints)
        }

    @Test
    fun hostAssignsRemoteOwnersInPlayerOrder() =
        runTest {
            val facade = RecordingConnectionsClientFacade()
            val coordinator = createCoordinator(facade)
            hostAndAcceptEndpoint(coordinator, facade, randomSeed = 16L, endpointId = "endpoint-1")
            hostAndAcceptEndpoint(coordinator, facade, randomSeed = 16L, endpointId = "endpoint-2")
            hostAndAcceptEndpoint(coordinator, facade, randomSeed = 16L, endpointId = "endpoint-3")
            val session = checkNotNull(coordinator.currentSession.value)

            session.sendPass(playerIndex = 0)
            sendPass(facade, endpointId = "endpoint-1", playerIndex = 1)
            sendPass(facade, endpointId = "endpoint-2", playerIndex = 2)
            sendPass(facade, endpointId = "endpoint-3", playerIndex = 3)
            advanceUntilIdle()

            assertTrue(
                session.gameState.value.players
                    .all { player -> player.passed },
            )
        }

    @Test
    fun twoColorDuelAssignsBothRemoteColorSlotsToOneEndpoint() =
        runTest {
            val facade = RecordingConnectionsClientFacade()
            val coordinator = createCoordinator(facade)
            hostAndAcceptEndpoint(
                coordinator = coordinator,
                facade = facade,
                randomSeed = 16L,
                mode = GameMode.TWO_COLOR_DUEL,
            )
            val session = checkNotNull(coordinator.currentSession.value)

            session.sendPass(playerIndex = 0)
            sendPass(facade, endpointId = "endpoint-1", playerIndex = 1)
            session.sendPass(playerIndex = 2)
            sendPass(facade, endpointId = "endpoint-1", playerIndex = 3)
            advanceUntilIdle()

            assertTrue(
                session.gameState.value.players
                    .all { player -> player.passed },
            )
        }

    @Test
    fun bytesPayloadDecodesAndRoutesToCurrentSession() =
        runTest {
            val facade = RecordingConnectionsClientFacade()
            val coordinator = createCoordinator(facade)
            hostAndAcceptEndpoint(coordinator = coordinator, facade = facade, randomSeed = 17L)
            checkNotNull(coordinator.currentSession.value).sendMove(
                Move(
                    playerIndex = 0,
                    pieceId = PieceCatalog.SINGLE_CELL_ID,
                    anchorRow = 0,
                    anchorCol = 0,
                    orientationIndex = 0,
                ),
            )
            facade.sentPayloads.clear()
            val move =
                Move(
                    playerIndex = 1,
                    pieceId = PieceCatalog.SINGLE_CELL_ID,
                    anchorRow = 0,
                    anchorCol = 19,
                    orientationIndex = 0,
                )

            facade.payloadCallback?.onBytesPayload(
                "endpoint-1",
                GameProtocol.encode(GameMessage.PlaceMove(move)).encodeToByteArray(),
            )
            advanceUntilIdle()

            assertEquals(
                2,
                coordinator.currentSession.value
                    ?.gameState
                    ?.value
                    ?.currentPlayerIndex,
            )
            assertTrue(facade.sentPayloads.isNotEmpty())
        }

    @Test
    fun payloadWithUnknownFieldFailsCleanly() =
        runTest {
            val facade = RecordingConnectionsClientFacade()
            val coordinator = createCoordinator(facade)
            hostAndAcceptEndpoint(coordinator = coordinator, facade = facade, randomSeed = 19L)

            facade.payloadCallback?.onBytesPayload(
                "endpoint-1",
                """{"type":"pass","playerIndex":0,"futureField":true}""".encodeToByteArray(),
            )
            advanceUntilIdle()

            assertEquals(ConnectionState.FAILED, coordinator.nearbyState.value.connectionState)
            assertTrue(
                coordinator.nearbyState.value.errorMessage
                    ?.contains("futureField") == true,
            )
        }

    @Test
    fun validPayloadClearsPreviousDecodeFailure() =
        runTest {
            val facade = RecordingConnectionsClientFacade()
            val coordinator = createCoordinator(facade)
            hostAndAcceptEndpoint(coordinator = coordinator, facade = facade, randomSeed = 19L)
            checkNotNull(coordinator.currentSession.value).sendPass(playerIndex = 0)

            facade.payloadCallback?.onBytesPayload(
                "endpoint-1",
                """{"type":"pass","playerIndex":1,"futureField":true}""".encodeToByteArray(),
            )
            advanceUntilIdle()
            assertEquals(ConnectionState.FAILED, coordinator.nearbyState.value.connectionState)

            facade.payloadCallback?.onBytesPayload(
                "endpoint-1",
                GameProtocol.encode(GameMessage.Pass(playerIndex = 1)).encodeToByteArray(),
            )
            advanceUntilIdle()

            assertEquals(ConnectionState.CONNECTED, coordinator.nearbyState.value.connectionState)
            assertNull(coordinator.nearbyState.value.errorMessage)
        }

    @Test
    fun clientProcessesFullSyncBeforeLaterMoveAcceptedWhenDispatcherRunsTasksInReverseOrder() {
        val dispatcher = ReverseOrderDispatcher()
        val facade = RecordingConnectionsClientFacade()
        val coordinator =
            NearbyConnectionsCoordinator(
                facade = facade,
                gameEngine = GameEngine(),
                localEndpointName = "Corners Apart",
                callbackScope = CoroutineScope(SupervisorJob() + dispatcher),
            )
        connectClientToHost(coordinator = coordinator, facade = facade)
        val engine = GameEngine()
        val initialState =
            engine.newGame(
                GameConfig(mode = GameMode.FOUR_PLAYER, randomSeed = 20L, bonusTiles = emptyList()),
            )
        val move =
            Move(
                playerIndex = 0,
                pieceId = PieceCatalog.SINGLE_CELL_ID,
                anchorRow = 0,
                anchorCol = 0,
                orientationIndex = 0,
            )
        val accepted = engine.applyMove(initialState, move) as MoveResult.Accepted

        facade.payloadCallback?.onBytesPayload(
            "host-1",
            GameProtocol.encode(GameMessage.FullSync(initialState)).encodeToByteArray(),
        )
        facade.payloadCallback?.onBytesPayload(
            "host-1",
            GameProtocol
                .encode(GameMessage.MoveAccepted(move = move, state = accepted.state, scoreDelta = accepted.scoreDelta))
                .encodeToByteArray(),
        )
        dispatcher.runAllLastFirst()

        assertEquals(
            accepted.state,
            coordinator.currentSession.value
                ?.gameState
                ?.value,
        )
    }

    @Test
    fun hostRejectsMoveForPlayerNotOwnedByEndpoint() =
        runTest {
            val facade = RecordingConnectionsClientFacade()
            val coordinator = createCoordinator(facade)
            hostAndAcceptEndpoint(coordinator = coordinator, facade = facade, randomSeed = 71L)
            val forgedHostMove =
                Move(
                    playerIndex = 0,
                    pieceId = PieceCatalog.SINGLE_CELL_ID,
                    anchorRow = 0,
                    anchorCol = 0,
                    orientationIndex = 0,
                )

            facade.payloadCallback?.onBytesPayload(
                "endpoint-1",
                GameProtocol.encode(GameMessage.PlaceMove(forgedHostMove)).encodeToByteArray(),
            )
            advanceUntilIdle()

            assertEquals(
                0,
                coordinator.currentSession.value
                    ?.gameState
                    ?.value
                    ?.currentPlayerIndex,
            )
            assertTrue(
                facade.sentPayloads
                    .map { (_, bytes) -> GameProtocol.decode(bytes.decodeToString()) }
                    .any { message -> message is GameMessage.MoveRejected },
            )
        }

    @Test
    fun hostRejectsPassForPlayerNotOwnedByEndpoint() =
        runTest {
            val facade = RecordingConnectionsClientFacade()
            val coordinator = createCoordinator(facade)
            hostAndAcceptEndpoint(coordinator = coordinator, facade = facade, randomSeed = 72L)

            facade.payloadCallback?.onBytesPayload(
                "endpoint-1",
                GameProtocol.encode(GameMessage.Pass(playerIndex = 0)).encodeToByteArray(),
            )
            advanceUntilIdle()

            assertEquals(
                0,
                coordinator.currentSession.value
                    ?.gameState
                    ?.value
                    ?.currentPlayerIndex,
            )
            val rejection =
                facade.sentPayloads
                    .map { (_, bytes) -> GameProtocol.decode(bytes.decodeToString()) }
                    .filterIsInstance<GameMessage.MoveRejected>()
                    .single()
            assertEquals(MoveRejectionReason.NOT_PLAYERS_TURN, rejection.reason)
            assertEquals(0, rejection.move.playerIndex)
        }

    @Test
    fun clientIgnoresFullSyncFromEndpointThatWasNotSelectedHost() =
        runTest {
            val facade = RecordingConnectionsClientFacade()
            val coordinator = createCoordinator(facade)
            val state =
                GameEngine().newGame(
                    GameConfig(mode = GameMode.FOUR_PLAYER, randomSeed = 73L, bonusTiles = emptyList()),
                )

            connectClientToHost(coordinator = coordinator, facade = facade)
            facade.payloadCallback?.onBytesPayload(
                "host-2",
                GameProtocol.encode(GameMessage.FullSync(state)).encodeToByteArray(),
            )
            advanceUntilIdle()

            assertNull(coordinator.currentSession.value)
        }

    @Test
    fun clientRejectsFullSyncWithInvalidIndexDomains() =
        runTest {
            val facade = RecordingConnectionsClientFacade()
            val coordinator = createCoordinator(facade)
            val invalidState =
                GameEngine()
                    .newGame(GameConfig(mode = GameMode.FOUR_PLAYER, randomSeed = 75L, bonusTiles = emptyList()))
                    .copy(currentPlayerIndex = 99)

            connectClientToHost(coordinator = coordinator, facade = facade)
            facade.payloadCallback?.onBytesPayload(
                "host-1",
                GameProtocol.encode(GameMessage.FullSync(invalidState)).encodeToByteArray(),
            )
            advanceUntilIdle()

            assertNull(coordinator.currentSession.value)
            assertEquals(ConnectionState.FAILED, coordinator.nearbyState.value.connectionState)
            assertEquals(
                "Game state index domains are invalid.",
                coordinator.nearbyState.value.errorMessage,
            )
        }

    @Test
    fun clientPreservesLaterInvalidFullSyncErrorMessage() =
        runTest {
            val facade = RecordingConnectionsClientFacade()
            val coordinator = createCoordinator(facade)
            val initialState =
                GameEngine()
                    .newGame(GameConfig(mode = GameMode.FOUR_PLAYER, randomSeed = 76L, bonusTiles = emptyList()))
            val invalidState = initialState.copy(currentPlayerIndex = 99)

            connectClientToHost(coordinator = coordinator, facade = facade)
            facade.payloadCallback?.onBytesPayload(
                "host-1",
                GameProtocol.encode(GameMessage.FullSync(initialState)).encodeToByteArray(),
            )
            advanceUntilIdle()
            facade.payloadCallback?.onBytesPayload(
                "host-1",
                GameProtocol.encode(GameMessage.FullSync(invalidState)).encodeToByteArray(),
            )
            advanceUntilIdle()

            assertNotNull(coordinator.currentSession.value)
            assertEquals(ConnectionState.FAILED, coordinator.nearbyState.value.connectionState)
            assertEquals(
                "Game state index domains are invalid.",
                coordinator.nearbyState.value.errorMessage,
            )
        }

    @Test
    fun connectionFailurePreservesStatusCode() =
        runTest {
            val facade = RecordingConnectionsClientFacade()
            val coordinator = createCoordinator(facade)

            coordinator.startDiscovery()
            coordinator.connectToEndpoint("host-1")
            facade.connectionCallback?.onConnectionResult(
                "host-1",
                NearbyConnectionResult.Failed(statusCode = 8004, message = "Remote endpoint rejected"),
            )

            assertEquals(ConnectionState.FAILED, coordinator.nearbyState.value.connectionState)
            assertEquals(
                "Connection failed (8004): Remote endpoint rejected",
                coordinator.nearbyState.value.errorMessage,
            )
        }

    @Test
    fun operationFailurePreservesStatusCode() =
        runTest {
            val facade = RecordingConnectionsClientFacade()
            val coordinator = createCoordinator(facade)

            coordinator.startDiscovery()
            facade.operationFailureCallback?.onOperationFailure(
                NearbyOperation.START_DISCOVERY,
                NearbyOperationFailure(statusCode = 8002, message = "Already discovering"),
            )

            assertEquals(ConnectionState.FAILED, coordinator.nearbyState.value.connectionState)
            assertEquals(
                "START_DISCOVERY failed (8002): Already discovering",
                coordinator.nearbyState.value.errorMessage,
            )
        }

    @Test
    fun hostRejectsPlayerJoinedForPlayerNotOwnedByEndpoint() =
        runTest {
            val facade = RecordingConnectionsClientFacade()
            val coordinator = createCoordinator(facade)
            hostAndAcceptEndpoint(coordinator = coordinator, facade = facade, randomSeed = 77L)
            facade.sentPayloads.clear()
            val spoofedHostPlayer =
                SessionPlayer(
                    index = 0,
                    name = "Spoofed",
                    isLocal = false,
                    isComputerControlled = false,
                    colorIndex = 0,
                    ownerIndex = 1,
                    usedPieceCount = 0,
                )

            facade.payloadCallback?.onBytesPayload(
                "endpoint-1",
                GameProtocol.encode(GameMessage.PlayerJoined(spoofedHostPlayer)).encodeToByteArray(),
            )
            advanceUntilIdle()

            assertTrue(facade.sentPayloads.isEmpty())
        }

    @Test
    fun hostRejectsPlayerJoinedForPlayerOwnedByEndpoint() =
        runTest {
            val facade = RecordingConnectionsClientFacade()
            val coordinator = createCoordinator(facade)
            hostAndAcceptEndpoint(coordinator = coordinator, facade = facade, randomSeed = 78L)
            val remotePlayer =
                checkNotNull(coordinator.currentSession.value)
                    .players.value
                    .single { player -> player.ownerIndex == 1 }
            facade.sentPayloads.clear()

            facade.payloadCallback?.onBytesPayload(
                "endpoint-1",
                GameProtocol.encode(GameMessage.PlayerJoined(remotePlayer)).encodeToByteArray(),
            )
            advanceUntilIdle()

            assertTrue(facade.sentPayloads.isEmpty())
        }

    @Test
    fun disconnectMarksMappedRemotePlayerReconnecting() =
        runTest {
            val facade = RecordingConnectionsClientFacade()
            val coordinator = createCoordinator(facade)
            hostAndAcceptEndpoint(coordinator = coordinator, facade = facade, randomSeed = 79L)

            facade.connectionCallback?.onDisconnected("endpoint-1")
            runCurrent()

            assertEquals(
                setOf(1),
                coordinator.currentSession.value
                    ?.lobbyState
                    ?.value
                    ?.reconnectingPlayerIndexes,
            )
        }

    @Test
    fun disconnectDuringBroadcastDoesNotBreakPayloadRouting() =
        runTest {
            val facade = RecordingConnectionsClientFacade()
            val coordinator = createCoordinator(facade)
            hostAndAcceptEndpoint(coordinator = coordinator, facade = facade, randomSeed = 81L)
            hostAndAcceptEndpoint(
                coordinator = coordinator,
                facade = facade,
                randomSeed = 81L,
                endpointId = "endpoint-2",
                endpointName = "Tablet",
                authenticationToken = "5678",
            )
            checkNotNull(coordinator.currentSession.value).sendMove(
                Move(
                    playerIndex = 0,
                    pieceId = PieceCatalog.SINGLE_CELL_ID,
                    anchorRow = 0,
                    anchorCol = 0,
                    orientationIndex = 0,
                ),
            )
            facade.sentPayloads.clear()
            var disconnected = false
            facade.onSendPayload = { endpointId ->
                if (!disconnected && endpointId == "endpoint-1") {
                    disconnected = true
                    facade.connectionCallback?.onDisconnected("endpoint-2")
                }
            }

            facade.payloadCallback?.onBytesPayload(
                "endpoint-1",
                GameProtocol
                    .encode(
                        GameMessage.PlaceMove(
                            Move(
                                playerIndex = 1,
                                pieceId = PieceCatalog.SINGLE_CELL_ID,
                                anchorRow = 0,
                                anchorCol = 19,
                                orientationIndex = 0,
                            ),
                        ),
                    ).encodeToByteArray(),
            )
            runCurrent()

            assertEquals(
                setOf(2),
                coordinator.currentSession.value
                    ?.lobbyState
                    ?.value
                    ?.reconnectingPlayerIndexes,
            )
        }

    @Test
    fun disconnectClearsEndpointsAndMarksStateDisconnected() =
        runTest {
            val facade = RecordingConnectionsClientFacade()
            val coordinator = createCoordinator(facade)
            coordinator.startDiscovery()

            coordinator.disconnect()

            assertTrue(facade.stopAllEndpointsCalled)
            assertEquals(ConnectionState.DISCONNECTED, coordinator.nearbyState.value.connectionState)
        }

    @Test
    fun staleDisconnectCallbackAfterDisconnectDoesNotReopenReconnectState() =
        runTest {
            val facade = RecordingConnectionsClientFacade()
            val coordinator = createCoordinator(facade)
            hostAndAcceptEndpoint(coordinator = coordinator, facade = facade, randomSeed = 83L)

            coordinator.disconnect()
            facade.connectionCallback?.onDisconnected("endpoint-1")
            advanceUntilIdle()

            assertNull(coordinator.currentSession.value)
            assertEquals(ConnectionState.DISCONNECTED, coordinator.nearbyState.value.connectionState)
        }

    @Test
    fun clientHostDisconnectClearsSessionAndMarksDisconnected() =
        runTest {
            val facade = RecordingConnectionsClientFacade()
            val coordinator = createCoordinator(facade)
            connectClientToHost(coordinator = coordinator, facade = facade)
            val initialState =
                GameEngine().newGame(
                    GameConfig(mode = GameMode.FOUR_PLAYER, randomSeed = 82L, bonusTiles = emptyList()),
                )
            facade.payloadCallback?.onBytesPayload(
                "host-1",
                GameProtocol.encode(GameMessage.FullSync(initialState)).encodeToByteArray(),
            )
            runCurrent()
            assertNotNull(coordinator.currentSession.value)

            facade.connectionCallback?.onDisconnected("host-1")
            runCurrent()

            assertNull(coordinator.currentSession.value)
            assertEquals(ConnectionState.DISCONNECTED, coordinator.nearbyState.value.connectionState)
        }

    @Test
    fun staleDiscoveryCallbackAfterDisconnectDoesNotListEndpoint() =
        runTest {
            val facade = RecordingConnectionsClientFacade()
            val coordinator = createCoordinator(facade)
            coordinator.startDiscovery()
            val staleDiscoveryCallback = checkNotNull(facade.discoveryCallback)

            coordinator.disconnect()
            staleDiscoveryCallback.onEndpointFound("endpoint-1", "Tablet")

            assertEquals(ConnectionState.DISCONNECTED, coordinator.nearbyState.value.connectionState)
            assertTrue(
                coordinator.nearbyState.value.discoveredEndpoints
                    .isEmpty(),
            )
        }

    @Test
    fun reconnectingEndpointClearsPlayerMarkAndCancelsTimeout() =
        runTest {
            val facade = RecordingConnectionsClientFacade()
            val coordinator = createCoordinator(facade)
            hostAndAcceptEndpoint(coordinator = coordinator, facade = facade, randomSeed = 80L)

            facade.connectionCallback?.onDisconnected("endpoint-1")
            runCurrent()
            hostAndAcceptEndpoint(
                coordinator = coordinator,
                facade = facade,
                randomSeed = 80L,
                endpointId = "endpoint-2",
            )
            runCurrent()

            assertTrue(
                coordinator.currentSession.value
                    ?.lobbyState
                    ?.value
                    ?.reconnectingPlayerIndexes
                    ?.isEmpty() == true,
            )
            assertEquals(
                ConnectionState.CONNECTED,
                coordinator.currentSession.value
                    ?.connectionState
                    ?.value,
            )

            advanceTimeBy(NearbyConnectionsCoordinator.RECONNECT_TIMEOUT_MS)
            runCurrent()

            assertEquals(ConnectionState.CONNECTED, coordinator.nearbyState.value.connectionState)
            assertEquals(
                ConnectionState.CONNECTED,
                coordinator.currentSession.value
                    ?.connectionState
                    ?.value,
            )
        }

    @Test
    fun reconnectTimeoutEndsReconnectingState() =
        runTest {
            val facade = RecordingConnectionsClientFacade()
            val coordinator = createCoordinator(facade)
            hostAndAcceptEndpoint(coordinator = coordinator, facade = facade, randomSeed = 80L)

            facade.connectionCallback?.onDisconnected("endpoint-1")
            runCurrent()

            assertEquals(ConnectionState.RECONNECTING, coordinator.nearbyState.value.connectionState)
            assertEquals(
                ConnectionState.RECONNECTING,
                coordinator.currentSession.value
                    ?.connectionState
                    ?.value,
            )

            advanceTimeBy(NearbyConnectionsCoordinator.RECONNECT_TIMEOUT_MS)
            runCurrent()

            assertEquals(ConnectionState.FAILED, coordinator.nearbyState.value.connectionState)
            assertEquals(
                ConnectionState.FAILED,
                coordinator.currentSession.value
                    ?.connectionState
                    ?.value,
            )
            assertTrue(
                coordinator.currentSession.value
                    ?.lobbyState
                    ?.value
                    ?.reconnectingPlayerIndexes
                    ?.isEmpty() == true,
            )
        }

    @Test
    fun staleOperationFailureAfterDisconnectDoesNotMarkStateFailed() =
        runTest {
            val facade = RecordingConnectionsClientFacade()
            val coordinator = createCoordinator(facade)
            coordinator.startDiscovery()
            val staleOperationFailureCallback = checkNotNull(facade.operationFailureCallback)

            coordinator.disconnect()
            staleOperationFailureCallback.onOperationFailure(
                NearbyOperation.START_DISCOVERY,
                NearbyOperationFailure(statusCode = 8002, message = "Already discovering"),
            )

            assertEquals(ConnectionState.DISCONNECTED, coordinator.nearbyState.value.connectionState)
            assertNull(coordinator.nearbyState.value.errorMessage)
        }

    @Test
    fun stalePayloadFailureAfterDisconnectDoesNotMarkStateFailed() =
        runTest {
            val facade = RecordingConnectionsClientFacade()
            val coordinator = createCoordinator(facade)
            hostAndAcceptEndpoint(coordinator = coordinator, facade = facade, randomSeed = 85L)
            val stalePayloadCallback = checkNotNull(facade.payloadCallback)

            coordinator.disconnect()
            stalePayloadCallback.onPayloadFailure("endpoint-1")

            assertEquals(ConnectionState.DISCONNECTED, coordinator.nearbyState.value.connectionState)
            assertNull(coordinator.nearbyState.value.errorMessage)
        }

    private fun TestScope.createCoordinator(facade: RecordingConnectionsClientFacade): NearbyConnectionsCoordinator =
        NearbyConnectionsCoordinator(
            facade = facade,
            gameEngine = GameEngine(),
            localEndpointName = "Corners Apart",
            callbackScope = this,
        )

    private fun TestScope.createPendingHostConnection(
        mode: GameMode,
        randomSeed: Long,
        endpointId: String = "endpoint-1",
        endpointName: String = "Phone",
        authenticationToken: String = "1234",
    ): HostConnectionFixture {
        val facade = RecordingConnectionsClientFacade()
        val coordinator = createCoordinator(facade)
        coordinator.startHosting(
            GameConfig(mode = mode, randomSeed = randomSeed, bonusTiles = emptyList()),
        )
        facade.connectionCallback?.onConnectionInitiated(endpointId, endpointName, authenticationToken)
        return HostConnectionFixture(facade = facade, coordinator = coordinator)
    }

    private fun hostAndAcceptEndpoint(
        coordinator: NearbyConnectionsCoordinator,
        facade: RecordingConnectionsClientFacade,
        randomSeed: Long,
        endpointId: String = "endpoint-1",
        endpointName: String = "Phone",
        authenticationToken: String = "1234",
        mode: GameMode = GameMode.FOUR_PLAYER,
    ) {
        if (coordinator.currentSession.value == null) {
            coordinator.startHosting(
                GameConfig(mode = mode, randomSeed = randomSeed, bonusTiles = emptyList()),
            )
        }
        facade.connectionCallback?.onConnectionInitiated(endpointId, endpointName, authenticationToken)
        coordinator.acceptPendingConnection(endpointId)
        facade.connectionCallback?.onConnectionResult(endpointId, NearbyConnectionResult.Accepted)
    }

    private fun sendPass(
        facade: RecordingConnectionsClientFacade,
        endpointId: String,
        playerIndex: Int,
    ) {
        facade.payloadCallback?.onBytesPayload(
            endpointId,
            GameProtocol.encode(GameMessage.Pass(playerIndex)).encodeToByteArray(),
        )
    }

    private fun connectClientToHost(
        coordinator: NearbyConnectionsCoordinator,
        facade: RecordingConnectionsClientFacade,
    ) {
        coordinator.startDiscovery()
        coordinator.connectToEndpoint("host-1")
        facade.connectionCallback?.onConnectionInitiated("host-1", "Host", "9876")
        coordinator.acceptPendingConnection("host-1")
        facade.connectionCallback?.onConnectionResult("host-1", NearbyConnectionResult.Accepted)
    }

    private data class HostConnectionFixture(
        val facade: RecordingConnectionsClientFacade,
        val coordinator: NearbyConnectionsCoordinator,
    )
}

private class ReverseOrderDispatcher : CoroutineDispatcher() {
    private val tasks = mutableListOf<Runnable>()

    override fun dispatch(
        context: CoroutineContext,
        block: Runnable,
    ) {
        tasks += block
    }

    fun runAllLastFirst() {
        while (tasks.isNotEmpty()) {
            tasks.removeAt(tasks.lastIndex).run()
        }
    }
}

private class RecordingConnectionsClientFacade : ConnectionsClientFacade {
    var advertisingServiceId: String? = null
    var discoveryServiceId: String? = null
    var connectionCallback: NearbyConnectionLifecycleCallback? = null
    var discoveryCallback: NearbyEndpointDiscoveryCallback? = null
    var payloadCallback: NearbyPayloadCallback? = null
    var operationFailureCallback: NearbyOperationFailureCallback? = null
    val acceptedEndpoints = mutableListOf<String>()
    val rejectedEndpoints = mutableListOf<String>()
    val sentPayloads = mutableListOf<Pair<String, ByteArray>>()
    val operations = mutableListOf<String>()
    var onSendPayload: ((String) -> Unit)? = null
    var stopAllEndpointsCalled = false

    override fun startAdvertising(
        localEndpointName: String,
        serviceId: String,
        callback: NearbyConnectionLifecycleCallback,
        failureCallback: NearbyOperationFailureCallback,
    ) {
        operations += "startAdvertising"
        advertisingServiceId = serviceId
        connectionCallback = callback
        operationFailureCallback = failureCallback
    }

    override fun startDiscovery(
        serviceId: String,
        callback: NearbyEndpointDiscoveryCallback,
        failureCallback: NearbyOperationFailureCallback,
    ) {
        operations += "startDiscovery"
        discoveryServiceId = serviceId
        discoveryCallback = callback
        operationFailureCallback = failureCallback
    }

    override fun requestConnection(
        localEndpointName: String,
        endpointId: String,
        callback: NearbyConnectionLifecycleCallback,
        failureCallback: NearbyOperationFailureCallback,
    ) {
        connectionCallback = callback
        operationFailureCallback = failureCallback
    }

    override fun acceptConnection(
        endpointId: String,
        callback: NearbyPayloadCallback,
        failureCallback: NearbyOperationFailureCallback,
    ) {
        acceptedEndpoints += endpointId
        payloadCallback = callback
        operationFailureCallback = failureCallback
    }

    override fun rejectConnection(
        endpointId: String,
        failureCallback: NearbyOperationFailureCallback,
    ) {
        rejectedEndpoints += endpointId
        operationFailureCallback = failureCallback
    }

    override fun sendPayload(
        endpointId: String,
        bytes: ByteArray,
        failureCallback: NearbyOperationFailureCallback,
    ) {
        onSendPayload?.invoke(endpointId)
        sentPayloads += endpointId to bytes
        operationFailureCallback = failureCallback
    }

    override fun stopDiscovery() {
        operations += "stopDiscovery"
    }

    override fun stopAdvertising() {
        operations += "stopAdvertising"
    }

    override fun stopAllEndpoints() {
        operations += "stopAllEndpoints"
        stopAllEndpointsCalled = true
    }
}
