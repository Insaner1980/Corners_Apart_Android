package com.finnvek.cornersapart.ui.theme

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Test

class CornersApartTokensTest {
    @Test
    fun playerPaletteMatchesReviewedSpecification() {
        assertEquals(Color(0xFF4338CA), CornersApartColors.PlayerIndigo)
        assertEquals(Color(0xFFE88C0A), CornersApartColors.PlayerAmber)
        assertEquals(Color(0xFFE8513D), CornersApartColors.PlayerCoral)
        assertEquals(Color(0xFF0D9488), CornersApartColors.PlayerTeal)
    }

    @Test
    fun surfacePaletteMatchesReviewedSpecification() {
        assertEquals(Color(0xFF312B63), CornersApartColors.BackgroundGradientTop)
        assertEquals(Color(0xFF1D1940), CornersApartColors.BackgroundGradientBottom)
        assertEquals(Color(0xFF241F4E), CornersApartColors.BoardPanel)
        assertEquals(Color(0xFF1B173D), CornersApartColors.BoardCellEmpty)
        assertEquals(Color(0xFFD8A928), CornersApartColors.BonusAccent)
        assertEquals(Color(0xFFFFC53D), CornersApartColors.BonusAccentBright)
    }
}
