package com.finnvek.cornersapart.model

data class HistoryStats(
    val totalGamesPlayed: Int = 0,
    val winCount: Int = 0,
    val winRate: Double = 0.0,
    val averageScore: Double = 0.0,
    val bestScore: Int = 0,
    val averageRank: Double = 0.0,
    val averageClaimedBonusTiles: Double = 0.0,
    val completionBonusCount: Int = 0,
    val favoriteDifficulty: Int? = null,
    val scoreTrend: List<Int> = emptyList(),
    val statsPerDifficulty: Map<Int, DifficultyHistoryStats> = emptyMap(),
)

data class DifficultyHistoryStats(
    val difficulty: Int,
    val gamesPlayed: Int,
    val wins: Int,
    val averageScore: Double,
    val bestScore: Int,
)

object HistoryStatsCalculator {
    private const val RECENT_GAME_LIMIT = 20

    fun calculate(entries: List<HistoryEntry>): HistoryStats {
        if (entries.isEmpty()) return HistoryStats()
        val totalGames = entries.size
        val winCount = entries.count { entry -> entry.rank == 1 }
        return HistoryStats(
            totalGamesPlayed = totalGames,
            winCount = winCount,
            winRate = winCount.toDouble() / totalGames,
            averageScore = entries.averageOf { entry -> entry.totalScore },
            bestScore = entries.maxOf { entry -> entry.totalScore },
            averageRank = entries.averageOf { entry -> entry.rank },
            averageClaimedBonusTiles = entries.averageOf { entry -> entry.claimedBonusTiles },
            completionBonusCount = entries.count { entry -> entry.scoreBreakdown.completionBonus > 0 },
            favoriteDifficulty = entries.favoriteDifficulty(),
            scoreTrend =
                entries
                    .takeLast(RECENT_GAME_LIMIT)
                    .map { entry -> entry.totalScore }
                    .toSnapshotList(),
            statsPerDifficulty = entries.statsPerDifficulty(),
        )
    }

    private fun List<HistoryEntry>.favoriteDifficulty(): Int? =
        groupingBy { entry -> entry.difficulty }
            .eachCount()
            .maxWithOrNull(
                compareBy<Map.Entry<Int, Int>> { entry ->
                    entry.value
                }.thenByDescending { entry -> entry.key },
            )?.key

    private fun List<HistoryEntry>.statsPerDifficulty(): Map<Int, DifficultyHistoryStats> =
        groupBy { entry -> entry.difficulty }
            .mapValues { (difficulty, entries) ->
                DifficultyHistoryStats(
                    difficulty = difficulty,
                    gamesPlayed = entries.size,
                    wins = entries.count { entry -> entry.rank == 1 },
                    averageScore = entries.averageOf { entry -> entry.totalScore },
                    bestScore = entries.maxOf { entry -> entry.totalScore },
                )
            }.toSnapshotMap()

    private fun List<HistoryEntry>.averageOf(selector: (HistoryEntry) -> Int): Double =
        sumOf(selector).toDouble() / size
}
