package com.finnvek.cornersapart.model

fun targetCells(
    anchorRow: Int,
    anchorCol: Int,
    offsets: List<CellOffset>,
): List<CellPosition> =
    offsets.map { offset ->
        CellPosition(
            row = anchorRow + offset.row,
            col = anchorCol + offset.col,
        )
    }
