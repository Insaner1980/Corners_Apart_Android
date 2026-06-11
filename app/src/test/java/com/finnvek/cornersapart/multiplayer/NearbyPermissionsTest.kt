package com.finnvek.cornersapart.multiplayer

import android.Manifest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NearbyPermissionsTest {
    @Test
    fun runtimePermissionsMatchAndroidApiBands() {
        assertEquals(
            listOf(Manifest.permission.ACCESS_COARSE_LOCATION),
            NearbyPermissions.requiredRuntimePermissions(sdkInt = 28),
        )
        assertEquals(
            listOf(Manifest.permission.ACCESS_FINE_LOCATION),
            NearbyPermissions.requiredRuntimePermissions(sdkInt = 30),
        )
        assertEquals(
            listOf(
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.BLUETOOTH_ADVERTISE,
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH_SCAN,
            ),
            NearbyPermissions.requiredRuntimePermissions(sdkInt = 31),
        )
        assertEquals(
            listOf(
                Manifest.permission.BLUETOOTH_ADVERTISE,
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.NEARBY_WIFI_DEVICES,
            ),
            NearbyPermissions.requiredRuntimePermissions(sdkInt = 32),
        )
        assertEquals(
            listOf(
                Manifest.permission.BLUETOOTH_ADVERTISE,
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.NEARBY_WIFI_DEVICES,
            ),
            NearbyPermissions.requiredRuntimePermissions(sdkInt = 33),
        )
    }

    @Test
    fun emptyPermissionMapIsNotEnoughForNearby() {
        assertFalse(NearbyPermissions.hasRequiredPermissions(emptyMap(), sdkInt = 33))
        assertTrue(
            NearbyPermissions.hasRequiredPermissions(
                permissions =
                    NearbyPermissions.requiredRuntimePermissions(sdkInt = 33).associateWith { true },
                sdkInt = 33,
            ),
        )
    }
}
