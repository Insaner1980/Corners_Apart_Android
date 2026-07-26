package com.finnvek.cornersapart.model

/** Yksi rivi laitteen kaikkien aikojen listalla: profiili + pelin historiamerkintä. */
data class HallOfFameEntry(
    val profileName: String,
    val profileColorIndex: Int,
    val entry: HistoryEntry,
)

/**
 * Laitteen paikallinen Top 20: kokoaa kaikkien profiilien historiat yhteen
 * listaan pistejärjestyksessä. Tasapisteissä aiempi tulos on ylempänä, joten
 * uusi tulos ei koskaan ohita vanhaa samalla pistemäärällä.
 */
object HallOfFameCalculator {
    const val TOP_LIMIT = 20

    fun topEntries(
        profiles: List<Profile>,
        mode: GameMode? = null,
        limit: Int = TOP_LIMIT,
    ): List<HallOfFameEntry> =
        profiles
            .flatMap { profile ->
                profile.history
                    .filter { entry -> mode == null || entry.gameMode == mode }
                    .map { entry ->
                        HallOfFameEntry(
                            profileName = profile.name,
                            profileColorIndex = profile.colorIndex,
                            entry = entry,
                        )
                    }
            }.sortedWith(
                compareByDescending<HallOfFameEntry> { item -> item.entry.totalScore }
                    .thenBy { item -> item.entry.date },
            ).take(limit)

    /**
     * Uuden tuloksen sijoitus pelimuodon kaikkien aikojen listalla ennen kuin
     * tulos on lisätty historiaan; null jos tulos ei mahdu Top-rajaan.
     */
    fun allTimeRank(
        profiles: List<Profile>,
        mode: GameMode,
        score: Int,
        limit: Int = TOP_LIMIT,
    ): Int? {
        val betterOrEqualCount =
            profiles.sumOf { profile ->
                profile.history.count { entry ->
                    entry.gameMode == mode && entry.totalScore >= score
                }
            }
        val rank = betterOrEqualCount + 1
        return rank.takeIf { it <= limit }
    }
}
