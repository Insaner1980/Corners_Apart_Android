package com.finnvek.cornersapart.engine

import com.finnvek.cornersapart.model.BonusTile
import com.finnvek.cornersapart.model.GameConfig
import com.finnvek.cornersapart.model.GameConstants
import com.finnvek.cornersapart.model.GameMode
import com.finnvek.cornersapart.model.GameModeConfigs
import com.finnvek.cornersapart.model.GameState
import com.finnvek.cornersapart.model.Move
import com.finnvek.cornersapart.model.MutableBoard
import com.finnvek.cornersapart.model.PieceCatalog
import com.finnvek.cornersapart.model.Player
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GameEngineScoringTest {
    private val engine = GameEngine()

    @Test
    fun coveringBonusTileAddsThreeBonusPoints() {
        val accepted =
            placeThreeBendOverBonusTiles(
                bonusTiles = listOf(BonusTile(row = 1, col = 1)),
                randomSeed = 11L,
            )

        assertEquals(3, accepted.scoreDelta.placedCellPoints)
        assertEquals(GameConstants.BONUS_TILE_POINTS, accepted.scoreDelta.bonusTilePoints)
        assertEquals(
            GameConstants.BONUS_TILE_POINTS,
            accepted.state.players[0]
                .scoreBreakdown.bonusTilePoints,
        )
        assertEquals(
            0,
            accepted.state.bonusTiles
                .single()
                .claimedByPlayerIndex,
        )
        assertEquals(
            0,
            accepted.state.bonusTiles
                .single()
                .claimedOnTurn,
        )
    }

    @Test
    fun duplicateBonusTilePositionsAwardPointsOnlyOnce() {
        val duplicateBonusTile = BonusTile(row = 1, col = 1)
        val accepted =
            placeThreeBendOverBonusTiles(
                bonusTiles = listOf(duplicateBonusTile, duplicateBonusTile),
                randomSeed = 13L,
            )

        assertEquals(GameConstants.BONUS_TILE_POINTS, accepted.scoreDelta.bonusTilePoints)
        assertEquals(
            GameConstants.BONUS_TILE_POINTS,
            accepted.state.players[0]
                .scoreBreakdown.bonusTilePoints,
        )
    }

    @Test
    fun finalPieceAwardsCompletionBonusOnce() {
        val allExceptSingle =
            PieceCatalog.all
                .map { piece -> piece.id }
                .filterNot { id ->
                    id ==
                        PieceCatalog.SINGLE_CELL_ID
                }.toSet()
        val board =
            MutableBoard(GameConstants.STANDARD_BOARD_SIZE).apply {
                set(row = 0, col = 0, value = 0)
            }
        val standardState = EngineTestFixtures.standardState(engine)
        val state =
            standardState.copy(
                board = board.toSnapshot(),
                players =
                    standardState.players.map { player ->
                        if (player.index == 0) {
                            player.copy(usedPieceIds = allExceptSingle)
                        } else {
                            player
                        }
                    },
                currentPlayerIndex = 0,
            )

        val result =
            engine.applyMove(
                state,
                Move(
                    playerIndex = 0,
                    pieceId = PieceCatalog.SINGLE_CELL_ID,
                    anchorRow = 1,
                    anchorCol = 1,
                    orientationIndex = 0,
                ),
            )

        assertTrue(result is MoveResult.Accepted)
        val accepted = result as MoveResult.Accepted
        assertEquals(GameConstants.COMPLETION_BONUS_POINTS, accepted.scoreDelta.completionBonus)
        assertEquals(
            GameConstants.COMPLETION_BONUS_POINTS,
            accepted.state.players[0]
                .scoreBreakdown.completionBonus,
        )
    }

    @Test
    fun rankingSortsHigherTotalScoreFirst() {
        val state =
            standardStateWithPlayers { player ->
                when (player.index) {
                    0 -> player.copy(scoreBreakdown = ScoreFixtures.breakdown(totalCells = 8, bonusPoints = 0))
                    1 -> player.copy(scoreBreakdown = ScoreFixtures.breakdown(totalCells = 5, bonusPoints = 6))
                    2 -> player.copy(scoreBreakdown = ScoreFixtures.breakdown(totalCells = 10, bonusPoints = 0))
                    else -> player.copy(scoreBreakdown = ScoreFixtures.breakdown(totalCells = 2, bonusPoints = 0))
                }
            }

        val rankings = Scoring.rankPlayers(state)

        assertEquals(listOf("Mango", "Cyan", "Pink", "Lime"), rankings.map { score -> score.name })
        assertEquals(listOf(11, 10, 8, 2), rankings.map { score -> score.totalScore })
    }

    @Test
    fun rankingUsesFewerRemainingPiecesAsTieBreaker() {
        val state =
            standardStateWithPlayers { player ->
                when (player.index) {
                    0 ->
                        player.copy(
                            scoreBreakdown = ScoreFixtures.breakdown(totalCells = 8, bonusPoints = 0),
                            usedPieceIds = setOf(PieceCatalog.THREE_BEND_ID),
                        )
                    1 ->
                        player.copy(
                            scoreBreakdown = ScoreFixtures.breakdown(totalCells = 8, bonusPoints = 0),
                            usedPieceIds = setOf(PieceCatalog.THREE_BEND_ID, PieceCatalog.TWO_LINE_ID),
                        )
                    else -> player.copy(scoreBreakdown = ScoreFixtures.breakdown(totalCells = 1, bonusPoints = 0))
                }
            }

        val rankings = Scoring.rankPlayers(state)

        assertEquals(listOf("Mango", "Pink"), rankings.take(2).map { score -> score.name })
    }

    private fun standardStateWithPlayers(transform: (Player) -> Player): GameState {
        val standardState = EngineTestFixtures.standardState(engine)
        return standardState.copy(players = standardState.players.map(transform))
    }

    @Test
    fun twoColorDuelRankingAggregatesScoresByOwner() {
        val state =
            engine
                .newGame(
                    GameModeConfigs.defaultGameConfig(
                        mode = GameMode.TWO_COLOR_DUEL,
                        randomSeed = 29L,
                        bonusTiles = emptyList(),
                    ),
                ).copy(
                    players =
                        engine
                            .newGame(
                                GameModeConfigs.defaultGameConfig(
                                    mode = GameMode.TWO_COLOR_DUEL,
                                    randomSeed = 29L,
                                    bonusTiles = emptyList(),
                                ),
                            ).players
                            .map { player ->
                                when (player.index) {
                                    0 ->
                                        player.copy(
                                            scoreBreakdown = ScoreFixtures.breakdown(totalCells = 5, bonusPoints = 0),
                                        )
                                    1 ->
                                        player.copy(
                                            scoreBreakdown = ScoreFixtures.breakdown(totalCells = 7, bonusPoints = 0),
                                        )
                                    2 ->
                                        player.copy(
                                            scoreBreakdown = ScoreFixtures.breakdown(totalCells = 6, bonusPoints = 3),
                                        )
                                    else ->
                                        player.copy(
                                            scoreBreakdown = ScoreFixtures.breakdown(totalCells = 2, bonusPoints = 0),
                                        )
                                }
                            },
                )

        val rankings = Scoring.rankPlayers(state)

        assertEquals(listOf("Player 1", "Player 2"), rankings.map { score -> score.name })
        assertEquals(listOf(14, 9), rankings.map { score -> score.totalScore })
        assertEquals(listOf(0, 1), rankings.map { score -> score.ownerIndex })
    }

    private fun placeThreeBendOverBonusTiles(
        bonusTiles: List<BonusTile>,
        randomSeed: Long,
    ): MoveResult.Accepted {
        val state =
            engine.newGame(
                GameConfig(
                    mode = GameMode.FOUR_PLAYER,
                    boardSize = GameConstants.STANDARD_BOARD_SIZE,
                    randomSeed = randomSeed,
                    bonusTiles = bonusTiles,
                ),
            )
        val result =
            engine.applyMove(
                state,
                Move(
                    playerIndex = 0,
                    pieceId = PieceCatalog.THREE_BEND_ID,
                    anchorRow = 0,
                    anchorCol = 0,
                    orientationIndex = 0,
                ),
            )

        assertTrue(result is MoveResult.Accepted)
        return result as MoveResult.Accepted
    }
}
