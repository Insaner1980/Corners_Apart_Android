package com.finnvek.cornersapart.multiplayer

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
