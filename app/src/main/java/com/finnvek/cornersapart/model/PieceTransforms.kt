package com.finnvek.cornersapart.model

import java.util.concurrent.ConcurrentHashMap

object PieceTransforms {
    const val MAX_ORIENTATIONS = 8

    private val orientationCache: ConcurrentHashMap<String, List<List<CellOffset>>> = ConcurrentHashMap()

    fun rotateCW(cells: List<CellOffset>): List<CellOffset> =
        cells.map { cell -> CellOffset(row = cell.col, col = -cell.row) }

    fun rotateCCW(cells: List<CellOffset>): List<CellOffset> =
        cells.map { cell -> CellOffset(row = -cell.col, col = cell.row) }

    fun flipH(cells: List<CellOffset>): List<CellOffset> =
        cells.map { cell -> CellOffset(row = cell.row, col = -cell.col) }

    fun normalize(cells: List<CellOffset>): List<CellOffset> {
        val minRow = cells.minOf { cell -> cell.row }
        val minCol = cells.minOf { cell -> cell.col }
        return cells
            .map { cell -> CellOffset(row = cell.row - minRow, col = cell.col - minCol) }
            .sortedWith(compareBy<CellOffset> { cell -> cell.row }.thenBy { cell -> cell.col })
    }

    fun getAllOrientations(piece: PieceDef): List<List<CellOffset>> =
        orientationCache.getOrPut(piece.id) {
            buildOrientations(piece.cells)
        }

    fun getOrientation(
        piece: PieceDef,
        orientationIndex: Int,
    ): List<CellOffset>? = getAllOrientations(piece).getOrNull(orientationIndex)

    private fun buildOrientations(cells: List<CellOffset>): List<List<CellOffset>> {
        val orientations = mutableListOf<List<CellOffset>>()
        collectRotations(cells, orientations)
        collectRotations(flipH(cells), orientations)
        return orientations.distinct()
    }

    private fun collectRotations(
        cells: List<CellOffset>,
        orientations: MutableList<List<CellOffset>>,
    ) {
        var rotated = cells
        repeat(4) {
            orientations += normalize(rotated)
            rotated = rotateCW(rotated)
        }
    }
}
