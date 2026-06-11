package com.finnvek.cornersapart.engine

import com.finnvek.cornersapart.model.GameConstants
import com.finnvek.cornersapart.model.GameState
import com.finnvek.cornersapart.model.PieceCatalog
import com.finnvek.cornersapart.model.Player
import com.finnvek.cornersapart.model.PlayerScore
import com.finnvek.cornersapart.model.ScoreBreakdown

object Scoring {
    fun scoreMove(
        placedCellCount: Int,
        claimedBonusTileCount: Int,
        completesPieceSet: Boolean,
    ): ScoreDelta =
        ScoreDelta(
            placedCellPoints = placedCellCount * GameConstants.PLACED_CELL_POINTS,
            bonusTilePoints = claimedBonusTileCount * GameConstants.BONUS_TILE_POINTS,
            completionBonus = if (completesPieceSet) GameConstants.COMPLETION_BONUS_POINTS else 0,
        )

    fun rankPlayers(state: GameState): List<PlayerScore> {
        val claimedCounts = claimedBonusCountsByOwner(state)
        return state.players
            .filter { player -> player.isActiveScoring }
            .groupBy { player -> player.ownerIndex }
            .map { (ownerIndex, players) ->
                val scoreBreakdown = players.combinedScoreBreakdown()
                RankedOwnerScore(
                    score =
                        PlayerScore(
                            name = ownerName(ownerIndex, players),
                            totalScore = scoreBreakdown.total,
                            scoreBreakdown = scoreBreakdown,
                            claimedBonusTiles = claimedCounts[ownerIndex] ?: 0,
                            colorIndex = players.minOf { player -> player.colorIndex },
                            ownerIndex = ownerIndex,
                        ),
                    remainingPieceCount = players.sumOf { player -> PieceCatalog.all.size - player.usedPieceIds.size },
                )
            }.sortedWith(playerRankingComparator())
            .map { ranked -> ranked.score }
    }

    private fun playerRankingComparator(): Comparator<RankedOwnerScore> =
        compareByDescending<RankedOwnerScore> { ranked -> ranked.score.totalScore }
            .thenByDescending { ranked -> ranked.score.scoreBreakdown.placedCellPoints }
            .thenByDescending { ranked -> ranked.score.claimedBonusTiles }
            .thenBy { ranked -> ranked.remainingPieceCount }
            .thenBy { ranked -> ranked.score.ownerIndex }

    private data class RankedOwnerScore(
        val score: PlayerScore,
        val remainingPieceCount: Int,
    )

    private fun ownerName(
        ownerIndex: Int,
        players: List<Player>,
    ): String =
        if (players.size == 1) {
            players.single().name
        } else {
            "Player ${ownerIndex + 1}"
        }

    private fun claimedBonusCountsByOwner(state: GameState): Map<Int, Int> =
        state.bonusTiles
            .mapNotNull { bonusTile ->
                bonusTile.claimedByPlayerIndex
                    ?.let { playerIndex -> state.players.getOrNull(playerIndex)?.ownerIndex }
            }.groupingBy { ownerIndex -> ownerIndex }
            .eachCount()

    private fun List<Player>.combinedScoreBreakdown(): ScoreBreakdown =
        ScoreBreakdown(
            placedCellPoints = sumOf { player -> player.scoreBreakdown.placedCellPoints },
            bonusTilePoints = sumOf { player -> player.scoreBreakdown.bonusTilePoints },
            completionBonus = sumOf { player -> player.scoreBreakdown.completionBonus },
        )
}
