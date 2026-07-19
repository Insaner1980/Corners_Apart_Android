package com.finnvek.cornersapart.opponents

import com.finnvek.cornersapart.model.GameConstants

object OpponentDifficultyMapper {
    fun fromPersistedLevel(level: Int): OpponentDifficulty =
        when (level.coerceIn(MIN_DIFFICULTY_LEVEL, GameConstants.DIFFICULTY_LEVELS)) {
            1 -> OpponentDifficulty.BEGINNER
            2 -> OpponentDifficulty.EASY
            3 -> OpponentDifficulty.MEDIUM
            4 -> OpponentDifficulty.HARD
            5 -> OpponentDifficulty.EXPERT
            else -> OpponentDifficulty.MASTER
        }

    fun toPersistedLevel(level: Int): Int = level.coerceIn(MIN_DIFFICULTY_LEVEL, GameConstants.DIFFICULTY_LEVELS)

    private const val MIN_DIFFICULTY_LEVEL = 1
}
