package com.finnvek.cornersapart.ui.theme

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Test

class CornersApartTokensTest {
    @Test
    fun playerPaletteMatchesReviewedSpecification() {
        assertEquals(Color(0xFFF0509E), CornersApartColors.PlayerPink)
        assertEquals(Color(0xFFFFA726), CornersApartColors.PlayerMango)
        assertEquals(Color(0xFF29C8E0), CornersApartColors.PlayerCyan)
        assertEquals(Color(0xFF9BD934), CornersApartColors.PlayerLime)
    }

    @Test
    fun surfacePaletteMatchesReviewedSpecification() {
        assertEquals(Color(0xFF3A3378), CornersApartColors.BackgroundGradientTop)
        assertEquals(Color(0xFF1D1940), CornersApartColors.BackgroundGradientBottom)
        assertEquals(Color(0xFF241F4E), CornersApartColors.BoardPanel)
        assertEquals(Color(0xFF1B173D), CornersApartColors.BoardCellEmpty)
        assertEquals(Color(0xFFD8A928), CornersApartColors.BonusAccent)
        assertEquals(Color(0xFFFFC53D), CornersApartColors.BonusAccentBright)
    }

    @Test
    fun expandedLayoutBreakpointIsCentralized() {
        assertEquals(840, CornersApartBreakpoints.EXPANDED_WIDTH_DP)
    }
}
