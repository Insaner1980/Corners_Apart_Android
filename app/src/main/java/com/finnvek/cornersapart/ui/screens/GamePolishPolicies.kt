package com.finnvek.cornersapart.ui.screens

import com.finnvek.cornersapart.viewmodel.GameEffect

enum class GameLayoutMode {
    COMPACT,
    EXPANDED,
}

object GameLayoutPolicy {
    fun modeForWidthDp(widthDp: Int): GameLayoutMode =
        if (widthDp >= EXPANDED_WIDTH_DP) {
            GameLayoutMode.EXPANDED
        } else {
            GameLayoutMode.COMPACT
        }

    private const val EXPANDED_WIDTH_DP = 840
}

enum class GameSoundEvent {
    PLACEMENT,
    BONUS_CLAIM,
    GAME_OVER,
}

object GameSoundPolicy {
    fun eventFor(
        effect: GameEffect,
        soundEnabled: Boolean,
    ): GameSoundEvent? {
        if (!soundEnabled) return null
        return when (effect) {
            is GameEffect.MoveAccepted ->
                if (effect.bonusTileClaimed) {
                    GameSoundEvent.BONUS_CLAIM
                } else {
                    GameSoundEvent.PLACEMENT
                }
            GameEffect.GameOver -> GameSoundEvent.GAME_OVER
            is GameEffect.MoveRejected -> null
            is GameEffect.ActionFailed -> null
        }
    }
}
