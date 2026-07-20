package com.finnvek.cornersapart.opponents

import com.finnvek.cornersapart.model.GameConstants
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OpponentRosterTest {
    @Test
    fun rosterHasTwelveUniqueCharacters() {
        assertEquals(12, OpponentRoster.all.size)
        assertEquals(OpponentRoster.all.size, OpponentRoster.all.map { it.id }.distinct().size)
        assertEquals(OpponentRoster.all.size, OpponentRoster.all.map { it.name }.distinct().size)
    }

    @Test
    fun tiersAreNonDecreasingAndCoverAllDifficulties() {
        val tiers = OpponentRoster.all.map { it.tier }
        assertEquals(tiers, tiers.sorted())
        assertEquals(
            (1..GameConstants.DIFFICULTY_LEVELS).toSet(),
            tiers.toSet(),
        )
    }

    @Test
    fun colorIndexesStayInPlayerPaletteRange() {
        OpponentRoster.all.forEach { character ->
            assertTrue(character.colorIndex in 0 until GameConstants.PLAYER_COLORS.size)
        }
    }

    @Test
    fun firstCharacterIsAlwaysUnlocked() {
        assertTrue(OpponentRoster.isUnlocked(OpponentRoster.all.first().id, emptyMap()))
    }

    @Test
    fun laterCharactersStayLockedWithoutWins() {
        OpponentRoster.all.drop(1).forEach { character ->
            assertFalse(OpponentRoster.isUnlocked(character.id, emptyMap()))
        }
    }

    @Test
    fun winningPreviousCharacterUnlocksNext() {
        val first = OpponentRoster.all[0]
        val second = OpponentRoster.all[1]
        val third = OpponentRoster.all[2]
        val wins = mapOf(first.id to 1)
        assertTrue(OpponentRoster.isUnlocked(second.id, wins))
        assertFalse(OpponentRoster.isUnlocked(third.id, wins))
    }

    @Test
    fun unknownCharacterIsNeverUnlocked() {
        assertFalse(OpponentRoster.isUnlocked("nobody", mapOf(OpponentRoster.all.first().id to 5)))
        assertNull(OpponentRoster.forId("nobody"))
    }

    @Test
    fun nextChallengerIsFirstUndefeatedUnlockedCharacter() {
        assertEquals(OpponentRoster.all[0], OpponentRoster.nextChallenger(emptyMap()))
        val wins = mapOf(OpponentRoster.all[0].id to 2, OpponentRoster.all[1].id to 1)
        assertEquals(OpponentRoster.all[2], OpponentRoster.nextChallenger(wins))
    }

    @Test
    fun nextChallengerIsNullWhenAllDefeated() {
        val wins = OpponentRoster.all.associate { character -> character.id to 1 }
        assertNull(OpponentRoster.nextChallenger(wins))
    }

    @Test
    fun firstWinUnlocksTheFollowingCharacter() {
        assertEquals(OpponentRoster.all[1], OpponentRoster.unlockedByFirstWinOf(OpponentRoster.all[0].id))
        assertNull(OpponentRoster.unlockedByFirstWinOf(OpponentRoster.all.last().id))
        assertNull(OpponentRoster.unlockedByFirstWinOf("nobody"))
    }
}
