package com.finnvek.cornersapart.model

import kotlinx.serialization.Serializable

@Serializable
data class HistoryEntry(
    val date: String,
    val rank: Int,
    val totalScore: Int,
    val scoreBreakdown: ScoreBreakdown,
    val claimedBonusTiles: Int,
    val piecesPlaced: Int,
    val difficulty: Int,
    val ruleset: Ruleset,
    val gameMode: GameMode,
    val timeSeconds: Int,
    val scores: List<PlayerScore>,
)

@Serializable
data class PlayerScore(
    val name: String,
    val totalScore: Int,
    val scoreBreakdown: ScoreBreakdown,
    val claimedBonusTiles: Int,
    val colorIndex: Int,
    val ownerIndex: Int = 0,
)
