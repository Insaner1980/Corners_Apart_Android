package com.finnvek.cornersapart.multiplayer

import kotlinx.serialization.Serializable

@Serializable
data class SessionPlayer(
    val index: Int,
    val name: String,
    val isLocal: Boolean,
    val isComputerControlled: Boolean,
    val colorIndex: Int,
    val ownerIndex: Int,
    val usedPieceCount: Int,
)

data class NearbyLobbyState(
    val connectedPlayers: List<SessionPlayer>,
    val reconnectingPlayerIndexes: Set<Int> = emptySet(),
)

enum class SessionType {
    LOCAL,
    NEARBY,
}

enum class ConnectionState {
    DISCONNECTED,
    CONNECTED,
    RECONNECTING,
    FAILED,
}
