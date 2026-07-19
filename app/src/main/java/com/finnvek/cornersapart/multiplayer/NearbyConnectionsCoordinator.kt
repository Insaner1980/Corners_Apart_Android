package com.finnvek.cornersapart.multiplayer

import com.finnvek.cornersapart.engine.GameEngine
import com.finnvek.cornersapart.engine.MoveRejectedException
import com.finnvek.cornersapart.engine.MoveRejectionReason
import com.finnvek.cornersapart.model.GameConfig
import com.finnvek.cornersapart.model.INVALID_GAME_STATE_INDEX_DOMAINS
import com.finnvek.cornersapart.model.Move
import com.finnvek.cornersapart.model.hasValidIndexDomains
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

@Suppress("TooManyFunctions")
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
    private val reconnectTimeoutJobs = mutableMapOf<Int, Job>()
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
                publishNearbyStateLocked(NearbyUiState(connectionState = ConnectionState.CONNECTED))
                callbackGeneration
            }
        stopNearbyActivity()
        facade.startAdvertising(
            localEndpointName,
            SERVICE_ID,
            CoordinatorConnectionLifecycleCallback(generation),
            CoordinatorOperationFailureCallback(generation),
        )
    }

    fun startDiscovery() {
        val generation =
            synchronized(stateLock) {
                resetEndpointStateLocked()
                advanceCallbackGenerationLocked()
                sessionRole = NearbyRole.CLIENT
                _currentSession.value = null
                publishNearbyStateLocked(NearbyUiState(connectionState = ConnectionState.DISCONNECTED))
                callbackGeneration
            }
        stopNearbyActivity()
        facade.startDiscovery(
            SERVICE_ID,
            CoordinatorEndpointDiscoveryCallback(generation),
            CoordinatorOperationFailureCallback(generation),
        )
    }

    fun connectToEndpoint(endpointId: String) {
        facade.stopDiscovery()
        val generation =
            synchronized(stateLock) {
                resetEndpointStateLocked()
                clearPendingConnectionLocked()
                advanceCallbackGenerationLocked()
                hostEndpointId = endpointId
                callbackGeneration
            }
        facade.requestConnection(
            localEndpointName,
            endpointId,
            CoordinatorConnectionLifecycleCallback(generation),
            CoordinatorOperationFailureCallback(generation),
        )
    }

    fun acceptPendingConnection(endpointId: String) {
        val decision =
            synchronized(stateLock) {
                val pendingEndpointId = _nearbyState.value.pendingConnection?.endpointId
                if (pendingEndpointId == endpointId) {
                    val shouldAccept =
                        sessionRole != NearbyRole.HOST || assignEndpointOwnerLocked(endpointId) != null
                    if (shouldAccept) {
                        approvedEndpointIds += endpointId
                    } else {
                        clearPendingConnectionLocked(endpointId)
                    }
                    shouldAccept to callbackGeneration
                } else {
                    null
                }
            }
        if (decision == null) return
        val (shouldAccept, generation) = decision
        if (shouldAccept) {
            facade.acceptConnection(
                endpointId,
                CoordinatorPayloadCallback(generation),
                CoordinatorOperationFailureCallback(generation, endpointId),
            )
        } else {
            facade.rejectConnection(endpointId, CoordinatorOperationFailureCallback(generation))
        }
    }

    fun rejectPendingConnection(endpointId: String) {
        val generation =
            synchronized(stateLock) {
                clearPendingConnectionLocked(endpointId)
                callbackGeneration
            }
        facade.rejectConnection(endpointId, CoordinatorOperationFailureCallback(generation))
    }

    fun disconnect() {
        stopNearbyActivity()
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
        val (targetEndpointIds, generation) =
            synchronized(stateLock) {
                val endpointIds =
                    when (target) {
                        MessageTarget.Broadcast -> connectedEndpointIds.toList()
                        MessageTarget.Host -> hostEndpointId?.let(::listOf).orEmpty()
                        is MessageTarget.Endpoint -> listOf(target.endpointId)
                    }
                endpointIds to callbackGeneration
            }
        targetEndpointIds.forEach { endpointId ->
            facade.sendPayload(endpointId, bytes, CoordinatorOperationFailureCallback(generation))
        }
    }

    private fun stopNearbyActivity() {
        facade.stopAdvertising()
        facade.stopDiscovery()
        facade.stopAllEndpoints()
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
                        when (error) {
                            null -> null
                            is MoveRejectedException -> _nearbyState.value.errorMessage
                            else -> error.message
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
        reconnectTimeoutJobs.values.forEach(Job::cancel)
        reconnectTimeoutJobs.clear()
        connectedEndpointIds.clear()
        approvedEndpointIds.clear()
        endpointOwnerIndexes.clear()
        hostEndpointId = null
        sessionRole = null
    }

    private fun clearPendingConnectionLocked(endpointId: String? = null) {
        if (endpointId == null) {
            approvedEndpointIds.clear()
        } else {
            approvedEndpointIds -= endpointId
            if (endpointId !in connectedEndpointIds) {
                endpointOwnerIndexes.remove(endpointId)
            }
        }
        val pendingConnection = _nearbyState.value.pendingConnection ?: return
        if (endpointId != null && pendingConnection.endpointId != endpointId) return
        publishNearbyStateLocked(_nearbyState.value.copy(pendingConnection = null))
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
            is GameMessage.PlayerJoined -> false
            is GameMessage.PlayerLeft -> false
            is GameMessage.FullSync,
            is GameMessage.GameConfig,
            is GameMessage.MoveAccepted,
            is GameMessage.MoveRejected,
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

    private fun assignEndpointOwnerLocked(endpointId: String): Int? {
        endpointOwnerIndexes[endpointId]?.let { ownerIndex -> return ownerIndex }
        val state = _currentSession.value?.gameState?.value ?: return null
        val assignedOwners = endpointOwnerIndexes.values.toSet() + LOCAL_OWNER_INDEX
        val nextOwner =
            state.players
                .asSequence()
                .filterNot { player -> player.isComputerControlled }
                .map { player -> player.ownerIndex }
                .distinct()
                .firstOrNull { ownerIndex -> ownerIndex !in assignedOwners }
                ?: return null
        endpointOwnerIndexes[endpointId] = nextOwner
        return nextOwner
    }

    private suspend fun markDisconnectedEndpoint(
        endpointId: String,
        generation: Int,
    ): DisconnectedEndpoint? {
        val disconnectedEndpoint =
            synchronized(stateLock) {
                if (!isCurrentCallbackGenerationLocked(generation)) {
                    return@synchronized null
                }
                val isActiveEndpoint =
                    endpointId in connectedEndpointIds ||
                        endpointId in endpointOwnerIndexes ||
                        endpointId == hostEndpointId
                if (!isActiveEndpoint) {
                    return@synchronized null
                }
                if (sessionRole == NearbyRole.CLIENT && endpointId == hostEndpointId) {
                    resetEndpointStateLocked()
                    advanceCallbackGenerationLocked()
                    _currentSession.value = null
                    publishNearbyStateLocked(NearbyUiState(connectionState = ConnectionState.DISCONNECTED))
                    return@synchronized DisconnectedEndpoint(
                        session = null,
                        ownerIndex = null,
                        playerLeftMessages = emptyList(),
                        clientHostDisconnected = true,
                    )
                }
                val ownerIndex = endpointOwnerIndexes.remove(endpointId)
                connectedEndpointIds -= endpointId
                if (endpointId == hostEndpointId) hostEndpointId = null
                val session = _currentSession.value
                val reconnectingSession =
                    session.takeIf { sessionRole == NearbyRole.HOST && ownerIndex != null }
                val playerLeftMessages =
                    if (sessionRole == NearbyRole.HOST && ownerIndex != null && session != null) {
                        session.gameState.value.players
                            .filter { player -> player.ownerIndex == ownerIndex }
                            .map { player -> GameMessage.PlayerLeft(playerIndex = player.index) }
                    } else {
                        emptyList()
                    }
                DisconnectedEndpoint(
                    session = reconnectingSession,
                    ownerIndex = ownerIndex.takeIf { reconnectingSession != null },
                    playerLeftMessages = playerLeftMessages,
                    clientHostDisconnected = false,
                )
            } ?: return null
        disconnectedEndpoint.session?.let { session ->
            disconnectedEndpoint.playerLeftMessages.forEach { message ->
                session.applyRemoteMessage(endpointId, message)
            }
        }
        return disconnectedEndpoint
    }

    private fun scheduleReconnectTimeout(
        session: NearbySession,
        ownerIndex: Int,
        generation: Int,
    ) {
        synchronized(stateLock) {
            reconnectTimeoutJobs.remove(ownerIndex)?.cancel()
            reconnectTimeoutJobs[ownerIndex] =
                callbackScope.launch {
                    delay(RECONNECT_TIMEOUT_MS)
                    callbackMutex.withLock {
                        val shouldExpire =
                            synchronized(stateLock) {
                                isCurrentCallbackGenerationLocked(generation) &&
                                    _currentSession.value === session &&
                                    ownerIndex !in endpointOwnerIndexes.values
                            }
                        if (!shouldExpire) return@withLock

                        val expired = session.expireReconnect(ownerIndex)
                        synchronized(stateLock) {
                            reconnectTimeoutJobs.remove(ownerIndex)
                            if (expired && isCurrentCallbackGenerationLocked(generation)) {
                                publishNearbyStateLocked(
                                    _nearbyState.value.copy(
                                        connectionState = ConnectionState.FAILED,
                                        errorMessage = RECONNECT_TIMEOUT_ERROR,
                                    ),
                                )
                            }
                        }
                    }
                }
        }
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
            val joinedPlayers =
                synchronized(stateLock) {
                    if (!isCurrentCallbackGenerationLocked(generation)) return
                    if (result == NearbyConnectionResult.Accepted && endpointId in approvedEndpointIds) {
                        connectedEndpointIds += endpointId
                        val ownerIndex =
                            if (sessionRole == NearbyRole.HOST) assignEndpointOwnerLocked(endpointId) else null
                        if (ownerIndex != null) reconnectTimeoutJobs.remove(ownerIndex)?.cancel()
                        publishNearbyStateLocked(
                            _nearbyState.value.copy(
                                connectionState = ConnectionState.CONNECTED,
                                pendingConnection = null,
                                errorMessage = null,
                            ),
                        )
                        val session = _currentSession.value
                        if (session != null && ownerIndex != null) {
                            val reconnectingPlayerIndexes = session.lobbyState.value.reconnectingPlayerIndexes
                            session to
                                session.players.value.filter { player ->
                                    player.ownerIndex == ownerIndex && player.index in reconnectingPlayerIndexes
                                }
                        } else {
                            null
                        }
                    } else {
                        clearPendingConnectionLocked(endpointId)
                        if (sessionRole != NearbyRole.HOST) {
                            publishNearbyStateLocked(
                                _nearbyState.value.copy(
                                    connectionState = ConnectionState.FAILED,
                                    errorMessage = result.connectionFailureMessage(endpointId),
                                ),
                            )
                        }
                        null
                    }
                }
            joinedPlayers?.let { (session, players) ->
                callbackScope.launch(start = CoroutineStart.UNDISPATCHED) {
                    callbackMutex.withLock {
                        sendMessage(MessageTarget.Endpoint(endpointId), GameMessage.FullSync(session.gameState.value))
                        players.forEach { player ->
                            session.applyRemoteMessage(endpointId, GameMessage.PlayerJoined(player))
                        }
                        synchronized(stateLock) {
                            if (isCurrentCallbackGenerationLocked(generation)) {
                                publishNearbyStateLocked(
                                    _nearbyState.value.copy(connectionState = session.connectionState.value),
                                )
                            }
                        }
                    }
                }
            }
        }

        override fun onDisconnected(endpointId: String) {
            synchronized(stateLock) {
                if (!isCurrentCallbackGenerationLocked(generation)) return
                clearPendingConnectionLocked(endpointId)
            }
            callbackScope.launch {
                callbackMutex.withLock {
                    val disconnectedEndpoint = markDisconnectedEndpoint(endpointId, generation)
                    if (disconnectedEndpoint != null) {
                        if (!disconnectedEndpoint.clientHostDisconnected) {
                            synchronized(stateLock) {
                                if (isCurrentCallbackGenerationLocked(generation)) {
                                    publishNearbyStateLocked(
                                        _nearbyState.value.copy(connectionState = ConnectionState.RECONNECTING),
                                    )
                                }
                            }
                        }
                        val session = disconnectedEndpoint.session
                        val ownerIndex = disconnectedEndpoint.ownerIndex
                        if (session != null && ownerIndex != null) {
                            scheduleReconnectTimeout(session, ownerIndex, generation)
                        }
                    }
                }
            }
        }
    }

    private inner class CoordinatorEndpointDiscoveryCallback(
        private val generation: Int,
    ) : NearbyEndpointDiscoveryCallback {
        override fun onEndpointFound(
            endpointId: String,
            endpointName: String,
        ) {
            synchronized(stateLock) {
                if (!isCurrentCallbackGenerationLocked(generation)) return
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
                if (!isCurrentCallbackGenerationLocked(generation)) return
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

    private inner class CoordinatorPayloadCallback(
        private val generation: Int,
    ) : NearbyPayloadCallback {
        override fun onBytesPayload(
            endpointId: String,
            bytes: ByteArray,
        ) {
            callbackScope.launch(start = CoroutineStart.UNDISPATCHED) {
                callbackMutex.withLock {
                    val isCurrentGeneration =
                        synchronized(stateLock) {
                            isCurrentCallbackGenerationLocked(generation)
                        }
                    if (isCurrentGeneration) {
                        handleBytesPayload(endpointId, bytes)
                    }
                }
            }
        }

        override fun onPayloadFailure(endpointId: String) {
            synchronized(stateLock) {
                if (!isCurrentCallbackGenerationLocked(generation)) return
                publishNearbyStateLocked(
                    _nearbyState.value.copy(
                        connectionState = ConnectionState.FAILED,
                        errorMessage = "Payload failed from $endpointId",
                    ),
                )
            }
        }
    }

    private inner class CoordinatorOperationFailureCallback(
        private val generation: Int,
        private val endpointId: String? = null,
    ) : NearbyOperationFailureCallback {
        override fun onOperationFailure(
            operation: NearbyOperation,
            failure: NearbyOperationFailure,
        ) {
            synchronized(stateLock) {
                if (!isCurrentCallbackGenerationLocked(generation)) return
                if (operation == NearbyOperation.ACCEPT_CONNECTION && endpointId != null) {
                    clearPendingConnectionLocked(endpointId)
                }
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
        const val RECONNECT_TIMEOUT_MS = 60_000L
        private const val RECONNECT_TIMEOUT_ERROR = "Player reconnect timed out"
        private const val LOCAL_OWNER_INDEX = 0
    }

    private data class DisconnectedEndpoint(
        val session: NearbySession?,
        val ownerIndex: Int?,
        val playerLeftMessages: List<GameMessage.PlayerLeft>,
        val clientHostDisconnected: Boolean,
    )
}
