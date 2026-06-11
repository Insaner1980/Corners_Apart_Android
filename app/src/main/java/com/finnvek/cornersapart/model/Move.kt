package com.finnvek.cornersapart.model

import kotlinx.serialization.Serializable

@Serializable
data class Move(
    val playerIndex: Int,
    val pieceId: String,
    val anchorRow: Int,
    val anchorCol: Int,
    val orientationIndex: Int,
)
