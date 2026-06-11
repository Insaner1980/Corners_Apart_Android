package com.finnvek.cornersapart.model

import kotlinx.serialization.Serializable

@Serializable
data class GameSettings(
    val preferredDifficulty: Int = DEFAULT_DIFFICULTY,
    val soundEnabled: Boolean = true,
    val hapticsEnabled: Boolean = true,
    val reducedMotionEnabled: Boolean = false,
    val preferredMode: GameMode = GameMode.FOUR_PLAYER,
    val preferredRuleset: Ruleset = Ruleset.STANDARD,
) {
    companion object {
        const val DEFAULT_DIFFICULTY = 2
    }
}
