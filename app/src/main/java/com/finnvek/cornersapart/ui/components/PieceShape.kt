package com.finnvek.cornersapart.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import com.finnvek.cornersapart.model.CellOffset
import com.finnvek.cornersapart.model.PieceTransforms
import com.finnvek.cornersapart.ui.theme.CornersApartColors
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
 * Piirtää yhden lasimaisen candy-laatan referenssityyliin: ulompi viiste
 * liukuvärillä, ohut valokehä ulkoreunassa, tumma ura viisteen ja kannen
 * välissä sekä sisempi kansi, jossa on yläkiilto ja alareunan hehku.
 */
fun DrawScope.drawCandyCell(
    topLeft: Offset,
    cellSize: Float,
    colors: PlayerPieceColors,
    alpha: Float = 1f,
    scale: Float = 1f,
) {
    drawScaledCandyCell(
        topLeft = topLeft + Offset(cellSize * (1f - scale) / 2f, cellSize * (1f - scale) / 2f),
        cellSize = cellSize * scale,
        colors = colors,
        alpha = alpha,
    )
}

private fun DrawScope.drawScaledCandyCell(
    topLeft: Offset,
    cellSize: Float,
    colors: PlayerPieceColors,
    alpha: Float,
) {
    drawCellBevel(topLeft, cellSize, colors, alpha)
    drawCellFace(topLeft, cellSize, colors, alpha)
    drawCellGloss(topLeft, cellSize, alpha)
}

/** Ulompi viiste, valokehä ja tumma ura. */
private fun DrawScope.drawCellBevel(
    topLeft: Offset,
    cellSize: Float,
    colors: PlayerPieceColors,
    alpha: Float,
) {
    val gloss = CornersApartColors.GlossTint
    val corner = CornerRadius(cellSize * CELL_CORNER_FRACTION)
    drawRoundRect(
        brush =
            Brush.verticalGradient(
                colorStops =
                    arrayOf(
                        0f to lerp(colors.base, colors.highlight, BEVEL_TOP_LIGHTEN).copy(alpha = alpha),
                        BEVEL_MID_STOP to colors.base.copy(alpha = colors.base.alpha * alpha),
                        1f to
                            lerp(colors.dark, CornersApartColors.TextShadow, BEVEL_BOTTOM_DEEPEN)
                                .copy(alpha = alpha),
                    ),
                startY = topLeft.y,
                endY = topLeft.y + cellSize,
            ),
        topLeft = topLeft,
        size = Size(cellSize, cellSize),
        cornerRadius = corner,
    )
    val rimWidth = cellSize * RIM_WIDTH_FRACTION
    drawRoundRect(
        brush =
            Brush.verticalGradient(
                colors =
                    listOf(
                        gloss.copy(alpha = RIM_ALPHA_TOP * alpha),
                        gloss.copy(alpha = RIM_ALPHA_BOTTOM * alpha),
                    ),
                startY = topLeft.y,
                endY = topLeft.y + cellSize,
            ),
        topLeft = topLeft + Offset(rimWidth / 2f, rimWidth / 2f),
        size = Size(cellSize - rimWidth, cellSize - rimWidth),
        cornerRadius = CornerRadius(corner.x - rimWidth / 2f),
        style = Stroke(rimWidth),
    )
    val grooveInset = cellSize * GROOVE_INSET_FRACTION
    drawRoundRect(
        color =
            lerp(colors.dark, CornersApartColors.TextShadow, GROOVE_DEEPEN)
                .copy(alpha = alpha),
        topLeft = topLeft + Offset(grooveInset, grooveInset),
        size = Size(cellSize - grooveInset * 2f, cellSize - grooveInset * 2f),
        cornerRadius = CornerRadius(cellSize * FACE_CORNER_FRACTION * GROOVE_CORNER_SCALE),
    )
}

