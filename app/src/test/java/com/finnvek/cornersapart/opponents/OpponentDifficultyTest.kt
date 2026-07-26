package com.finnvek.cornersapart.opponents

import org.junit.Assert.assertEquals
import org.junit.Test

class OpponentDifficultyTest {
    @Test
    fun difficultyParametersMatchSpecifiedTable() {
        val expected =
            listOf(
                DifficultyParameters(3.0, 10, -0.4, 0.2, 0.0),
                DifficultyParameters(2.0, 25, -0.15, 0.6, 0.3),
                DifficultyParameters(1.0, 80, 0.0, 1.0, 0.8),
                DifficultyParameters(0.5, 200, 0.25, 1.4, 1.3),
                DifficultyParameters(0.2, 500, 0.45, 1.8, 1.7),
                DifficultyParameters(0.1, 500, 0.5, 2.0, 2.0),
            )

        val actual =
            OpponentDifficulty.entries.map { difficulty ->
                DifficultyParameters(
                    temperature = difficulty.temperature,
                    candidateSoftCap = difficulty.candidateSoftCap,
                    largePieceBias = difficulty.largePieceBias,
                    bonusTileAwareness = difficulty.bonusTileAwareness,
                    blockingAwareness = difficulty.blockingAwareness,
                )
            }

        assertEquals(expected, actual)
    }

    private data class DifficultyParameters(
        val temperature: Double,
        val candidateSoftCap: Int,
        val largePieceBias: Double,
        val bonusTileAwareness: Double,
        val blockingAwareness: Double,
    )
}
