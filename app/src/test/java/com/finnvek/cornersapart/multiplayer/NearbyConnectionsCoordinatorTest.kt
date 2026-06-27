package com.finnvek.cornersapart.multiplayer

import com.finnvek.cornersapart.engine.GameEngine
import com.finnvek.cornersapart.engine.MoveRejectionReason
import com.finnvek.cornersapart.model.GameConfig
import com.finnvek.cornersapart.model.GameMode
import com.finnvek.cornersapart.model.Move
import com.finnvek.cornersapart.model.PieceCatalog
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class NearbyConnectionsCoordinatorTest {
    @Test
    fun startHostingUsesPackageServiceIdAndCreatesSession() =
        runTest {
            val facade = RecordingConnectionsClientFacade()
            val coordinator = createCoordinator(facade)

            coordinator.startHosting(
                GameConfig(mode = GameMode.FOUR_PLAYER, randomSeed = 11L, bonusTiles = emptyList()),
            )

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
    fun acceptAndRejectPendingConnectionCallFacadeMethods() =
        runTest {
            val facade = RecordingConnectionsClientFacade()
            val coordinator = createCoordinator(facade)

            coordinator.startHosting(
                GameConfig(mode = GameMode.FOUR_PLAYER, randomSeed = 13L, bonusTiles = emptyList()),
            )
            facade.connectionCallback?.onConnectionInitiated("endpoint-1", "Phone", "1234")
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
    fun disconnectMarksMappedRemotePlayerReconnecting() =
        runTest {
            val facade = RecordingConnectionsClientFacade()
            val coordinator = createCoordinator(facade)
            hostAndAcceptEndpoint(coordinator = coordinator, facade = facade, randomSeed = 79L)

            facade.connectionCallback?.onDisconnected("endpoint-1")
            advanceUntilIdle()

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
            advanceUntilIdle()

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

    private fun TestScope.createCoordinator(facade: RecordingConnectionsClientFacade): NearbyConnectionsCoordinator =
        NearbyConnectionsCoordinator(
            facade = facade,
            gameEngine = GameEngine(),
            localEndpointName = "Corners Apart",
            callbackScope = this,
        )

    private fun hostAndAcceptEndpoint(
        coordinator: NearbyConnectionsCoordinator,
        facade: RecordingConnectionsClientFacade,
        randomSeed: Long,
        endpointId: String = "endpoint-1",
        endpointName: String = "Phone",
        authenticationToken: String = "1234",
    ) {
        if (coordinator.currentSession.value == null) {
            coordinator.startHosting(
                GameConfig(mode = GameMode.FOUR_PLAYER, randomSeed = randomSeed, bonusTiles = emptyList()),
            )
        }
        facade.connectionCallback?.onConnectionInitiated(endpointId, endpointName, authenticationToken)
        coordinator.acceptPendingConnection(endpointId)
        facade.connectionCallback?.onConnectionResult(endpointId, NearbyConnectionResult.Accepted)
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
    var onSendPayload: ((String) -> Unit)? = null
    var stopAllEndpointsCalled = false

    override fun startAdvertising(
        localEndpointName: String,
        serviceId: String,
        callback: NearbyConnectionLifecycleCallback,
        failureCallback: NearbyOperationFailureCallback,
    ) {
        advertisingServiceId = serviceId
        connectionCallback = callback
        operationFailureCallback = failureCallback
    }

    override fun startDiscovery(
        serviceId: String,
        callback: NearbyEndpointDiscoveryCallback,
        failureCallback: NearbyOperationFailureCallback,
    ) {
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

    override fun stopDiscovery() = Unit

    override fun stopAdvertising() = Unit

    override fun stopAllEndpoints() {
        stopAllEndpointsCalled = true
    }
}
