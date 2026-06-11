package com.finnvek.cornersapart.model

import kotlinx.serialization.Serializable

@Serializable
data class BonusTileLayout(
    val id: String,
    val boardSize: Int,
    val positions: List<CellPosition>,
)
