package com.finnvek.cornersapart.viewmodel

import com.finnvek.cornersapart.engine.MoveRejectionReason

sealed interface GameEffect {
    data class MoveRejected(
        val reason: MoveRejectionReason,
    ) : GameEffect

    data class MoveAccepted(
        val playerName: String,
        val scoreDelta: Int,
        val bonusTileClaimed: Boolean = false,
    ) : GameEffect

    data object GameOver : GameEffect
}
