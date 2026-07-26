package com.finnvek.cornersapart.model

import org.junit.Assert.assertEquals
import org.junit.Test

class DailyStreakCalculatorTest {
    @Test
    fun emptyHistoryHasNoStreak() {
        assertEquals(0, DailyStreakCalculator.currentStreak(emptySet(), "2026-07-20"))
    }

    @Test
    fun playingTodayStartsStreakOfOne() {
        assertEquals(1, DailyStreakCalculator.currentStreak(setOf("2026-07-20"), "2026-07-20"))
    }

    @Test
    fun consecutiveDaysCountUpToToday() {
        val dates = setOf("2026-07-18", "2026-07-19", "2026-07-20")
        assertEquals(3, DailyStreakCalculator.currentStreak(dates, "2026-07-20"))
    }

    @Test
    fun streakSurvivesWhenTodayNotYetPlayed() {
        val dates = setOf("2026-07-18", "2026-07-19")
        assertEquals(2, DailyStreakCalculator.currentStreak(dates, "2026-07-20"))
    }

    @Test
    fun gapBreaksStreak() {
        val dates = setOf("2026-07-16", "2026-07-17", "2026-07-20")
        assertEquals(1, DailyStreakCalculator.currentStreak(dates, "2026-07-20"))
    }

    @Test
    fun streakDiesWhenLastPlayWasTwoDaysAgo() {
        assertEquals(0, DailyStreakCalculator.currentStreak(setOf("2026-07-18"), "2026-07-20"))
    }

    @Test
    fun streakCrossesMonthBoundary() {
        val dates = setOf("2026-06-29", "2026-06-30", "2026-07-01")
        assertEquals(3, DailyStreakCalculator.currentStreak(dates, "2026-07-01"))
    }

    @Test
    fun invalidTodayDateIsSafe() {
        assertEquals(0, DailyStreakCalculator.currentStreak(setOf("2026-07-20"), "not-a-date"))
    }

    @Test
    fun invalidStoredDatesAreIgnored() {
        val dates = setOf("garbage", "2026-07-20")
        assertEquals(1, DailyStreakCalculator.currentStreak(dates, "2026-07-20"))
    }
}
