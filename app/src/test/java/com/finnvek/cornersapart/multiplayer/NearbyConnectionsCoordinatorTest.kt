package com.finnvek.cornersapart.multiplayer

import com.finnvek.cornersapart.engine.GameEngine
import com.finnvek.cornersapart.model.GameConfig
import com.finnvek.cornersapart.model.GameMode
import com.finnvek.cornersapart.model.Move
import com.finnvek.cornersapart.model.PieceCatalog
import com.google.android.gms.nearby.connection.Strategy
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class NearbyConnectionsCoordinatorTest {
    @Test
    fun startHostingUsesPackageServiceIdAndP2pStarStrategy() =
        runTest {
            val facade = RecordingConnectionsClientFacade()
            val coordinator = createCoordinator(facade)

            coordinator.startHosting(
                GameConfig(mode = GameMode.FOUR_PLAYER, randomSeed = 11L, bonusTiles = emptyList()),
            )

            assertEquals(NearbyConnectionsCoordinator.SERVICE_ID, facade.advertisingServiceId)
            assertEquals(Strategy.P2P_STAR, facade.advertisingStrategy)
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
            assertEquals(Strategy.P2P_STAR, facade.discoveryStrategy)
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
            coordinator.startHosting(
                GameConfig(mode = GameMode.FOUR_PLAYER, randomSeed = 17L, bonusTiles = emptyList()),
            )
            facade.connectionCallback?.onConnectionInitiated("endpoint-1", "Phone", "1234")
            coordinator.acceptPendingConnection("endpoint-1")
            facade.connectionCallback?.onConnectionResult("endpoint-1", true)
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
    fun clientIgnoresFullSyncFromEndpointThatWasNotSelectedHost() =
        runTest {
            val facade = RecordingConnectionsClientFacade()
            val coordinator = createCoordinator(facade)
            val state =
                GameEngine().newGame(
                    GameConfig(mode = GameMode.FOUR_PLAYER, randomSeed = 73L, bonusTiles = emptyList()),
                )

            coordinator.startDiscovery()
            coordinator.connectToEndpoint("host-1")
            facade.connectionCallback?.onConnectionInitiated("host-1", "Host", "9876")
            coordinator.acceptPendingConnection("host-1")
            facade.connectionCallback?.onConnectionResult("host-1", true)
            facade.payloadCallback?.onBytesPayload(
                "host-2",
                GameProtocol.encode(GameMessage.FullSync(state)).encodeToByteArray(),
            )

            assertNull(coordinator.currentSession.value)
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

            assertTrue(facade.sentPayloads.isEmpty())
        }

    @Test
    fun disconnectMarksMappedRemotePlayerReconnecting() =
        runTest {
            val facade = RecordingConnectionsClientFacade()
            val coordinator = createCoordinator(facade)
            coordinator.startHosting(
                GameConfig(mode = GameMode.FOUR_PLAYER, randomSeed = 79L, bonusTiles = emptyList()),
            )
            facade.connectionCallback?.onConnectionInitiated("endpoint-1", "Phone", "1234")
            coordinator.acceptPendingConnection("endpoint-1")
            facade.connectionCallback?.onConnectionResult("endpoint-1", true)

            facade.connectionCallback?.onDisconnected("endpoint-1")

            assertEquals(
                setOf(1),
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

    private fun createCoordinator(facade: RecordingConnectionsClientFacade): NearbyConnectionsCoordinator =
        NearbyConnectionsCoordinator(
            facade = facade,
            gameEngine = GameEngine(),
            localEndpointName = "Corners Apart",
        )

    private fun hostAndAcceptEndpoint(
        coordinator: NearbyConnectionsCoordinator,
        facade: RecordingConnectionsClientFacade,
        randomSeed: Long,
    ) {
        coordinator.startHosting(
            GameConfig(mode = GameMode.FOUR_PLAYER, randomSeed = randomSeed, bonusTiles = emptyList()),
        )
        facade.connectionCallback?.onConnectionInitiated("endpoint-1", "Phone", "1234")
        coordinator.acceptPendingConnection("endpoint-1")
        facade.connectionCallback?.onConnectionResult("endpoint-1", true)
    }
}

private class RecordingConnectionsClientFacade : ConnectionsClientFacade {
    var advertisingServiceId: String? = null
    var advertisingStrategy: Strategy? = null
    var discoveryServiceId: String? = null
    var discoveryStrategy: Strategy? = null
    var connectionCallback: NearbyConnectionLifecycleCallback? = null
    var discoveryCallback: NearbyEndpointDiscoveryCallback? = null
    var payloadCallback: NearbyPayloadCallback? = null
    val acceptedEndpoints = mutableListOf<String>()
    val rejectedEndpoints = mutableListOf<String>()
    val sentPayloads = mutableListOf<Pair<String, ByteArray>>()
    var stopAllEndpointsCalled = false

    override fun startAdvertising(
        localEndpointName: String,
        serviceId: String,
        strategy: Strategy,
        callback: NearbyConnectionLifecycleCallback,
    ) {
        advertisingServiceId = serviceId
        advertisingStrategy = strategy
        connectionCallback = callback
    }

    override fun startDiscovery(
        serviceId: String,
        strategy: Strategy,
        callback: NearbyEndpointDiscoveryCallback,
    ) {
        discoveryServiceId = serviceId
        discoveryStrategy = strategy
        discoveryCallback = callback
    }

    override fun requestConnection(
        localEndpointName: String,
        endpointId: String,
        callback: NearbyConnectionLifecycleCallback,
    ) {
        connectionCallback = callback
    }

    override fun acceptConnection(
        endpointId: String,
        callback: NearbyPayloadCallback,
    ) {
        acceptedEndpoints += endpointId
        payloadCallback = callback
    }

    override fun rejectConnection(endpointId: String) {
        rejectedEndpoints += endpointId
    }

    override fun sendPayload(
        endpointId: String,
        bytes: ByteArray,
    ) {
        sentPayloads += endpointId to bytes
    }

    override fun stopDiscovery() = Unit

    override fun stopAdvertising() = Unit

    override fun stopAllEndpoints() {
        stopAllEndpointsCalled = true
    }
}
