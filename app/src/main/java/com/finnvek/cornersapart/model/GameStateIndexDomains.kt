package com.finnvek.cornersapart.model

const val INVALID_GAME_STATE_INDEX_DOMAINS = "Game state index domains are invalid."

fun GameState.hasValidIndexDomains(): Boolean {
    if (players.isEmpty()) return false
    if (players.map { player -> player.index } != players.indices.toList()) return false
    if (currentPlayerIndex !in players.indices) return false
    if (players.any { player -> player.ownerIndex !in players.indices || player.colorIndex < 0 }) return false
    if (board.cells.any { cell -> cell != BoardSnapshot.EMPTY && cell !in players.indices }) return false
    val hasInvalidBonusClaim =
        bonusTiles.any { tile ->
            tile.claimedByPlayerIndex?.let { playerIndex -> playerIndex !in players.indices } == true
        }
    if (hasInvalidBonusClaim) {
        return false
    }
    if (moveHistory.any { move -> move.playerIndex !in players.indices }) return false
    return true
}
