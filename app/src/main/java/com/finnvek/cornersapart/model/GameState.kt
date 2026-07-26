package com.finnvek.cornersapart.model

import kotlinx.serialization.Serializable

@Serializable
data class GameState(
    val board: BoardSnapshot,
    val players: List<Player>,
    val currentPlayerIndex: Int,
    val turnNumber: Int,
    val ruleset: Ruleset,
    val gameMode: GameMode,
    val randomSeed: Long,
    val bonusTiles: List<BonusTile>,
    val bonusLayoutId: String?,
    val moveHistory: List<Move>,
    val isGameOver: Boolean,
)
