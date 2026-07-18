package com.finnvek.cornersapart.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.finnvek.cornersapart.R
import com.finnvek.cornersapart.model.BoardSnapshot
import com.finnvek.cornersapart.model.CellPosition
import com.finnvek.cornersapart.model.targetCells
import com.finnvek.cornersapart.ui.components.drawCandyCell
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
    externalPreviewAnchor: CellPosition? = null,
    onCanvasPositioned: (LayoutCoordinates) -> Unit = {},
) {
    val boardDescription = stringResource(R.string.game_board_content_description)
    val gapPx = with(LocalDensity.current) { CornersApartSpacing.BoardCellGap.toPx() }
    var previewCells by remember(state.board.size, state.selectedCells) { mutableStateOf(emptyList<CellPosition>()) }
    val placementPop = remember { Animatable(1f) }
    var popCells by remember { mutableStateOf(emptySet<CellPosition>()) }
    var previousBoard by remember { mutableStateOf(state.board) }
    LaunchedEffect(state.board) {
        val added = state.board.newlyOccupiedCellsSince(previousBoard)
        previousBoard = state.board
        if (added.isNotEmpty()) {
            popCells = added
            placementPop.snapTo(PLACEMENT_POP_START_SCALE)
            placementPop.animateTo(
                targetValue = 1f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
            )
            popCells = emptySet()
        }
    }
    val bonusPulse = rememberInfiniteTransition(label = "bonusPulse")
    val bonusAlpha by
        bonusPulse.animateFloat(
            initialValue = BONUS_PULSE_MIN_ALPHA,
            targetValue = 1f,
            animationSpec =
                infiniteRepeatable(
                    animation = tween(durationMillis = BONUS_PULSE_DURATION_MS),
                    repeatMode = RepeatMode.Reverse,
                ),
            label = "bonusAlpha",
        )
    val externalPreviewCells =
        remember(externalPreviewAnchor, state.selectedCells) {
            externalPreviewAnchor?.let { anchor ->
                targetCells(
                    anchorRow = anchor.row,
                    anchorCol = anchor.col,
                    offsets = state.selectedCells,
                )
            }.orEmpty()
        }
    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(CornersApartSpacing.BoardPanelRadius))
                .background(CornersApartColors.BoardPanel)
                .border(
                    width = CornersApartSpacing.BoardPanelBorderWidth,
                    color = CornersApartColors.PanelSurfaceRaised,
                    shape = RoundedCornerShape(CornersApartSpacing.BoardPanelRadius),
                ).padding(CornersApartSpacing.BoardPanelPadding),
    ) {
        Canvas(
            modifier =
                Modifier
                    .matchParentSize()
                    .onGloballyPositioned(onCanvasPositioned)
                    .semantics { contentDescription = boardDescription }
                    .pointerInput(state.board.size, state.selectedCells) {
                        // Paina näyttääksesi esikatselun, raahaa siirtääksesi sitä,
                        // vapauta laudan päällä asettaaksesi palan.
                        awaitEachGesture {
                            val down = awaitFirstDown()
                            var lastPosition = down.position
                            var cancelled = false
                            val downAnchor = lastPosition.toBoardCell(state.board.size, size.width)
                            previewCells =
                                targetCells(
                                    anchorRow = downAnchor.row,
                                    anchorCol = downAnchor.col,
                                    offsets = state.selectedCells,
                                )
                            while (true) {
                                val event = awaitPointerEvent()
                                val change = event.changes.firstOrNull { it.id == down.id }
                                if (change == null || change.isConsumed) {
                                    cancelled = true
                                    break
                                }
                                lastPosition = change.position
                                if (!change.pressed) break
                                val anchor = lastPosition.toBoardCell(state.board.size, size.width)
                                previewCells =
                                    targetCells(
                                        anchorRow = anchor.row,
                                        anchorCol = anchor.col,
                                        offsets = state.selectedCells,
                                    )
                                change.consume()
                            }
                            previewCells = emptyList()
                            val insideBoard =
                                lastPosition.x in 0f..size.width.toFloat() &&
                                    lastPosition.y in 0f..size.height.toFloat()
                            if (!cancelled && insideBoard) {
                                val anchor = lastPosition.toBoardCell(state.board.size, size.width)
                                onPlaceCell(anchor.row, anchor.col)
                            }
                        }
                    },
        ) {
            val boardSize = state.board.size
            val cellSize = (size.minDimension - gapPx * (boardSize - 1)) / boardSize
            val pitch = cellSize + gapPx
            drawEmptyCells(state.board, cellSize, pitch)
            drawBonusTiles(state, cellSize, pitch, bonusAlpha)
            drawStartMarkers(state, cellSize, pitch)
            drawOccupiedCells(state, cellSize, pitch, popCells, placementPop.value)
            drawPlacementPreview(state, previewCells.ifEmpty { externalPreviewCells }, cellSize, pitch)
        }
    }
}

