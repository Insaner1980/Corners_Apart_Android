package com.finnvek.cornersapart.model

import kotlinx.serialization.Serializable

@Serializable
enum class GameMode {
    SOLO,
    TWO_COLOR_DUEL,
    COMPACT_DUEL,
    THREE_PLAYER,
    FOUR_PLAYER,
}
