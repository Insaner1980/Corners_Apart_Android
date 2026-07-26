package com.finnvek.cornersapart.model

import kotlinx.serialization.Serializable

@Serializable
data class CellPosition(
    val row: Int,
    val col: Int,
)

@Serializable
data class CellOffset(
    val row: Int,
    val col: Int,
)
