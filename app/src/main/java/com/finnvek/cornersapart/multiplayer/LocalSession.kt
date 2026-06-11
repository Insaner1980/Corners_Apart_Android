package com.finnvek.cornersapart.multiplayer

import com.finnvek.cornersapart.engine.GameEngine
import com.finnvek.cornersapart.engine.MoveResult
import com.finnvek.cornersapart.model.GameConfig
import com.finnvek.cornersapart.model.GameMode
import com.finnvek.cornersapart.model.GameModeConfigs
import com.finnvek.cornersapart.model.GameState
import com.finnvek.cornersapart.model.Move
import com.finnvek.cornersapart.opponents.ComputerOpponentEngine
import com.finnvek.cornersapart.opponents.OpponentAction
import com.finnvek.cornersapart.opponents.OpponentDifficulty
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class LocalSession(
    private val engine: GameEngine = GameEngine(),
    private val opponentEngine: ComputerOpponentEngine = ComputerOpponentEngine(gameEngine = engine),
    private val opponentDifficulty: OpponentDifficulty = OpponentDifficulty.MEDIUM,
    initialConfig: GameConfig = defaultFourPlayerConfig(),
) : GameSession {
    private val _gameState = MutableStateFlow(engine.newGame(initialConfig))
    private val _players = MutableStateFlow(_gameState.value.toSessionPlayers())
    private val _connectionState = MutableStateFlow(ConnectionState.CONNECTED)

    override val sessionType: SessionType = SessionType.LOCAL
    override val players: StateFlow<List<SessionPlayer>> = _players.asStateFlow()
    override val gameState: StateFlow<GameState> = _gameState.asStateFlow()
    override val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()
    override val gameMode: GameMode
        get() = _gameState.value.gameMode

    override suspend fun sendMove(move: Move): Result<Unit> =
        when (val result = engine.applyMove(_gameState.value, move)) {
            is MoveResult.Accepted -> {
                publish(result.state.withComputerActions())
                Result.success(Unit)
            }
            is MoveResult.Rejected -> Result.failure(IllegalArgumentException(result.reason.name))
        }

    override suspend fun sendPass(playerIndex: Int): Result<Unit> =
        runCatching {
            publish(engine.pass(_gameState.value, playerIndex).withComputerActions())
        }

    override fun startNewGame(config: GameConfig) {
        publish(engine.newGame(config))
    }

    private fun publish(state: GameState) {
        _gameState.value = state
        _players.value = state.toSessionPlayers()
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
        gameMode == GameMode.SOLO &&
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
        fun defaultFourPlayerConfig(): GameConfig = defaultConfigFor(GameMode.FOUR_PLAYER)

        fun defaultSoloConfig(): GameConfig = defaultConfigFor(GameMode.SOLO)

        fun defaultTwoColorDuelConfig(): GameConfig = defaultConfigFor(GameMode.TWO_COLOR_DUEL)

        fun defaultCompactDuelConfig(): GameConfig = defaultConfigFor(GameMode.COMPACT_DUEL)

        fun defaultThreePlayerConfig(): GameConfig = defaultConfigFor(GameMode.THREE_PLAYER)

        fun defaultConfigFor(mode: GameMode): GameConfig = GameModeConfigs.defaultGameConfig(mode)
    }
}
