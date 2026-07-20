package com.finnvek.cornersapart.model

import java.time.LocalDate

/**
 * Päivähaasteputki: montako peräkkäistä päivää päivähaaste on pelattu.
 * Putki lasketaan pelattujen ISO-päivämäärien joukosta ja se on voimassa,
 * jos viimeisin pelipäivä on tänään tai eilen (tämän päivän haastetta ei
 * ole vielä ehtinyt menettää).
 */
object DailyStreakCalculator {
    fun currentStreak(
        playedDates: Set<String>,
        todayIsoDate: String,
    ): Int {
        val today = todayIsoDate.toLocalDateOrNull() ?: return 0
        val anchor =
            when {
                today.toString() in playedDates -> today
                today.minusDays(1).toString() in playedDates -> today.minusDays(1)
                else -> return 0
            }
        var streak = 0
        var cursor = anchor
        while (cursor.toString() in playedDates) {
            streak++
            cursor = cursor.minusDays(1)
        }
        return streak
    }

    private fun String.toLocalDateOrNull(): LocalDate? = runCatching { LocalDate.parse(this) }.getOrNull()
}
