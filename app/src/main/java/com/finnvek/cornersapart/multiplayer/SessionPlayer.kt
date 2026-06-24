package com.finnvek.cornersapart.multiplayer

import com.finnvek.cornersapart.model.toSnapshotList
import com.finnvek.cornersapart.model.toSnapshotSet
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

internal fun NearbyLobbyState.toSnapshotCopy(): NearbyLobbyState =
    copy(
        connectedPlayers = connectedPlayers.toSnapshotList(),
        reconnectingPlayerIndexes = reconnectingPlayerIndexes.toSnapshotSet(),
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
