package com.finnvek.cornersapart.multiplayer

import com.google.android.gms.nearby.connection.Strategy

interface ConnectionsClientFacade {
    fun startAdvertising(
        localEndpointName: String,
        serviceId: String,
        strategy: Strategy,
        callback: NearbyConnectionLifecycleCallback,
    )

    fun startDiscovery(
        serviceId: String,
        strategy: Strategy,
        callback: NearbyEndpointDiscoveryCallback,
    )

    fun requestConnection(
        localEndpointName: String,
        endpointId: String,
        callback: NearbyConnectionLifecycleCallback,
    )

    fun acceptConnection(
        endpointId: String,
        callback: NearbyPayloadCallback,
    )

    fun rejectConnection(endpointId: String)

    fun sendPayload(
        endpointId: String,
        bytes: ByteArray,
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
        accepted: Boolean,
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
