package com.finnvek.cornersapart.model

object GameConstants {
    const val STANDARD_BOARD_SIZE = 20
    const val COMPACT_BOARD_SIZE = 14

    const val STANDARD_BONUS_TILE_COUNT = 10
    const val COMPACT_BONUS_TILE_COUNT = 6
    const val BONUS_TILE_POINTS = 3
    const val PLACED_CELL_POINTS = 1
    const val COMPLETION_BONUS_POINTS = 10

    const val MAX_HISTORY_ENTRIES = 50
    const val DIFFICULTY_LEVELS = 5
    const val OPPONENT_TURN_DELAY_MIN_MS = 300L
    const val OPPONENT_TURN_DELAY_RANGE_MS = 400L

    val PLAYER_NAMES = listOf("Pink", "Mango", "Cyan", "Lime").toSnapshotList()
    val PLAYER_COLORS = listOf("Pink", "Mango", "Cyan", "Lime").toSnapshotList()
}
