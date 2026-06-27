package com.finnvek.cornersapart.ui.screens

import com.finnvek.cornersapart.engine.MoveRejectionReason
import com.finnvek.cornersapart.viewmodel.GameEffect
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GamePolishPolicyTest {
    @Test
    fun layoutPolicySwitchesToExpandedAtTabletWidth() {
        assertEquals(GameLayoutMode.COMPACT, GameLayoutPolicy.modeForWidthDp(599))
        assertEquals(GameLayoutMode.COMPACT, GameLayoutPolicy.modeForWidthDp(839))
        assertEquals(GameLayoutMode.EXPANDED, GameLayoutPolicy.modeForWidthDp(840))
    }

    @Test
    fun soundPolicyMapsEnabledHumanEventsOnly() {
        assertEquals(
            GameSoundEvent.PLACEMENT,
            GameSoundPolicy.eventFor(
                effect = GameEffect.MoveAccepted("Indigo", scoreDelta = 1, bonusTileClaimed = false),
                soundEnabled = true,
            ),
        )
        assertEquals(
            GameSoundEvent.BONUS_CLAIM,
            GameSoundPolicy.eventFor(
                effect = GameEffect.MoveAccepted("Indigo", scoreDelta = 4, bonusTileClaimed = true),
                soundEnabled = true,
            ),
        )
        assertEquals(GameSoundEvent.GAME_OVER, GameSoundPolicy.eventFor(GameEffect.GameOver, soundEnabled = true))
        assertNull(
            GameSoundPolicy.eventFor(
                effect = GameEffect.MoveRejected(MoveRejectionReason.CELL_OCCUPIED),
                soundEnabled = true,
            ),
        )
        assertNull(
            GameSoundPolicy.eventFor(
                effect = GameEffect.MoveAccepted("Indigo", scoreDelta = 1, bonusTileClaimed = false),
                soundEnabled = false,
            ),
        )
    }
}
