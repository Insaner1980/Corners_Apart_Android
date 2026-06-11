package com.finnvek.cornersapart.model

import org.junit.Assert.assertEquals
import org.junit.Test

class HistoryStatsCalculatorTest {
    @Test
    fun statsTreatHigherScoresAsBetterAndTrackDifficultyBreakdowns() {
        val entries =
            listOf(
                historyEntry(totalScore = 12, rank = 2, difficulty = 1, bonusTiles = 1),
                historyEntry(totalScore = 30, rank = 1, difficulty = 2, bonusTiles = 3),
                historyEntry(totalScore = 18, rank = 3, difficulty = 2, bonusTiles = 2),
            )

        val stats = HistoryStatsCalculator.calculate(entries)

        assertEquals(3, stats.totalGamesPlayed)
        assertEquals(1, stats.winCount)
        assertEquals(30, stats.bestScore)
        assertEquals(20.0, stats.averageScore, 0.001)
        assertEquals(2.0, stats.averageRank, 0.001)
        assertEquals(2, stats.favoriteDifficulty)
        assertEquals(listOf(12, 30, 18), stats.scoreTrend)
        assertEquals(2, stats.statsPerDifficulty.getValue(2).gamesPlayed)
    }

    private fun historyEntry(
        totalScore: Int,
        rank: Int,
        difficulty: Int,
        bonusTiles: Int,
    ): HistoryEntry =
        HistoryEntry(
            date = "2026-06-${10 + totalScore}",
            rank = rank,
            totalScore = totalScore,
            scoreBreakdown =
                ScoreBreakdown(
                    placedCellPoints = totalScore - bonusTiles * 3,
                    bonusTilePoints = bonusTiles * 3,
                ),
            claimedBonusTiles = bonusTiles,
            piecesPlaced = totalScore,
            difficulty = difficulty,
            ruleset = Ruleset.STANDARD,
            gameMode = GameMode.FOUR_PLAYER,
            timeSeconds = 90,
            scores = emptyList(),
        )
}
