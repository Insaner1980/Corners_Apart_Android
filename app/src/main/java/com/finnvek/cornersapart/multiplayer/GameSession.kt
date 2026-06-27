package com.finnvek.cornersapart.multiplayer

import com.finnvek.cornersapart.engine.MoveRejectionReason
import com.finnvek.cornersapart.model.GameConfig
import com.finnvek.cornersapart.model.GameMode
import com.finnvek.cornersapart.model.GameState
import com.finnvek.cornersapart.model.Move
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

interface GameSession {
    val sessionType: SessionType
    val gameMode: GameMode
    val players: StateFlow<List<SessionPlayer>>
    val gameState: StateFlow<GameState>
    val connectionState: StateFlow<ConnectionState>

    suspend fun sendMove(move: Move): Result<Unit>

    suspend fun sendPass(playerIndex: Int): Result<Unit>

    fun startNewGame(config: GameConfig)
}

interface NearbyGameSession : GameSession {
    val lobbyState: StateFlow<NearbyLobbyState>
    val events: SharedFlow<GameSessionEvent>
}

sealed interface GameSessionEvent {
    data class MoveRejected(
        val reason: MoveRejectionReason,
    ) : GameSessionEvent

    data class ActionFailed(
        val message: String,
    ) : GameSessionEvent
}
