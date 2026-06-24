package com.finnvek.cornersapart.engine

import com.finnvek.cornersapart.model.CellPosition
import com.finnvek.cornersapart.model.GameState

sealed interface MoveResult {
    data class Accepted(
        val state: GameState,
        val scoreDelta: ScoreDelta,
    ) : MoveResult

    data class Rejected(
        val reason: MoveRejectionReason,
    ) : MoveResult
}

enum class MoveRejectionReason {
    GAME_OVER,
    NOT_PLAYERS_TURN,
    INVALID_PLAYER,
    PLAYER_HAS_PASSED,
    UNKNOWN_PIECE,
    UNKNOWN_ORIENTATION,
    PIECE_ALREADY_USED,
    OUT_OF_BOUNDS,
    CELL_OCCUPIED,
    START_CORNER_NOT_COVERED,
    SAME_PLAYER_EDGE_TOUCH,
    NO_DIAGONAL_TOUCH,
}

class MoveRejectedException(
    val reason: MoveRejectionReason,
) : IllegalArgumentException(reason.name)

data class PlacementPreview(
    val isValid: Boolean,
    val targetCells: List<CellPosition>,
    val rejectionReason: MoveRejectionReason?,
    val scoreDelta: ScoreDelta,
)
