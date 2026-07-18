package com.finnvek.cornersapart.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.finnvek.cornersapart.model.CellOffset
import com.finnvek.cornersapart.model.PieceTransforms
import com.finnvek.cornersapart.ui.theme.CornersApartAlpha
import com.finnvek.cornersapart.ui.theme.CornersApartPlayerPalette
import com.finnvek.cornersapart.ui.theme.PlayerPieceColors

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
            colors = colors,
            alpha = alpha,
        )
    }
}

fun DrawScope.drawPieceCells(
    cells: List<CellOffset>,
    colors: PlayerPieceColors,
    alpha: Float = 1f,
) {
    if (cells.isEmpty()) return
    val rowCount = cells.maxOf { cell -> cell.row } + 1
    val colCount = cells.maxOf { cell -> cell.col } + 1
    val pitch = minOf(size.width / colCount, size.height / rowCount)
    val gap = pitch * CELL_GAP_FRACTION
    val cellSize = pitch - gap * 2f
    val leftOffset = (size.width - colCount * pitch) / 2f
    val topOffset = (size.height - rowCount * pitch) / 2f
    cells.forEach { cell ->
        drawCandyCell(
            topLeft =
                Offset(
                    leftOffset + cell.col * pitch + gap,
                    topOffset + cell.row * pitch + gap,
                ),
            cellSize = cellSize,
            colors = colors,
            alpha = alpha,
        )
    }
}

/**
 * Piirtää yhden candy-tyylisen 3D-laatan: tumma pohjaviiste, jonka päältä
 * paljastuu alareunasta kaistale, vaalea→perusväri-liukuvärillä täytetty
 * kansi ja kiiltoraita ylhäällä.
 */
fun DrawScope.drawCandyCell(
    topLeft: Offset,
    cellSize: Float,
    colors: PlayerPieceColors,
    alpha: Float = 1f,
) {
    val corner = CornerRadius(cellSize * CELL_CORNER_FRACTION)
    drawRoundRect(
        color = colors.dark.copy(alpha = colors.dark.alpha * alpha),
        topLeft = topLeft,
        size = Size(cellSize, cellSize),
        cornerRadius = corner,
    )
    val faceHeight = cellSize * CELL_FACE_HEIGHT_FRACTION
    drawRoundRect(
        brush =
            Brush.verticalGradient(
                colors =
                    listOf(
                        colors.highlight.copy(alpha = colors.highlight.alpha * alpha),
                        colors.base.copy(alpha = colors.base.alpha * alpha),
                    ),
                startY = topLeft.y,
                endY = topLeft.y + faceHeight,
            ),
        topLeft = topLeft,
        size = Size(cellSize, faceHeight),
        cornerRadius = corner,
    )
    drawRoundRect(
        color = Color.White.copy(alpha = CornersApartAlpha.CellGloss * alpha),
        topLeft =
            topLeft +
                Offset(
                    cellSize * CELL_GLOSS_X_FRACTION,
                    cellSize * CELL_GLOSS_Y_FRACTION,
                ),
        size = Size(cellSize * CELL_GLOSS_WIDTH_FRACTION, cellSize * CELL_GLOSS_HEIGHT_FRACTION),
        cornerRadius = CornerRadius(cellSize * CELL_GLOSS_CORNER_FRACTION),
    )
}

private const val CELL_GAP_FRACTION = 0.015f
private const val CELL_CORNER_FRACTION = 0.18f
private const val CELL_FACE_HEIGHT_FRACTION = 0.86f
private const val CELL_GLOSS_X_FRACTION = 0.10f
private const val CELL_GLOSS_Y_FRACTION = 0.07f
private const val CELL_GLOSS_WIDTH_FRACTION = 0.80f
private const val CELL_GLOSS_HEIGHT_FRACTION = 0.16f
private const val CELL_GLOSS_CORNER_FRACTION = 0.08f
