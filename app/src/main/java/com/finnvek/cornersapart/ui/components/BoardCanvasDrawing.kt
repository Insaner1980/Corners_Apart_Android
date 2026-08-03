package com.finnvek.cornersapart.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.finnvek.cornersapart.model.BoardSnapshot
import com.finnvek.cornersapart.model.BonusTile
import com.finnvek.cornersapart.model.CellPosition
import com.finnvek.cornersapart.ui.theme.CornersApartAlpha
import com.finnvek.cornersapart.ui.theme.CornersApartColors
import com.finnvek.cornersapart.ui.theme.CornersApartPlayerPalette
import com.finnvek.cornersapart.ui.theme.CornersApartSpacing

data class BoardVisualPlayer(
    val index: Int,
    val colorIndex: Int,
    val startCorner: CellPosition,
)

fun Modifier.candyBoardPanel(): Modifier =
    fillMaxWidth()
        .aspectRatio(1f)
        .clip(RoundedCornerShape(CornersApartSpacing.BoardPanelRadius))
        .background(CornersApartColors.BoardPanel)
        .border(
            width = CornersApartSpacing.BoardPanelBorderWidth,
            color = CornersApartColors.PanelSurfaceRaised,
            shape = RoundedCornerShape(CornersApartSpacing.BoardPanelRadius),
        ).padding(CornersApartSpacing.BoardPanelPadding)

fun DrawScope.drawBoardBase(
    board: BoardSnapshot,
    bonusTiles: List<BonusTile>,
    players: List<BoardVisualPlayer>,
    cellSize: Float,
    pitch: Float,
    bonusAlpha: Float,
    popCells: Set<CellPosition> = emptySet(),
    popScale: Float = 1f,
) {
    drawEmptyCells(board, cellSize, pitch)
    drawBonusTiles(bonusTiles, cellSize, pitch, bonusAlpha)
    drawStartMarkers(board, players, cellSize, pitch)
    drawOccupiedCells(board, players, cellSize, pitch, popCells, popScale)
}

fun DrawScope.drawCellPerimeter(
    cells: List<CellPosition>,
    cellSize: Float,
    pitch: Float,
    color: Color,
) {
    val cellSet = cells.toSet()
    val stroke = cellSize * PERIMETER_STROKE_FRACTION
    cells.forEach { cell ->
        val x = cell.col * pitch
        val y = cell.row * pitch
        if (CellPosition(cell.row - 1, cell.col) !in cellSet) {
            drawLine(color, Offset(x, y), Offset(x + cellSize, y), stroke, StrokeCap.Round)
        }
        if (CellPosition(cell.row + 1, cell.col) !in cellSet) {
            drawLine(color, Offset(x, y + cellSize), Offset(x + cellSize, y + cellSize), stroke, StrokeCap.Round)
        }
        if (CellPosition(cell.row, cell.col - 1) !in cellSet) {
            drawLine(color, Offset(x, y), Offset(x, y + cellSize), stroke, StrokeCap.Round)
        }
        if (CellPosition(cell.row, cell.col + 1) !in cellSet) {
            drawLine(color, Offset(x + cellSize, y), Offset(x + cellSize, y + cellSize), stroke, StrokeCap.Round)
        }
    }
}

private fun DrawScope.drawEmptyCells(
    board: BoardSnapshot,
    cellSize: Float,
    pitch: Float,
) {
    val corner = CornerRadius(cellSize * EMPTY_CELL_CORNER_FRACTION)
    for (row in 0 until board.size) {
        for (col in 0 until board.size) {
            drawRoundRect(
                color = CornersApartColors.BoardCellEmpty,
                topLeft = Offset(col * pitch, row * pitch),
                size = Size(cellSize, cellSize),
                cornerRadius = corner,
            )
        }
    }
}

private fun DrawScope.drawBonusTiles(
    bonusTiles: List<BonusTile>,
    cellSize: Float,
    pitch: Float,
    pulseAlpha: Float,
) {
    bonusTiles
        .filter { tile -> tile.claimedByPlayerIndex == null }
        .forEach { tile ->
            val center = Offset(tile.col * pitch + cellSize / 2f, tile.row * pitch + cellSize / 2f)
            val radius = cellSize * BONUS_MARKER_RADIUS_FRACTION
            drawCircle(
                color =
                    CornersApartColors.BonusAccentBright.copy(
                        alpha = CornersApartAlpha.BonusGlow * pulseAlpha,
                    ),
                radius = cellSize * BONUS_GLOW_RADIUS_FRACTION,
                center = center,
            )
            drawPath(diamondPath(center, radius), CornersApartColors.BonusAccentBright.copy(alpha = pulseAlpha))
            drawPath(
                diamondPath(center, radius * BONUS_INNER_DIAMOND_FRACTION),
                CornersApartColors.BonusAccent.copy(alpha = pulseAlpha),
            )
        }
}

private fun diamondPath(
    center: Offset,
    radius: Float,
): Path =
    Path().apply {
        moveTo(center.x, center.y - radius)
        lineTo(center.x + radius, center.y)
        lineTo(center.x, center.y + radius)
        lineTo(center.x - radius, center.y)
        close()
    }

private fun DrawScope.drawStartMarkers(
    board: BoardSnapshot,
    players: List<BoardVisualPlayer>,
    cellSize: Float,
    pitch: Float,
) {
    players.forEach { player ->
        val position = player.startCorner
        if (board.contains(position) && board.get(position) == BoardSnapshot.EMPTY) {
            val colors = CornersApartPlayerPalette.colorsFor(player.colorIndex)
            val center = Offset(position.col * pitch + cellSize / 2f, position.row * pitch + cellSize / 2f)
            drawCircle(
                color = colors.base.copy(alpha = CornersApartAlpha.BonusGlow),
                radius = cellSize * START_MARKER_GLOW_RADIUS_FRACTION,
                center = center,
            )
            drawCircle(
                color = colors.highlight.copy(alpha = CornersApartAlpha.StartMarker),
                radius = cellSize * START_MARKER_RADIUS_FRACTION,
                center = center,
            )
        }
    }
}

private fun DrawScope.drawOccupiedCells(
    board: BoardSnapshot,
    players: List<BoardVisualPlayer>,
    cellSize: Float,
    pitch: Float,
    popCells: Set<CellPosition>,
    popScale: Float,
) {
    for (row in 0 until board.size) {
        for (col in 0 until board.size) {
            val playerIndex = board.get(row, col)
            if (playerIndex != BoardSnapshot.EMPTY) {
                val colorIndex =
                    players.firstOrNull { player -> player.index == playerIndex }?.colorIndex ?: playerIndex
                drawCandyCell(
                    topLeft = Offset(col * pitch, row * pitch),
                    cellSize = cellSize,
                    colors = CornersApartPlayerPalette.colorsFor(colorIndex),
                    scale = if (CellPosition(row, col) in popCells) popScale else 1f,
                )
            }
        }
    }
}

private const val BONUS_MARKER_RADIUS_FRACTION = 0.24f
private const val BONUS_GLOW_RADIUS_FRACTION = 0.42f
private const val BONUS_INNER_DIAMOND_FRACTION = 0.55f
private const val START_MARKER_RADIUS_FRACTION = 0.18f
private const val START_MARKER_GLOW_RADIUS_FRACTION = 0.40f
private const val EMPTY_CELL_CORNER_FRACTION = 0.12f
private const val PERIMETER_STROKE_FRACTION = 0.12f
