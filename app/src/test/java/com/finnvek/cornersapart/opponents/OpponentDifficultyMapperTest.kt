package com.finnvek.cornersapart.opponents

import org.junit.Assert.assertEquals
import org.junit.Test

class OpponentDifficultyMapperTest {
    @Test
    fun persistedDifficultyLevelsMapToOpponentDifficulties() {
        assertEquals(OpponentDifficulty.BEGINNER, OpponentDifficultyMapper.fromPersistedLevel(1))
        assertEquals(OpponentDifficulty.EASY, OpponentDifficultyMapper.fromPersistedLevel(2))
        assertEquals(OpponentDifficulty.MEDIUM, OpponentDifficultyMapper.fromPersistedLevel(3))
        assertEquals(OpponentDifficulty.HARD, OpponentDifficultyMapper.fromPersistedLevel(4))
        assertEquals(OpponentDifficulty.EXPERT, OpponentDifficultyMapper.fromPersistedLevel(5))
        assertEquals(OpponentDifficulty.MASTER, OpponentDifficultyMapper.fromPersistedLevel(6))
    }

    @Test
    fun unknownPersistedDifficultyLevelsAreClamped() {
        assertEquals(OpponentDifficulty.BEGINNER, OpponentDifficultyMapper.fromPersistedLevel(0))
        assertEquals(OpponentDifficulty.MASTER, OpponentDifficultyMapper.fromPersistedLevel(99))
    }
}
