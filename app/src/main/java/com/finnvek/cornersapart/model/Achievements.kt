package com.finnvek.cornersapart.model

enum class Achievement(
    val id: String,
) {
    FIRST_WIN("first_win"),
    BONUS_HUNTER("bonus_hunter"),
    ALL_PIECES("all_pieces"),
    EXPERT_WIN("expert_win"),
    WIN_STREAK_3("win_streak_3"),
    PERFECT_LEVEL("perfect_level"),
    CHALLENGE_CHAMP("challenge_champ"),
    ;

    companion object {
        fun fromId(id: String): Achievement? = entries.firstOrNull { achievement -> achievement.id == id }
    }
}

/** Arvioi pelin päättyessä ansaitut saavutukset (kaikkien aikojen ehdot). */
object AchievementEvaluator {
    fun earnedAfterGame(
        entry: HistoryEntry,
        previousHistory: List<HistoryEntry>,
        challengeStars: Map<Int, Int>,
    ): Set<Achievement> =
        buildSet {
            val won = entry.rank == 1
            if (won) add(Achievement.FIRST_WIN)
            if (entry.claimedBonusTiles >= BONUS_HUNTER_TILES) add(Achievement.BONUS_HUNTER)
            if (entry.piecesPlaced >= PieceCatalog.all.size) add(Achievement.ALL_PIECES)
            if (won && entry.difficulty >= EXPERT_DIFFICULTY_LEVEL) add(Achievement.EXPERT_WIN)
            val previousWins =
                previousHistory.takeLast(WIN_STREAK_LENGTH - 1).let { recent ->
                    recent.size == WIN_STREAK_LENGTH - 1 && recent.all { game -> game.rank == 1 }
                }
            if (won && previousWins) add(Achievement.WIN_STREAK_3)
            if (challengeStars.values.any { stars -> stars >= ChallengeLevels.MAX_STARS }) {
                add(Achievement.PERFECT_LEVEL)
            }
            if (challengeStars.count { (_, stars) -> stars > 0 } >= CHALLENGE_CHAMP_LEVELS) {
                add(Achievement.CHALLENGE_CHAMP)
            }
        }

    private const val BONUS_HUNTER_TILES = 3
    private const val EXPERT_DIFFICULTY_LEVEL = 5
    private const val WIN_STREAK_LENGTH = 3
    private const val CHALLENGE_CHAMP_LEVELS = 10
}
