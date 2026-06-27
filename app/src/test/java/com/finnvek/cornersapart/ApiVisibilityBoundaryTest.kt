package com.finnvek.cornersapart

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ApiVisibilityBoundaryTest {
    @Test
    fun genericGameSessionDoesNotExposeRestoreOnlyStateReplacement() {
        val source = readSource("app/src/main/java/com/finnvek/cornersapart/multiplayer/GameSession.kt")

        assertFalse(source.contains("fun replaceState("))
    }

    @Test
    fun nearbyCoordinatorExposesSessionThroughNarrowInterface() {
        val source =
            readSource(
                "app/src/main/java/com/finnvek/cornersapart/multiplayer/NearbyConnectionsCoordinator.kt",
            )

        assertTrue(source.contains("val currentSession: StateFlow<NearbyGameSession?>"))
        assertFalse(source.contains("val currentSession: StateFlow<NearbySession?>"))
    }

    @Test
    fun engineAndOpponentImplementationDetailsAreInternal() {
        val placementValidator = readSource("app/src/main/java/com/finnvek/cornersapart/engine/PlacementValidator.kt")
        val opponentDifficulty =
            readSource(
                "app/src/main/java/com/finnvek/cornersapart/opponents/OpponentDifficulty.kt",
            )

        assertTrue(placementValidator.contains("internal object PlacementValidator"))
        assertTrue(placementValidator.contains("internal data class PlacementValidation"))
        assertTrue(opponentDifficulty.contains("internal val temperature"))
        assertTrue(opponentDifficulty.contains("internal val candidateSoftCap"))
        assertTrue(opponentDifficulty.contains("internal val largePieceBias"))
        assertTrue(opponentDifficulty.contains("internal val bonusTileAwareness"))
        assertTrue(opponentDifficulty.contains("internal val blockingAwareness"))
    }

    private fun readSource(relativePath: String): String = projectRoot().resolve(relativePath).toFile().readText()
}
