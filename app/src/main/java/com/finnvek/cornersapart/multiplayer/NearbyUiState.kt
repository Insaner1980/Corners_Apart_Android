package com.finnvek.cornersapart.multiplayer

import com.finnvek.cornersapart.model.toSnapshotList

data class NearbyUiState(
    val connectionState: ConnectionState = ConnectionState.DISCONNECTED,
    val discoveredEndpoints: List<NearbyEndpointUiState> = emptyList(),
    val pendingConnection: NearbyPendingConnection? = null,
    val errorMessage: String? = null,
)

data class NearbyEndpointUiState(
    val endpointId: String,
    val endpointName: String,
)

data class NearbyPendingConnection(
    val endpointId: String,
    val endpointName: String,
    val authenticationToken: String,
)

internal fun NearbyUiState.toSnapshotCopy(): NearbyUiState =
    copy(
        discoveredEndpoints = discoveredEndpoints.toSnapshotList(),
    )
