package com.finnvek.cornersapart.engine

import com.finnvek.cornersapart.model.BoardSnapshot
import com.finnvek.cornersapart.model.BonusTile
import com.finnvek.cornersapart.model.CellOffset
import com.finnvek.cornersapart.model.CellPosition
import com.finnvek.cornersapart.model.GameState
import com.finnvek.cornersapart.model.Move
import com.finnvek.cornersapart.model.MutableBoard
import com.finnvek.cornersapart.model.PieceCatalog
import com.finnvek.cornersapart.model.PieceTransforms
import com.finnvek.cornersapart.model.Player

internal object PlacementValidator {
    fun validate(
        state: GameState,
        move: Move,
        enforceTurn: Boolean = true,
    ): PlacementValidation {
        val player = state.players.getOrNull(move.playerIndex) ?: return invalid(MoveRejectionReason.INVALID_PLAYER)
        if (state.isGameOver) return invalid(MoveRejectionReason.GAME_OVER)
        if (enforceTurn &&
            move.playerIndex != state.currentPlayerIndex
        ) {
            return invalid(MoveRejectionReason.NOT_PLAYERS_TURN)
        }
        if (player.passed) return invalid(MoveRejectionReason.PLAYER_HAS_PASSED)
        if (move.pieceId in player.usedPieceIds) return invalid(MoveRejectionReason.PIECE_ALREADY_USED)

        val piece = PieceCatalog.find(move.pieceId) ?: return invalid(MoveRejectionReason.UNKNOWN_PIECE)
        val orientation =
            PieceTransforms.getOrientation(piece, move.orientationIndex)
                ?: return invalid(MoveRejectionReason.UNKNOWN_ORIENTATION)
        val targetCells = targetCells(move, orientation)
        val board = MutableBoard(state.board)

        return validateTargetCells(state, board, player, targetCells)
    }

    fun targetCells(
        move: Move,
        orientation: List<CellOffset>,
    ): List<CellPosition> =
        orientation.map { offset ->
            CellPosition(
                row = move.anchorRow + offset.row,
                col = move.anchorCol + offset.col,
            )
        }

    private fun validateTargetCells(
        state: GameState,
        board: MutableBoard,
        player: Player,
        targetCells: List<CellPosition>,
    ): PlacementValidation {
        val issue =
            boundsOrOccupancyIssue(board, targetCells)
                ?: connectionIssue(board, player, targetCells)
        if (issue != null) return invalid(issue, targetCells)
        return PlacementValidation(
            isValid = true,
            reason = null,
            targetCells = targetCells,
            claimedBonusTiles = claimableBonusTiles(state, targetCells),
        )
    }

    private fun boundsOrOccupancyIssue(
        board: MutableBoard,
        targetCells: List<CellPosition>,
    ): MoveRejectionReason? =
        targetCells.firstNotNullOfOrNull { target ->
            when {
                !board.contains(target) -> MoveRejectionReason.OUT_OF_BOUNDS
                board.get(target) != BoardSnapshot.EMPTY -> MoveRejectionReason.CELL_OCCUPIED
                else -> null
            }
        }

    private fun connectionIssue(
        board: MutableBoard,
        player: Player,
        targetCells: List<CellPosition>,
    ): MoveRejectionReason? =
        when {
            !player.hasPlacedAnyPiece &&
                player.startCorner !in targetCells
            -> MoveRejectionReason.START_CORNER_NOT_COVERED
            touchesSamePlayerByEdge(board, player.index, targetCells) -> MoveRejectionReason.SAME_PLAYER_EDGE_TOUCH
            player.hasPlacedAnyPiece &&
                !touchesSamePlayerDiagonally(
                    board,
                    player.index,
                    targetCells,
                ) -> MoveRejectionReason.NO_DIAGONAL_TOUCH
            else -> null
        }

    private fun touchesSamePlayerByEdge(
        board: MutableBoard,
        playerIndex: Int,
        targetCells: List<CellPosition>,
    ): Boolean =
        targetCells.any { target ->
            CellNeighbors.orthogonal(target).any { neighbor ->
                board.contains(neighbor) && board.get(neighbor) == playerIndex
            }
        }

    private fun touchesSamePlayerDiagonally(
        board: MutableBoard,
        playerIndex: Int,
        targetCells: List<CellPosition>,
    ): Boolean =
        targetCells.any { target ->
            CellNeighbors.diagonal(target).any { neighbor ->
                board.contains(neighbor) && board.get(neighbor) == playerIndex
            }
        }

    private fun claimableBonusTiles(
        state: GameState,
        targetCells: List<CellPosition>,
    ): List<BonusTile> {
        val targets = targetCells.toSet()
        return state.bonusTiles.filter { bonusTile ->
            bonusTile.claimedByPlayerIndex == null && bonusTile.position in targets
        }
    }

    private fun invalid(
        reason: MoveRejectionReason,
        targetCells: List<CellPosition> = emptyList(),
    ): PlacementValidation =
        PlacementValidation(
            isValid = false,
            reason = reason,
            targetCells = targetCells,
            claimedBonusTiles = emptyList(),
        )
}

internal data class PlacementValidation(
    val isValid: Boolean,
    val reason: MoveRejectionReason?,
    val targetCells: List<CellPosition>,
    val claimedBonusTiles: List<BonusTile>,
)
