package com.finnvek.cornersapart.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.finnvek.cornersapart.model.CellOffset
import com.finnvek.cornersapart.model.PieceTransforms
import com.finnvek.cornersapart.ui.theme.CornersApartAlpha
import com.finnvek.cornersapart.ui.theme.CornersApartPlayerPalette

@Composable
fun PieceShape(
    cells: List<CellOffset>,
    colorIndex: Int,
    modifier: Modifier = Modifier,
    alpha: Float = 1f,
) {
    val colors = CornersApartPlayerPalette.colorsFor(colorIndex)
    Canvas(modifier = modifier) {
        drawPieceCells(
            cells = PieceTransforms.normalize(cells),
            base = colors.base.copy(alpha = alpha),
            dark = colors.dark.copy(alpha = alpha),
            highlight = colors.highlight.copy(alpha = alpha),
        )
    }
}

fun DrawScope.drawPieceCells(
    cells: List<CellOffset>,
    base: Color,
    dark: Color,
    highlight: Color,
) {
    if (cells.isEmpty()) return
    val rowCount = cells.maxOf { cell -> cell.row } + 1
    val colCount = cells.maxOf { cell -> cell.col } + 1
    val cellSize = minOf(size.width / colCount, size.height / rowCount)
    val leftOffset = (size.width - colCount * cellSize) / 2f
    val topOffset = (size.height - rowCount * cellSize) / 2f
    cells.forEach { cell ->
        drawGlossyCell(
            topLeft = Offset(leftOffset + cell.col * cellSize, topOffset + cell.row * cellSize),
            cellSize = cellSize,
            base = base,
            dark = dark,
            highlight = highlight,
        )
    }
}

fun DrawScope.drawGlossyCell(
    topLeft: Offset,
    cellSize: Float,
    base: Color,
    dark: Color,
    highlight: Color,
) {
    val shadowOffset = cellSize * CELL_SHADOW_OFFSET_FRACTION
    val inset = cellSize * CELL_INSET_FRACTION
    drawRect(
        color = Color.Black.copy(alpha = CornersApartAlpha.PieceDropShadow),
        topLeft = topLeft + Offset(shadowOffset, shadowOffset),
        size = Size(cellSize, cellSize),
    )
    drawRect(color = base, topLeft = topLeft, size = Size(cellSize, cellSize))
    drawRect(
        color = highlight.copy(alpha = CornersApartAlpha.PieceHighlight),
        topLeft = topLeft,
        size = Size(cellSize, cellSize * CELL_HIGHLIGHT_HEIGHT_FRACTION),
    )
    drawRect(
        color = dark.copy(alpha = CornersApartAlpha.PieceShadow),
        topLeft = Offset(topLeft.x, topLeft.y + cellSize * CELL_BOTTOM_SHADOW_START_FRACTION),
        size = Size(cellSize, cellSize * CELL_BOTTOM_SHADOW_HEIGHT_FRACTION),
    )
    drawRect(
        color = Color.Black.copy(alpha = CornersApartAlpha.PieceInnerInset),
        topLeft = topLeft + Offset(inset, inset),
        size = Size(cellSize - inset * 2f, cellSize - inset * 2f),
    )
}

private const val CELL_SHADOW_OFFSET_FRACTION = 0.05f
private const val CELL_INSET_FRACTION = 0.08f
private const val CELL_HIGHLIGHT_HEIGHT_FRACTION = 0.25f
private const val CELL_BOTTOM_SHADOW_START_FRACTION = 0.85f
private const val CELL_BOTTOM_SHADOW_HEIGHT_FRACTION = 0.15f
