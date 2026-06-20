package com.finnvek.cornersapart.multiplayer

import android.content.Context
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.connection.AdvertisingOptions
import com.google.android.gms.nearby.connection.ConnectionInfo
import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback
import com.google.android.gms.nearby.connection.ConnectionResolution
import com.google.android.gms.nearby.connection.ConnectionsClient
import com.google.android.gms.nearby.connection.ConnectionsStatusCodes
import com.google.android.gms.nearby.connection.DiscoveredEndpointInfo
import com.google.android.gms.nearby.connection.DiscoveryOptions
import com.google.android.gms.nearby.connection.EndpointDiscoveryCallback
import com.google.android.gms.nearby.connection.Payload
import com.google.android.gms.nearby.connection.PayloadCallback
import com.google.android.gms.nearby.connection.PayloadTransferUpdate
import com.google.android.gms.nearby.connection.Strategy

class PlayServicesConnectionsClientFacade(
    context: Context,
) : ConnectionsClientFacade {
    private val client: ConnectionsClient = Nearby.getConnectionsClient(context)

    override fun startAdvertising(
        localEndpointName: String,
        serviceId: String,
        strategy: Strategy,
        callback: NearbyConnectionLifecycleCallback,
    ) {
        client.startAdvertising(
            localEndpointName,
            serviceId,
            callback.toPlayServicesCallback(),
            AdvertisingOptions
                .Builder()
                .setStrategy(strategy)
                .build(),
        )
    }

    override fun startDiscovery(
        serviceId: String,
        strategy: Strategy,
        callback: NearbyEndpointDiscoveryCallback,
    ) {
        client.startDiscovery(
            serviceId,
            object : EndpointDiscoveryCallback() {
                override fun onEndpointFound(
                    endpointId: String,
                    info: DiscoveredEndpointInfo,
                ) {
                    callback.onEndpointFound(endpointId, info.endpointName)
                }

                override fun onEndpointLost(endpointId: String) {
                    callback.onEndpointLost(endpointId)
                }
            },
            DiscoveryOptions
                .Builder()
                .setStrategy(strategy)
                .build(),
        )
    }

    override fun requestConnection(
        localEndpointName: String,
        endpointId: String,
        callback: NearbyConnectionLifecycleCallback,
    ) {
        client.requestConnection(localEndpointName, endpointId, callback.toPlayServicesCallback())
    }

    override fun acceptConnection(
        endpointId: String,
        callback: NearbyPayloadCallback,
    ) {
        client.acceptConnection(endpointId, callback.toPlayServicesCallback())
    }

    override fun rejectConnection(endpointId: String) {
        client.rejectConnection(endpointId)
    }

    override fun sendPayload(
        endpointId: String,
        bytes: ByteArray,
    ) {
        client.sendPayload(endpointId, Payload.fromBytes(bytes))
    }

    override fun stopDiscovery() {
        client.stopDiscovery()
    }

    override fun stopAdvertising() {
        client.stopAdvertising()
    }

    override fun stopAllEndpoints() {
        client.stopAllEndpoints()
    }

    private fun NearbyConnectionLifecycleCallback.toPlayServicesCallback(): ConnectionLifecycleCallback =
        object : ConnectionLifecycleCallback() {
            override fun onConnectionInitiated(
                endpointId: String,
                connectionInfo: ConnectionInfo,
            ) {
                onConnectionInitiated(
                    endpointId = endpointId,
                    endpointName = connectionInfo.endpointName,
                    authenticationToken = connectionInfo.authenticationDigits,
                )
            }

            override fun onConnectionResult(
                endpointId: String,
                resolution: ConnectionResolution,
            ) {
                onConnectionResult(endpointId, resolution.status.statusCode == ConnectionsStatusCodes.STATUS_OK)
            }

            override fun onDisconnected(endpointId: String) {
                this@toPlayServicesCallback.onDisconnected(endpointId)
            }
        }

    private fun NearbyPayloadCallback.toPlayServicesCallback(): PayloadCallback =
        object : PayloadCallback() {
            override fun onPayloadReceived(
                endpointId: String,
                payload: Payload,
            ) {
                if (payload.type == Payload.Type.BYTES) {
                    val bytes = payload.asBytes()
                    if (bytes != null) {
                        onBytesPayload(endpointId, bytes)
                    } else {
                        onPayloadFailure(endpointId)
                    }
                }
            }

            override fun onPayloadTransferUpdate(
                endpointId: String,
                update: PayloadTransferUpdate,
            ) {
                if (update.status == PayloadTransferUpdate.Status.FAILURE) {
                    onPayloadFailure(endpointId)
                }
            }
        }
}
