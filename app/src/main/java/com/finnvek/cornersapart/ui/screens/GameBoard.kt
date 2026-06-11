package com.finnvek.cornersapart.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.finnvek.cornersapart.R
import com.finnvek.cornersapart.model.BoardSnapshot
import com.finnvek.cornersapart.model.CellPosition
import com.finnvek.cornersapart.ui.components.drawGlossyCell
import com.finnvek.cornersapart.ui.theme.CornersApartAlpha
import com.finnvek.cornersapart.ui.theme.CornersApartColors
import com.finnvek.cornersapart.ui.theme.CornersApartPlayerPalette
import com.finnvek.cornersapart.ui.theme.CornersApartSpacing
import com.finnvek.cornersapart.viewmodel.GameUiState
import kotlin.math.floor

@Composable
fun GameBoard(
    state: GameUiState,
    onPlaceCell: (row: Int, col: Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val boardDescription = stringResource(R.string.game_board_content_description)
    val gapPx = with(LocalDensity.current) { CornersApartSpacing.BoardCellGap.toPx() }
    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .background(CornersApartColors.BoardFrame)
                .padding(CornersApartSpacing.BoardFrameWidth),
    ) {
        Canvas(
            modifier =
                Modifier
                    .matchParentSize()
                    .semantics { contentDescription = boardDescription }
                    .pointerInput(state.board) {
                        detectTapGestures { offset ->
                            val cellPitch = size.width / state.board.size
                            val row = floor(offset.y / cellPitch).toInt().coerceIn(0, state.board.size - 1)
                            val col = floor(offset.x / cellPitch).toInt().coerceIn(0, state.board.size - 1)
                            onPlaceCell(row, col)
                        }
                    },
        ) {
            val boardSize = state.board.size
            val cellSize = (size.minDimension - gapPx * (boardSize - 1)) / boardSize
            val pitch = cellSize + gapPx
            drawEmptyCells(state.board, cellSize, pitch)
            drawBonusTiles(state, cellSize, pitch)
            drawStartMarkers(state, cellSize, pitch)
            drawOccupiedCells(state, cellSize, pitch)
        }
    }
}

private fun DrawScope.drawEmptyCells(
    board: BoardSnapshot,
    cellSize: Float,
    pitch: Float,
) {
    for (row in 0 until board.size) {
        for (col in 0 until board.size) {
            drawRect(
                color = CornersApartColors.BoardCellSurface,
                topLeft = Offset(col * pitch, row * pitch),
                size = Size(cellSize, cellSize),
            )
        }
    }
}

private fun DrawScope.drawBonusTiles(
    state: GameUiState,
    cellSize: Float,
    pitch: Float,
) {
    state.bonusTiles
        .filter { tile -> tile.claimedByPlayerIndex == null }
        .forEach { tile ->
            val center = Offset(tile.col * pitch + cellSize / 2f, tile.row * pitch + cellSize / 2f)
            val radius = cellSize * BONUS_MARKER_RADIUS_FRACTION
            val marker =
                Path().apply {
                    moveTo(center.x, center.y - radius)
                    lineTo(center.x + radius, center.y)
                    lineTo(center.x, center.y + radius)
                    lineTo(center.x - radius, center.y)
                    close()
                }
            drawPath(marker, CornersApartColors.BonusAccent)
        }
}

private fun DrawScope.drawStartMarkers(
    state: GameUiState,
    cellSize: Float,
    pitch: Float,
) {
    state.players.forEach { player ->
        val position = CellPosition(player.startRow, player.startCol)
        if (state.board.get(position) == BoardSnapshot.EMPTY) {
            val colors = CornersApartPlayerPalette.colorsFor(player.colorIndex)
            drawCircle(
                color = colors.base.copy(alpha = CornersApartAlpha.StartMarker),
                radius = cellSize * START_MARKER_RADIUS_FRACTION,
                center = Offset(position.col * pitch + cellSize / 2f, position.row * pitch + cellSize / 2f),
            )
        }
    }
}

private fun DrawScope.drawOccupiedCells(
    state: GameUiState,
    cellSize: Float,
    pitch: Float,
) {
    for (row in 0 until state.board.size) {
        for (col in 0 until state.board.size) {
            val playerIndex = state.board.get(row, col)
            if (playerIndex != BoardSnapshot.EMPTY) {
                val colors = CornersApartPlayerPalette.colorsFor(playerIndex)
                drawGlossyCell(
                    topLeft = Offset(col * pitch, row * pitch),
                    cellSize = cellSize,
                    base = colors.base,
                    dark = colors.dark,
                    highlight = colors.highlight,
                )
            }
        }
    }
}

private const val BONUS_MARKER_RADIUS_FRACTION = 0.24f
private const val START_MARKER_RADIUS_FRACTION = 0.18f
