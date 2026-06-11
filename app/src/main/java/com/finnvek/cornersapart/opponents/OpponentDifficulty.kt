package com.finnvek.cornersapart.opponents

import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

enum class OpponentDifficulty(
    val temperature: Double,
    val candidateSoftCap: Int,
    val timeBudget: Duration,
    val largePieceBias: Double,
    val bonusTileAwareness: Double,
    val blockingAwareness: Double,
) {
    BEGINNER(
        temperature = 3.0,
        candidateSoftCap = 10,
        timeBudget = 250.milliseconds,
        largePieceBias = -0.4,
        bonusTileAwareness = 0.2,
        blockingAwareness = 0.0,
    ),
    EASY(
        temperature = 2.0,
        candidateSoftCap = 25,
        timeBudget = 400.milliseconds,
        largePieceBias = -0.15,
        bonusTileAwareness = 0.6,
        blockingAwareness = 0.3,
    ),
    MEDIUM(
        temperature = 1.0,
        candidateSoftCap = 80,
        timeBudget = 700.milliseconds,
        largePieceBias = 0.0,
        bonusTileAwareness = 1.0,
        blockingAwareness = 0.8,
    ),
    HARD(
        temperature = 0.5,
        candidateSoftCap = 200,
        timeBudget = 1_200.milliseconds,
        largePieceBias = 0.25,
        bonusTileAwareness = 1.4,
        blockingAwareness = 1.3,
    ),
    EXPERT(
        temperature = 0.2,
        candidateSoftCap = 500,
        timeBudget = 1_800.milliseconds,
        largePieceBias = 0.45,
        bonusTileAwareness = 1.8,
        blockingAwareness = 1.7,
    ),
}
