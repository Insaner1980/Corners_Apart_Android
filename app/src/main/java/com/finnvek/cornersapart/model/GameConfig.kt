package com.finnvek.cornersapart.model

import kotlinx.serialization.Serializable

@Serializable
data class GameConfig(
    val mode: GameMode = GameModeConfigs.defaultMode,
    val ruleset: Ruleset = Ruleset.STANDARD,
    val boardSize: Int = GameModeConfigs.defaultBoardSizeFor(mode),
    val randomSeed: Long = 0L,
    val bonusTiles: List<BonusTile>? = null,
    val bonusTileCount: Int? = null,
)
