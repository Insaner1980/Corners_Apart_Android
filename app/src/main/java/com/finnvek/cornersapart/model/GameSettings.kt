package com.finnvek.cornersapart.model

import kotlinx.serialization.Serializable

@Serializable
data class GameSettings(
    val preferredDifficulty: Int = DEFAULT_DIFFICULTY,
    val soundEnabled: Boolean = true,
    val hapticsEnabled: Boolean = true,
    val preferredMode: GameMode = GameModeConfigs.defaultMode,
) {
    companion object {
        const val DEFAULT_DIFFICULTY = 3
    }
}
