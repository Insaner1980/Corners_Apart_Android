package com.finnvek.cornersapart.multiplayer

import com.finnvek.cornersapart.engine.GameEngine
import com.finnvek.cornersapart.model.GameConfig
import com.finnvek.cornersapart.model.GameMode
import com.finnvek.cornersapart.model.GameState
import com.finnvek.cornersapart.model.Move
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class NearbySession private constructor(
    private val role: NearbyRole,
    private val engine: GameEngine,
    private val transport: NearbyTransport,
    private var coordinator: HostGameCoordinator?,
    initialState: GameState,
) : GameSession {
    private val _gameState = MutableStateFlow(initialState)
    private val _players = MutableStateFlow(initialState.toSessionPlayers())
    private val _lobbyState = MutableStateFlow(NearbyLobbyState(connectedPlayers = initialState.toSessionPlayers()))
    private val _connectionState = MutableStateFlow(ConnectionState.CONNECTED)

    override val sessionType: SessionType = SessionType.NEARBY
    override val players: StateFlow<List<SessionPlayer>> = _players.asStateFlow()
    override val gameState: StateFlow<GameState> = _gameState.asStateFlow()
    override val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()
    val lobbyState: StateFlow<NearbyLobbyState> = _lobbyState.asStateFlow()
    override val gameMode: GameMode
        get() = _gameState.value.gameMode

    override suspend fun sendMove(move: Move): Result<Unit> =
        if (role == NearbyRole.HOST) {
            handleHostMessage(GameMessage.PlaceMove(move))
        } else {
            transport.send(MessageTarget.Host, GameMessage.PlaceMove(move))
            Result.success(Unit)
        }

    override suspend fun sendPass(playerIndex: Int): Result<Unit> =
        if (role == NearbyRole.HOST) {
            handleHostMessage(GameMessage.Pass(playerIndex))
        } else {
            transport.send(MessageTarget.Host, GameMessage.Pass(playerIndex))
            Result.success(Unit)
        }

    override fun startNewGame(config: GameConfig) {
        check(role == NearbyRole.HOST) { "Only the Nearby host can start a new game." }
        val state = engine.newGame(config)
        coordinator = HostGameCoordinator(engine = engine, initialState = state)
        publish(state)
    }

    override fun replaceState(state: GameState) {
        if (role == NearbyRole.HOST) {
            coordinator?.replaceState(state)
        }
        publish(state)
    }

    suspend fun applyRemoteMessage(
        endpointId: String,
        message: GameMessage,
    ): Result<Unit> =
        if (role == NearbyRole.HOST) {
            handleHostMessage(message, endpointId)
        } else {
            applyClientMessage(message)
            Result.success(Unit)
        }

    private suspend fun handleHostMessage(
        message: GameMessage,
        endpointId: String = LOCAL_ENDPOINT_ID,
    ): Result<Unit> {
        val host = checkNotNull(coordinator) { "Nearby host coordinator is not initialized." }
        val outputs = host.handle(endpointId, message)
        val rejection = outputs.firstOrNull { output -> output.message is GameMessage.MoveRejected }
        outputs.forEach { output -> transport.send(output.target, output.message) }
        applyLobbyMessage(message)
        publish(host.state)
        return if (rejection == null) {
            Result.success(Unit)
        } else {
            Result.failure(IllegalArgumentException((rejection.message as GameMessage.MoveRejected).reason))
        }
    }

    private fun applyClientMessage(message: GameMessage) {
        when (message) {
            is GameMessage.FullSync -> publish(message.state)
            is GameMessage.MoveAccepted -> publish(message.state)
            is GameMessage.PlayerJoined,
            is GameMessage.PlayerLeft,
            -> applyLobbyMessage(message)
            else -> Unit
        }
    }

    private fun publish(state: GameState) {
        val sessionPlayers = state.toSessionPlayers()
        _gameState.value = state
        _players.value = sessionPlayers
        _lobbyState.value = _lobbyState.value.copy(connectedPlayers = sessionPlayers)
        refreshConnectionState()
    }

    private fun applyLobbyMessage(message: GameMessage) {
        when (message) {
            is GameMessage.PlayerLeft -> markPlayerReconnecting(message.playerIndex)
            is GameMessage.PlayerJoined -> markPlayerConnected(message.player)
            else -> Unit
        }
    }

    private fun markPlayerReconnecting(playerIndex: Int) {
        _lobbyState.value =
            _lobbyState.value.copy(
                reconnectingPlayerIndexes = _lobbyState.value.reconnectingPlayerIndexes + playerIndex,
            )
        refreshConnectionState()
    }

    private fun markPlayerConnected(player: SessionPlayer) {
        val connectedPlayers =
            _lobbyState.value.connectedPlayers.map { existing ->
                if (existing.index == player.index) player else existing
            }
        _lobbyState.value =
            _lobbyState.value.copy(
                connectedPlayers = connectedPlayers,
                reconnectingPlayerIndexes = _lobbyState.value.reconnectingPlayerIndexes - player.index,
            )
        refreshConnectionState()
    }

    private fun refreshConnectionState() {
        _connectionState.value =
            if (_lobbyState.value.reconnectingPlayerIndexes.isEmpty()) {
                ConnectionState.CONNECTED
            } else {
                ConnectionState.RECONNECTING
            }
    }

    private fun GameState.toSessionPlayers(): List<SessionPlayer> =
        players.map { player ->
            SessionPlayer(
                index = player.index,
                name = player.name,
                isLocal = role == NearbyRole.HOST,
                isComputerControlled = player.isComputerControlled,
                colorIndex = player.colorIndex,
                ownerIndex = player.ownerIndex,
                usedPieceCount = player.usedPieceIds.size,
            )
        }

    companion object {
        private const val LOCAL_ENDPOINT_ID = "local"

        fun host(
            engine: GameEngine = GameEngine(),
            transport: NearbyTransport,
            initialConfig: GameConfig = LocalSession.defaultFourPlayerConfig(),
        ): NearbySession {
            val state = engine.newGame(initialConfig)
            return NearbySession(
                role = NearbyRole.HOST,
                engine = engine,
                transport = transport,
                coordinator = HostGameCoordinator(engine = engine, initialState = state),
                initialState = state,
            )
        }

        fun client(
            engine: GameEngine = GameEngine(),
            transport: NearbyTransport,
            initialState: GameState,
        ): NearbySession =
            NearbySession(
                role = NearbyRole.CLIENT,
                engine = engine,
                transport = transport,
                coordinator = null,
                initialState = initialState,
            )
    }
}

enum class NearbyRole {
    HOST,
    CLIENT,
}

fun interface NearbyTransport {
    suspend fun send(
        target: MessageTarget,
        message: GameMessage,
    )
}
