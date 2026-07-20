package com.finnvek.cornersapart.multiplayer

import com.finnvek.cornersapart.engine.GameEngine
import com.finnvek.cornersapart.engine.MoveRejectedException
import com.finnvek.cornersapart.engine.MoveResult
import com.finnvek.cornersapart.model.GameConfig
import com.finnvek.cornersapart.model.GameConstants
import com.finnvek.cornersapart.model.GameMode
import com.finnvek.cornersapart.model.GameModeConfigs
import com.finnvek.cornersapart.model.GameState
import com.finnvek.cornersapart.model.Move
import com.finnvek.cornersapart.model.hasValidIndexDomains
import com.finnvek.cornersapart.model.toSnapshotCopy
import com.finnvek.cornersapart.opponents.ComputerOpponentEngine
import com.finnvek.cornersapart.opponents.OpponentAction
import com.finnvek.cornersapart.opponents.OpponentDifficulty
import com.finnvek.cornersapart.opponents.OpponentStyle
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalForInheritanceCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.atomic.AtomicInteger
import kotlin.random.Random

class LocalSession(
    private val engine: GameEngine = GameEngine(),
    private val opponentEngine: ComputerOpponentEngine = ComputerOpponentEngine(gameEngine = engine),
    internal val opponentDifficulty: OpponentDifficulty = OpponentDifficulty.MEDIUM,
    internal val opponentStyleOverride: OpponentStyle? = null,
    initialConfig: GameConfig = defaultConfig(),
    private val randomSeedProvider: () -> Long = { Random.nextLong() },
) : GameSession {
    private val publication =
        MutableStateFlow(
            engine
                .newGame(initialConfig)
                .toSnapshotCopy()
                .toPublication(),
        )
    private val mutationMutex = Mutex()
    private val replacementVersion = AtomicInteger(0)

    override val sessionType: SessionType = SessionType.LOCAL
    override val players: StateFlow<List<SessionPlayer>> = publication.mapState { state -> state.players }
    override val gameState: StateFlow<GameState> = publication.mapState { state -> state.gameState }
    override val connectionState: StateFlow<ConnectionState> = publication.mapState { state -> state.connectionState }
    override val gameMode: GameMode
        get() = publication.value.gameState.gameMode

    override suspend fun sendMove(move: Move): Result<Unit> =
        mutationMutex.withLock {
            val version = replacementVersion.get()
            when (val result = engine.applyMove(publication.value.gameState, move)) {
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

    // Yleinen catch on tahallinen: istuntoraja muuntaa kaikki virheet Result-arvoiksi
    @Suppress("TooGenericExceptionCaught")
    override suspend fun sendPass(playerIndex: Int): Result<Unit> =
        mutationMutex.withLock {
            try {
                val version = replacementVersion.get()
                val nextState = engine.pass(publication.value.gameState, playerIndex).withComputerActions()
                check(version == replacementVersion.get()) { SESSION_REPLACED_MESSAGE }
                publish(nextState)
                Result.success(Unit)
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                Result.failure(error)
            }
        }

    override fun startNewGame(config: GameConfig) {
        replacementVersion.incrementAndGet()
        val randomSeed = randomSeedProvider().ensureDifferentFrom(publication.value.gameState.randomSeed)
        publish(
            engine.newGame(
                config.copy(
                    randomSeed = randomSeed,
                    bonusTiles = null,
                ),
            ),
        )
    }

    fun replaceState(state: GameState) {
        if (!state.hasValidIndexDomains()) return
        replacementVersion.incrementAndGet()
        publish(state)
    }

    private fun publish(state: GameState) {
        val snapshot = state.toSnapshotCopy()
        publication.value = snapshot.toPublication()
    }

    private fun GameState.toPublication(): LocalSessionPublication =
        LocalSessionPublication(
            gameState = this,
            players = toSessionPlayers(),
            connectionState = ConnectionState.CONNECTED,
        )

    private suspend fun GameState.withComputerActions(): GameState {
        var nextState = this
        while (nextState.shouldPlayComputerTurn()) {
            delay(randomOpponentTurnDelayMillis())
            nextState =
                when (
                    val action =
                        opponentEngine.chooseAction(
                            nextState,
                            nextState.currentPlayerIndex,
                            style =
                                opponentStyleOverride
                                    ?: ComputerOpponentEngine.defaultStyleFor(nextState.currentPlayerIndex),
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

        fun defaultConfig(): GameConfig = defaultConfigFor(GameModeConfigs.defaultMode)

        fun defaultConfigFor(mode: GameMode): GameConfig = GameModeConfigs.defaultGameConfig(mode)

        private fun randomOpponentTurnDelayMillis(): Long =
            GameConstants.OPPONENT_TURN_DELAY_MIN_MS +
                Random.nextLong(GameConstants.OPPONENT_TURN_DELAY_RANGE_MS + 1L)
    }
}

private fun Long.ensureDifferentFrom(previousSeed: Long): Long =
    if (this == previousSeed) this + 1L else this

private data class LocalSessionPublication(
    val gameState: GameState,
    val players: List<SessionPlayer>,
    val connectionState: ConnectionState,
)

@OptIn(ExperimentalForInheritanceCoroutinesApi::class)
private fun <T, R> StateFlow<T>.mapState(transform: (T) -> R): StateFlow<R> =
    object : StateFlow<R> {
        override val value: R
            get() = transform(this@mapState.value)

        override val replayCache: List<R>
            get() = listOf(value)

        override suspend fun collect(collector: FlowCollector<R>): Nothing {
            this@mapState.map(transform).distinctUntilChanged().collect(collector)
            error("StateFlow collection completed unexpectedly.")
        }
    }
