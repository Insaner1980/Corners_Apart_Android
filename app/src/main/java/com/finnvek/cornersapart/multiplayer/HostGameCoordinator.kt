package com.finnvek.cornersapart.multiplayer

import com.finnvek.cornersapart.engine.GameEngine
import com.finnvek.cornersapart.engine.MoveRejectedException
import com.finnvek.cornersapart.engine.MoveResult
import com.finnvek.cornersapart.model.GameState
import com.finnvek.cornersapart.model.Move
import com.finnvek.cornersapart.model.hasValidIndexDomains
import com.finnvek.cornersapart.model.toSnapshotCopy

class HostGameCoordinator(
    private val engine: GameEngine = GameEngine(),
    initialState: GameState,
) {
    var state: GameState = initialState.toSnapshotCopy()
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
        if (!nextState.hasValidIndexDomains()) return
        state = nextState.toSnapshotCopy()
    }

    private fun handleMove(
        endpointId: String,
        move: Move,
    ): List<HostMessage> =
        when (val result = engine.applyMove(state, move)) {
            is MoveResult.Accepted -> {
                state = result.state.toSnapshotCopy()
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
                        message = GameMessage.MoveRejected(move = move, reason = result.reason),
                    ),
                )
        }

    private fun handlePass(
        endpointId: String,
        playerIndex: Int,
    ): List<HostMessage> =
        try {
            state = engine.pass(state, playerIndex).toSnapshotCopy()
            listOf(HostMessage(MessageTarget.Broadcast, GameMessage.FullSync(state)))
        } catch (error: MoveRejectedException) {
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
                            reason = error.reason,
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
