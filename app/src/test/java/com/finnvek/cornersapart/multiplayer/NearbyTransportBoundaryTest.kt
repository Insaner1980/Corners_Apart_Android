package com.finnvek.cornersapart.multiplayer

import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path

class NearbyTransportBoundaryTest {
    @Test
    fun coordinatorAndFacadeHidePlayServicesTypes() {
        val root = repositoryRoot()
        val matches =
            checkedSourcePaths.flatMap { path ->
                root
                    .resolve(path)
                    .toFile()
                    .readLines()
                    .mapIndexedNotNull { index, line ->
                        val leaksPlayServices = line.contains("com.google.android.gms")
                        val leaksPlayServicesStrategy = line.contains("Strategy.P2P")
                        if (leaksPlayServices || leaksPlayServicesStrategy) {
                            "${path.toString().replace('\\', '/')}:${index + 1}: ${line.trim()}"
                        } else {
                            null
                        }
                    }
            }

        assertTrue(
            matches.joinToString(
                separator = System.lineSeparator(),
                prefix = "Play Services transport details leaked past the adapter:${System.lineSeparator()}",
            ),
            matches.isEmpty(),
        )
    }

    private fun repositoryRoot(): Path =
        generateSequence(Path.of(System.getProperty("user.dir")).toAbsolutePath()) { path -> path.parent }
            .first { path -> Files.exists(path.resolve("settings.gradle.kts")) }

    private companion object {
        val checkedSourcePaths =
            listOf(
                Path.of("app/src/main/java/com/finnvek/cornersapart/multiplayer/ConnectionsClientFacade.kt"),
                Path.of("app/src/main/java/com/finnvek/cornersapart/multiplayer/NearbyConnectionsCoordinator.kt"),
            )
    }
}
