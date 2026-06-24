package com.finnvek.cornersapart.multiplayer

interface ConnectionsClientFacade {
    fun startAdvertising(
        localEndpointName: String,
        serviceId: String,
        callback: NearbyConnectionLifecycleCallback,
        failureCallback: NearbyOperationFailureCallback,
    )

    fun startDiscovery(
        serviceId: String,
        callback: NearbyEndpointDiscoveryCallback,
        failureCallback: NearbyOperationFailureCallback,
    )

    fun requestConnection(
        localEndpointName: String,
        endpointId: String,
        callback: NearbyConnectionLifecycleCallback,
        failureCallback: NearbyOperationFailureCallback,
    )

    fun acceptConnection(
        endpointId: String,
        callback: NearbyPayloadCallback,
        failureCallback: NearbyOperationFailureCallback,
    )

    fun rejectConnection(
        endpointId: String,
        failureCallback: NearbyOperationFailureCallback,
    )

    fun sendPayload(
        endpointId: String,
        bytes: ByteArray,
        failureCallback: NearbyOperationFailureCallback,
    )

    fun stopDiscovery()

    fun stopAdvertising()

    fun stopAllEndpoints()
}

interface NearbyConnectionLifecycleCallback {
    fun onConnectionInitiated(
        endpointId: String,
        endpointName: String,
        authenticationToken: String,
    )

    fun onConnectionResult(
        endpointId: String,
        result: NearbyConnectionResult,
    )

    fun onDisconnected(endpointId: String)
}

interface NearbyEndpointDiscoveryCallback {
    fun onEndpointFound(
        endpointId: String,
        endpointName: String,
    )

    fun onEndpointLost(endpointId: String)
}

interface NearbyPayloadCallback {
    fun onBytesPayload(
        endpointId: String,
        bytes: ByteArray,
    )

    fun onPayloadFailure(endpointId: String)
}

sealed interface NearbyConnectionResult {
    data object Accepted : NearbyConnectionResult

    data class Failed(
        val statusCode: Int?,
        val message: String?,
    ) : NearbyConnectionResult
}

enum class NearbyOperation {
    START_ADVERTISING,
    START_DISCOVERY,
    REQUEST_CONNECTION,
    ACCEPT_CONNECTION,
    REJECT_CONNECTION,
    SEND_PAYLOAD,
}

data class NearbyOperationFailure(
    val statusCode: Int?,
    val message: String?,
)

fun interface NearbyOperationFailureCallback {
    fun onOperationFailure(
        operation: NearbyOperation,
        failure: NearbyOperationFailure,
    )
}
