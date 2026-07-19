package com.finnvek.cornersapart.opponents

import com.finnvek.cornersapart.engine.GameEngine
import com.finnvek.cornersapart.model.BoardSnapshot
import com.finnvek.cornersapart.model.CellPosition
import com.finnvek.cornersapart.model.GameState
import com.finnvek.cornersapart.model.Move
import com.finnvek.cornersapart.model.PieceCatalog
import kotlin.math.abs

class MoveEvaluator(
    private val engine: GameEngine = GameEngine(),
) {
    private var cachedState: GameState? = null
    private var cachedAttachments: Map<Int, Set<CellPosition>> = emptyMap()

    fun evaluate(
        state: GameState,
        move: Move,
        style: OpponentStyle,
        difficulty: OpponentDifficulty,
    ): MoveEvaluation {
        ensureAttachmentCache(state)
        val preview = engine.previewPlacement(state, move)
        val targetCells = preview.targetCells
        val targetSet = targetCells.toSet()
        val placedCells = PieceCatalog.require(move.pieceId).cells.size
        val bonusClaims = preview.claimedBonusTileCount
        return MoveEvaluation(
            placedCellScore = placedCells * placedCellWeight(difficulty),
            bonusScore = bonusClaims * bonusWeight(style, difficulty),
            spreadScore = spreadScore(state, targetCells, move.playerIndex) * spreadWeight(style),
            centerScore = centerScore(state, targetCells) * centerWeight(style),
            blockingScore =
                denialScore(targetSet, move.playerIndex) * difficulty.blockingAwareness *
                    blockingWeight(style),
            mobilityScore =
                mobilityScore(state, targetCells, targetSet, move.playerIndex) *
                    difficulty.blockingAwareness * MOBILITY_WEIGHT,
            conservationScore = conservationScore(state, move.playerIndex, placedCells, difficulty),
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

    /** Kuinka monta vastustajien nykyistä kulmakiinnityspistettä siirto vie. */
    private fun denialScore(
        targetSet: Set<CellPosition>,
        playerIndex: Int,
    ): Double =
        cachedAttachments
            .filterKeys { index -> index != playerIndex }
            .values
            .sumOf { attachments -> attachments.count { attachment -> attachment in targetSet } }
            .toDouble()

    /** Siirron luomat uudet omat kulmavapaudet miinus menetetyt vanhat. */
    private fun mobilityScore(
        state: GameState,
        targetCells: List<CellPosition>,
        targetSet: Set<CellPosition>,
        playerIndex: Int,
    ): Double {
        val created =
            buildSet {
                targetCells.forEach { cell ->
                    DIAGONAL_OFFSETS.forEach { (rowDelta, colDelta) ->
                        val candidate = CellPosition(cell.row + rowDelta, cell.col + colDelta)
                        if (isFreeCell(state, candidate) &&
                            candidate !in targetSet &&
                            !touchesOwnEdgeAfterMove(state, candidate, playerIndex, targetSet)
                        ) {
                            add(candidate)
                        }
                    }
                }
            }.size
        val lost =
            cachedAttachments[playerIndex].orEmpty().count { attachment ->
                attachment in targetSet ||
                    ORTHOGONAL_OFFSETS.any { (rowDelta, colDelta) ->
                        CellPosition(attachment.row + rowDelta, attachment.col + colDelta) in targetSet
                    }
            }
        return (created - lost).toDouble()
    }

    /** Sakottaa pienten palojen tuhlaamisesta loppupelissä (säästö completion bonusta varten). */
    private fun conservationScore(
        state: GameState,
        playerIndex: Int,
        placedCells: Int,
        difficulty: OpponentDifficulty,
    ): Double {
        val remaining = PieceCatalog.all.size - state.players[playerIndex].usedPieceIds.size
        if (remaining !in ENDGAME_MIN_REMAINING..ENDGAME_MAX_REMAINING) return 0.0
        if (placedCells > SMALL_PIECE_MAX_CELLS) return 0.0
        return -(SMALL_PIECE_MAX_CELLS + 1 - placedCells) * ENDGAME_HOLD_WEIGHT * difficulty.blockingAwareness
    }

    /** Laskee kaikkien pelaajien nykyiset kulmakiinnityspisteet kerran per pelitila. */
    private fun ensureAttachmentCache(state: GameState) {
        if (cachedState === state) return
        cachedState = state
        val cellsByPlayer = mutableMapOf<Int, MutableList<CellPosition>>()
        for (row in 0 until state.board.size) {
            for (col in 0 until state.board.size) {
                val owner = state.board.get(row, col)
                if (owner != BoardSnapshot.EMPTY) {
                    cellsByPlayer.getOrPut(owner) { mutableListOf() } += CellPosition(row, col)
                }
            }
        }
        cachedAttachments =
            state.players.associate { player ->
                player.index to attachmentCells(state, player.index, cellsByPlayer[player.index].orEmpty())
            }
    }

    private fun attachmentCells(
        state: GameState,
        playerIndex: Int,
        ownCells: List<CellPosition>,
    ): Set<CellPosition> =
        buildSet {
            ownCells.forEach { cell ->
                DIAGONAL_OFFSETS.forEach { (rowDelta, colDelta) ->
                    val candidate = CellPosition(cell.row + rowDelta, cell.col + colDelta)
                    if (isFreeCell(state, candidate) && !touchesOwnEdge(state, candidate, playerIndex)) {
                        add(candidate)
                    }
                }
            }
        }

    private fun isFreeCell(
        state: GameState,
        cell: CellPosition,
    ): Boolean = state.board.contains(cell) && state.board.get(cell) == BoardSnapshot.EMPTY

    private fun touchesOwnEdge(
        state: GameState,
        cell: CellPosition,
        playerIndex: Int,
    ): Boolean =
        ORTHOGONAL_OFFSETS.any { (rowDelta, colDelta) ->
            val neighbor = CellPosition(cell.row + rowDelta, cell.col + colDelta)
            state.board.contains(neighbor) && state.board.get(neighbor) == playerIndex
        }

    private fun touchesOwnEdgeAfterMove(
        state: GameState,
        cell: CellPosition,
        playerIndex: Int,
        targetSet: Set<CellPosition>,
    ): Boolean =
        ORTHOGONAL_OFFSETS.any { (rowDelta, colDelta) ->
            val neighbor = CellPosition(cell.row + rowDelta, cell.col + colDelta)
            neighbor in targetSet ||
                (state.board.contains(neighbor) && state.board.get(neighbor) == playerIndex)
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
        const val HIGH_BLOCKING_WEIGHT = 4.0
        const val MEDIUM_BLOCKING_WEIGHT = 2.0
        const val LOW_BLOCKING_WEIGHT = 0.8
        const val MOBILITY_WEIGHT = 1.5
        const val ENDGAME_MIN_REMAINING = 3
        const val ENDGAME_MAX_REMAINING = 6
        const val SMALL_PIECE_MAX_CELLS = 2
        const val ENDGAME_HOLD_WEIGHT = 6.0
        val DIAGONAL_OFFSETS = listOf(-1 to -1, -1 to 1, 1 to -1, 1 to 1)
        val ORTHOGONAL_OFFSETS = listOf(-1 to 0, 1 to 0, 0 to -1, 0 to 1)
    }
}
