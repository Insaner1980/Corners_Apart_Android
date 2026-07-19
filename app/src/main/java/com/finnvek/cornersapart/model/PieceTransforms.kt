package com.finnvek.cornersapart.model

import java.util.concurrent.ConcurrentHashMap

object PieceTransforms {
    private val orientationCache: ConcurrentHashMap<String, List<List<CellOffset>>> = ConcurrentHashMap()

    fun rotateCW(cells: List<CellOffset>): List<CellOffset> =
        cells
            .map { cell -> CellOffset(row = cell.col, col = -cell.row) }
            .toSnapshotList()

    fun rotateCCW(cells: List<CellOffset>): List<CellOffset> =
        cells
            .map { cell -> CellOffset(row = -cell.col, col = cell.row) }
            .toSnapshotList()

    fun flipH(cells: List<CellOffset>): List<CellOffset> =
        cells
            .map { cell -> CellOffset(row = cell.row, col = -cell.col) }
            .toSnapshotList()

    fun normalize(cells: List<CellOffset>): List<CellOffset> {
        val minRow = cells.minOf { cell -> cell.row }
        val minCol = cells.minOf { cell -> cell.col }
        return cells
            .map { cell -> CellOffset(row = cell.row - minRow, col = cell.col - minCol) }
            .sortedWith(compareBy<CellOffset> { cell -> cell.row }.thenBy { cell -> cell.col })
            .toSnapshotList()
    }

    fun getAllOrientations(piece: PieceDef): List<List<CellOffset>> =
        orientationCache
            .getOrPut(piece.id) {
                buildOrientations(piece.cells)
            }.map { orientation -> orientation.toSnapshotList() }
            .toSnapshotList()

    fun getOrientation(
        piece: PieceDef,
        orientationIndex: Int,
    ): List<CellOffset>? = getAllOrientations(piece).getOrNull(orientationIndex)

    private fun buildOrientations(cells: List<CellOffset>): List<List<CellOffset>> {
        val orientations = mutableListOf<List<CellOffset>>()
        collectRotations(cells, orientations)
        collectRotations(flipH(cells), orientations)
        return orientations
            .distinct()
            .map { orientation -> orientation.toSnapshotList() }
            .toSnapshotList()
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
