package com.finnvek.cornersapart.model

object GameConstants {
    const val STANDARD_BOARD_SIZE = 20
    const val COMPACT_BOARD_SIZE = 14
    const val PLAYER_COUNT = 4
    const val PIECE_COUNT = 21
    const val TOTAL_PIECE_CELLS = 89

    const val STANDARD_BONUS_TILE_COUNT = 10
    const val COMPACT_BONUS_TILE_COUNT = 6
    const val BONUS_TILE_POINTS = 3
    const val PLACED_CELL_POINTS = 1
    const val COMPLETION_BONUS_POINTS = 10

    const val MAX_HISTORY_ENTRIES = 50
    const val DIFFICULTY_LEVELS = 5

    const val BOARD_INTERACTION_LOCK_MS = 160L
    const val INVALID_FEEDBACK_COOLDOWN_MS = 180L
    const val OPPONENT_TURN_DELAY_MIN_MS = 300L
    const val OPPONENT_TURN_DELAY_RANGE_MS = 400L
    const val TURN_ADVANCE_DELAY_MS = 400L
    const val HUMAN_AUTO_PASS_DELAY_MS = 1_500L
    const val SAVE_NOTIFICATION_DURATION_MS = 2_000L
    const val RECONNECT_TIMEOUT_MS = 60_000L
    const val BACKGROUND_TIMEOUT_MS = 300_000L

    const val MAX_AVATAR_DIMENSION = 160
    const val MAX_AVATAR_FILE_SIZE = 5 * 1024 * 1024

    val STANDARD_CORNERS = listOf(0 to 0, 0 to 19, 19 to 19, 19 to 0).toSnapshotList()
    val COMPACT_DUEL_CORNERS = listOf(0 to 0, 13 to 13).toSnapshotList()

    val PLAYER_NAMES = listOf("Indigo", "Amber", "Coral", "Teal").toSnapshotList()
    val PLAYER_COLORS = listOf("Indigo", "Amber", "Coral", "Teal").toSnapshotList()
}
