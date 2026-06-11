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
        assertEquals(Color(0xFFE4E4E8), CornersApartColors.AppBackground)
        assertEquals(Color(0xFF2C2C30), CornersApartColors.BoardFrame)
        assertEquals(Color(0xFFD8A928), CornersApartColors.BonusAccent)
    }
}
