package com.finnvek.cornersapart.ui.screens

import com.finnvek.cornersapart.ui.theme.CornersApartAnimationTokens
import org.junit.Assert.assertEquals
import org.junit.Test

class GamePolishPolicyTest {
    @Test
    fun layoutPolicySwitchesToExpandedAtTabletWidth() {
        assertEquals(GameLayoutMode.COMPACT, GameLayoutPolicy.modeForWidthDp(599))
        assertEquals(GameLayoutMode.COMPACT, GameLayoutPolicy.modeForWidthDp(839))
        assertEquals(GameLayoutMode.EXPANDED, GameLayoutPolicy.modeForWidthDp(840))
    }

    @Test
    fun motionPolicyDisablesDurationsWhenReducedMotionIsEnabled() {
        assertEquals(
            CornersApartAnimationTokens.PIECE_PLACEMENT_MS,
            MotionPolicy.durationMillis(
                defaultMillis = CornersApartAnimationTokens.PIECE_PLACEMENT_MS,
                reducedMotionEnabled = false,
            ),
        )
        assertEquals(
            0,
            MotionPolicy.durationMillis(
                defaultMillis = CornersApartAnimationTokens.PIECE_PLACEMENT_MS,
                reducedMotionEnabled = true,
            ),
        )
    }
}
