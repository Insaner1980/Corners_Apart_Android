package com.finnvek.cornersapart.multiplayer

import android.Manifest
import android.os.Build

object NearbyPermissions {
    private const val ACCESS_LOCAL_NETWORK = "android.permission.ACCESS_LOCAL_NETWORK"
    private const val ANDROID_9 = 28
    private const val ANDROID_10 = 29
    private const val ANDROID_12 = 31
    private const val ANDROID_12L = 32
    private const val ANDROID_17 = 37
    private const val BLUETOOTH_ADVERTISE = "android.permission.BLUETOOTH_ADVERTISE"
    private const val BLUETOOTH_CONNECT = "android.permission.BLUETOOTH_CONNECT"
    private const val BLUETOOTH_SCAN = "android.permission.BLUETOOTH_SCAN"
    private const val NEARBY_WIFI_DEVICES = "android.permission.NEARBY_WIFI_DEVICES"

    fun requiredRuntimePermissions(sdkInt: Int = Build.VERSION.SDK_INT): List<String> =
        buildList {
            when {
                sdkInt <= ANDROID_9 -> add(Manifest.permission.ACCESS_COARSE_LOCATION)
                sdkInt in ANDROID_10 until ANDROID_12 -> add(Manifest.permission.ACCESS_FINE_LOCATION)
                sdkInt == ANDROID_12 -> {
                    add(Manifest.permission.ACCESS_COARSE_LOCATION)
                    add(Manifest.permission.ACCESS_FINE_LOCATION)
                    addBluetoothPermissions()
                }
                sdkInt >= ANDROID_12L -> {
                    addBluetoothPermissions()
                    add(NEARBY_WIFI_DEVICES)
                    if (sdkInt >= ANDROID_17) add(ACCESS_LOCAL_NETWORK)
                }
            }
        }

    fun hasRequiredPermissions(
        permissions: Map<String, Boolean>,
        sdkInt: Int = Build.VERSION.SDK_INT,
    ): Boolean = requiredRuntimePermissions(sdkInt).all { permission -> permissions[permission] == true }

    private fun MutableList<String>.addBluetoothPermissions() {
        add(BLUETOOTH_ADVERTISE)
        add(BLUETOOTH_CONNECT)
        add(BLUETOOTH_SCAN)
    }
}
