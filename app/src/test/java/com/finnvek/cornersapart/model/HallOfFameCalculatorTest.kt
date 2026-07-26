package com.finnvek.cornersapart.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class HallOfFameCalculatorTest {
    private fun entry(
        score: Int,
        date: String = "2026-07-01",
        mode: GameMode = GameMode.SOLO,
    ): HistoryEntry =
        HistoryEntry(
            date = date,
            rank = 1,
            totalScore = score,
            scoreBreakdown = ScoreBreakdown(placedCellPoints = score, bonusTilePoints = 0, completionBonus = 0),
            claimedBonusTiles = 0,
            piecesPlaced = 0,
            difficulty = 3,
            ruleset = Ruleset.STANDARD,
            gameMode = mode,
            timeSeconds = 60,
            scores = emptyList(),
        )

    private fun profile(
        name: String,
        vararg entries: HistoryEntry,
    ): Profile =
        Profile(
            id = name.lowercase(),
            name = name,
            history = entries.toList(),
        )

    @Test
    fun entriesAreSortedByScoreAcrossProfiles() {
        val profiles =
            listOf(
                profile("Emma", entry(50), entry(90)),
                profile("Sam", entry(70)),
            )
        val top = HallOfFameCalculator.topEntries(profiles)
        assertEquals(listOf(90, 70, 50), top.map { item -> item.entry.totalScore })
        assertEquals(listOf("Emma", "Sam", "Emma"), top.map { item -> item.profileName })
    }

    @Test
    fun tieGoesToEarlierDate() {
        val profiles =
            listOf(
                profile("Emma", entry(70, date = "2026-07-02")),
                profile("Sam", entry(70, date = "2026-07-01")),
            )
        val top = HallOfFameCalculator.topEntries(profiles)
        assertEquals(listOf("Sam", "Emma"), top.map { item -> item.profileName })
    }

    @Test
    fun modeFilterKeepsOnlyMatchingEntries() {
        val profiles =
            listOf(
                profile(
                    "Emma",
                    entry(90, mode = GameMode.SOLO),
                    entry(40, mode = GameMode.COMPACT_DUEL),
                ),
            )
        val top = HallOfFameCalculator.topEntries(profiles, mode = GameMode.COMPACT_DUEL)
        assertEquals(listOf(40), top.map { item -> item.entry.totalScore })
    }

    @Test
    fun listIsLimitedToTopLimit() {
        val entries = (1..30).map { score -> entry(score) }.toTypedArray()
        val top = HallOfFameCalculator.topEntries(listOf(profile("Emma", *entries)))
        assertEquals(HallOfFameCalculator.TOP_LIMIT, top.size)
        assertEquals(30, top.first().entry.totalScore)
    }

    @Test
    fun newHighScoreRanksFirst() {
        val profiles = listOf(profile("Emma", entry(50), entry(70)))
        assertEquals(1, HallOfFameCalculator.allTimeRank(profiles, GameMode.SOLO, score = 80))
    }

    @Test
    fun equalScoreRanksAfterExistingEntries() {
        val profiles = listOf(profile("Emma", entry(70)))
        assertEquals(2, HallOfFameCalculator.allTimeRank(profiles, GameMode.SOLO, score = 70))
    }

    @Test
    fun rankOnlyCountsSameMode() {
        val profiles = listOf(profile("Emma", entry(90, mode = GameMode.FOUR_PLAYER)))
        assertEquals(1, HallOfFameCalculator.allTimeRank(profiles, GameMode.SOLO, score = 10))
    }

    @Test
    fun scoreOutsideTopLimitHasNoRank() {
        val entries = (100..119).map { score -> entry(score) }.toTypedArray()
        val profiles = listOf(profile("Emma", *entries))
        assertNull(HallOfFameCalculator.allTimeRank(profiles, GameMode.SOLO, score = 10))
    }
}
