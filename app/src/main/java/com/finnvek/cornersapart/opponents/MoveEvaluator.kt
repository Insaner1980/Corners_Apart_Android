package com.finnvek.cornersapart.opponents

import com.finnvek.cornersapart.engine.GameEngine
import com.finnvek.cornersapart.model.CellPosition
import com.finnvek.cornersapart.model.GameState
import com.finnvek.cornersapart.model.Move
import com.finnvek.cornersapart.model.PieceCatalog
import kotlin.math.abs

class MoveEvaluator(
    private val engine: GameEngine = GameEngine(),
) {
    fun evaluate(
        state: GameState,
        move: Move,
        style: OpponentStyle,
        difficulty: OpponentDifficulty,
    ): MoveEvaluation {
        val preview = engine.previewPlacement(state, move)
        val targetCells = preview.targetCells
        val placedCells = PieceCatalog.require(move.pieceId).cells.size
        val bonusClaims =
            preview.scoreDelta.bonusTilePoints / com.finnvek.cornersapart.model.GameConstants.BONUS_TILE_POINTS
        return MoveEvaluation(
            placedCellScore = placedCells * placedCellWeight(difficulty),
            bonusScore = bonusClaims * bonusWeight(style, difficulty),
            spreadScore = spreadScore(state, targetCells, move.playerIndex) * spreadWeight(style),
            centerScore = centerScore(state, targetCells) * centerWeight(style),
            blockingScore =
                blockingScore(state, targetCells, move.playerIndex) * difficulty.blockingAwareness *
                    blockingWeight(style),
        )
    }

    fun evaluate(
        state: GameState,
        candidate: MoveCandidate,
        style: OpponentStyle,
        difficulty: OpponentDifficulty,
    ): MoveEvaluation = evaluate(state, candidate.move, style, difficulty)

    private fun placedCellWeight(difficulty: OpponentDifficulty): Double =
        BASE_PLACED_CELL_WEIGHT + difficulty.largePieceBias

    private fun bonusWeight(
        style: OpponentStyle,
        difficulty: OpponentDifficulty,
    ): Double =
        when (style) {
            OpponentStyle.OPPORTUNIST -> HIGH_BONUS_WEIGHT
            OpponentStyle.EXPANSIONIST -> MEDIUM_BONUS_WEIGHT
            OpponentStyle.BLOCKER -> LOW_BONUS_WEIGHT
        } * difficulty.bonusTileAwareness

    private fun spreadWeight(style: OpponentStyle): Double =
        when (style) {
            OpponentStyle.EXPANSIONIST -> HIGH_SPREAD_WEIGHT
            OpponentStyle.OPPORTUNIST -> MEDIUM_SPREAD_WEIGHT
            OpponentStyle.BLOCKER -> LOW_SPREAD_WEIGHT
        }

    private fun centerWeight(style: OpponentStyle): Double =
        when (style) {
            OpponentStyle.BLOCKER -> HIGH_CENTER_WEIGHT
            OpponentStyle.OPPORTUNIST -> MEDIUM_CENTER_WEIGHT
            OpponentStyle.EXPANSIONIST -> LOW_CENTER_WEIGHT
        }

    private fun blockingWeight(style: OpponentStyle): Double =
        when (style) {
            OpponentStyle.BLOCKER -> HIGH_BLOCKING_WEIGHT
            OpponentStyle.OPPORTUNIST -> MEDIUM_BLOCKING_WEIGHT
            OpponentStyle.EXPANSIONIST -> LOW_BLOCKING_WEIGHT
        }

    private fun spreadScore(
        state: GameState,
        targetCells: List<CellPosition>,
        playerIndex: Int,
    ): Double {
        val player = state.players[playerIndex]
        return targetCells
            .maxOfOrNull { target ->
                abs(target.row - player.startCorner.row) + abs(target.col - player.startCorner.col)
            }?.toDouble()
            .orZero()
    }

    private fun centerScore(
        state: GameState,
        targetCells: List<CellPosition>,
    ): Double {
        val center = (state.board.size - 1) / 2.0
        return targetCells
            .minOfOrNull { target ->
                val distance = abs(target.row - center) + abs(target.col - center)
                state.board.size - distance
            }.orZero()
    }

    private fun blockingScore(
        state: GameState,
        targetCells: List<CellPosition>,
        playerIndex: Int,
    ): Double {
        val opponents = state.players.filter { player -> player.index != playerIndex }
        return targetCells
            .sumOf { target ->
                opponents.count { opponent ->
                    abs(target.row - opponent.startCorner.row) + abs(target.col - opponent.startCorner.col) <=
                        state.board.size / 2
                }
            }.toDouble()
    }

    private fun Double?.orZero(): Double = this ?: 0.0

    private companion object {
        const val BASE_PLACED_CELL_WEIGHT = 10.0
        const val HIGH_BONUS_WEIGHT = 25.0
        const val MEDIUM_BONUS_WEIGHT = 12.0
        const val LOW_BONUS_WEIGHT = 8.0
        const val HIGH_SPREAD_WEIGHT = 0.8
        const val MEDIUM_SPREAD_WEIGHT = 0.4
        const val LOW_SPREAD_WEIGHT = 0.2
        const val HIGH_CENTER_WEIGHT = 0.7
        const val MEDIUM_CENTER_WEIGHT = 0.4
        const val LOW_CENTER_WEIGHT = 0.1
        const val HIGH_BLOCKING_WEIGHT = 1.2
        const val MEDIUM_BLOCKING_WEIGHT = 0.6
        const val LOW_BLOCKING_WEIGHT = 0.2
    }
}
