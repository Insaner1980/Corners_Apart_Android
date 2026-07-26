package com.finnvek.cornersapart.model

data class ChallengeLevel(
    val number: Int,
    val difficultyLevel: Int,
    val randomSeed: Long,
    val twoStarScore: Int,
    val threeStarScore: Int,
)

/**
 * Solo-haastetasot: kiinteä siemen per taso (sama lauta joka yrityksellä),
 * vaikeus ja tähtirajat kiristyvät tasaisesti. Tähdet: 1 = voitto,
 * 2/3 = voitto ja pisteraja ylittyy.
 */
object ChallengeLevels {
    const val LEVEL_COUNT = 20
    const val MAX_STARS = 3

    val all: List<ChallengeLevel> =
        (1..LEVEL_COUNT).map { number ->
            ChallengeLevel(
                number = number,
                difficultyLevel = 1 + (number - 1) * (GameConstants.DIFFICULTY_LEVELS - 1) / (LEVEL_COUNT - 1),
                randomSeed = SEED_BASE + number * SEED_STEP,
                twoStarScore = TWO_STAR_BASE + number * STAR_SCORE_STEP,
                threeStarScore = THREE_STAR_BASE + number * STAR_SCORE_STEP,
            )
        }

    fun forNumber(number: Int): ChallengeLevel? = all.getOrNull(number - 1)

    /** Ensimmäinen taso on aina auki; seuraava aukeaa edellisen läpäisystä. */
    fun isUnlocked(
        number: Int,
        stars: Map<Int, Int>,
    ): Boolean = number == 1 || (stars[number - 1] ?: 0) > 0

    fun starsFor(
        level: ChallengeLevel,
        rank: Int,
        score: Int,
    ): Int =
        when {
            rank != 1 -> 0
            score >= level.threeStarScore -> 3
            score >= level.twoStarScore -> 2
            else -> 1
        }

    private const val SEED_BASE = 91_000L
    private const val SEED_STEP = 7L
    private const val TWO_STAR_BASE = 55
    private const val THREE_STAR_BASE = 70
    private const val STAR_SCORE_STEP = 1
}
