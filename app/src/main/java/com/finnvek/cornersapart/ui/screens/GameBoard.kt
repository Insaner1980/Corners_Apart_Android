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
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.finnvek.cornersapart.R
import com.finnvek.cornersapart.model.BoardSnapshot
import com.finnvek.cornersapart.model.CellOffset
import com.finnvek.cornersapart.model.CellPosition
import com.finnvek.cornersapart.model.targetCells
import com.finnvek.cornersapart.ui.components.BoardVisualPlayer
import com.finnvek.cornersapart.ui.components.candyBoardPanel
import com.finnvek.cornersapart.ui.components.drawBoardBase
import com.finnvek.cornersapart.ui.components.drawCandyCell
import com.finnvek.cornersapart.ui.components.drawCellPerimeter
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
    onCanvasPositionChange: (LayoutCoordinates) -> Unit = {},
    isPlacementLegal: (row: Int, col: Int) -> Boolean = { _, _ -> true },
) {
    val boardDescription = stringResource(R.string.game_board_content_description)
    val gapPx = with(LocalDensity.current) { CornersApartSpacing.BoardCellGap.toPx() }
    var pressAnchor by remember(state.board.size, state.selectedCells) { mutableStateOf<CellPosition?>(null) }
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
    val activeAnchor = pressAnchor ?: externalPreviewAnchor
    val previewCells =
        remember(activeAnchor, state.selectedCells) {
            activeAnchor
                ?.let { anchor ->
                    targetCells(
                        anchorRow = anchor.row,
                        anchorCol = anchor.col,
                        offsets = state.selectedCells,
                    )
                }.orEmpty()
        }
    val previewIsValid =
        remember(activeAnchor, state.board, state.selectedCells) {
            activeAnchor?.let { anchor -> isPlacementLegal(anchor.row, anchor.col) } ?: true
        }
    val visualPlayers =
        remember(state.players) {
            state.players.map { player ->
                BoardVisualPlayer(
                    index = player.index,
                    colorIndex = player.colorIndex,
                    startCorner = CellPosition(player.startRow, player.startCol),
                )
            }
        }
    Box(
        modifier = modifier.candyBoardPanel(),
    ) {
        Canvas(
            modifier =
                Modifier
                    .matchParentSize()
                    .onGloballyPositioned(onCanvasPositionChange)
                    .semantics { contentDescription = boardDescription }
                    .pointerInput(state.board.size, state.selectedCells) {
                        // Paina näyttääksesi esikatselun sormen yläpuolella, raahaa
                        // siirtääksesi sitä, vapauta laudan päällä asettaaksesi palan.
                        awaitEachGesture {
                            val down = awaitFirstDown()
                            var lastPosition = down.position
                            var cancelled = false
                            pressAnchor =
                                liftedBoardAnchor(
                                    position = lastPosition,
                                    boardSize = state.board.size,
                                    boardWidthPx = size.width.toFloat(),
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
                                pressAnchor =
                                    liftedBoardAnchor(
                                        position = lastPosition,
                                        boardSize = state.board.size,
                                        boardWidthPx = size.width.toFloat(),
                                        offsets = state.selectedCells,
                                    )
                                change.consume()
                            }
                            pressAnchor = null
                            val insideBoard =
                                lastPosition.x in 0f..size.width.toFloat() &&
                                    lastPosition.y in 0f..size.height.toFloat()
                            if (!cancelled && insideBoard) {
                                liftedBoardAnchor(
                                    position = lastPosition,
                                    boardSize = state.board.size,
                                    boardWidthPx = size.width.toFloat(),
                                    offsets = state.selectedCells,
                                )?.let { anchor -> onPlaceCell(anchor.row, anchor.col) }
                            }
                        }
                    },
        ) {
            val boardSize = state.board.size
            val cellSize = (size.minDimension - gapPx * (boardSize - 1)) / boardSize
            val pitch = cellSize + gapPx
            drawBoardBase(
                board = state.board,
                bonusTiles = state.bonusTiles,
                players = visualPlayers,
                cellSize = cellSize,
                pitch = pitch,
                bonusAlpha = bonusAlpha,
                popCells = popCells,
                popScale = placementPop.value,
            )
            drawPlacementPreview(state, previewCells, previewIsValid, cellSize, pitch)
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

/**
 * Laskee palan ankkurisolun niin, että pala kelluu sormen yläpuolella
 * ([PREVIEW_LIFT_CELLS] solua) ja on vaakasuunnassa keskitetty sormeen.
 * Ankkuri rajataan niin, että koko pala pysyy laudalla — pala liukuu
 * laidoilla reunaa pitkin sen sijaan että osa jäisi laudan ulkopuolelle.
 */
internal fun liftedBoardAnchor(
    position: Offset,
    boardSize: Int,
    boardWidthPx: Float,
    offsets: List<CellOffset>,
): CellPosition? {
    if (boardSize <= 0 || boardWidthPx <= 0f) return null
    val cellPitch = boardWidthPx / boardSize
    val fingerRow = floor(position.y / cellPitch).toInt()
    val fingerCol = floor(position.x / cellPitch).toInt()
    val pieceRowSpan = offsets.maxOfOrNull { offset -> offset.row } ?: 0
    val pieceColSpan = offsets.maxOfOrNull { offset -> offset.col } ?: 0
    return CellPosition(
        row =
            (fingerRow - pieceRowSpan - PREVIEW_LIFT_CELLS)
                .coerceIn(0, (boardSize - 1 - pieceRowSpan).coerceAtLeast(0)),
        col = (fingerCol - pieceColSpan / 2).coerceIn(0, (boardSize - 1 - pieceColSpan).coerceAtLeast(0)),
    )
}

/**
 * Piirtää sijoituksen esikatselun: himmentää muun laudan, piirtää palan rivien
 * ja sarakkeiden tähtäysviivat laudan reunoihin, palan oikeina candy-laattoina
 * (laillinen = pelaajan väri, laiton = punainen) ja paksun vaalean ääriviivan
 * palan ulkoreunoille, jotta pala erottuu pienistäkin ruuduista.
 */
private fun DrawScope.drawPlacementPreview(
    state: GameUiState,
    previewCells: List<CellPosition>,
    isValid: Boolean,
    cellSize: Float,
    pitch: Float,
) {
    val visibleCells = previewCells.filter { position -> state.board.contains(position) }
    if (visibleCells.isEmpty()) return
    drawRect(color = CornersApartColors.TextShadow.copy(alpha = CornersApartAlpha.PreviewDim))
    drawAlignmentGuides(visibleCells, cellSize, pitch)
    val colors =
        if (isValid) {
            CornersApartPlayerPalette.colorsFor(state.currentPlayer.colorIndex)
        } else {
            CornersApartPlayerPalette.invalidPreview
        }
    visibleCells.forEach { position ->
        drawCandyCell(
            topLeft = Offset(position.col * pitch, position.row * pitch),
            cellSize = cellSize,
            colors = colors,
            alpha = CornersApartAlpha.PreviewCell,
        )
    }
    drawCellPerimeter(
        cells = visibleCells,
        cellSize = cellSize,
        pitch = pitch,
        color = CornersApartColors.TextOnDarkPrimary.copy(alpha = CornersApartAlpha.PreviewOutline),
    )
}

/** Vaaleat tähtäysviivat: palan rivi- ja sarakekaistat koko laudan yli. */
private fun DrawScope.drawAlignmentGuides(
    cells: List<CellPosition>,
    cellSize: Float,
    pitch: Float,
) {
    val bandColor = CornersApartColors.TextOnDarkPrimary.copy(alpha = CornersApartAlpha.GuideBand)
    val lineColor = CornersApartColors.TextOnDarkPrimary.copy(alpha = CornersApartAlpha.GuideLine)
    val lineWidth = cellSize * GUIDE_LINE_WIDTH_FRACTION
    val top = cells.minOf { cell -> cell.row } * pitch
    val bottom = cells.maxOf { cell -> cell.row } * pitch + cellSize
    val left = cells.minOf { cell -> cell.col } * pitch
    val right = cells.maxOf { cell -> cell.col } * pitch + cellSize
    drawRect(color = bandColor, topLeft = Offset(0f, top), size = Size(size.width, bottom - top))
    drawRect(color = bandColor, topLeft = Offset(left, 0f), size = Size(right - left, size.height))
    drawLine(lineColor, Offset(0f, top), Offset(size.width, top), lineWidth)
    drawLine(lineColor, Offset(0f, bottom), Offset(size.width, bottom), lineWidth)
    drawLine(lineColor, Offset(left, 0f), Offset(left, size.height), lineWidth)
    drawLine(lineColor, Offset(right, 0f), Offset(right, size.height), lineWidth)
}

/** Montako solua esikatselu kelluu sormen yläpuolella. */
internal const val PREVIEW_LIFT_CELLS = 2

private const val PLACEMENT_POP_START_SCALE = 0.55f
private const val BONUS_PULSE_MIN_ALPHA = 0.72f
private const val BONUS_PULSE_DURATION_MS = 1200
private const val GUIDE_LINE_WIDTH_FRACTION = 0.05f
