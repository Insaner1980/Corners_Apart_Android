package com.finnvek.cornersapart.multiplayer

import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.name

class NearbyConfigurationTest {
    @Test
    fun nearbyDependencyManifestPermissionsAndUiTermsAreConfigured() {
        val root = projectRoot()
        val versionsCatalog = root.resolve("gradle/libs.versions.toml").toFile().readText()
        val appBuildFile = root.resolve("app/build.gradle.kts").toFile().readText()
        val manifest = root.resolve("app/src/main/AndroidManifest.xml").toFile().readText()
        val strings = root.resolve("app/src/main/res/values/strings.xml").toFile().readText()
        val gameScreen =
            root
                .resolve(
                    "app/src/main/java/com/finnvek/cornersapart/ui/screens/GameScreen.kt",
                ).toFile()
                .readText()

        assertTrue(versionsCatalog.contains("playServicesNearby = \"19.3.0\""))
        val nearbyAlias =
            "play-services-nearby = { group = \"com.google.android.gms\", " +
                "name = \"play-services-nearby\", version.ref = \"playServicesNearby\" }"
        assertTrue(versionsCatalog.contains(nearbyAlias))
        assertTrue(appBuildFile.contains("implementation(libs.play.services.nearby)"))

        assertTrue(manifest.contains("android.permission.ACCESS_WIFI_STATE"))
        assertTrue(manifest.contains("android.permission.CHANGE_WIFI_STATE"))
        assertTrue(manifest.contains("android.permission.BLUETOOTH\""))
        assertTrue(manifest.contains("android.permission.BLUETOOTH_ADMIN"))
        assertTrue(manifest.contains("android.permission.ACCESS_COARSE_LOCATION"))
        assertTrue(manifest.contains("android.permission.ACCESS_FINE_LOCATION"))
        assertTrue(manifest.contains("android.permission.BLUETOOTH_ADVERTISE"))
        assertTrue(manifest.contains("android.permission.BLUETOOTH_CONNECT"))
        assertTrue(manifest.contains("android.permission.BLUETOOTH_SCAN"))
        assertTrue(manifest.contains("android.permission.NEARBY_WIFI_DEVICES"))
        assertTrue(manifest.contains("android:usesPermissionFlags=\"neverForLocation\""))

        assertTrue(strings.contains("<string name=\"nearby_game\">Nearby game</string>"))
        assertTrue(strings.contains("<string name=\"create_nearby_game\">Create nearby game</string>"))
        assertTrue(strings.contains("<string name=\"find_nearby_game\">Find nearby game</string>"))

        assertTrue(gameScreen.contains("ActivityResultContracts.RequestMultiplePermissions"))
        assertTrue(gameScreen.contains("NearbyPermissions"))
        assertTrue(gameScreen.contains("requiredRuntimePermissions"))
        assertTrue(gameScreen.contains("R.string.nearby_game"))
        assertTrue(gameScreen.contains("R.string.create_nearby_game"))
        assertTrue(gameScreen.contains("R.string.find_nearby_game"))
    }

    private fun projectRoot(): Path {
        var current = Path.of(System.getProperty("user.dir")).toAbsolutePath()
        while (current.name.isNotEmpty()) {
            if (Files.exists(current.resolve("settings.gradle.kts"))) return current
            current = current.parent
        }
        error("Project root was not found from ${System.getProperty("user.dir")}")
    }
}
