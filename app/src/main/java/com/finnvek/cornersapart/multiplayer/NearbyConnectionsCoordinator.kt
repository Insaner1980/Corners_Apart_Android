package com.finnvek.cornersapart.multiplayer

import com.finnvek.cornersapart.engine.GameEngine
import com.finnvek.cornersapart.engine.MoveRejectedException
import com.finnvek.cornersapart.engine.MoveRejectionReason
import com.finnvek.cornersapart.model.GameConfig
import com.finnvek.cornersapart.model.INVALID_GAME_STATE_INDEX_DOMAINS
import com.finnvek.cornersapart.model.hasValidIndexDomains
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.runBlocking

class NearbyConnectionsCoordinator(
    private val facade: ConnectionsClientFacade,
    private val gameEngine: GameEngine,
    private val localEndpointName: String,
) {
    private val connectedEndpointIds = linkedSetOf<String>()
    private val approvedEndpointIds = linkedSetOf<String>()
    private val endpointOwnerIndexes = linkedMapOf<String, Int>()
    private val lifecycleCallback = CoordinatorConnectionLifecycleCallback()
    private val payloadCallback = CoordinatorPayloadCallback()
    private val operationFailureCallback = CoordinatorOperationFailureCallback()
    private var hostEndpointId: String? = null
    private var sessionRole: NearbyRole? = null
    private val _nearbyState = MutableStateFlow(NearbyUiState().toSnapshotCopy())
    private val _currentSession = MutableStateFlow<NearbySession?>(null)

    val nearbyState: StateFlow<NearbyUiState> = _nearbyState.asStateFlow()
    val currentSession: StateFlow<NearbySession?> = _currentSession.asStateFlow()

    fun startHosting(config: GameConfig) {
        resetEndpointState()
        sessionRole = NearbyRole.HOST
        _currentSession.value =
            NearbySession.host(
                engine = gameEngine,
                transport = ::sendMessage,
                initialConfig = config,
            )
        publishNearbyState(_nearbyState.value.copy(connectionState = ConnectionState.CONNECTED, errorMessage = null))
        facade.startAdvertising(localEndpointName, SERVICE_ID, lifecycleCallback, operationFailureCallback)
    }

    fun startDiscovery() {
        resetEndpointState()
        facade.startDiscovery(SERVICE_ID, CoordinatorEndpointDiscoveryCallback(), operationFailureCallback)
        publishNearbyState(
            _nearbyState.value.copy(connectionState = ConnectionState.DISCONNECTED, errorMessage = null),
        )
    }

    fun connectToEndpoint(endpointId: String) {
        facade.stopDiscovery()
        resetEndpointState()
        hostEndpointId = endpointId
        facade.requestConnection(localEndpointName, endpointId, lifecycleCallback, operationFailureCallback)
    }

    fun acceptPendingConnection(endpointId: String) {
        val pendingEndpointId = _nearbyState.value.pendingConnection?.endpointId ?: return
        if (pendingEndpointId != endpointId) return
        approvedEndpointIds += endpointId
        facade.acceptConnection(endpointId, payloadCallback, operationFailureCallback)
    }

    fun rejectPendingConnection(endpointId: String) {
        approvedEndpointIds -= endpointId
        facade.rejectConnection(endpointId, operationFailureCallback)
    }

    fun disconnect() {
        facade.stopAdvertising()
        facade.stopDiscovery()
        facade.stopAllEndpoints()
        resetEndpointState()
        _currentSession.value = null
        publishNearbyState(NearbyUiState(connectionState = ConnectionState.DISCONNECTED))
    }

    private suspend fun sendMessage(
        target: MessageTarget,
        message: GameMessage,
    ) {
        val bytes = GameProtocol.encode(message).encodeToByteArray()
        when (target) {
            MessageTarget.Broadcast ->
                connectedEndpointIds.forEach { endpointId ->
                    facade.sendPayload(endpointId, bytes, operationFailureCallback)
                }
            MessageTarget.Host ->
                hostEndpointId?.let { endpointId ->
                    facade.sendPayload(endpointId, bytes, operationFailureCallback)
                }
            is MessageTarget.Endpoint -> facade.sendPayload(target.endpointId, bytes, operationFailureCallback)
        }
    }

    private suspend fun handleBytesPayload(
        endpointId: String,
        bytes: ByteArray,
    ) {
        val message =
            runCatching { GameProtocol.decode(bytes.decodeToString()) }
                .getOrElse { error ->
                    publishNearbyState(
                        _nearbyState.value.copy(
                            connectionState = ConnectionState.FAILED,
                            errorMessage = error.message,
                        ),
                    )
                    return
                }
        val session =
            _currentSession.value ?: createClientSessionFromFirstSync(endpointId, message) ?: return
        if (!isAuthorizedInboundMessage(endpointId, message)) {
            rejectUnauthorizedMove(endpointId, message)
            return
        }
        val result = session.applyRemoteMessage(endpointId, message)
        val error = result.exceptionOrNull()
        publishNearbyState(
            _nearbyState.value.copy(
                connectionState = session.connectionState.value,
                errorMessage =
                    if (error == null || error is MoveRejectedException) {
                        _nearbyState.value.errorMessage
                    } else {
                        error.message
                    },
            ),
        )
    }

    private fun createClientSessionFromFirstSync(
        endpointId: String,
        message: GameMessage,
    ): NearbySession? {
        val sync = message as? GameMessage.FullSync ?: return null
        if (endpointId != hostEndpointId || endpointId !in connectedEndpointIds) return null
        if (!sync.state.hasValidIndexDomains()) {
            publishNearbyState(
                _nearbyState.value.copy(
                    connectionState = ConnectionState.FAILED,
                    errorMessage = INVALID_GAME_STATE_INDEX_DOMAINS,
                ),
            )
            return null
        }
        sessionRole = NearbyRole.CLIENT
        hostEndpointId = endpointId
        return NearbySession
            .client(
                engine = gameEngine,
                transport = ::sendMessage,
                initialState = sync.state,
            ).also { session -> _currentSession.value = session }
    }

    private fun resetEndpointState() {
        connectedEndpointIds.clear()
        approvedEndpointIds.clear()
        endpointOwnerIndexes.clear()
        hostEndpointId = null
        sessionRole = null
    }

    private fun publishNearbyState(state: NearbyUiState) {
        _nearbyState.value = state.toSnapshotCopy()
    }

    private fun isAuthorizedInboundMessage(
        endpointId: String,
        message: GameMessage,
    ): Boolean =
        when (sessionRole) {
            NearbyRole.HOST -> isAuthorizedClientMessage(endpointId, message)
            NearbyRole.CLIENT -> endpointId == hostEndpointId && endpointId in connectedEndpointIds
            null -> false
        }

    private fun isAuthorizedClientMessage(
        endpointId: String,
        message: GameMessage,
    ): Boolean {
        if (endpointId !in connectedEndpointIds) return false
        return when (message) {
            is GameMessage.PlaceMove -> endpointOwnsPlayer(endpointId, message.move.playerIndex)
            is GameMessage.Pass -> endpointOwnsPlayer(endpointId, message.playerIndex)
            is GameMessage.PlayerJoined ->
                endpointOwnerIndexes[endpointId] == message.player.ownerIndex &&
                    endpointOwnsPlayer(endpointId, message.player.index)
            is GameMessage.PlayerLeft -> false
            GameMessage.Ping -> true
            is GameMessage.FullSync,
            is GameMessage.GameConfig,
            is GameMessage.MoveAccepted,
            is GameMessage.MoveRejected,
            GameMessage.Pong,
            -> false
        }
    }

    private fun endpointOwnsPlayer(
        endpointId: String,
        playerIndex: Int,
    ): Boolean {
        val ownerIndex = endpointOwnerIndexes[endpointId] ?: return false
        val player =
            _currentSession.value
                ?.gameState
                ?.value
                ?.players
                ?.firstOrNull { candidate -> candidate.index == playerIndex }
        return player?.ownerIndex == ownerIndex
    }

    private suspend fun rejectUnauthorizedMove(
        endpointId: String,
        message: GameMessage,
    ) {
        val move = (message as? GameMessage.PlaceMove)?.move ?: return
        sendMessage(
            MessageTarget.Endpoint(endpointId),
            GameMessage.MoveRejected(move = move, reason = MoveRejectionReason.NOT_PLAYERS_TURN),
        )
    }

    private fun assignEndpointOwner(endpointId: String) {
        if (endpointId in endpointOwnerIndexes) return
        val state = _currentSession.value?.gameState?.value ?: return
        val assignedOwners = endpointOwnerIndexes.values.toSet() + LOCAL_OWNER_INDEX
        val nextOwner =
            state.players
                .asSequence()
                .filterNot { player -> player.isComputerControlled }
                .map { player -> player.ownerIndex }
                .distinct()
                .firstOrNull { ownerIndex -> ownerIndex !in assignedOwners }
                ?: return
        endpointOwnerIndexes[endpointId] = nextOwner
    }

    private suspend fun markDisconnectedEndpoint(endpointId: String) {
        val ownerIndex = endpointOwnerIndexes.remove(endpointId)
        connectedEndpointIds -= endpointId
        approvedEndpointIds -= endpointId
        if (endpointId == hostEndpointId) hostEndpointId = null
        val session = _currentSession.value ?: return
        if (sessionRole != NearbyRole.HOST || ownerIndex == null) return
        session.gameState.value.players
            .filter { player -> player.ownerIndex == ownerIndex }
            .forEach { player ->
                session.applyRemoteMessage(endpointId, GameMessage.PlayerLeft(playerIndex = player.index))
            }
    }

    private inner class CoordinatorConnectionLifecycleCallback : NearbyConnectionLifecycleCallback {
        override fun onConnectionInitiated(
            endpointId: String,
            endpointName: String,
            authenticationToken: String,
        ) {
            publishNearbyState(
                _nearbyState.value.copy(
                    pendingConnection =
                        NearbyPendingConnection(
                            endpointId = endpointId,
                            endpointName = endpointName,
                            authenticationToken = authenticationToken,
                        ),
                ),
            )
        }

        override fun onConnectionResult(
            endpointId: String,
            result: NearbyConnectionResult,
        ) {
            if (result == NearbyConnectionResult.Accepted && endpointId in approvedEndpointIds) {
                connectedEndpointIds += endpointId
                if (sessionRole == NearbyRole.HOST) {
                    assignEndpointOwner(endpointId)
                }
                publishNearbyState(
                    _nearbyState.value.copy(
                        connectionState = ConnectionState.CONNECTED,
                        pendingConnection = null,
                        errorMessage = null,
                    ),
                )
            } else {
                publishNearbyState(
                    _nearbyState.value.copy(
                        connectionState = ConnectionState.FAILED,
                        errorMessage = result.connectionFailureMessage(endpointId),
                    ),
                )
            }
        }

        override fun onDisconnected(endpointId: String) {
            runBlocking {
                markDisconnectedEndpoint(endpointId)
            }
            publishNearbyState(_nearbyState.value.copy(connectionState = ConnectionState.RECONNECTING))
        }
    }

    private inner class CoordinatorEndpointDiscoveryCallback : NearbyEndpointDiscoveryCallback {
        override fun onEndpointFound(
            endpointId: String,
            endpointName: String,
        ) {
            val existing =
                _nearbyState.value.discoveredEndpoints.filterNot { endpoint ->
                    endpoint.endpointId ==
                        endpointId
                }
            publishNearbyState(
                _nearbyState.value.copy(
                    discoveredEndpoints = existing + NearbyEndpointUiState(endpointId, endpointName),
                ),
            )
        }

        override fun onEndpointLost(endpointId: String) {
            publishNearbyState(
                _nearbyState.value.copy(
                    discoveredEndpoints =
                        _nearbyState.value.discoveredEndpoints.filterNot { endpoint ->
                            endpoint.endpointId == endpointId
                        },
                ),
            )
        }
    }

    private inner class CoordinatorPayloadCallback : NearbyPayloadCallback {
        override fun onBytesPayload(
            endpointId: String,
            bytes: ByteArray,
        ) {
            runBlocking {
                handleBytesPayload(endpointId, bytes)
            }
        }

        override fun onPayloadFailure(endpointId: String) {
            publishNearbyState(
                _nearbyState.value.copy(
                    connectionState = ConnectionState.FAILED,
                    errorMessage = "Payload failed from $endpointId",
                ),
            )
        }
    }

    private inner class CoordinatorOperationFailureCallback : NearbyOperationFailureCallback {
        override fun onOperationFailure(
            operation: NearbyOperation,
            failure: NearbyOperationFailure,
        ) {
            publishNearbyState(
                _nearbyState.value.copy(
                    connectionState = ConnectionState.FAILED,
                    errorMessage = failure.operationFailureMessage(operation),
                ),
            )
        }
    }

    private fun NearbyConnectionResult.connectionFailureMessage(endpointId: String): String =
        when (this) {
            NearbyConnectionResult.Accepted -> "Connection failed: $endpointId was not approved"
            is NearbyConnectionResult.Failed -> "Connection failed${statusCodeText()}: ${message.orUnknownFailure()}"
        }

    private fun NearbyOperationFailure.operationFailureMessage(operation: NearbyOperation): String =
        "$operation failed${statusCodeText()}: ${message.orUnknownFailure()}"

    private fun NearbyConnectionResult.Failed.statusCodeText(): String =
        statusCode
            ?.let { code ->
                " ($code)"
            }.orEmpty()

    private fun NearbyOperationFailure.statusCodeText(): String =
        statusCode
            ?.let { code ->
                " ($code)"
            }.orEmpty()

    private fun String?.orUnknownFailure(): String = takeUnless { it.isNullOrBlank() } ?: "Unknown failure"

    companion object {
        const val SERVICE_ID = "com.finnvek.cornersapart"
        private const val LOCAL_OWNER_INDEX = 0
    }
}
