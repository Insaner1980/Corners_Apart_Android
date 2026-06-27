package com.finnvek.cornersapart.opponents

enum class OpponentDifficulty(
    internal val temperature: Double,
    internal val candidateSoftCap: Int,
    internal val largePieceBias: Double,
    internal val bonusTileAwareness: Double,
    internal val blockingAwareness: Double,
) {
    BEGINNER(
        temperature = 3.0,
        candidateSoftCap = 10,
        largePieceBias = -0.4,
        bonusTileAwareness = 0.2,
        blockingAwareness = 0.0,
    ),
    EASY(
        temperature = 2.0,
        candidateSoftCap = 25,
        largePieceBias = -0.15,
        bonusTileAwareness = 0.6,
        blockingAwareness = 0.3,
    ),
    MEDIUM(
        temperature = 1.0,
        candidateSoftCap = 80,
        largePieceBias = 0.0,
        bonusTileAwareness = 1.0,
        blockingAwareness = 0.8,
    ),
    HARD(
        temperature = 0.5,
        candidateSoftCap = 200,
        largePieceBias = 0.25,
        bonusTileAwareness = 1.4,
        blockingAwareness = 1.3,
    ),
    EXPERT(
        temperature = 0.2,
        candidateSoftCap = 500,
        largePieceBias = 0.45,
        bonusTileAwareness = 1.8,
        blockingAwareness = 1.7,
    ),
}
