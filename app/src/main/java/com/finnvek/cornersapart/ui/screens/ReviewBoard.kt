package com.finnvek.cornersapart.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.finnvek.cornersapart.model.CellPosition
import com.finnvek.cornersapart.model.PieceCatalog
import com.finnvek.cornersapart.model.PieceTransforms
import com.finnvek.cornersapart.model.targetCells
import com.finnvek.cornersapart.review.MoveAssessment
import com.finnvek.cornersapart.review.ReviewAction
import com.finnvek.cornersapart.review.ReviewTimelineStep
import com.finnvek.cornersapart.ui.components.BoardVisualPlayer
import com.finnvek.cornersapart.ui.components.candyBoardPanel
import com.finnvek.cornersapart.ui.components.drawBoardBase
import com.finnvek.cornersapart.ui.components.drawCandyCell
import com.finnvek.cornersapart.ui.components.drawCellPerimeter
import com.finnvek.cornersapart.ui.theme.CornersApartAlpha
import com.finnvek.cornersapart.ui.theme.CornersApartColors
import com.finnvek.cornersapart.ui.theme.CornersApartPlayerPalette
import com.finnvek.cornersapart.ui.theme.CornersApartSpacing
import com.finnvek.cornersapart.viewmodel.MatchReviewPlayerUiState

@Composable
fun ReviewBoard(
    step: ReviewTimelineStep,
    players: List<MatchReviewPlayerUiState>,
    contentDescription: String,
    modifier: Modifier = Modifier,
    assessment: MoveAssessment? = null,
    showBestMove: Boolean = false,
) {
    val gapPx = with(LocalDensity.current) { CornersApartSpacing.BoardCellGap.toPx() }
    val displayedState = if (showBestMove && assessment != null) step.stateBefore else step.stateAfter
    val visualPlayers =
        remember(displayedState.players, players) {
            displayedState.players.map { player ->
                BoardVisualPlayer(
                    index = player.index,
                    colorIndex =
                        players.firstOrNull { uiPlayer -> uiPlayer.index == player.index }?.colorIndex
                            ?: player.colorIndex,
                    startCorner = player.startCorner,
                )
            }
        }
    Box(modifier = modifier.candyBoardPanel()) {
        Canvas(
            modifier =
                Modifier
                    .matchParentSize()
                    .semantics { this.contentDescription = contentDescription },
        ) {
            val boardSize = displayedState.board.size
            val cellSize = (size.minDimension - gapPx * (boardSize - 1)) / boardSize
            val pitch = cellSize + gapPx
            drawBoardBase(
                board = displayedState.board,
                bonusTiles = displayedState.bonusTiles,
                players = visualPlayers,
                cellSize = cellSize,
                pitch = pitch,
                bonusAlpha = 1f,
            )

            if (showBestMove && assessment != null) {
                val bestMoveCells = assessment.bestMove.cells()
                val colorIndex =
                    players.firstOrNull { player -> player.index == assessment.bestMove.playerIndex }?.colorIndex
                        ?: assessment.bestMove.playerIndex
                bestMoveCells.forEach { cell ->
                    drawCandyCell(
                        topLeft = Offset(cell.col * pitch, cell.row * pitch),
                        cellSize = cellSize,
                        colors = CornersApartPlayerPalette.colorsFor(colorIndex),
                        alpha = CornersApartAlpha.BestMoveGhost,
                    )
                }
                drawCellPerimeter(
                    cells = bestMoveCells,
                    cellSize = cellSize,
                    pitch = pitch,
                    color = CornersApartColors.ReviewCurrentMoveOutline,
                )
            } else {
                val playedCells =
                    (step.action as? ReviewAction.Placement)
                        ?.move
                        ?.cells()
                        .orEmpty()
                if (playedCells.isNotEmpty()) {
                    drawCellPerimeter(
                        cells = playedCells,
                        cellSize = cellSize,
                        pitch = pitch,
                        color = CornersApartColors.ReviewCurrentMoveOutline,
                    )
                }
            }
        }
    }
}

private fun com.finnvek.cornersapart.model.Move.cells(): List<CellPosition> {
    val piece = PieceCatalog.require(pieceId)
    val offsets = PieceTransforms.getOrientation(piece, orientationIndex).orEmpty()
    return targetCells(anchorRow, anchorCol, offsets)
}