/** Sisempi kansi liukuvärillä ja yläreunan valojuovalla. */
private fun DrawScope.drawCellFace(
    topLeft: Offset,
    cellSize: Float,
    colors: PlayerPieceColors,
    alpha: Float,
) {
    val gloss = CornersApartColors.GlossTint
    val faceInset = cellSize * FACE_INSET_FRACTION
    val faceSize = cellSize - faceInset * 2f
    val faceTopLeft = topLeft + Offset(faceInset, faceInset)
    val faceCorner = CornerRadius(cellSize * FACE_CORNER_FRACTION)
    drawRoundRect(
        brush =
            Brush.verticalGradient(
                colorStops =
                    arrayOf(
                        0f to lerp(colors.base, colors.highlight, FACE_TOP_LIGHTEN).copy(alpha = alpha),
                        FACE_MID_STOP to colors.base.copy(alpha = colors.base.alpha * alpha),
                        1f to lerp(colors.base, colors.dark, FACE_BOTTOM_DEEPEN).copy(alpha = alpha),
                    ),
                startY = faceTopLeft.y,
                endY = faceTopLeft.y + faceSize,
            ),
        topLeft = faceTopLeft,
        size = Size(faceSize, faceSize),
        cornerRadius = faceCorner,
    )
    val faceRimWidth = cellSize * FACE_RIM_WIDTH_FRACTION
    drawRoundRect(
        brush =
            Brush.verticalGradient(
                colors =
                    listOf(
                        gloss.copy(alpha = FACE_RIM_ALPHA * alpha),
                        gloss.copy(alpha = 0f),
                    ),
                startY = faceTopLeft.y,
                endY = faceTopLeft.y + faceSize * FACE_RIM_FADE_FRACTION,
            ),
        topLeft = faceTopLeft + Offset(faceRimWidth / 2f, faceRimWidth / 2f),
        size = Size(faceSize - faceRimWidth, faceSize - faceRimWidth),
        cornerRadius = CornerRadius(faceCorner.x - faceRimWidth / 2f),
        style = Stroke(faceRimWidth),
    )
}

/** Kannen yläosan kiilto ja alareunan lasimainen hehku. */
private fun DrawScope.drawCellGloss(
    topLeft: Offset,
    cellSize: Float,
    alpha: Float,
) {
    val gloss = CornersApartColors.GlossTint
    val faceInset = cellSize * FACE_INSET_FRACTION
    val faceSize = cellSize - faceInset * 2f
    val faceTopLeft = topLeft + Offset(faceInset, faceInset)
    val faceCorner = CornerRadius(cellSize * FACE_CORNER_FRACTION)
    val sheenHeight = faceSize * SHEEN_HEIGHT_FRACTION
    drawRoundRect(
        brush =
            Brush.verticalGradient(
                colors =
                    listOf(
                        gloss.copy(alpha = SHEEN_ALPHA * alpha),
                        gloss.copy(alpha = 0f),
                    ),
                startY = faceTopLeft.y,
                endY = faceTopLeft.y + sheenHeight,
            ),
        topLeft = faceTopLeft,
        size = Size(faceSize, sheenHeight),
        cornerRadius = faceCorner,
    )
    val glowHeight = faceSize * BOTTOM_GLOW_HEIGHT_FRACTION
    drawRoundRect(
        brush =
            Brush.verticalGradient(
                colors =
                    listOf(
                        gloss.copy(alpha = 0f),
                        gloss.copy(alpha = BOTTOM_GLOW_ALPHA * alpha),
                    ),
                startY = faceTopLeft.y + faceSize - glowHeight,
                endY = faceTopLeft.y + faceSize,
            ),
        topLeft = Offset(faceTopLeft.x, faceTopLeft.y + faceSize - glowHeight),
        size = Size(faceSize, glowHeight),
        cornerRadius = faceCorner,
    )
}

private const val CELL_GAP_FRACTION = 0.015f
private const val CELL_CORNER_FRACTION = 0.10f
private const val RIM_WIDTH_FRACTION = 0.018f
private const val RIM_ALPHA_TOP = 0.70f
private const val RIM_ALPHA_BOTTOM = 0.30f
private const val BEVEL_TOP_LIGHTEN = 0.90f
private const val BEVEL_MID_STOP = 0.42f
private const val BEVEL_BOTTOM_DEEPEN = 0.15f
private const val GROOVE_INSET_FRACTION = 0.115f
private const val GROOVE_DEEPEN = 0.30f
private const val GROOVE_CORNER_SCALE = 1.4f
private const val FACE_INSET_FRACTION = 0.135f
private const val FACE_CORNER_FRACTION = 0.06f
private const val FACE_TOP_LIGHTEN = 0.45f
private const val FACE_MID_STOP = 0.4f
private const val FACE_BOTTOM_DEEPEN = 0.38f
private const val FACE_RIM_WIDTH_FRACTION = 0.014f
private const val FACE_RIM_ALPHA = 0.50f
private const val FACE_RIM_FADE_FRACTION = 0.6f
private const val SHEEN_HEIGHT_FRACTION = 0.40f
private const val SHEEN_ALPHA = 0.14f
private const val BOTTOM_GLOW_HEIGHT_FRACTION = 0.16f
private const val BOTTOM_GLOW_ALPHA = 0.10f
