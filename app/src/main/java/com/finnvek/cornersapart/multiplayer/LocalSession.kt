package com.finnvek.cornersapart.multiplayer

import com.finnvek.cornersapart.engine.GameEngine
import com.finnvek.cornersapart.engine.MoveRejectedException
import com.finnvek.cornersapart.engine.MoveResult
import com.finnvek.cornersapart.model.GameConfig
import com.finnvek.cornersapart.model.GameMode
import com.finnvek.cornersapart.model.GameModeConfigs
import com.finnvek.cornersapart.model.GameState
import com.finnvek.cornersapart.model.Move
import com.finnvek.cornersapart.model.hasValidIndexDomains
import com.finnvek.cornersapart.model.toSnapshotCopy
import com.finnvek.cornersapart.opponents.ComputerOpponentEngine
import com.finnvek.cornersapart.opponents.OpponentAction
import com.finnvek.cornersapart.opponents.OpponentDifficulty
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.atomic.AtomicInteger

class LocalSession(
    private val engine: GameEngine = GameEngine(),
    private val opponentEngine: ComputerOpponentEngine = ComputerOpponentEngine(gameEngine = engine),
    internal val opponentDifficulty: OpponentDifficulty = OpponentDifficulty.MEDIUM,
    initialConfig: GameConfig = defaultFourPlayerConfig(),
) : GameSession {
    private val _gameState = MutableStateFlow(engine.newGame(initialConfig).toSnapshotCopy())
    private val _players = MutableStateFlow(_gameState.value.toSessionPlayers())
    private val _connectionState = MutableStateFlow(ConnectionState.CONNECTED)
    private val mutationMutex = Mutex()
    private val replacementVersion = AtomicInteger(0)

    override val sessionType: SessionType = SessionType.LOCAL
    override val players: StateFlow<List<SessionPlayer>> = _players.asStateFlow()
    override val gameState: StateFlow<GameState> = _gameState.asStateFlow()
    override val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()
    override val gameMode: GameMode
        get() = _gameState.value.gameMode

    override suspend fun sendMove(move: Move): Result<Unit> =
        mutationMutex.withLock {
            val version = replacementVersion.get()
            when (val result = engine.applyMove(_gameState.value, move)) {
                is MoveResult.Accepted -> {
                    val nextState = result.state.withComputerActions()
                    if (version == replacementVersion.get()) {
                        publish(nextState)
                        Result.success(Unit)
                    } else {
                        Result.failure(IllegalStateException(SESSION_REPLACED_MESSAGE))
                    }
                }
                is MoveResult.Rejected -> Result.failure(MoveRejectedException(result.reason))
            }
        }

    override suspend fun sendPass(playerIndex: Int): Result<Unit> =
        mutationMutex.withLock {
            runCatching {
                val version = replacementVersion.get()
                val nextState = engine.pass(_gameState.value, playerIndex).withComputerActions()
                check(version == replacementVersion.get()) { SESSION_REPLACED_MESSAGE }
                publish(nextState)
            }
        }

    override fun startNewGame(config: GameConfig) {
        replacementVersion.incrementAndGet()
        publish(engine.newGame(config))
    }

    override fun replaceState(state: GameState) {
        if (!state.hasValidIndexDomains()) return
        replacementVersion.incrementAndGet()
        publish(state)
    }

    private fun publish(state: GameState) {
        val snapshot = state.toSnapshotCopy()
        _gameState.value = snapshot
        _players.value = snapshot.toSessionPlayers()
    }

    private suspend fun GameState.withComputerActions(): GameState {
        var nextState = this
        while (nextState.shouldPlayComputerTurn()) {
            nextState =
                when (
                    val action =
                        opponentEngine.chooseAction(
                            nextState,
                            nextState.currentPlayerIndex,
                            difficulty = opponentDifficulty,
                        )
                ) {
                    is OpponentAction.PlaceMove -> nextState.applyComputerMove(action.move)
                    is OpponentAction.Pass -> engine.pass(nextState, action.playerIndex)
                }
        }
        return nextState
    }

    private fun GameState.shouldPlayComputerTurn(): Boolean =
        !isGameOver &&
            players[currentPlayerIndex].isComputerControlled

    private fun GameState.applyComputerMove(move: Move): GameState =
        when (val result = engine.applyMove(this, move)) {
            is MoveResult.Accepted -> result.state
            is MoveResult.Rejected -> engine.pass(this, currentPlayerIndex)
        }

    private fun GameState.toSessionPlayers(): List<SessionPlayer> =
        players.map { player ->
            SessionPlayer(
                index = player.index,
                name = player.name,
                isLocal = true,
                isComputerControlled = player.isComputerControlled,
                colorIndex = player.colorIndex,
                ownerIndex = player.ownerIndex,
                usedPieceCount = player.usedPieceIds.size,
            )
        }

    companion object {
        private const val SESSION_REPLACED_MESSAGE = "Session state changed while applying action."

        fun defaultFourPlayerConfig(): GameConfig = defaultConfigFor(GameMode.FOUR_PLAYER)

        fun defaultSoloConfig(): GameConfig = defaultConfigFor(GameMode.SOLO)

        fun defaultTwoColorDuelConfig(): GameConfig = defaultConfigFor(GameMode.TWO_COLOR_DUEL)

        fun defaultCompactDuelConfig(): GameConfig = defaultConfigFor(GameMode.COMPACT_DUEL)

        fun defaultThreePlayerConfig(): GameConfig = defaultConfigFor(GameMode.THREE_PLAYER)

        fun defaultConfigFor(mode: GameMode): GameConfig = GameModeConfigs.defaultGameConfig(mode)
    }
}
