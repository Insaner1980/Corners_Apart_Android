package com.finnvek.cornersapart.data

import com.finnvek.cornersapart.engine.ScoreFixtures
import com.finnvek.cornersapart.model.GameMode
import com.finnvek.cornersapart.model.HistoryEntry
import com.finnvek.cornersapart.model.LocalAvatarStyle
import com.finnvek.cornersapart.model.Profile
import com.finnvek.cornersapart.model.ProfilesData
import com.finnvek.cornersapart.model.Ruleset
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProfileRepositoryTest {
    @Test
    fun profilesKeepExactlyOneActiveProfileAndAppendHistory() =
        runTest {
            val store = InMemoryJsonStateStore(ProfilesData())
            val repository = ProfileRepository(store)
            val firstProfile =
                Profile(
                    id = "first",
                    name = "Emma",
                    colorIndex = 0,
                    avatarStyle = LocalAvatarStyle.INITIALS,
                    avatarSeed = "emma",
                    active = true,
                )
            val secondProfile =
                Profile(
                    id = "second",
                    name = "Kai",
                    colorIndex = 2,
                    avatarStyle = LocalAvatarStyle.MOSAIC,
                    avatarSeed = "kai",
                )

            repository.upsertProfile(firstProfile)
            repository.upsertProfile(secondProfile)
            repository.setActiveProfile("second")
            repository.appendHistory("second", historyEntry(totalScore = 42, rank = 1))

            val profiles = repository.profiles.first()
            assertEquals("second", repository.activeProfile.first()?.id)
            assertFalse(profiles.single { profile -> profile.id == "first" }.active)
            assertTrue(profiles.single { profile -> profile.id == "second" }.active)
            assertEquals(1, profiles.single { profile -> profile.id == "second" }.history.size)
        }

    @Test
    fun appendHistoryKeepsMostRecentMaxHistoryEntries() =
        runTest {
            val store =
                InMemoryJsonStateStore(
                    ProfilesData(
                        profiles =
                            listOf(
                                Profile(
                                    id = "active",
                                    name = "Player",
                                    active = true,
                                ),
                            ),
                    ),
                )
            val repository = ProfileRepository(store)

            repeat(com.finnvek.cornersapart.model.GameConstants.MAX_HISTORY_ENTRIES + 5) { index ->
                repository.appendHistory("active", historyEntry(totalScore = index, rank = 1))
            }

            val history =
                repository.profiles
                    .first()
                    .single()
                    .history
            assertEquals(com.finnvek.cornersapart.model.GameConstants.MAX_HISTORY_ENTRIES, history.size)
            assertEquals(5, history.first().totalScore)
            assertEquals(54, history.last().totalScore)
        }

    private fun historyEntry(
        totalScore: Int,
        rank: Int,
    ): HistoryEntry =
        HistoryEntry(
            date = "2026-06-11",
            rank = rank,
            totalScore = totalScore,
            scoreBreakdown = ScoreFixtures.breakdown(totalCells = totalScore, bonusPoints = 0),
            claimedBonusTiles = 0,
            piecesPlaced = totalScore,
            difficulty = 2,
            ruleset = Ruleset.STANDARD,
            gameMode = GameMode.FOUR_PLAYER,
            timeSeconds = 120,
            scores = emptyList(),
        )
}
