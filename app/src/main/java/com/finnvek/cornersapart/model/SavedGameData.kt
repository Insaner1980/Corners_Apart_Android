package com.finnvek.cornersapart.model

import kotlinx.serialization.Serializable

@Serializable
data class SavedGameData(
    val gameState: GameState? = null,
    val savedAtEpochMillis: Long = 0L,
)