/** Solut, jotka ovat täyttyneet edelliseen tilannekuvaan verrattuna. */
private fun BoardSnapshot.newlyOccupiedCellsSince(previous: BoardSnapshot): Set<CellPosition> {
    if (previous.size != size) return emptySet()
    return buildSet {
        for (row in 0 until size) {
            for (col in 0 until size) {
                if (get(row, col) != BoardSnapshot.EMPTY && previous.get(row, col) == BoardSnapshot.EMPTY) {
                    add(CellPosition(row, col))
                }
            }
        }
    }
}

internal fun Offset.toBoardCell(
    boardSize: Int,
    boardWidth: Int,
): CellPosition {
    val cellPitch = boardWidth.toFloat() / boardSize
    return CellPosition(
        row = floor(y / cellPitch).toInt().coerceIn(0, boardSize - 1),
        col = floor(x / cellPitch).toInt().coerceIn(0, boardSize - 1),
    )
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
    state: GameUiState,
    cellSize: Float,
    pitch: Float,
    pulseAlpha: Float,
) {
    state.bonusTiles
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
    state: GameUiState,
    cellSize: Float,
    pitch: Float,
) {
    state.players.forEach { player ->
        val position = CellPosition(player.startRow, player.startCol)
        if (state.board.get(position) == BoardSnapshot.EMPTY) {
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
    state: GameUiState,
    cellSize: Float,
    pitch: Float,
    popCells: Set<CellPosition>,
    popScale: Float,
) {
    for (row in 0 until state.board.size) {
        for (col in 0 until state.board.size) {
            val playerIndex = state.board.get(row, col)
            if (playerIndex != BoardSnapshot.EMPTY) {
                drawCandyCell(
                    topLeft = Offset(col * pitch, row * pitch),
                    cellSize = cellSize,
                    colors = CornersApartPlayerPalette.colorsFor(playerIndex),
                    scale = if (CellPosition(row, col) in popCells) popScale else 1f,
                )
            }
        }
    }
}

private fun DrawScope.drawPlacementPreview(
    state: GameUiState,
    previewCells: List<CellPosition>,
    cellSize: Float,
    pitch: Float,
) {
    val colors = CornersApartPlayerPalette.colorsFor(state.currentPlayer.colorIndex)
    val corner = CornerRadius(cellSize * PREVIEW_CORNER_FRACTION)
    previewCells
        .filter { position -> state.board.contains(position) }
        .forEach { position ->
            val topLeft = Offset(position.col * pitch, position.row * pitch)
            drawRoundRect(
                color = colors.ghost,
                topLeft = topLeft,
                size = Size(cellSize, cellSize),
                cornerRadius = corner,
            )
            drawRoundRect(
                color = colors.highlight.copy(alpha = CornersApartAlpha.GhostOutline),
                topLeft = topLeft,
                size = Size(cellSize, cellSize),
                cornerRadius = corner,
                style = Stroke(width = cellSize * PREVIEW_OUTLINE_STROKE_FRACTION),
            )
        }
}

private const val PLACEMENT_POP_START_SCALE = 0.55f
private const val BONUS_PULSE_MIN_ALPHA = 0.72f
private const val BONUS_PULSE_DURATION_MS = 1200
private const val BONUS_MARKER_RADIUS_FRACTION = 0.24f
private const val BONUS_GLOW_RADIUS_FRACTION = 0.42f
private const val BONUS_INNER_DIAMOND_FRACTION = 0.55f
private const val START_MARKER_RADIUS_FRACTION = 0.18f
private const val START_MARKER_GLOW_RADIUS_FRACTION = 0.40f
private const val EMPTY_CELL_CORNER_FRACTION = 0.12f
private const val PREVIEW_CORNER_FRACTION = 0.18f
private const val PREVIEW_OUTLINE_STROKE_FRACTION = 0.06f
