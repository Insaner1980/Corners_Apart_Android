package com.finnvek.cornersapart.multiplayer

import android.content.Context
import com.google.android.gms.common.api.ApiException
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
import com.google.android.gms.tasks.Task

class PlayServicesConnectionsClientFacade(
    context: Context,
) : ConnectionsClientFacade {
    private val client: ConnectionsClient = Nearby.getConnectionsClient(context)

    override fun startAdvertising(
        localEndpointName: String,
        serviceId: String,
        callback: NearbyConnectionLifecycleCallback,
        failureCallback: NearbyOperationFailureCallback,
    ) {
        client
            .startAdvertising(
                localEndpointName,
                serviceId,
                callback.toPlayServicesCallback(),
                AdvertisingOptions
                    .Builder()
                    .setStrategy(Strategy.P2P_STAR)
                    .build(),
            ).notifyFailure(NearbyOperation.START_ADVERTISING, failureCallback)
    }

    override fun startDiscovery(
        serviceId: String,
        callback: NearbyEndpointDiscoveryCallback,
        failureCallback: NearbyOperationFailureCallback,
    ) {
        client
            .startDiscovery(
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
                    .setStrategy(Strategy.P2P_STAR)
                    .build(),
            ).notifyFailure(NearbyOperation.START_DISCOVERY, failureCallback)
    }

    override fun requestConnection(
        localEndpointName: String,
        endpointId: String,
        callback: NearbyConnectionLifecycleCallback,
        failureCallback: NearbyOperationFailureCallback,
    ) {
        client
            .requestConnection(localEndpointName, endpointId, callback.toPlayServicesCallback())
            .notifyFailure(NearbyOperation.REQUEST_CONNECTION, failureCallback)
    }

    override fun acceptConnection(
        endpointId: String,
        callback: NearbyPayloadCallback,
        failureCallback: NearbyOperationFailureCallback,
    ) {
        client
            .acceptConnection(endpointId, callback.toPlayServicesCallback())
            .notifyFailure(NearbyOperation.ACCEPT_CONNECTION, failureCallback)
    }

    override fun rejectConnection(
        endpointId: String,
        failureCallback: NearbyOperationFailureCallback,
    ) {
        client
            .rejectConnection(endpointId)
            .notifyFailure(NearbyOperation.REJECT_CONNECTION, failureCallback)
    }

    override fun sendPayload(
        endpointId: String,
        bytes: ByteArray,
        failureCallback: NearbyOperationFailureCallback,
    ) {
        client
            .sendPayload(endpointId, Payload.fromBytes(bytes))
            .notifyFailure(NearbyOperation.SEND_PAYLOAD, failureCallback)
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
                val status = resolution.status
                val result =
                    if (status.statusCode == ConnectionsStatusCodes.STATUS_OK) {
                        NearbyConnectionResult.Accepted
                    } else {
                        NearbyConnectionResult.Failed(
                            statusCode = status.statusCode,
                            message = status.statusCode.toNearbyStatusMessage(),
                        )
                    }
                onConnectionResult(endpointId, result)
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
                } else {
                    payload.close()
                    onPayloadFailure(endpointId)
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

    private fun Task<Void>.notifyFailure(
        operation: NearbyOperation,
        failureCallback: NearbyOperationFailureCallback,
    ): Task<Void> =
        addOnFailureListener { exception ->
            failureCallback.onOperationFailure(operation, exception.toNearbyOperationFailure())
        }

    private fun Exception.toNearbyOperationFailure(): NearbyOperationFailure =
        when (this) {
            is ApiException -> {
                NearbyOperationFailure(
                    statusCode = statusCode,
                    message = statusCode.toNearbyStatusMessage(),
                )
            }

            else -> {
                NearbyOperationFailure(
                    statusCode = null,
                    message = message,
                )
            }
        }

    private fun Int.toNearbyStatusMessage(): String = ConnectionsStatusCodes.getStatusCodeString(this)
}
