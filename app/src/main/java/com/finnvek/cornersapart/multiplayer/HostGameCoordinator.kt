package com.finnvek.cornersapart.multiplayer

import com.finnvek.cornersapart.engine.GameEngine
import com.finnvek.cornersapart.engine.MoveRejectionReason
import com.finnvek.cornersapart.engine.MoveResult
import com.finnvek.cornersapart.model.GameState
import com.finnvek.cornersapart.model.Move

class HostGameCoordinator(
    private val engine: GameEngine = GameEngine(),
    initialState: GameState,
) {
    var state: GameState = initialState
        private set

    fun handle(
        endpointId: String,
        message: GameMessage,
    ): List<HostMessage> =
        when (message) {
            is GameMessage.PlaceMove -> handleMove(endpointId, message.move)
            is GameMessage.Pass -> handlePass(endpointId, message.playerIndex)
            is GameMessage.PlayerJoined -> handlePlayerJoined(endpointId, message.player)
            is GameMessage.PlayerLeft -> listOf(HostMessage(MessageTarget.Broadcast, message))
            GameMessage.Ping -> listOf(HostMessage(MessageTarget.Endpoint(endpointId), GameMessage.Pong))
            is GameMessage.FullSync,
            is GameMessage.GameConfig,
            is GameMessage.MoveAccepted,
            is GameMessage.MoveRejected,
            GameMessage.Pong,
            -> emptyList()
        }

    fun replaceState(nextState: GameState) {
        state = nextState
    }

    private fun handleMove(
        endpointId: String,
        move: Move,
    ): List<HostMessage> =
        when (val result = engine.applyMove(state, move)) {
            is MoveResult.Accepted -> {
                state = result.state
                listOf(
                    HostMessage(
                        target = MessageTarget.Broadcast,
                        message = GameMessage.MoveAccepted(move = move, state = state, scoreDelta = result.scoreDelta),
                    ),
                )
            }
            is MoveResult.Rejected ->
                listOf(
                    HostMessage(
                        target = MessageTarget.Endpoint(endpointId),
                        message = GameMessage.MoveRejected(move = move, reason = result.reason.name),
                    ),
                )
        }

    private fun handlePass(
        endpointId: String,
        playerIndex: Int,
    ): List<HostMessage> =
        runCatching {
            state = engine.pass(state, playerIndex)
            listOf(HostMessage(MessageTarget.Broadcast, GameMessage.FullSync(state)))
        }.getOrElse { error ->
            listOf(
                HostMessage(
                    target = MessageTarget.Endpoint(endpointId),
                    message =
                        GameMessage.MoveRejected(
                            move =
                                Move(
                                    playerIndex = playerIndex,
                                    pieceId = "",
                                    anchorRow = 0,
                                    anchorCol = 0,
                                    orientationIndex = 0,
                                ),
                            reason = error.message ?: MoveRejectionReason.NOT_PLAYERS_TURN.name,
                        ),
                ),
            )
        }

    private fun handlePlayerJoined(
        endpointId: String,
        player: SessionPlayer,
    ): List<HostMessage> =
        listOf(
            HostMessage(MessageTarget.Broadcast, GameMessage.PlayerJoined(player)),
            HostMessage(MessageTarget.Endpoint(endpointId), GameMessage.FullSync(state)),
        )
}

data class HostMessage(
    val target: MessageTarget,
    val message: GameMessage,
)

sealed interface MessageTarget {
    data object Broadcast : MessageTarget

    data object Host : MessageTarget

    data class Endpoint(
        val endpointId: String,
    ) : MessageTarget
}
