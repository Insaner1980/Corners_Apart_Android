package com.finnvek.cornersapart.ui.screens

import com.finnvek.cornersapart.ui.theme.CornersApartBreakpoints
import com.finnvek.cornersapart.viewmodel.GameEffect

enum class GameLayoutMode {
    COMPACT,
    EXPANDED,
}

object GameLayoutPolicy {
    fun modeForWidthDp(widthDp: Int): GameLayoutMode =
        if (widthDp >= CornersApartBreakpoints.EXPANDED_WIDTH_DP) {
            GameLayoutMode.EXPANDED
        } else {
            GameLayoutMode.COMPACT
        }
}

enum class GameSoundEvent {
    PLACEMENT,
    BONUS_CLAIM,
    GAME_OVER,
    REJECT,
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
            is GameEffect.MoveRejected -> GameSoundEvent.REJECT
            is GameEffect.ActionFailed -> GameSoundEvent.REJECT
        }
    }
}
