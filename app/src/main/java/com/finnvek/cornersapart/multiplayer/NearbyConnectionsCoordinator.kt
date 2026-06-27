package com.finnvek.cornersapart.multiplayer

import com.finnvek.cornersapart.engine.GameEngine
import com.finnvek.cornersapart.engine.MoveRejectedException
import com.finnvek.cornersapart.engine.MoveRejectionReason
import com.finnvek.cornersapart.model.GameConfig
import com.finnvek.cornersapart.model.INVALID_GAME_STATE_INDEX_DOMAINS
import com.finnvek.cornersapart.model.Move
import com.finnvek.cornersapart.model.hasValidIndexDomains
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class NearbyConnectionsCoordinator(
    private val facade: ConnectionsClientFacade,
    private val gameEngine: GameEngine,
    private val localEndpointName: String,
    private val callbackScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
) {
    private val stateLock = Any()
    private val callbackMutex = Mutex()
    private val connectedEndpointIds = linkedSetOf<String>()
    private val approvedEndpointIds = linkedSetOf<String>()
    private val endpointOwnerIndexes = linkedMapOf<String, Int>()
    private val payloadCallback = CoordinatorPayloadCallback()
    private val operationFailureCallback = CoordinatorOperationFailureCallback()
    private var hostEndpointId: String? = null
    private var sessionRole: NearbyRole? = null
    private var callbackGeneration: Int = 0
    private val _nearbyState = MutableStateFlow(NearbyUiState().toSnapshotCopy())
    private val _currentSession = MutableStateFlow<NearbySession?>(null)

    val nearbyState: StateFlow<NearbyUiState> = _nearbyState.asStateFlow()
    val currentSession: StateFlow<NearbyGameSession?> = _currentSession.asStateFlow()

    fun startHosting(config: GameConfig) {
        val session =
            NearbySession.host(
                engine = gameEngine,
                transport = ::sendMessage,
                initialConfig = config,
            )
        val generation =
            synchronized(stateLock) {
                resetEndpointStateLocked()
                advanceCallbackGenerationLocked()
                sessionRole = NearbyRole.HOST
                _currentSession.value = session
                publishNearbyStateLocked(
                    _nearbyState.value.copy(connectionState = ConnectionState.CONNECTED, errorMessage = null),
                )
                callbackGeneration
            }
        facade.startAdvertising(
            localEndpointName,
            SERVICE_ID,
            CoordinatorConnectionLifecycleCallback(generation),
            operationFailureCallback,
        )
    }

    fun startDiscovery() {
        synchronized(stateLock) {
            resetEndpointStateLocked()
            advanceCallbackGenerationLocked()
            publishNearbyStateLocked(
                _nearbyState.value.copy(connectionState = ConnectionState.DISCONNECTED, errorMessage = null),
            )
        }
        facade.startDiscovery(SERVICE_ID, CoordinatorEndpointDiscoveryCallback(), operationFailureCallback)
    }

    fun connectToEndpoint(endpointId: String) {
        facade.stopDiscovery()
        val generation =
            synchronized(stateLock) {
                resetEndpointStateLocked()
                advanceCallbackGenerationLocked()
                hostEndpointId = endpointId
                callbackGeneration
            }
        facade.requestConnection(
            localEndpointName,
            endpointId,
            CoordinatorConnectionLifecycleCallback(generation),
            operationFailureCallback,
        )
    }

    fun acceptPendingConnection(endpointId: String) {
        val shouldAccept =
            synchronized(stateLock) {
                val pendingEndpointId = _nearbyState.value.pendingConnection?.endpointId
                if (pendingEndpointId == endpointId) {
                    approvedEndpointIds += endpointId
                    true
                } else {
                    false
                }
            }
        if (!shouldAccept) return
        facade.acceptConnection(endpointId, payloadCallback, operationFailureCallback)
    }

    fun rejectPendingConnection(endpointId: String) {
        synchronized(stateLock) {
            approvedEndpointIds -= endpointId
        }
        facade.rejectConnection(endpointId, operationFailureCallback)
    }

    fun disconnect() {
        facade.stopAdvertising()
        facade.stopDiscovery()
        facade.stopAllEndpoints()
        synchronized(stateLock) {
            resetEndpointStateLocked()
            advanceCallbackGenerationLocked()
            _currentSession.value = null
            publishNearbyStateLocked(NearbyUiState(connectionState = ConnectionState.DISCONNECTED))
        }
    }

    private suspend fun sendMessage(
        target: MessageTarget,
        message: GameMessage,
    ) {
        val bytes = GameProtocol.encode(message).encodeToByteArray()
        val targetEndpointIds =
            synchronized(stateLock) {
                when (target) {
                    MessageTarget.Broadcast -> connectedEndpointIds.toList()
                    MessageTarget.Host -> hostEndpointId?.let(::listOf).orEmpty()
                    is MessageTarget.Endpoint -> listOf(target.endpointId)
                }
            }
        targetEndpointIds.forEach { endpointId ->
            facade.sendPayload(endpointId, bytes, operationFailureCallback)
        }
    }

    private suspend fun handleBytesPayload(
        endpointId: String,
        bytes: ByteArray,
    ) {
        val message =
            runCatching { GameProtocol.decode(bytes.decodeToString()) }
                .getOrElse { error ->
                    synchronized(stateLock) {
                        publishNearbyStateLocked(
                            _nearbyState.value.copy(
                                connectionState = ConnectionState.FAILED,
                                errorMessage = error.message,
                            ),
                        )
                    }
                    return
                }
        val session =
            synchronized(stateLock) {
                _currentSession.value ?: createClientSessionFromFirstSyncLocked(endpointId, message)
            } ?: return
        val authorized =
            synchronized(stateLock) {
                isAuthorizedInboundMessageLocked(endpointId, message)
            }
        if (!authorized) {
            rejectUnauthorizedMove(endpointId, message)
            return
        }
        val result = session.applyRemoteMessage(endpointId, message)
        val error = result.exceptionOrNull()
        synchronized(stateLock) {
            publishNearbyStateLocked(
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
    }

    private fun createClientSessionFromFirstSyncLocked(
        endpointId: String,
        message: GameMessage,
    ): NearbySession? {
        val sync = message as? GameMessage.FullSync ?: return null
        if (endpointId != hostEndpointId || endpointId !in connectedEndpointIds) return null
        if (!sync.state.hasValidIndexDomains()) {
            publishNearbyStateLocked(
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

    private fun resetEndpointStateLocked() {
        connectedEndpointIds.clear()
        approvedEndpointIds.clear()
        endpointOwnerIndexes.clear()
        hostEndpointId = null
        sessionRole = null
    }

    private fun advanceCallbackGenerationLocked() {
        callbackGeneration += 1
    }

    private fun publishNearbyStateLocked(state: NearbyUiState) {
        _nearbyState.value = state.toSnapshotCopy()
    }

    private fun isCurrentCallbackGenerationLocked(generation: Int): Boolean = generation == callbackGeneration

    private fun isAuthorizedInboundMessageLocked(
        endpointId: String,
        message: GameMessage,
    ): Boolean =
        when (sessionRole) {
            NearbyRole.HOST -> isAuthorizedClientMessageLocked(endpointId, message)
            NearbyRole.CLIENT -> endpointId == hostEndpointId && endpointId in connectedEndpointIds
            null -> false
        }

    private fun isAuthorizedClientMessageLocked(
        endpointId: String,
        message: GameMessage,
    ): Boolean {
        if (endpointId !in connectedEndpointIds) return false
        return when (message) {
            is GameMessage.PlaceMove -> endpointOwnsPlayerLocked(endpointId, message.move.playerIndex)
            is GameMessage.Pass -> endpointOwnsPlayerLocked(endpointId, message.playerIndex)
            is GameMessage.PlayerJoined ->
                endpointOwnerIndexes[endpointId] == message.player.ownerIndex &&
                    endpointOwnsPlayerLocked(endpointId, message.player.index)
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

    private fun endpointOwnsPlayerLocked(
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
        val move =
            when (message) {
                is GameMessage.PlaceMove -> message.move
                is GameMessage.Pass ->
                    Move(
                        playerIndex = message.playerIndex,
                        pieceId = "",
                        anchorRow = 0,
                        anchorCol = 0,
                        orientationIndex = 0,
                    )
                else -> return
            }
        sendMessage(
            MessageTarget.Endpoint(endpointId),
            GameMessage.MoveRejected(move = move, reason = MoveRejectionReason.NOT_PLAYERS_TURN),
        )
    }

    private fun assignEndpointOwnerLocked(endpointId: String) {
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

    private suspend fun markDisconnectedEndpoint(
        endpointId: String,
        generation: Int,
    ): Boolean {
        val (isActiveEndpoint, playerLeftMessages) =
            synchronized(stateLock) {
                if (!isCurrentCallbackGenerationLocked(generation)) {
                    return@synchronized false to emptyList<Pair<NearbySession, GameMessage>>()
                }
                val isActiveEndpoint =
                    endpointId in connectedEndpointIds ||
                        endpointId in endpointOwnerIndexes ||
                        endpointId == hostEndpointId
                if (!isActiveEndpoint) {
                    return@synchronized false to emptyList<Pair<NearbySession, GameMessage>>()
                }
                val ownerIndex = endpointOwnerIndexes.remove(endpointId)
                connectedEndpointIds -= endpointId
                approvedEndpointIds -= endpointId
                if (endpointId == hostEndpointId) hostEndpointId = null
                val session = _currentSession.value
                val messages =
                    if (sessionRole == NearbyRole.HOST && ownerIndex != null && session != null) {
                        session.gameState.value.players
                            .filter { player -> player.ownerIndex == ownerIndex }
                            .map { player -> session to GameMessage.PlayerLeft(playerIndex = player.index) }
                    } else {
                        emptyList()
                    }
                isActiveEndpoint to messages
            }
        playerLeftMessages.forEach { (session, message) ->
            session.applyRemoteMessage(endpointId, message)
        }
        return isActiveEndpoint
    }

    private inner class CoordinatorConnectionLifecycleCallback(
        private val generation: Int,
    ) : NearbyConnectionLifecycleCallback {
        override fun onConnectionInitiated(
            endpointId: String,
            endpointName: String,
            authenticationToken: String,
        ) {
            synchronized(stateLock) {
                if (!isCurrentCallbackGenerationLocked(generation)) return
                publishNearbyStateLocked(
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
        }

        override fun onConnectionResult(
            endpointId: String,
            result: NearbyConnectionResult,
        ) {
            synchronized(stateLock) {
                if (!isCurrentCallbackGenerationLocked(generation)) return
                if (result == NearbyConnectionResult.Accepted && endpointId in approvedEndpointIds) {
                    connectedEndpointIds += endpointId
                    if (sessionRole == NearbyRole.HOST) {
                        assignEndpointOwnerLocked(endpointId)
                    }
                    publishNearbyStateLocked(
                        _nearbyState.value.copy(
                            connectionState = ConnectionState.CONNECTED,
                            pendingConnection = null,
                            errorMessage = null,
                        ),
                    )
                } else {
                    publishNearbyStateLocked(
                        _nearbyState.value.copy(
                            connectionState = ConnectionState.FAILED,
                            errorMessage = result.connectionFailureMessage(endpointId),
                        ),
                    )
                }
            }
        }

        override fun onDisconnected(endpointId: String) {
            callbackScope.launch {
                callbackMutex.withLock {
                    val handledDisconnect = markDisconnectedEndpoint(endpointId, generation)
                    if (handledDisconnect) {
                        synchronized(stateLock) {
                            if (isCurrentCallbackGenerationLocked(generation)) {
                                publishNearbyStateLocked(
                                    _nearbyState.value.copy(connectionState = ConnectionState.RECONNECTING),
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    private inner class CoordinatorEndpointDiscoveryCallback : NearbyEndpointDiscoveryCallback {
        override fun onEndpointFound(
            endpointId: String,
            endpointName: String,
        ) {
            synchronized(stateLock) {
                val existing =
                    _nearbyState.value.discoveredEndpoints.filterNot { endpoint ->
                        endpoint.endpointId ==
                            endpointId
                    }
                publishNearbyStateLocked(
                    _nearbyState.value.copy(
                        discoveredEndpoints = existing + NearbyEndpointUiState(endpointId, endpointName),
                    ),
                )
            }
        }

        override fun onEndpointLost(endpointId: String) {
            synchronized(stateLock) {
                publishNearbyStateLocked(
                    _nearbyState.value.copy(
                        discoveredEndpoints =
                            _nearbyState.value.discoveredEndpoints.filterNot { endpoint ->
                                endpoint.endpointId == endpointId
                            },
                    ),
                )
            }
        }
    }

    private inner class CoordinatorPayloadCallback : NearbyPayloadCallback {
        override fun onBytesPayload(
            endpointId: String,
            bytes: ByteArray,
        ) {
            callbackScope.launch {
                callbackMutex.withLock {
                    handleBytesPayload(endpointId, bytes)
                }
            }
        }

        override fun onPayloadFailure(endpointId: String) {
            synchronized(stateLock) {
                publishNearbyStateLocked(
                    _nearbyState.value.copy(
                        connectionState = ConnectionState.FAILED,
                        errorMessage = "Payload failed from $endpointId",
                    ),
                )
            }
        }
    }

    private inner class CoordinatorOperationFailureCallback : NearbyOperationFailureCallback {
        override fun onOperationFailure(
            operation: NearbyOperation,
            failure: NearbyOperationFailure,
        ) {
            synchronized(stateLock) {
                publishNearbyStateLocked(
                    _nearbyState.value.copy(
                        connectionState = ConnectionState.FAILED,
                        errorMessage = failure.operationFailureMessage(operation),
                    ),
                )
            }
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
