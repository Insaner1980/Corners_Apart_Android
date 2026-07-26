package com.finnvek.cornersapart.multiplayer

import com.finnvek.cornersapart.projectFiles
import com.finnvek.cornersapart.projectRoot
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NearbyConfigurationTest {
    @Test
    fun nearbyDependencyManifestPermissionsAndUiTermsAreConfigured() {
        val root = projectRoot()
        val files = projectFiles(root)
        val gameScreen =
            root
                .resolve(
                    "app/src/main/java/com/finnvek/cornersapart/ui/screens/GameScreen.kt",
                ).toFile()
                .readText()

        assertTrue(files.versionsCatalog.contains("playServicesNearby = \"19.3.0\""))
        val nearbyAlias =
            "play-services-nearby = { group = \"com.google.android.gms\", " +
                "name = \"play-services-nearby\", version.ref = \"playServicesNearby\" }"
        assertTrue(files.versionsCatalog.contains(nearbyAlias))
        assertTrue(files.appBuildFile.contains("implementation(libs.play.services.nearby)"))

        assertTrue(files.manifest.contains("android.permission.ACCESS_WIFI_STATE"))
        assertTrue(files.manifest.contains("android.permission.CHANGE_WIFI_STATE"))
        assertTrue(files.manifest.contains("android.permission.BLUETOOTH\""))
        assertTrue(files.manifest.contains("android.permission.BLUETOOTH_ADMIN"))
        assertTrue(files.manifest.contains("android.permission.ACCESS_COARSE_LOCATION"))
        assertTrue(
            files.manifest.contains(
                "android.permission.ACCESS_COARSE_LOCATION\" android:maxSdkVersion=\"31\"",
            ),
        )
        assertTrue(files.manifest.contains("android.permission.ACCESS_FINE_LOCATION"))
        assertTrue(files.manifest.contains("android.permission.BLUETOOTH_ADVERTISE"))
        assertTrue(files.manifest.contains("android.permission.BLUETOOTH_CONNECT"))
        assertTrue(files.manifest.contains("android.permission.BLUETOOTH_SCAN"))
        assertTrue(files.manifest.contains("android.permission.NEARBY_WIFI_DEVICES"))
        assertTrue(files.manifest.contains("android.permission.ACCESS_LOCAL_NETWORK"))
        assertTrue(files.manifest.contains("android:usesPermissionFlags=\"neverForLocation\""))

        assertTrue(files.strings.contains("<string name=\"nearby_game\">Nearby game</string>"))
        assertTrue(files.strings.contains("<string name=\"create_nearby_game\">Create nearby game</string>"))
        assertTrue(files.strings.contains("<string name=\"find_nearby_game\">Find nearby game</string>"))

        assertTrue(gameScreen.contains("ActivityResultContracts.RequestMultiplePermissions"))
        assertTrue(gameScreen.contains("rememberSaveable"))
        assertTrue(gameScreen.contains("PendingNearbyAction.Host"))
        assertTrue(gameScreen.contains("PendingNearbyAction.Discover"))
        assertFalse(gameScreen.contains("mutableStateOf<(() -> Unit)?>"))
        assertTrue(gameScreen.contains("NearbyPermissions"))
        assertTrue(gameScreen.contains("requiredRuntimePermissions"))
        assertTrue(gameScreen.contains("R.string.nearby_game"))
        assertTrue(gameScreen.contains("R.string.create_nearby_game"))
        assertTrue(gameScreen.contains("R.string.find_nearby_game"))
    }
}
