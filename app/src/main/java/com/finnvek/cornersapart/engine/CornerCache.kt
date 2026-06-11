package com.finnvek.cornersapart.engine

import com.finnvek.cornersapart.model.BoardSnapshot
import com.finnvek.cornersapart.model.CellOffset
import com.finnvek.cornersapart.model.CellPosition
import com.finnvek.cornersapart.model.GameState

object CornerCache {
    fun candidateAnchors(
        state: GameState,
        playerIndex: Int,
        orientation: List<CellOffset>,
    ): Set<CellPosition> {
        val player = state.players[playerIndex]
        val targetCorners =
            if (player.hasPlacedAnyPiece) {
                cornerPositions(state.board, playerIndex)
            } else {
                setOf(player.startCorner)
            }
        return targetCorners
            .flatMap { corner ->
                orientation.map { offset ->
                    CellPosition(
                        row = corner.row - offset.row,
                        col = corner.col - offset.col,
                    )
                }
            }.toSet()
    }

    fun cornerPositions(
        board: BoardSnapshot,
        playerIndex: Int,
    ): Set<CellPosition> {
        val corners = mutableSetOf<CellPosition>()
        for (row in 0 until board.size) {
            for (col in 0 until board.size) {
                if (board.get(row, col) == playerIndex) {
                    corners += openDiagonalCorners(board, playerIndex, CellPosition(row, col))
                }
            }
        }
        return corners
    }

    private fun openDiagonalCorners(
        board: BoardSnapshot,
        playerIndex: Int,
        position: CellPosition,
    ): List<CellPosition> =
        CellNeighbors.diagonal(position).filter { candidate ->
            board.contains(candidate) &&
                board.get(candidate) == BoardSnapshot.EMPTY &&
                !hasOwnEdgeNeighbor(board, playerIndex, candidate)
        }

    private fun hasOwnEdgeNeighbor(
        board: BoardSnapshot,
        playerIndex: Int,
        position: CellPosition,
    ): Boolean =
        CellNeighbors.orthogonal(position).any { neighbor ->
            board.contains(neighbor) && board.get(neighbor) == playerIndex
        }
}
